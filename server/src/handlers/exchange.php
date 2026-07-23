<?php
// GET /v1/exchange?base=USD — 汇率(open.er-api.com 免费日更),缓存 1 小时
declare(strict_types=1);

$base = strtoupper(q('base', 'USD') ?? 'USD');
if (!preg_match('/^[A-Z]{3}$/', $base)) json_err('币种代码不合法');

$data = cached("exchange:$base", 3600, function () use ($base) {
    [, $j] = http_json("https://open.er-api.com/v6/latest/$base");
    if ($j === null || ($j['result'] ?? '') !== 'success') return null;
    return [
        'base'      => $j['base_code'],
        'updatedAt' => $j['time_last_update_unix'] ?? 0,
        'rates'     => $j['rates'] ?? [],
    ];
});

if ($data === null) json_err('汇率源暂不可用', 502);
json_out($data, 1800);
