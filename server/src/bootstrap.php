<?php
// astro-api 引导:配置 / SQLite 缓存与限频 / HTTP 客户端 / SSRF 防护 / 统一输出
declare(strict_types=1);

define('ASTRO_ROOT', dirname(__DIR__));
define('ASTRO_DATA', ASTRO_ROOT . '/data');

$GLOBALS['cfg'] = file_exists(ASTRO_ROOT . '/config.local.php')
    ? require ASTRO_ROOT . '/config.local.php'
    : require ASTRO_ROOT . '/config.example.php';

function cfg(string $k, mixed $d = null): mixed
{
    return $GLOBALS['cfg'][$k] ?? $d;
}

// ---------- 存储 ----------

function db(): PDO
{
    static $pdo = null;
    if ($pdo === null) {
        if (!is_dir(ASTRO_DATA)) @mkdir(ASTRO_DATA, 0775, true);
        $pdo = new PDO('sqlite:' . ASTRO_DATA . '/astro.db');
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $pdo->exec('PRAGMA journal_mode=WAL');
        $pdo->exec('PRAGMA busy_timeout=3000');
        $pdo->exec('CREATE TABLE IF NOT EXISTS cache(k TEXT PRIMARY KEY, v BLOB NOT NULL, exp INTEGER NOT NULL)');
        $pdo->exec('CREATE TABLE IF NOT EXISTS rate(k TEXT PRIMARY KEY, n INTEGER NOT NULL, exp INTEGER NOT NULL)');
    }
    return $pdo;
}

function cache_get(string $k): ?string
{
    $st = db()->prepare('SELECT v, exp FROM cache WHERE k = ?');
    $st->execute([$k]);
    $row = $st->fetch(PDO::FETCH_ASSOC);
    if (!$row) return null;
    if ((int)$row['exp'] < time()) return null;
    return (string)$row['v'];
}

function cache_put(string $k, string $v, int $ttl): void
{
    $st = db()->prepare('INSERT INTO cache(k, v, exp) VALUES(?, ?, ?)
        ON CONFLICT(k) DO UPDATE SET v = excluded.v, exp = excluded.exp');
    $st->execute([$k, $v, time() + $ttl]);
    if (random_int(0, 200) === 0) db()->exec('DELETE FROM cache WHERE exp < ' . (time() - 86400));
}

/** 命中缓存直接返回,否则执行 $fn(必须返回可 json 序列化数据)并写缓存 */
function cached(string $k, int $ttl, callable $fn): mixed
{
    $hit = cache_get($k);
    if ($hit !== null) return json_decode($hit, true);
    $val = $fn();
    cache_put($k, json_encode($val, JSON_UNESCAPED_UNICODE), $ttl);
    return $val;
}

/** 窗口计数限频:窗口内不超过 $limit 次返回 true */
function rate_hit(string $bucket, int $limit, int $win = 60): bool
{
    $now = time();
    $k = $bucket . '|' . intdiv($now, $win);
    $pdo = db();
    $pdo->prepare('INSERT INTO rate(k, n, exp) VALUES(?, 1, ?)
        ON CONFLICT(k) DO UPDATE SET n = n + 1')->execute([$k, $now + $win * 2]);
    $st = $pdo->prepare('SELECT n FROM rate WHERE k = ?');
    $st->execute([$k]);
    if (random_int(0, 500) === 0) $pdo->exec('DELETE FROM rate WHERE exp < ' . $now);
    return (int)$st->fetchColumn() <= $limit;
}

// ---------- HTTP 客户端 ----------

/**
 * 发起上游请求。opt: method/headers[]/body/timeout/follow/retry/max_bytes
 * 返回 [int $status, string $body, array $respHeaders]; 网络失败 $status = 0
 */
function http_req(string $url, array $opt = []): array
{
    $tries = 1 + (int)($opt['retry'] ?? 1);
    $status = 0; $body = ''; $hdrs = [];
    for ($i = 0; $i < $tries; $i++) {
        $ch = curl_init($url);
        $respHeaders = [];
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CUSTOMREQUEST  => strtoupper($opt['method'] ?? 'GET'),
            CURLOPT_TIMEOUT        => (int)($opt['timeout'] ?? 8),
            CURLOPT_CONNECTTIMEOUT => 5,
            CURLOPT_FOLLOWLOCATION => (bool)($opt['follow'] ?? true),
            CURLOPT_MAXREDIRS      => 5,
            CURLOPT_ENCODING       => '',
            CURLOPT_USERAGENT      => 'AstroKit-API/1.0 (+github.com/Beicho/native-toolbox)',
            CURLOPT_HTTPHEADER     => $opt['headers'] ?? [],
            CURLOPT_HEADERFUNCTION => function ($ch, $line) use (&$respHeaders) {
                $p = strpos($line, ':');
                if ($p !== false) $respHeaders[strtolower(trim(substr($line, 0, $p)))] = trim(substr($line, $p + 1));
                return strlen($line);
            },
        ]);
        if (isset($opt['body'])) curl_setopt($ch, CURLOPT_POSTFIELDS, $opt['body']);
        $body = (string)curl_exec($ch);
        $status = (int)curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        curl_close($ch);
        $hdrs = $respHeaders;
        if ($status > 0 && $status < 500) break;
        usleep(300_000);
    }
    $max = (int)($opt['max_bytes'] ?? 4_000_000);
    if (strlen($body) > $max) $body = substr($body, 0, $max);
    return [$status, $body, $hdrs];
}

function http_json(string $url, array $opt = []): array
{
    [$code, $body] = http_req($url, $opt);
    if ($code < 200 || $code >= 300) return [$code, null];
    $j = json_decode($body, true);
    return [$code, is_array($j) ? $j : null];
}

// ---------- SSRF 防护(探测类接口的目标校验) ----------

function is_private_ip(string $ip): bool
{
    return filter_var($ip, FILTER_VALIDATE_IP,
        FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE) === false;
}

/** 校验探测目标主机;返回 null 表示允许,否则返回拒绝原因 */
function guard_target(string $host): ?string
{
    $host = strtolower(trim($host, ". \t"));
    if ($host === '' || strlen($host) > 253) return '目标不合法';
    if (filter_var($host, FILTER_VALIDATE_IP)) {
        return is_private_ip($host) ? '不允许探测内网地址' : null;
    }
    if (!preg_match('/^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$/', $host)) {
        return '域名格式不合法';
    }
    $ips = @gethostbynamel($host) ?: [];
    $v6 = @dns_get_record($host, DNS_AAAA) ?: [];
    foreach ($v6 as $r) if (!empty($r['ipv6'])) $ips[] = $r['ipv6'];
    if (!$ips) return '域名无法解析';
    foreach ($ips as $ip) if (is_private_ip($ip)) return '不允许探测内网地址';
    return null;
}

// ---------- 请求上下文与输出 ----------

function client_ip(): string
{
    return $_SERVER['HTTP_X_REAL_IP'] ?? $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
}

function q(string $k, ?string $d = null): ?string
{
    $v = $_GET[$k] ?? $d;
    return is_string($v) ? trim($v) : $d;
}

function body_json(): array
{
    $raw = file_get_contents('php://input') ?: '';
    if (strlen($raw) > 512 * 1024) json_err('请求体过大', 413);
    $j = json_decode($raw, true);
    return is_array($j) ? $j : [];
}

function json_out(mixed $data, int $cacheSec = 0): never
{
    http_response_code(200);
    header('Content-Type: application/json; charset=utf-8');
    header($cacheSec > 0 ? "Cache-Control: public, max-age=$cacheSec" : 'Cache-Control: no-store');
    echo json_encode(['ok' => true, 'data' => $data], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function json_err(string $msg, int $http = 400): never
{
    http_response_code($http);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    echo json_encode(['ok' => false, 'err' => $msg], JSON_UNESCAPED_UNICODE);
    exit;
}
