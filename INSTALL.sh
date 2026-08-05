#!/bin/bash

echo "===================================="
echo "MAGNUS 974 - Installation Setup"
echo "===================================="
echo ""

# Vérifier si config.properties existe
if [ ! -f "config.properties" ]; then
    echo "⚠️  config.properties NOT FOUND!"
    echo ""
    echo "Copie depuis template..."
    cp config.properties.template config.properties
    
    echo "📝 Vous DEVEZ remplir config.properties avec vos identifiants:"
    echo "   - MQTT broker credentials"
    echo "   - InfluxDB credentials (optionnel)"
    echo ""
    echo "Editez: nano config.properties"
    echo ""
    exit 1
fi

echo "✅ config.properties trouvé"
echo ""

# Vérifier Android SDK
if ! command -v adb &> /dev/null; then
    echo "❌ adb NOT FOUND - installer Android SDK"
    exit 1
fi

echo "✅ ADB trouvé"
echo ""

# Vérifier device
echo "Appareils détectés:"
adb devices

echo ""
read -p "Continuer l'installation? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
fi

# Build
echo "🔨 Build APK..."
./gradlew clean build -x lint

if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build OK"
echo ""

# Install
echo "📱 Installation sur appareil..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Grant permissions
echo "🔐 Permissions..."
adb shell pm grant com.magnus.launcher android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.magnus.launcher android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.magnus.launcher android.permission.CAMERA
adb shell pm grant com.magnus.launcher android.permission.RECORD_AUDIO
adb shell pm grant com.magnus.launcher android.permission.WRITE_EXTERNAL_STORAGE
adb shell pm grant com.magnus.launcher android.permission.WAKE_LOCK

echo "✅ Permissions OK"
echo ""

# Launch
echo "🚀 Lancement..."
adb shell am start -n com.magnus.launcher/.MainActivity

echo ""
echo "===================================="
echo "✅ Installation terminée!"
echo "===================================="
echo ""
echo "L'app devrait maintenant tourner sur votre appareil"
echo "Les credentials MQTT sont dans: config.properties"
echo ""
