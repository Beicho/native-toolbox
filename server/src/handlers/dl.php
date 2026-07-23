<?php
// GET /v1/dl?tag=&name= — Release 资产中转下载(服务器磁盘缓存,同 tag 命中直出)
declare(strict_types=1);

$tag  = q('tag');
$name = q('name');
if (!$tag || !$name || !preg_match('/^[\w.+-]+$/', $tag) || !preg_match('/^[\w.+-]+$/', $name)) {
    json_err('参数不合法');
}
if (!rate_hit('dl:' . client_ip(), 10, 300)) json_err('下载太频繁', 429);

$cacheDir = ASTRO_DATA . '/dl';
if (!is_dir($cacheDir)) @mkdir($cacheDir, 0775, true);
$local = "$cacheDir/$tag-$name";

if (!is_file($local)) {
    $repo = cfg('github_repo');
    [, $j] = http_json('https://api.github.com/repos/' . $repo . '/releases/tags/' . rawurlencode($tag), [
        'headers' => ['Accept: application/vnd.github+json'],
    ]);
    if ($j === null) json_err('版本不存在或上游不可用', 502);
    $target = null;
    foreach ($j['assets'] ?? [] as $a) {
        if ($a['name'] === $name) { $target = $a; break; }
    }
    if (!$target) json_err('文件不存在', 404);
    if ((int)$target['size'] > 200 * 1024 * 1024) json_err('文件过大', 413);

    // 下载到临时文件再原子改名,避免半成品被命中
    $tmp = $local . '.part.' . getmypid();
    $fp = fopen($tmp, 'wb');
    if (!$fp) json_err('服务器存储异常', 500);
    $ch = curl_init($target['browser_download_url']);
    curl_setopt_array($ch, [
        CURLOPT_FILE           => $fp,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_MAXREDIRS      => 5,
        CURLOPT_TIMEOUT        => 300,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_USERAGENT      => 'AstroKit-API/1.0',
    ]);
    $okDl = curl_exec($ch);
    $code = (int)curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    curl_close($ch);
    fclose($fp);
    if (!$okDl || $code !== 200 || filesize($tmp) !== (int)$target['size']) {
        @unlink($tmp);
        json_err('从上游拉取失败,请稍后重试', 502);
    }
    rename($tmp, $local);
    // 只保留最近 3 个缓存文件
    $files = glob("$cacheDir/*") ?: [];
    usort($files, fn($a, $b) => filemtime($b) <=> filemtime($a));
    foreach (array_slice($files, 3) as $old) @unlink($old);
}

header('Content-Type: application/octet-stream');
header('Content-Disposition: attachment; filename="' . $name . '"');
header('Content-Length: ' . filesize($local));
header('Cache-Control: public, max-age=86400');
readfile($local);
exit;
