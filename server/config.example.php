<?php
// 配置模板。生产环境复制为 config.local.php 并填入真实值(config.local.php 不进版本库)。
return [
    // App 更新源(GitHub 仓库)
    'github_repo'     => 'Beicho/native-toolbox',

    // 翻译 LLM 网关(OpenAI 兼容)
    'translate_base'  => 'https://api.futureppo.top/v1',
    'translate_key'   => '',
    'translate_model' => 'cerebras/zai-glm-4.7',

    // 临时邮箱(Cloudflare Temp Email 部署实例)
    'tempmail_base'   => 'https://mail.123nhh.de',
    'tempmail_token'  => '',

    // 历史上的今天:GitHub 数据仓库(owner/repo)与路径模板(strftime 格式,如 docs/%m-%d.json)
    'today_repo'      => '',
    'today_path'      => '',
];
