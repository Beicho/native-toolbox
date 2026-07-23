<?php
// astro-api 前端控制器:路由分发,业务全在 src/handlers/
declare(strict_types=1);

require dirname(__DIR__) . '/src/bootstrap.php';

$path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';

if ($path === '/healthz') {
    header('Content-Type: text/plain');
    echo 'ok';
    exit;
}

// 轻量客户端标识:挡掉裸扫描器,不是强鉴权
if (!isset($_SERVER['HTTP_X_ASTRO_CLIENT'])) {
    json_err('forbidden', 403);
}

// 全局限频:每 IP 每分钟
if (!rate_hit('ip:' . client_ip(), 150)) {
    json_err('请求太频繁,稍后再试', 429);
}

$routes = [
    'update', 'dl', 'exchange', 'weather', 'ip', 'dns', 'whois', 'ssl',
    'sitecheck', 'today', 'holiday', 'hitokoto', 'translate', 'mail',
];

if (preg_match('#^/v1/([a-z]+)(?:/([a-z0-9_]+))?$#', $path, $m) && in_array($m[1], $routes, true)) {
    $GLOBALS['sub'] = $m[2] ?? null;
    require ASTRO_ROOT . '/src/handlers/' . $m[1] . '.php';
    json_err('handler 未产生输出', 500);
}

json_err('not found', 404);
