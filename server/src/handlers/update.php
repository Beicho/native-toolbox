<?php
// GET /v1/update — 检查更新:GitHub Releases 最新版反代
// 输出附 proxyUrl(走 /v1/dl 中转下载,国内直连 GitHub 资产不稳)
declare(strict_types=1);

$repo = cfg('github_repo');

$data = cached('update:' . $repo, 600, function () use ($repo) {
    [$code, $j] = http_json("https://api.github.com/repos/$repo/releases/latest", [
        'headers' => ['Accept: application/vnd.github+json'],
    ]);
    if ($j === null) return null;
    $assets = [];
    foreach ($j['assets'] ?? [] as $a) {
        $assets[] = [
            'name'     => $a['name'],
            'size'     => $a['size'],
            'url'      => $a['browser_download_url'],
            'proxyUrl' => '/v1/dl?tag=' . rawurlencode($j['tag_name']) . '&name=' . rawurlencode($a['name']),
        ];
    }
    return [
        'tag'         => $j['tag_name'] ?? '',
        'name'        => $j['name'] ?? '',
        'notes'       => $j['body'] ?? '',
        'publishedAt' => $j['published_at'] ?? '',
        'assets'      => $assets,
    ];
});

if ($data === null) json_err('上游暂不可用', 502);
json_out($data, 600);
