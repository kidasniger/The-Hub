# The Hub 📱

Application mobile Android moderne pour **The Hub**, développée avec Jetpack Compose, Material Design 3, Coroutines/Flow, Room et Firebase.

---

## 🚀 Pipeline CI/CD Automatisé (Build, Tag & Release)

Le workflow GitHub Actions est configuré dans [`.github/workflows/build-and-release.yml`](.github/workflows/build-and-release.yml).

### Ce qui est exécuté après chaque push :

1. **Compilation Android** :
   - Mise en place de l'environnement JDK 17 & SDK Android.
   - Compilation complète via Gradle (`./gradlew assembleDebug` et `assembleRelease`).
   - Génération des fichiers APK installables (`TheHub-vX.X.X.apk`).

2. **Création automatique du Tag Git** :
   - Si vous poussez directement sur la branche principale (`main` ou `master`), un tag incrémental est calculé automatiquement à partir de la version de l'application : `v<versionName>.<numéro_de_run>` (ex: `v1.0.1`, `v1.0.2`...).
   - Si vous poussez un tag explicite (ex: `git tag v1.1.0 && git push origin v1.1.0`), ce tag est automatiquement utilisé.

3. **Publication de la Release GitHub** :
   - Une nouvelle **GitHub Release** est créée automatiquement avec les notes de version générées (`generate_release_notes`).
   - L'APK prêt à l'emploi est directement attaché en téléchargement dans les assets de la release.

---

### Comment déclencher une release ?

#### Option A : Push standard sur la branche principale
```bash
git add .
git commit -m "feat: ajout de nouvelles fonctionnalités"
git push origin main
```
> Le workflow compile le projet, crée le tag automatique (ex: `v1.0.1`) et publie la release avec l'APK.

#### Option B : Push avec un tag versionné
```bash
git tag v1.0.0
git push origin v1.0.0
```
> Le workflow compile le projet et publie la release officielle pour le tag `v1.0.0`.

#### Option C : Déclenchement manuel (GitHub Actions)
1. Allez sur votre dépôt GitHub > onglet **Actions**.
2. Sélectionnez **Build, Tag and Release**.
3. Cliquez sur **Run workflow** (vous pouvez optionnellement spécifier un nom de tag personnalisé).

---

### 🔑 Configuration facultative de signature (Keystore)

Par défaut, l'APK est généré et signé afin d'être immédiatement installable sur n'importe quel smartphone Android.

Si vous possédez votre propre Keystore de production, vous pouvez ajouter les **GitHub Secrets** suivants dans les paramètres de votre dépôt (*Settings > Secrets and variables > Actions*) :
- `KEYSTORE_BASE64` : Votre fichier `.jks` encodé en base64 (`base64 -w 0 mon-keystore.jks`)
- `STORE_PASSWORD` : Mot de passe du keystore
- `KEY_PASSWORD` : Mot de passe de la clé
