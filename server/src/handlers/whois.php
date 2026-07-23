<?php
// GET /v1/whois?domain= — 域名注册信息(RDAP 标准协议,rdap.org 引导重定向)
declare(strict_types=1);

$domain = strtolower(q('domain', '') ?? '');
if (!preg_match('/^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$/', $domain)) {
    json_err('域名不合法');
}

$data = cached("whois:$domain", 86400, function () use ($domain) {
    [$code, $j] = http_json("https://rdap.org/domain/" . rawurlencode($domain), ['timeout' => 12]);
    if ($code === 404) return ['found' => false];
    if ($j === null) return null;

    $events = [];
    foreach ($j['events'] ?? [] as $e) {
        $events[$e['eventAction'] ?? ''] = $e['eventDate'] ?? '';
    }
    $registrar = '';
    foreach ($j['entities'] ?? [] as $ent) {
        if (in_array('registrar', $ent['roles'] ?? [], true)) {
            foreach ($ent['vcardArray'][1] ?? [] as $card) {
                if (($card[0] ?? '') === 'fn') { $registrar = (string)($card[3] ?? ''); break; }
            }
            break;
        }
    }
    $ns = [];
    foreach ($j['nameservers'] ?? [] as $n) {
        if (!empty($n['ldhName'])) $ns[] = strtolower($n['ldhName']);
    }
    return [
        'found'      => true,
        'domain'     => $j['ldhName'] ?? $domain,
        'registrar'  => $registrar,
        'created'    => $events['registration'] ?? '',
        'expires'    => $events['expiration'] ?? '',
        'updated'    => $events['last changed'] ?? '',
        'status'     => $j['status'] ?? [],
        'nameservers'=> $ns,
        'dnssec'     => (bool)($j['secureDNS']['delegationSigned'] ?? false),
    ];
});

if ($data === null) json_err('Whois 源暂不可用(部分后缀无公开数据)', 502);
json_out($data, 3600);
