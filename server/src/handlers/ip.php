<?php
// GET /v1/ip?q=  — IP/域名归属地(ip-api.com,免费版 HTTP、45次/分),缺 q 查客户端出口 IP
declare(strict_types=1);

$target = q('q', '') ?: client_ip();
if ($target !== client_ip() && !preg_match('/^[a-zA-Z0-9.\-:]+$/', $target)) {
    json_err('查询目标不合法');
}
if (filter_var($target, FILTER_VALIDATE_IP) && is_private_ip($target)) {
    json_err('内网/保留地址没有公网归属地');
}

$data = cached('ip:' . $target, 86400, function () use ($target) {
    $fields = 'status,message,query,country,regionName,city,zip,lat,lon,timezone,isp,org,as,reverse,mobile,proxy,hosting';
    $url = 'http://ip-api.com/json/' . rawurlencode($target) . '?lang=zh-CN&fields=' . $fields;
    [, $j] = http_json($url);
    if ($j === null || ($j['status'] ?? '') !== 'success') return null;
    return $j;
});

if ($data === null) json_err('归属地源暂不可用或目标无效', 502);
json_out($data, 3600);
