# astro-api

Astro Kit (星辰之匣) Android 工具箱 App 的 PHP 后端 API。

## 特性

- **纯中转**: 统一聚合公开数据源(天气/汇率/IP/DoH/节假日/一言/历史今天等) + 上游 LLM 翻译,零假数据
- **轻量级**: SQLite 缓存+限频,单文件 Bootstrap,零 Composer 依赖
- **私密性**: 上游密钥(翻译/临时邮箱)只在服务端配置,App 只认自己的 API 基址

## 部署

需求: **PHP 8.2+** (已测 8.2.32)、`php-curl`、`php-sqlite3`、`php-mbstring`

```bash
# Debian/Ubuntu
apt-get install -y php8.2-cli php8.2-fpm php8.2-curl php8.2-sqlite3 php8.2-mbstring nginx

# 克隆/复制本目录到服务器
cd /opt && git clone <repo> astro-api && cd astro-api/server

# 复制配置模板并填入真实密钥
cp config.example.php config.local.php
nano config.local.php  # 填 translate_key / tempmail_token / today_repo 等

# 创建数据目录
mkdir -p data && chmod 775 data

# Nginx 配置示例(监听 50003)
cat > /etc/nginx/sites-available/astro-api <<'EOF'
server {
    listen 50003;
    root /opt/astro-api/server/public;
    index index.php;
    location / { try_files $uri /index.php$is_args$args; }
    location ~ \.php$ {
        fastcgi_pass unix:/run/php/php8.2-fpm.sock;
        include fastcgi_params;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
    }
}
EOF
ln -sf /etc/nginx/sites-available/astro-api /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
```

访问 `http://服务器IP:50003/healthz` 应返回 `ok`。

## API 端点

客户端必须带请求头 `X-Astro-Client: 1` 以通过轻量标识检查,所有接口统一 JSON envelope: `{"ok":true,"data":{...}}` 或 `{"ok":false,"err":"..."}`。

| 路径 | 说明 | 缓存 |
|---|---|---|
| `/v1/update` | 检查更新(GitHub Releases 最新版反代) | 10 min |
| `/v1/dl?tag=&name=` | Release 资产中转下载(服务器磁盘缓存) | 1 day |
| `/v1/exchange?base=USD` | 汇率(open.er-api.com) | 1 h |
| `/v1/weather?lat=&lon=` | 天气+空气质量(Open-Meteo) | 30 min |
| `/v1/ip?q=` | IP/域名归属地(ip-api.com) | 1 day |
| `/v1/dns?name=&type=A` | DoH 查询(阿里云) | - |
| `/v1/whois?domain=` | 域名 Whois(RDAP) | 1 day |
| `/v1/ssl?host=&port=443` | SSL 证书链 | 30 min |
| `/v1/sitecheck?url=` | 网站体检(DNS→TLS→HTTP 分段耗时+安全评分) | 10 min |
| `/v1/holiday?year=` | 中国节假日(NateScarlet/holiday-cn) | 1 day |
| `/v1/hitokoto?c=a` | 一言随机句子 | 30 min |
| `/v1/today?m=&d=` | 历史上的今天(GitHub 数据仓库中转) | 7 day |
| `POST /v1/translate` | 翻译与词典(`{"mode":"text","text":"...","to":"en"}` 或 `{"mode":"word","text":"..."}`) | 7 day |
| `POST /v1/mail/new` | 创建临时邮箱(30 分钟自毁) | - |
| `GET /v1/mail/list?id=` | 收件箱 | - |
| `GET /v1/mail/detail?id=&eid=` | 读信 | - |
| `GET /v1/mail/drop?id=` | 提前销毁邮箱 | - |

## 配置项

`config.local.php` (从 `config.example.php` 复制并填入):

- `github_repo`: App 更新源 GitHub 仓库
- `translate_base/key/model`: 翻译 LLM 网关(OpenAI 兼容)
- `tempmail_base/token`: 临时邮箱服务(Cloudflare Temp Email 实例)
- `today_repo/today_path`: 历史上的今天数据仓库(owner/repo 与路径模板)

## License

MIT
