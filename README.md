# The Hub 📱

Application mobile Android moderne pour **The Hub**, développée avec Jetpack Compose, Material Design 3, Coroutines/Flow, Room et Firebase.

---

## 🚀 Pipeline CI/CD Automatisé (Build, Tag & Release)

Le workflow GitHub Actions est configuré dans [`.github/workflows/build-and-release.yml`](.github/workflows/build-and-release.yml).

### Ce qui est exécuté après chaque push :

1. **Compilation Android** :
   - JDK 17 & SDK Android.
   - Tests Gradle.
   - Build Release APK signé.
   - Vérification de la version réellement embarquée dans l'APK.

2. **Version unique et monotone** :
   - La source de vérité est `gradle/version.properties`.
   - `versionName` et `versionCode` sont validés ensemble.
   - Pour `1.0.X`, le `versionCode` est exactement `X`.
   - Le pipeline compare la version du dépôt au dernier GitHub Release et ne peut plus revenir à une version ancienne comme `1.0.21` après `1.0.74`.
   - Si nécessaire, il calcule automatiquement la version suivante.

3. **Tag Git** :
   - Toujours exactement `v<versionName>`.
   - Le tag est créé sur le même commit que le code compilé.
   - Un tag déjà existant fait échouer le pipeline.

4. **GitHub Release** :
   - La GitHub Release utilise exactement le même tag.
   - L'asset publié est un APK signé et utilise exactement la même version.
   - La version APK est vérifiée avec `aapt` avant publication.

---

### Comment déclencher une release ?

#### Push standard sur la branche principale
```bash
git add .
git commit -m "feat: ajout de nouvelles fonctionnalités"
git push origin main
```
> Le pipeline conserve la version du dépôt si elle est supérieure au dernier release. Sinon, il repart du dernier release et incrémente le patch avant le build.

#### Déclenchement manuel
1. Allez dans **Actions**.
2. Sélectionnez **Build, Tag and Release**.
3. Lancez le workflow et renseignez éventuellement un tag explicite comme `v1.0.75`.
> Le tag explicite doit être strictement supérieur au dernier release.

## 🛡️ P7 — Préparation production finale

La P7 ajoute les derniers garde-fous avant distribution :

- indication réactive de l'absence de connexion Internet dans l'interface principale ;
- détection réseau basée sur une connexion réellement validée ;
- lint Android exécuté dans la CI des Pull Requests et dans le pipeline de release ;
- vérification que l'APK release publié n'est pas marqué débogable ;
- test unitaire dédié à la détection de connectivité.

La signature de production et la publication APK-only restent inchangées.

---

### 🔑 Signature de production

Les releases de production utilisent la signature de production conservée dans les **GitHub Secrets** (*Settings > Secrets and variables > Actions*). Le pipeline vérifie la présence du keystore, ses identifiants et la continuité du certificat avant de publier l'APK.
