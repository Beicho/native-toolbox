pluginManagement {
    repositories {
        // 优先使用官方仓库确保最新插件可用
        google()
        mavenCentral()
        gradlePluginPortal()
        // 阿里云镜像作为备选加速
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 优先使用官方仓库确保最新依赖可用
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // 阿里云镜像作为备选加速
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "NativeToolbox"
include(":app")
