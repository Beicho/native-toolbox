<?php
// GET /v1/weather?lat=&lon= — 天气+空气质量(Open-Meteo,无需 key),缓存 30 分钟
declare(strict_types=1);

$lat = q('lat'); $lon = q('lon');
if ($lat === null || $lon === null || !is_numeric($lat) || !is_numeric($lon)) {
    json_err('缺少经纬度');
}
$lat = round((float)$lat, 2); $lon = round((float)$lon, 2);

$data = cached("weather:$lat,$lon", 1800, function () use ($lat, $lon) {
    $wUrl = 'https://api.open-meteo.com/v1/forecast?' . http_build_query([
        'latitude'  => $lat, 'longitude' => $lon, 'timezone' => 'auto', 'forecast_days' => 7,
        'current'   => 'temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,is_day',
        'hourly'    => 'temperature_2m,weather_code,precipitation_probability',
        'daily'     => 'weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset',
    ]);
    $aUrl = 'https://air-quality-api.open-meteo.com/v1/air-quality?' . http_build_query([
        'latitude' => $lat, 'longitude' => $lon,
        'current'  => 'pm2_5,pm10,us_aqi,european_aqi',
    ]);
    [, $w] = http_json($wUrl);
    [, $a] = http_json($aUrl);
    if ($w === null) return null;
    return ['weather' => $w, 'air' => $a['current'] ?? null];
});

if ($data === null) json_err('天气源暂不可用', 502);
json_out($data, 1800);
