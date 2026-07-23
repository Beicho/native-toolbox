<?php
// GET /v1/ssl?host=&port=443 — 证书链现场探测
declare(strict_types=1);

$host = strtolower(q('host', '') ?? '');
$port = (int)(q('port', '443') ?? 443);
if ($port < 1 || $port > 65535) json_err('端口不合法');
if (($why = guard_target($host)) !== null) json_err($why);

$data = cached("ssl:$host:$port", 1800, function () use ($host, $port) {
    $probe = function (bool $verify) use ($host, $port) {
        $sslOpt = [
            'capture_peer_cert_chain' => true,
            'verify_peer'             => $verify,
            'verify_peer_name'        => $verify,
            'allow_self_signed'       => !$verify,
            'SNI_enabled'             => true,
            'peer_name'               => $host,
        ];
        if ($verify && is_file('/etc/ssl/certs/ca-certificates.crt')) {
            $sslOpt['cafile'] = '/etc/ssl/certs/ca-certificates.crt';
        }
        $ctx = stream_context_create(['ssl' => $sslOpt]);
        $t0 = microtime(true);
        $fp = @stream_socket_client("ssl://$host:$port", $errno, $errstr, 8, STREAM_CLIENT_CONNECT, $ctx);
        $ms = (int)round((microtime(true) - $t0) * 1000);
        if (!$fp) return [null, $ms, $errstr];
        $params = stream_context_get_params($fp);
        fclose($fp);
        return [$params['options']['ssl']['peer_certificate_chain'] ?? [], $ms, ''];
    };

    [$chainRaw, $ms] = $probe(false);
    if ($chainRaw === null) return null;
    [$trustedChain] = $probe(true);

    $chain = [];
    foreach ($chainRaw as $certRes) {
        $c = openssl_x509_parse($certRes);
        if (!$c) continue;
        $san = [];
        foreach (explode(',', $c['extensions']['subjectAltName'] ?? '') as $s) {
            $s = trim($s);
            if (str_starts_with($s, 'DNS:')) $san[] = substr($s, 4);
        }
        $chain[] = [
            'subject'   => $c['subject']['CN'] ?? ($c['subject']['O'] ?? '?'),
            'issuer'    => $c['issuer']['CN'] ?? ($c['issuer']['O'] ?? '?'),
            'issuerOrg' => $c['issuer']['O'] ?? '',
            'validFrom' => (int)($c['validFrom_time_t'] ?? 0),
            'validTo'   => (int)($c['validTo_time_t'] ?? 0),
            'daysLeft'  => (int)floor((($c['validTo_time_t'] ?? 0) - time()) / 86400),
            'sigAlg'    => $c['signatureTypeSN'] ?? '',
            'san'       => array_slice($san, 0, 30),
        ];
    }
    return [
        'host'        => $host,
        'port'        => $port,
        'handshakeMs' => $ms,
        'trusted'     => $trustedChain !== null,
        'chain'       => $chain,
    ];
});

if ($data === null) json_err('无法建立 TLS 连接(目标不可达或非 TLS 服务)', 502);
json_out($data, 900);
