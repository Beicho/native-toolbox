<?php
// /v1/mail/* — 临时邮箱中转(上游:123nhh/tempmail,Key 只在服务端)
// POST /v1/mail/new            → 创建随机邮箱(30 分钟自毁)
// GET  /v1/mail/list?id=       → 收件箱
// GET  /v1/mail/detail?id=&eid=→ 读信
// GET  /v1/mail/drop?id=       → 提前销毁
declare(strict_types=1);

$base  = rtrim((string)cfg('tempmail_base'), '/') . '/api';
$token = (string)cfg('tempmail_token');
if ($token === '') json_err('临时邮箱服务未配置', 501);

$auth = ['Authorization: Bearer ' . $token, 'Content-Type: application/json'];
$uuid = '/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i';

switch ($GLOBALS['sub']) {
    case 'new':
        if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') json_err('需要 POST', 405);
        if (!rate_hit('mailnew:' . client_ip(), 6, 600)) json_err('创建太频繁,稍后再试', 429);
        [$code, $j] = http_json("$base/mailboxes", ['method' => 'POST', 'headers' => $auth, 'body' => '{}', 'timeout' => 10]);
        $mb = $j['mailbox'] ?? $j;
        if ($code !== 200 && $code !== 201 || empty($mb['id'])) json_err('邮箱服务暂不可用', 502);
        json_out([
            'id'        => $mb['id'],
            'address'   => $mb['full_address'] ?? ($mb['address'] ?? ''),
            'expiresAt' => $mb['expires_at'] ?? '',
        ]);
        // no break (json_out exits)

    case 'list':
        $id = q('id', '');
        if (!preg_match($uuid, $id)) json_err('邮箱标识不合法');
        [$code, $j] = http_json("$base/mailboxes/$id/emails", ['headers' => $auth, 'timeout' => 10]);
        if ($code === 404) json_err('邮箱不存在或已过期', 404);
        if ($j === null) json_err('邮箱服务暂不可用', 502);
        json_out(['emails' => $j['data'] ?? (is_array($j) ? $j : [])]);

    case 'detail':
        $id = q('id', ''); $eid = q('eid', '');
        if (!preg_match($uuid, $id) || !preg_match($uuid, $eid)) json_err('参数不合法');
        [$code, $j] = http_json("$base/mailboxes/$id/emails/$eid", ['headers' => $auth, 'timeout' => 10]);
        if ($code === 404) json_err('邮件不存在', 404);
        if ($j === null) json_err('邮箱服务暂不可用', 502);
        json_out(['email' => $j['email'] ?? $j]);

    case 'drop':
        $id = q('id', '');
        if (!preg_match($uuid, $id)) json_err('邮箱标识不合法');
        http_req("$base/mailboxes/$id", ['method' => 'DELETE', 'headers' => $auth, 'timeout' => 10]);
        json_out(['dropped' => true]);

    default:
        json_err('not found', 404);
}
