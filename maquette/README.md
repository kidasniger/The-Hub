# The Hub — Maquette UI « Aurora Glass » 🪟✨

Refonte complète de l'interface de **The Hub** (réseau social Android · Kotlin/Jetpack Compose)
en **glassmorphism**, déclinée en deux ambiances : **Dark glass** (aurore boréale) et **Light glass** (verre laiteux clair).

## 🚀 Ouvrir la maquette

1. Décompresser l'archive.
2. Double-cliquer sur **`index.html`** (Chrome, Edge ou Firefox, sans serveur requis).
3. Naviguer : cliquer une carte pour ouvrir l'écran en grand, utiliser le commutateur
   **🌙 Dark glass / ☀️ Light glass** (en haut à droite, mémorisé entre les pages).

## 📁 Contenu

| Chemin | Contenu |
|---|---|
| `index.html` | Galerie des 26 écrans avec filtres par catégorie et aperçus vivants |
| `ecrans/01…26-*.html` | Un fichier par écran, cadre Pixel 8, contenu réaliste FR |
| `design-system.html` | Palette, dégradés, typographie, boutons, champs, toggles, cartes + mapping Compose |
| `etats-composants.html` | Skeletons, états vides, hors-ligne, dialogs, bottom sheet, toasts |
| `icones.html` + `icones/` | 6 variantes du logo (SVG) + exports PNG 512 px, calques adaptatifs Android |
| `assets/glass.css` | Le design system complet en CSS (~2 thèmes via `html[data-theme]`) |

## 🎨 Tokens principaux

| Token | Dark glass | Light glass |
|---|---|---|
| Fond (aurore) | `#06060F → #0E0B28 → #120F33` + 3 lueurs | `#EEF0FF → #FDF3FF → #EAF4FF` |
| Verre | `rgba(255,255,255,6%)` blur 22 px sat. 165 % | `rgba(255,255,255,56%)` blur 22 px |
| Contour | `rgba(255,255,255,13%)` | `rgba(255,255,255,85%)` |
| Accent | `#8B5CF6 → #3B82F6` (135°) — conservé de la marque | idem |
| Texte | `#F5F3FF` / 68 % | `#241F52` / 66 % |
| Rayons | 10 / 14 / 18 / 24 / 30 px | idem |

## 🧩 Correspondance Jetpack Compose

- `Brush.linearGradient(135°, 0xFF8B5CF6, 0xFF3B82F6)` → boutons primaires, FAB, bulles sortantes, barre d'onglet active.
- Surfaces de verre : `Modifier.graphicsLayer { alpha = 0.06f }` + `RenderEffect.createBlurEffect(22.dp)` ou
  `Modifier.blur()` sur un calque arrière + carte `alpha 6%`, contour `White 13%`, lueur interne `inset 1px White 16%`.
- `HubShapes` suggérés : xs 10, sm 14, md 18, lg 24, xl 30 dp.
- Le mode « Effet Glass » existant (`AppThemeMode.GLASS`) peut porter cette palette directement dans `HubPalette`.

## 🖼 Icônes

Tous les SVG sont dessinés pour ce pack (aucun asset tiers). Pour Android adaptatif :
`icone-adaptative-fond.svg` = calque fond (108 dp), `icone-adaptative-avant.svg` = avant-plan (zone sûre 66 dp),
`icone-mono-claire.svg` = themed icon Material You.

---
Maquette produite le 2 novembre 2026 · Design « Aurora Glass » pour The Hub v1.0.173.
