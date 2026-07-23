<?php
// GET /v1/today?m=07&d=23 — 历史上的今天(agent 调研的 GitHub 数据仓库)
declare(strict_types=1);

$m = str_pad(q('m', (string)date('m')) ?? date('m'), 2, '0', STR_PAD_LEFT);
$d = str_pad(q('d', (string)date('d')) ?? date('d'), 2, '0', STR_PAD_LEFT);
if (!preg_match('/^\d{2}$/', $m) || !preg_match('/^\d{2}$/', $d)) json_err('日期不合法');
if ((int)$m < 1 || (int)$m > 12 || (int)$d < 1 || (int)$d > 31) json_err('日期不合法');

$repo = (string)cfg('today_repo');
$pathTpl = (string)cfg('today_path');
if ($repo === '' || $pathTpl === '') json_err('历史上的今天服务未配置', 501);

$data = cached("today:$m-$d", 86400 * 180, function () use ($repo, $pathTpl, $m, $d) {
    $path = str_replace(['%m', '%d'], [$m, $d], $pathTpl);
    $url = "https://api.github.com/repos/$repo/contents/$path";
    [$code, $j] = http_json($url, ['headers' => ['Accept: application/vnd.github+json'], 'timeout' => 12]);
    if ($code === 404) return ['found' => false];
    if ($j === null || ($j['encoding'] ?? '') !== 'base64') return null;
    $raw = base64_decode($j['content'] ?? '', true);
    if (!$raw) return null;
    $ev = json_decode($raw, true);
    return is_array($ev) ? ['found' => true, 'events' => $ev] : null;
});

if ($data === null) json_err('历史上的今天数据源暂不可用', 502);
json_out($data, 86400 * 7);
