<?php
// GET /v1/holiday?year=2026 — 中国节假日(GitHub NateScarlet/holiday-cn 通过 Contents API 中转)
declare(strict_types=1);

$year = (int)(q('year', (string)date('Y')) ?? date('Y'));
if ($year < 2020 || $year > 2030) json_err('年份不合法');

$data = cached("holiday:$year", 86400, function () use ($year) {
    $url = 'https://api.github.com/repos/NateScarlet/holiday-cn/contents/' . $year . '.json';
    [$code, $j] = http_json($url, ['headers' => ['Accept: application/vnd.github+json'], 'timeout' => 10]);
    if ($code === 404) return ['found' => false];
    if ($j === null || ($j['encoding'] ?? '') !== 'base64') return null;
    $raw = base64_decode($j['content'] ?? '', true);
    if (!$raw) return null;
    $cal = json_decode($raw, true);
    return is_array($cal) ? ['found' => true, 'data' => $cal] : null;
});

if ($data === null) json_err('节假日数据源暂不可用', 502);
json_out($data, 86400);
