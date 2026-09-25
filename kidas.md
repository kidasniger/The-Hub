---
name: "The Hub"
slug: "the-hub"
version: "1.0.170"
status: "Stable"
category: "Réseau social"
author: "kidasniger"
license: "À renseigner"
platforms:
  - "Android"
technologies:
  - "Kotlin"
  - "Jetpack Compose"
  - "Material Design 3"
  - "Coroutines/Flow"
  - "Room"
  - "Firebase"
  - "Firebase Hosting"
featured: false
github: "https://github.com/kidasniger/The-Hub"
demo: "https://the-hub-f95f4.web.app/"
website: "À renseigner"
documentation: "À renseigner"
download: "https://github.com/kidasniger/The-Hub/releases/download/v1.0.170/TheHub-v1.0.170.apk"
icon: "https://raw.githubusercontent.com/kidasniger/The-Hub/main/assets/dreamhub-logo-anime.svg"
cover: "À renseigner"
---

# Description

The Hub est une application mobile Android moderne développée avec Kotlin et Jetpack Compose. Le projet utilise Material Design 3, Coroutines/Flow, Room et Firebase.

# Description courte

Application mobile Android The Hub, développée avec Kotlin, Jetpack Compose, Room et Firebase.

# Fonctionnalités

- Authentification et gestion de compte
- Profils utilisateurs, abonnements, amis et blocage
- Publications, commentaires, réponses, likes, favoris et republications
- Recherche et fil d'actualité
- Messagerie et notifications push
- Gestion du thème et des langues FR/EN
- Système de mise à jour de l'application depuis les releases
- Liens de partage publics HTTPS pour les publications
- Deep links Android vers une publication précise
- Page web de secours pour les liens partagés
- Aperçu public d'une publication avec auteur, texte, média et compteurs
- Métadonnées Open Graph pour améliorer l'aperçu des liens partagés

# Partage des publications

Les publications peuvent être partagées avec une URL publique au format :

https://the-hub-f95f4.web.app/post/ID_DE_LA_PUBLICATION

L'objectif du lien est de présenter un aperçu réel de la publication sur le Web puis de proposer l'ouverture directe dans The Hub ou l'installation de l'application.

La page d'aperçu prend en charge :

- Nom/username et avatar de l'auteur
- Texte de la publication
- Image ou vidéo lorsqu'un média est disponible
- Date de publication
- Nombre de J'aime, commentaires et republications
- Bouton « Ouvrir dans The Hub »
- Bouton d'installation de The Hub

La page d’aperçu lit directement le document de la publication via l’API REST Firestore. Les règles Firestore autorisent uniquement la lecture publique des publications non masquées et non supprimées par l’administration. Les écritures et les autres données restent protégées par les règles existantes.

Cette architecture n’utilise pas Cloud Functions pour l’aperçu et reste compatible avec le plan Firebase Spark.

# Technologies

- Kotlin
- Jetpack Compose
- Material Design 3
- Coroutines/Flow
- Room
- Firebase
- Firebase Hosting

# Plateformes

- Android

# Captures d'écran

- https://i.ibb.co/Qvv8vz3Q/Screenshot-20260918-210616.jpg
- https://i.ibb.co/d0vbqsdR/Screenshot-20260918-210607.jpg
- https://i.ibb.co/wNrqSvzJ/Screenshot-20260918-210544.jpg

# Vidéos

- À renseigner

# Téléchargements

## Android

- URL: "https://github.com/kidasniger/The-Hub/releases/download/v1.0.170/TheHub-v1.0.170.apk"
- Version: "1.0.170"
- Architecture: "À renseigner"
- Taille: "22.2 MB"
- SHA-256: "4f6820d5aa8f267a15d75b82531f82285a00384c33e339468f1e331880d7578f"

## Windows

- URL: "À renseigner"
- Version: "À renseigner"
- Architecture: "À renseigner"
- Taille: "À renseigner"

## Linux

- URL: "À renseigner"
- Version: "À renseigner"
- Architecture: "À renseigner"
- Taille: "À renseigner"

## macOS

- URL: "À renseigner"
- Version: "À renseigner"
- Architecture: "À renseigner"
- Taille: "À renseigner"

# Liens

- GitHub: "https://github.com/kidasniger/The-Hub"
- Démo / page de partage: "https://the-hub-f95f4.web.app/"
- Site web: "À renseigner"
- Documentation: "À renseigner"
- Téléchargement: "https://github.com/kidasniger/The-Hub/releases/download/v1.0.170/TheHub-v1.0.170.apk"
- YouTube: "À renseigner"
- Vidéo de présentation: "À renseigner"

# Nouveautés

- Passage à la release Android v1.0.170
- Ajout de liens publics HTTPS pour partager les publications
- Ajout du deep link Android vers les publications partagées
- Ajout d'une page web de secours pour les liens de publication
- Ajout de l'aperçu réel d'une publication sur le Web
- Ajout des métadonnées Open Graph pour les aperçus dans les messageries et réseaux sociaux
- Ajout d'une Cloud Function `postPreview` utilisant Firebase Admin pour servir l'aperçu sans ouvrir Firestore aux visiteurs anonymes
- Les publications masquées ou supprimées par l'administration ne sont pas exposées par l'aperçu
- Remplacement de l'écran hors-ligne initial par une fenêtre contextuelle afin d'éviter le flash d'écran au lancement
- Restauration du système de mise à jour basé sur le comportement de v1.0.150
- Suppression de la limite applicative de 500 caractères pour le texte des publications

# Changelog

## 1.0.170

- Ajout de l'aperçu réel des publications partagées
- Ajout du routage Firebase Hosting `/post/**` vers l'aperçu de publication
- Ajout des métadonnées Open Graph
- Conservation du bouton d'ouverture directe dans The Hub
- Ajout du contrôle des publications masquées ou supprimées par l'administration

## 1.0.169

- Publication d'une nouvelle version Android après stabilisation du système de partage HTTPS
- Validation des tests unitaires associés aux liens de partage publics

## 1.0.168

- Remplacement de l'écran hors-ligne au démarrage par une fenêtre contextuelle
- Évite l'affichage temporaire de l'état hors-ligne lorsque la connexion est disponible

## 1.0.167

- Restauration du système de mise à jour basé sur l'implémentation de v1.0.150
- Conservation de la continuité de signature de l'APK de production
- Ajout d'une exception Lint ciblée nécessaire au pipeline Android

## 1.0.166

- Suppression de la limite applicative de 500 caractères pour le texte des publications
- Conservation des contraintes de taille imposées par le stockage Firestore

## 1.0.22

- Ajout de la localisation FR/EN
- Ajout de la vérification du statut des comptes supprimés
- Amélioration du traitement des notes de version pour les mises à jour

## 1.0.75

- Alignement et validation stricte du versionName, du versionCode, du tag Git et de la GitHub Release.
- Le pipeline ne peut plus régresser silencieusement vers une ancienne version après un release plus récent.

# SEO

## Title

The Hub - Application mobile Android et réseau social

## Description

The Hub est une application mobile Android développée avec Kotlin, Jetpack Compose, Material Design 3 et Firebase. Les publications peuvent être partagées avec des liens HTTPS ouvrant un aperçu Web puis l'application.

## Keywords

- The Hub
- application Android
- réseau social
- publications
- partage de publications
- liens HTTPS
- Kotlin
- Jetpack Compose
- Firebase

## Open Graph Image

À renseigner

## Canonical URL

https://the-hub-f95f4.web.app/

# Informations supplémentaires

Identifiant d'application Android : com.thehub.hb

Projet Firebase : the-hub-f95f4

Dernière release Android vérifiée : v1.0.170

SHA-256 de l'APK v1.0.170 :
4f6820d5aa8f267a15d75b82531f82285a00384c33e339468f1e331880d7578f
