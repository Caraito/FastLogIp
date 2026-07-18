# ⚔️ FastLogIp

> Plugin d'authentification rapide pour Minecraft sans mot de passe, basé sur l'adresse IP + token UUID. Léger, sécurisé, fluide.

## 🔧 Description

FastAuth permet aux joueurs de se connecter automatiquement s'ils sont déjà reconnus par leur IP. Plus besoin de commandes `/login` ou `/register` classiques. Un système simple, basé sur un UUID/token sécurisé.

🔒 Bloque toutes les actions (dégâts, inventaire, commandes) tant que le joueur n’est pas connecté.

## 🛠️ Fonctionnalités

- 🔐 Connexion automatique par IP
- 🧠 Génération de token sécurisé
- 🔑 Mot de passe personnalisé au choix du joueur (en plus du token)
- 👮‍♂️ Blocage total tant que non connecté
- 🔁 Commandes d’administration pour régénérer les tokens
- 📂 Sauvegarde automatique de l’inventaire des joueurs non connectés

## 🖥️ Commandes

| Commande                    | Description |
|-----------------------------|-------------|
| `/regentoken`               | Génère un nouveau token pour soi |
| `/setpassword <mot de passe>` | Définit un mot de passe personnalisé (≥ 6 caractères) ; se connecter fonctionne ensuite avec le mot de passe **ou** le token d’origine |
| `/adminregentoken <pseudo>` | Admin uniquement – régénère le token d’un autre joueur |

## 🔐 Permissions

| Permission | Description                 |
|------------|-----------------------------|
| `fastlogip.admin` | Utiliser `/adminregentoken` |

## ⚙️ Configuration

Aucune config nécessaire pour la V1. Les tokens et IPs sont sauvegardés automatiquement.

## ✅ Compatibilité

- ✅ Minecraft 1.20.4 – 1.21.x
- ✅ Fonctionne sur **Spigot**, **Paper**, **Purpur**

## 🧩 Dépendances

Aucune dépendance externe.


## 📥 Installation

1. Télécharge `FastLogIp.jar`
2. Place-le dans le dossier `plugins/`
3. Redémarre ton serveur

## 💬 Support

Tu peux poser tes questions :
- En commentaire Spigot
- Par message privé
- Ou ouvrir une issue sur GitHub (si dispo)

## Note

A noter que les personnes habitant chez vous peuvent aussi se connencter via la même IP.

## 📄 Licence

Ce plugin est distribué sous licence **MIT** (voir fichier LICENSE).
