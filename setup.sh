#!/bin/bash

# Создаём директорию для SDK
mkdir -p ~/Android/sdk/cmdline-tools/latest

# Скачиваем Android Command Line Tools (актуальная версия на октябрь 2025)
wget -q https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -O /tmp/tools.zip
unzip /tmp/tools.zip -d /tmp/tools
mv /tmp/tools/cmdline-tools/* ~/Android/sdk/cmdline-tools/latest/
rm -rf /tmp/tools /tmp/tools.zip

# Устанавливаем переменные окружения (эти сохранятся в сессии Jules)
export ANDROID_SDK_ROOT="$HOME/Android/sdk"
export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin"
export PATH="$PATH:$ANDROID_SDK_ROOT/platform-tools"

# Добавляем в .bashrc для persistence в VM (если Jules перезапустит shell)
echo 'export ANDROID_SDK_ROOT="$HOME/Android/sdk"' >> ~/.bashrc
echo 'export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin"' >> ~/.bashrc
echo 'export PATH="$PATH:$ANDROID_SDK_ROOT/platform-tools"' >> ~/.bashrc

# Устанавливаем необходимые компоненты SDK
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.1"
yes | sdkmanager --licenses  # Автоматически принимаем лицензии

# Для Godot Android plugin: Убедитесь, что в проекте есть gradlew (если билд на Gradle)
# Если нужно, скачайте Godot exporter или дополнительные deps здесь
# Например: wget https://downloads.tuxfamily.org/godotengine/4.3/Godot_v4.3-stable_linux.x86_64.zip -O /tmp/godot.zip
# (Адаптируйте под вашу версию Godot)

echo "Android SDK установлен в $ANDROID_SDK_ROOT. Теперь можно билдить проект."
