<?php
// POST /v1/translate — 翻译与词典(上游 OpenAI 兼容网关,Key 只在服务端)
// body: {"mode":"text","text":"...","to":"en"} 或 {"mode":"word","text":"serendipity"}
declare(strict_types=1);

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') json_err('需要 POST', 405);
$key = (string)cfg('translate_key');
if ($key === '') json_err('翻译服务未配置', 501);
if (!rate_hit('translate:' . client_ip(), 20)) json_err('翻译太频繁,稍后再试', 429);

$in   = body_json();
$mode = $in['mode'] ?? 'text';
$text = trim((string)($in['text'] ?? ''));
if ($text === '' || mb_strlen($text) > 5000) json_err('文本为空或超过 5000 字');

$cacheKey = 'tr:' . $mode . ':' . ($in['to'] ?? '') . ':' . sha1($text);
$hit = cache_get($cacheKey);
if ($hit !== null) json_out(json_decode($hit, true), 3600);

if ($mode === 'word') {
    $system = '你是权威词典。对用户给出的词条,只输出一个严格 JSON 对象,不要任何其他文字:'
        . '{"word":"原词","phonetic":"音标(若适用)","pos":[{"tag":"词性","defs":["中文释义"]}],'
        . '"examples":[{"src":"例句","dst":"例句翻译"}],"tip":"一句记忆提示"}。例句给 2 条。';
    $user = $text;
} else {
    $to = trim((string)($in['to'] ?? ''));
    if ($to === '' || $to === 'auto') {
        $to = preg_match('/\p{Han}/u', $text) ? '英文' : '简体中文';
    }
    $system = "你是精准翻译引擎。把用户给出的内容翻译成{$to}。只输出译文本身,不要任何解释、前缀或引号。";
    $user = $text;
}

[$code, $j] = http_json(rtrim((string)cfg('translate_base'), '/') . '/chat/completions', [
    'method'  => 'POST',
    'timeout' => 25,
    'headers' => ['Authorization: Bearer ' . $key, 'Content-Type: application/json'],
    'body'    => json_encode([
        'model'       => cfg('translate_model'),
        'temperature' => 0.2,
        'max_tokens'  => 4000,
        'messages'    => [
            ['role' => 'system', 'content' => $system],
            ['role' => 'user', 'content' => $user],
        ],
    ], JSON_UNESCAPED_UNICODE),
]);

$content = $j['choices'][0]['message']['content'] ?? null;
if ($code !== 200 || !is_string($content) || $content === '') json_err('翻译服务暂不可用', 502);
$content = trim($content);

if ($mode === 'word') {
    $clean = preg_replace('/^```(?:json)?|```$/m', '', $content);
    $obj = json_decode(trim((string)$clean), true);
    $data = is_array($obj) ? ['entry' => $obj] : ['raw' => $content];
} else {
    $data = ['result' => $content];
}

cache_put($cacheKey, json_encode($data, JSON_UNESCAPED_UNICODE), 7 * 86400);
json_out($data, 3600);
