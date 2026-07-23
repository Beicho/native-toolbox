<?php
// GET /v1/hitokoto?c=a — 一言(随机句子,默认动漫)
declare(strict_types=1);

$c = q('c', 'a') ?? 'a';
if (!preg_match('/^[a-z]$/', $c)) json_err('分类不合法');

[$code, $j] = http_json("https://v1.hitokoto.cn/?encode=json&c=$c", ['timeout' => 5]);
if ($j === null) json_err('一言源暂不可用', 502);

json_out([
    'text'   => $j['hitokoto'] ?? '',
    'from'   => $j['from'] ?? '',
    'author' => $j['from_who'] ?? '',
    'type'   => $j['type'] ?? '',
], 1800);
