#!/bin/bash
# 自动打包构建脚本 (SingSync AI / AI跟唱 APK 打包程序)
set -e

echo "🚀 开始执行 Android APK 打包构建流程..."

# 检查/生成 debug.keystore 签名文件
if [ -n "$DEBUG_KEYSTORE_BASE64" ]; then
    echo "🔑 从环境变量解码 debug.keystore..."
    echo "$DEBUG_KEYSTORE_BASE64" | base64 -d > debug.keystore
elif [ -f "debug.keystore.base64" ] && [ ! -f "debug.keystore" ]; then
    echo "🔑 从 debug.keystore.base64 恢复 debug.keystore..."
    base64 -d debug.keystore.base64 > debug.keystore
elif [ ! -f "debug.keystore" ]; then
    echo "🔑 生成临时 debug.keystore..."
    keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "C=US, O=Android, CN=Android Debug"
fi

# 检查 .env 配置文件
if [ ! -f ".env" ]; then
    echo "📝 创建默认 .env 配置文件..."
    cp .env.example .env 2>/dev/null || touch .env
fi

# 执行 Gradle assembleDebug 打包
echo "📦 执行 Gradle assembleDebug 打包编译..."
gradle assembleDebug --stacktrace --no-daemon --no-configuration-cache

APK_DIR="app/build/outputs/apk/debug"
echo "✅ 打包完成！生成文件位于: $APK_DIR"
ls -la $APK_DIR/*.apk 2>/dev/null || true
