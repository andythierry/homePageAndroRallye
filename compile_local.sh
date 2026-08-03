#!/bin/bash

set -e

echo "🔨 MAGNUS 974 Launcher - Compilation Locale"
echo "==========================================="
echo ""

# Vérifie l'environnement
echo "📋 Vérifications..."

if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME non défini. Détection..."
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
        echo "✅ ANDROID_HOME=$ANDROID_HOME"
    else
        echo "❌ Erreur : Android SDK non trouvé"
        echo "📦 Installe d'abord : https://developer.android.com/studio"
        exit 1
    fi
fi

# Vérifie Gradle
if ! command -v gradle &> /dev/null; then
    echo "⚠️  Gradle pas trouvé. Installation..."
    mkdir -p ~/tools
    cd ~/tools
    if ! [ -f gradle-8.4-bin.zip ]; then
        wget -q https://services.gradle.org/distributions/gradle-8.4-bin.zip
    fi
    unzip -q gradle-8.4-bin.zip
    export PATH="$PATH:$HOME/tools/gradle-8.4/bin"
    cd -
fi

gradle_version=$(gradle --version | head -1)
echo "✅ Gradle : $gradle_version"

# Compilation
cd "$(dirname "$0")"
echo ""
echo "📦 Compilation de l'APK..."
echo ""

./gradlew clean assembleDebug

APK="$(pwd)/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "❌ Erreur : APK non généré"
    exit 1
fi

echo ""
echo "✅ Compilation réussie !"
echo "📍 APK : $APK"
echo ""

# Installation
read -p "Installer sur le S8 maintenant ? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "📱 Installation..."
    adb devices
    adb install -r "$APK"
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ APK installée !"
        echo ""
        echo "📋 Procédure d'activation :"
        echo "  1. Settings → Apps → Default apps → Home app"
        echo "  2. Sélectionne 'MAGNUS 974 Launcher'"
        echo "  3. Appuie HOME → 6 boutons"
    else
        echo "❌ Installation échouée"
        exit 1
    fi
else
    echo "⏭️  Installation ignorée"
    echo "Installation manuelle : adb install -r $APK"
fi

echo ""
echo "🎉 Terminé !"
