# 综合工具包 - Kotlin 原生版

## 项目简介

这是原 uni-app 版本的完整 Kotlin 原生重写，采用 Jetpack Compose + Material You 设计。

### 原版问题
- 17MB Weex 运行时 + 40+MB APK 体积
- WebView 渲染性能低
- 依赖 DCloud 黑盒容器

### 原生版优势
- ✅ **包体积**：8-12MB（减少 70%）
- ✅ **启动速度**：< 1s（原版 3-5s）
- ✅ **内存占用**：< 80MB（原版 150-200MB）
- ✅ **Material You**：动态取色 + 深色模式
- ✅ **可维护性**：纯 Kotlin，类型安全

## 功能特性

### 1. 编码转换器
- 自动编码检测（UTF-8/GBK/GB2312/UTF-16/BIG5）
- 批量文件转换
- MediaStore API（Android 10+）

### 2. 小说下载器
- 番茄小说 API 集成
- 高速/降速双模式
- 后台下载 + 通知进度

### 3. 应用更新
- GitHub Releases 自动检查
- 语义化版本比对

## 技术栈

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose + Material 3
- **Network**: Retrofit 2 + OkHttp
- **Async**: Coroutines + Flow
- **Serialization**: Kotlinx Serialization
- **Background**: WorkManager
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)

## 构建指南

### 方法 1: GitHub Codespaces（推荐）

```bash
# 1. 创建 Codespace
gh codespace create -r YOUR_REPO

# 2. SSH 连接
gh codespace ssh

# 3. 构建
./gradlew assembleRelease

# 4. 下载 APK
gh codespace cp remote:app/build/outputs/apk/release/app-release.apk ./
```

### 方法 2: 本地构建

```bash
./gradlew assembleDebug
```

## 项目结构

```
app/src/main/kotlin/com/toolbox/native/
├── ui/
│   ├── theme/          # Material3 主题
│   ├── encoding/       # 编码转换
│   ├── novel/          # 小说下载
│   └── about/          # 关于页面
├── data/
│   ├── api/            # Retrofit 接口
│   ├── model/          # 数据类
│   └── repository/     # 数据仓库
└── domain/
    ├── usecase/        # 业务逻辑
    └── util/           # 工具类
```

## 开发进度

- [x] 项目脚手架
- [ ] 编码转换模块
- [ ] 小说下载模块
- [ ] 更新检查
- [ ] 性能测试
- [ ] 发布 Release

## License

MIT

## 致谢

原 uni-app 版本：[TD-JZ/E-Ink-Display-Reader-Adapter-Software](https://github.com/TD-JZ/E-Ink-Display-Reader-Adapter-Software)
