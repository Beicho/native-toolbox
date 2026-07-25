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

// 手动缓存:命中数据缓 180 天,数据缺失(404)只缓 10 分钟 —— 数据仓库随时会补上,负结果不能长期粘住
$cacheKey = "today:$m-$d";
$hit = cache_get($cacheKey);
$data = $hit !== null ? json_decode($hit, true) : null;
if ($data === null || !($data['found'] ?? false)) {
    $path = str_replace(['%m', '%d'], [$m, $d], $pathTpl);
    $url = "https://api.github.com/repos/$repo/contents/$path";
    [$code, $j] = http_json($url, ['headers' => ['Accept: application/vnd.github+json'], 'timeout' => 12]);
    $fresh = null;
    if ($code === 404) {
        $fresh = ['found' => false];
    } elseif ($j !== null && ($j['encoding'] ?? '') === 'base64') {
        $raw = base64_decode($j['content'] ?? '', true);
        if ($raw) {
            $ev = json_decode($raw, true);
            if (is_array($ev)) $fresh = ['found' => true, 'events' => $ev];
        }
    }
    if ($fresh !== null) {
        cache_put($cacheKey, json_encode($fresh, JSON_UNESCAPED_UNICODE), ($fresh['found'] ? 86400 * 180 : 600));
        $data = $fresh;
    } elseif ($data === null) {
        // 上游失败且无任何缓存
        json_err('历史上的今天数据源暂不可用', 502);
    }
    // 上游失败但有旧缓存(即使是负缓存)→ 继续用旧值
}

if ($data === null) json_err('历史上的今天数据源暂不可用', 502);
json_out($data, 86400 * 7);
