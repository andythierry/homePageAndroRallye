# MAGNUS 974 - Guide d'Installation

## Prérequis

- Android SDK (ADB)
- Git
- Java JDK 11+

## Installation Plug & Play

### 1️⃣ Cloner le repo

```bash
git clone https://github.com/andythierry/homePageAndroRallye.git
cd homePageAndroRallye
```

### 2️⃣ Configurer les credentials

```bash
cp config.properties.template config.properties
nano config.properties
```

Remplir les informations MQTT et InfluxDB.

### 3️⃣ Connecter l'appareil et lancer l'installation

```bash
./INSTALL.sh
```

L'installation automatique:
- ✅ Build l'APK
- ✅ Install sur l'appareil
- ✅ Accorde les permissions
- ✅ Lance l'app

## Configuration MQTT

Editer `config.properties`:

```properties
mqtt.broker.host=161.97.83.80
mqtt.broker.port=1883
mqtt.broker.username=votre_username
mqtt.broker.password=votre_password
```

## Appareils testés

- ✅ Samsung Galaxy S8 (LineageOS 18.1)
- ✅ Xiaomi Mi 10T (MIUI/stock Android)
- ⏳ Autres (à tester)

## Partager le projet

**PAS DE CREDENTIALS DANS LE GIT!**

- Le fichier `config.properties` est dans `.gitignore`
- Template `config.properties.template` est dans Git
- Chaque utilisateur remplit ses propres credentials

## Support

Voir le repo: https://github.com/andythierry/homePageAndroRallye

