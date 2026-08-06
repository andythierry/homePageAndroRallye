## 🚀 Compilation et déploiement

### Prérequis
- Android Studio ou Gradle 8.4+
- OpenJDK 21
- ADB configuré

### Build
```bash
./gradlew clean build
./gradlew installDebug
```

### Lancer l'app
```bash
adb shell am start -n com.magnus.launcher/.MainActivity
```

## ⚙️ Configuration GPS

La classe `SettingsActivity` propose 4 solutions de veille GPS :

1. **🚫 Désactiver la veille** → Paramètres affichage (simple, consomme batterie)
2. **⚡ WakeLock (Partiel)** → Garde GPS + réseau actifs (DEFAULT, bon compromis)
3. **📲 Service au premier plan** → Notification persistante (le plus fiable)
4. **⏰ AlarmManager** → Réveille toutes les 30s (très fiable, bonne batterie)

## 🎨 Thèmes

- **Couleurs** : Fond noir (#000000), texte blanc, accents vert (#00FF00), orange (#FFAA00)
- **Orientation** : Portrait (Settings), Paysage (Main)
- **Typo** : Sans titre (NoTitleBar)

## 📊 Permissions requises

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## ⚠️ État actuel

✅ **Complété**
- MainActivity en paysage sans bandeau
- SettingsActivity en portrait avec back button
- 4 solutions GPS opérationnelles
- Indicateurs temps réel (Batterie, Bluetooth, Réseau, GPS)

🔄 **En cours**
- Tests complets des solutions GPS
- Enregistrement vidéo arrière-plan (bloqué LineageOS)

❌ **Connu**
- Enregistrement vidéo avant caméra échoue (caméra occupée par RaceChrono ou permissions)

## 🔗 Repo GitHub

https://github.com/andythierry/homePageAndroRallye

## 📝 Notes de développement

- Build: Gradle Kotlin DSL
- Cibles : API 21+ (Android 5+)
- Deprecated : `Camera` API, `systemUiVisibility`, `activeNetworkInfo` (utiliser alternatives modernes)

## 🤝 Contributeur

Thierry - La Réunion (974)

EOF

cat README.md
