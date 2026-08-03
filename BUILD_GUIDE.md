# 🚀 Guide de Compilation MAGNUS 974 Launcher

## Environnement requis

- Android SDK (Platform 34)
- Gradle 8.4+
- JDK 11+
- ADB (pour installation)

## Vérifications préalables

```bash
# Android SDK
export ANDROID_HOME=~/Android/Sdk
ls $ANDROID_HOME/platforms/android-34/

# Gradle
gradle --version
# Si pas trouvé : wget https://services.gradle.org/distributions/gradle-8.4-bin.zip

# ADB
adb version
```

## Compilation

```bash
cd ~/launcher_magnus

# Nettoie les builds précédentes
./gradlew clean

# Compile l'APK de debug
./gradlew assembleDebug

# L'APK est généré ici :
# app/build/outputs/apk/debug/app-debug.apk
```

## Installation

```bash
# Vérifie que le S8 est connecté
adb devices

# Installe l'APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Succès : "Success"
```

## Configuration sur le S8

1. **Déverrouille** et **Settings**
2. **Apps → Default apps → Home app**
3. **Sélectionne "MAGNUS 974 Launcher"**
4. **Appuie HOME** → 6 boutons s'affichent

## Troubleshooting

### "gradle: command not found"
```bash
# Télécharge Gradle (si pas disponible)
mkdir -p ~/tools
cd ~/tools
wget https://services.gradle.org/distributions/gradle-8.4-bin.zip
unzip gradle-8.4-bin.zip
export PATH="$PATH:$HOME/tools/gradle-8.4/bin"
```

### "ANDROID_HOME not set"
```bash
export ANDROID_HOME=~/Android/Sdk
```

### "Platform 34 not found"
```bash
# Télécharge via SDK Manager
sdkmanager "platforms;android-34"
```

### APK refusée à l'installation
```bash
# Purge l'ancienne version
adb uninstall com.magnus.launcher

# Réinstalle
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Structure finale

```
app/
├── build/
│   └── outputs/apk/debug/
│       └── app-debug.apk ✅ C'EST CELUI-LÀ
├── src/
│   └── main/
│       ├── java/com/magnus/launcher/MainActivity.kt
│       ├── res/layout/activity_main.xml
│       ├── res/values/strings.xml
│       └── AndroidManifest.xml
└── build.gradle.kts
```

---

**Créé pour MAGNUS 974 Rally Tracker** 🏁
