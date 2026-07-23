<?php
// GET /v1/sitecheck?url= — 网站体检:DNS→TCP→TLS→HTTP 分段耗时+跳转链+安全响应头评分
declare(strict_types=1);

$url = q('url', '');
if (!$url || !filter_var($url, FILTER_VALIDATE_URL)) json_err('URL 不合法');
$host = parse_url($url, PHP_URL_HOST);
if (!$host || ($why = guard_target($host)) !== null) json_err($why ?? '目标不合法');
if (!rate_hit('sitecheck:' . client_ip(), 5)) json_err('探测太频繁,稍后再试', 429);

$p = parse_url($url);
$scheme = $p['scheme'] ?? 'http';
$port = $p['port'] ?? ($scheme === 'https' ? 443 : 80);

// DNS
$t0 = microtime(true); $ips = @gethostbynamel($host) ?: []; $dnsMs = (int)round((microtime(true) - $t0) * 1000);
if (!$ips) json_err('DNS 解析失败', 502);

// TCP + 可选 TLS
$t1 = microtime(true);
$ctx = stream_context_create(['ssl' => [
    'verify_peer' => false, 'verify_peer_name' => false, 'SNI_enabled' => true, 'peer_name' => $host,
]]);
$fp = @stream_socket_client(($scheme === 'https' ? 'ssl://' : '') . "$host:$port", $errno, $errstr, 8, STREAM_CLIENT_CONNECT, $ctx);
$connectMs = (int)round((microtime(true) - $t1) * 1000);
if (!$fp) json_err("目标连接失败(可能网络不可达): $errstr", 502);
fclose($fp);

// HTTP
$t2 = microtime(true); [$code, $body, $hdrs] = http_req($url, ['method' => 'GET', 'timeout' => 15, 'follow' => true, 'max_bytes' => 512*1024]);
$httpMs = (int)round((microtime(true) - $t2) * 1000);
if ($code === 0) json_err('HTTP 请求失败', 502);

// 响应头安全评分
$sec = ['hsts' => 0, 'xfo' => 0, 'csp' => 0, 'xcto' => 0, 'referrer' => 0];
if (isset($hdrs['strict-transport-security'])) $sec['hsts'] = 20;
if (isset($hdrs['x-frame-options'])) $sec['xfo'] = 20;
if (isset($hdrs['content-security-policy'])) $sec['csp'] = 30;
if (isset($hdrs['x-content-type-options'])) $sec['xcto'] = 15;
if (isset($hdrs['referrer-policy'])) $sec['referrer'] = 15;
$score = array_sum($sec);

json_out([
    'url'        => $url,
    'ip'         => $ips[0] ?? '',
    'statusCode' => $code,
    'timing'     => ['dns' => $dnsMs, 'connect' => $connectMs, 'http' => $httpMs, 'total' => $dnsMs + $connectMs + $httpMs],
    'security'   => ['score' => $score, 'detail' => $sec],
], 600);
