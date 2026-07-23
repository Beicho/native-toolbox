<?php
// GET /v1/dns?name=&type=A — DNS over HTTPS 查询(阿里 DoH,国内延迟最低)
declare(strict_types=1);

$name = strtolower(q('name', '') ?? '');
$type = strtoupper(q('type', 'A') ?? 'A');
$allowed = ['A', 'AAAA', 'CNAME', 'MX', 'TXT', 'NS', 'SOA', 'CAA', 'SRV', 'PTR'];
if (!preg_match('/^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$/', $name)) json_err('域名不合法');
if (!in_array($type, $allowed, true)) json_err('不支持的记录类型');

[$code, $j] = http_json('https://dns.alidns.com/resolve?' . http_build_query(['name' => $name, 'type' => $type]), ['timeout' => 6]);
if ($j === null) json_err('DNS 服务暂不可用', 502);

json_out([
    'name'   => $name,
    'type'   => $type,
    'status' => $j['Status'] ?? -1,
    'answer' => $j['Answer'] ?? [],
]);
