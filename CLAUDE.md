# CLAUDE.md — carnet de passation de Samoth Home

Ce fichier sert à reprendre le projet sans rien perdre. Lis-le en entier avant de toucher au code.

## Règle permanente (à appliquer sans qu'on le demande)

À la fin de **chaque** modification :
1. `git commit` puis `git push` sur `main` (pas de branche, pas de PR).
2. **Surveiller la fabrication sur GitHub Actions jusqu'au bout** (`gh run watch <id> -R BDBOFF/SamothHOME --exit-status`)
   et vérifier qu'une nouvelle Release `build-N` contenant `SamothHome.apk` est bien publiée.
   Si la compilation échoue : lire le journal, corriger, repousser, jusqu'au vert.
   (Un commit qui ne touche **que** des fichiers `.md`, le dossier `.claude/` ou `.mcp.json` ne lance pas de compilation.)
3. **Mettre à jour ce CLAUDE.md** (historique des versions, bugs corrigés, reste à faire), puis commit + push.

Tout ce circuit est décrit pas à pas dans la skill **`/publier-home`** : l'utiliser à la fin de chaque modification.

## Préférences du propriétaire

- **Toujours répondre en français**, avec des **explications simples**, sans jargon technique inutile.
- Il teste sur sa TV et revient avec des retours précis : corriger exactement ce qu'il décrit.
- Avant un changement visuel important, il aime voir une **maquette** et choisir parmi 2–3 propositions.
- Il utilise surtout son **téléphone** : rester bref, éviter les manipulations compliquées de son côté.
- Il a une autre appli, **Samoth TV** (lecteur IPTV, dépôt séparé `BDBOFF/NovaTv`, paquet `com.nova.tv`).
  **Ne jamais mélanger les deux dépôts** : une mise à jour de l'un ne doit jamais bloquer l'autre.

## Ce que fait l'appli

**Samoth Home** est un **lanceur (écran d'accueil) épuré** pour Android TV, inspiré de l'Apple TV,
aux couleurs de Samoth TV : fond noir `#07070A`, rouge `#D10A20`.

TV du propriétaire : **Sony KE-65A8** (Android TV, pas Google TV), à La Roquette-sur-Siagne (06550).

- **Allumage** : grande horloge blanche, date + **météo du jour**, grand « SAMOTH HOME » en fond (police Unbounded,
  très grasse, rouge transparent).
- **Navigation** : dès qu'on appuie sur une flèche, le fond affiche le **nom de l'appli sélectionnée dans sa couleur**
  (léger délai pour ne pas clignoter) ; petite signature « SAMOTH HOME » en bas à droite.
- **5 favoris fixes** : tuiles « verre » avec la **vraie bannière** de l'appli en plein cadre et un **halo** à sa couleur
  (couleur dominante calculée depuis la bannière ; rouge forcé pour Samoth TV). L'ordre ne bouge **jamais** tout seul.
  Une place libre = tuile « + » qui ouvre Toutes les applis.
- **Toutes les applis** : grille 5 colonnes qui défile, **triée par nombre de lancements**, bouton « Tout fermer ».
- **Appui long sur OK** (sur une tuile) : Mettre sur l'accueil / Remplacer / Déplacer gauche-droite / Retirer /
  Ouvrir / Forcer l'arrêt / Désinstaller (ou Désactiver pour une appli Sony) / Infos de l'appli.
- **Bouton réglages** (icône « curseurs » : 3 réglettes, un bouton rouge) → réglages de la TV.
- **Mise à jour intégrée** : pastille rouge « Mise à jour disponible » à côté des réglages quand une Release plus
  récente existe sur GitHub → télécharge l'APK et lance l'installation (l'utilisateur confirme).
- **Touche Home / rallumage** : ramènent toujours sur Samoth Home (via le service d'accessibilité, voir plus bas).
- **Écran de veille** (fond d'écran Google) : à sa fermeture, on rouvre l'appli qui était en cours (ex. Netflix en pause).

## Organisation du code

```
app/src/main/java/com/samoth/home/
  MainActivity.kt        Activité unique (catégorie HOME + LAUNCHER). onResume : rafraîchit, vérifie MAJ/météo,
                         rouvre l'appli après écran de veille. onNewIntent(HOME) → retour à l'écran d'allumage.
  HomeViewModel.kt       Tout l'état : applis, favoris (pinned), écran courant, menu appui long, toast,
                         navigation/fond, météo, mise à jour.
  AppsRepository.kt      Liste des applis installées (LEANBACK + LAUNCHER), bannières/icônes, couleur dominante,
                         compteur de lancements et favoris (SharedPreferences "usage"), lancer / désinstaller /
                         fiche appli / killBackgroundProcesses.
  HomeGuardService.kt    Service d'accessibilité : intercepte la touche Home, détecte l'ouverture de l'accueil
                         Sony/Google et ramène sur Samoth Home ; suit l'écran de veille (DREAMING_STARTED/STOPPED).
  ResumeAfterDream.kt    Mémorise l'appli au premier plan au début de l'écran de veille et la rouvre ensuite.
  Updater.kt             Lit la dernière Release GitHub (tag build-N), compare au versionCode, télécharge, installe.
  Weather.kt             Météo Open-Meteo (gratuit, sans clé), coordonnées fixes 43.5808, 6.9561.
  BootReceiver.kt        Au démarrage de la TV, ouvre Samoth Home (filet de sécurité).
  ui/HomeScreen.kt       Accueil (horloge, date, météo, fond, favoris) + HomeRoot (toast, dialogues) + TopActions.
  ui/AllAppsScreen.kt    Grille Toutes les applis.
  ui/Components.kt       AppTile (tuile verre + halo + appui long), AddTile, boutons ronds, pastilles, icône curseurs.
  ui/Dialogs.kt          Menu de l'appui long, fenêtre de configuration du premier lancement, toast.
  ui/Watermark.kt        Grand texte de fond, taille ajustée automatiquement à la largeur.
  ui/WeatherIcon.kt      Icônes météo dessinées (soleil, lune, nuage, pluie, orage, neige, brouillard).
  ui/Theme.kt            Couleurs et polices (Unbounded, Inter).
app/src/main/res/        Icône, bannière TV (320×180), chaînes, thème, config du service d'accessibilité.
```

Pile : Kotlin 2.1, Jetpack Compose (BOM 2024.12.01), AGP 8.7.3, Gradle 8.11.1, minSdk 26, targetSdk 35.
Paquet / applicationId : `com.samoth.home`.

## Fabrication et publication de l'APK

- **Tout se fait sur GitHub Actions** (`.github/workflows/build.yml`) à chaque push sur `main`
  (sauf si le push ne modifie que des fichiers `.md`) :
  téléchargement des polices → `./gradlew assembleRelease -PBUILD_NUMBER=<numéro du run>` → Release `build-N`
  avec `SamothHome.apk` (marquée *latest*).
- **versionCode = numéro du run GitHub.** C'est ce qui permet à la mise à jour intégrée de comparer les versions.
  Ne jamais fixer le versionCode à la main.
- Lien fixe vers la dernière version : `https://github.com/BDBOFF/SamothHOME/releases/latest/download/SamothHome.apk`
- **Signature** : keystore `app/samoth-home.jks` versionné, configuré dans `app/build.gradle.kts`.
  Il doit rester **toujours le même** : sinon les mises à jour ne s'installent plus par-dessus
  (il faudrait désinstaller, et l'utilisateur perdrait ses favoris). Ne jamais le régénérer ni le supprimer.
- **On ne compile pas en local** dans l'environnement de l'assistant (pas de SDK Android, et le proxy bloque
  `raw.githubusercontent.com`) : on pousse et on regarde le résultat de l'Action.
- Polices **Unbounded** et **Inter** (licence OFL) : téléchargées pendant la compilation, ignorées par git
  (`app/src/main/res/font/*.ttf` dans `.gitignore`).
- `gh` : l'API GraphQL est bloquée dans cet environnement → utiliser l'API REST (`gh api repos/...`).
  `gh release download` ne marche pas ; passer par `gh api -H "Accept: application/octet-stream" repos/.../releases/assets/<id>`.
- Le dépôt a été renommé en `SamothHOME` (casse) : remote `https://github.com/BDBOFF/SamothHOME`.

## Automatisations Claude Code (dossier `.claude/`)

Mises en place le 26/11/2026, actives à chaque session sur ce dépôt :

- **Hook `guard_secrets.py`** (avant chaque écriture, commit ou push) : bloque jetons, clés privées, clés API,
  adresse e-mail personnelle (reconnue par empreinte SHA-256, jamais en clair), et **toute modification ou
  suppression de `app/samoth-home.jks`**. Ne regarde que les lignes ajoutées (la config de signature déjà
  présente dans `app/build.gradle.kts` est une décision assumée). Ne jamais le contourner.
- **Hook `check_kotlin.py`** (après chaque modification d'un `.kt`) : imports `getValue`/`setValue` manquants,
  API Android > 26 sans test de version, `Context.RECEIVER_*` au lieu de `ContextCompat`, et `AppTile` sans
  `.okPress(...)`. `// check-ok` en fin de ligne pour un faux positif.
- **Subagent `relecteur-home`** : joue le compilateur avant chaque push et protège les comportements validés
  (touche Home, appui long, favoris fixes, signature).
- **Skill `/publier-home`** : relecture → commit → push → surveillance → correction si échec → release →
  CLAUDE.md → message au propriétaire.
- **Skill `regles-lanceur`** (automatique) : conventions d'interface (focus, `okPress`, `Panel`, couleurs `Samoth.*`,
  tailles TV, maquettes avant un gros changement visuel).
- **MCP `context7`** (`.mcp.json`) : documentation à jour de Jetpack Compose / Android.
- Noms distincts de ceux de Samoth TV (`publier`, `relecteur`, `regles-tv`) pour ne pas mélanger les deux dépôts.
- Commits : auteur `BDBOFF` avec l'adresse `BDBOFF@users.noreply.github.com` (ne plus utiliser l'adresse perso).

## Installation sur la TV (rappel)

1. APK via l'appli **Downloader** (lien fixe ci-dessus) ou clé USB.
2. Au premier lancement, la fenêtre « Faire de Samoth Home votre accueil » envoie vers
   **Accessibilité → Samoth Home – touche Home → Activer**. Indispensable sur la Sony.
3. Après une mise à jour qui change les capacités du service, il peut falloir le **désactiver puis le réactiver**.
4. Méthode ADB facultative (détaillée dans README.md) pour désactiver l'accueil d'origine.

## Décisions prises (et pourquoi)

- **Lanceur maison plutôt que Projectivy** : le propriétaire voulait un design précis et des mises à jour sans clé USB.
- **Dépôt séparé de NovaTv** : pour que les deux applis évoluent sans se bloquer.
- **Design « Zen »** choisi parmi 3 maquettes (Vitrine / Grille / Zen) : horloge, date, 5 favoris, bouton Toutes les applis.
- **Fond** : « SAMOTH HOME » à l'allumage, puis nom de l'appli sélectionnée. Police **Unbounded** choisie parmi 3.
  Pas de point rouge dans l'horloge, pas de « Bonsoir », horloge entièrement blanche.
- **Tuiles en verre transparent + halo** à la couleur de l'appli ; vraies bannières en plein cadre (pas de logos redessinés).
- **Favoris fixes** dans l'ordre choisi par l'utilisateur (il a explicitement refusé le réordonnancement automatique).
  Seule la page Toutes les applis est triée par utilisation.
- **Mise à jour** : pastille à côté du bouton réglages, visible seulement quand une version existe
  (Android impose une confirmation d'installation, impossible de faire 100 % silencieux).
- **Touche Home** : la Sony ne propose pas de choisir l'accueil → service d'accessibilité (même principe que Projectivy).
- **Icône réglages** : « curseurs » choisie parmi 3 (engrenage fin / badge rouge / curseurs).
- **Météo** : Open-Meteo, pas de clé à stocker (dépôt public).

## Bugs déjà corrigés — pièges à éviter

- **Touche Home renvoyait sur l'accueil Sony** (pas de choix d'accueil sur la Sony) → service d'accessibilité.
- **2–3 s d'accueil Sony avant Samoth Home** → le service intercepte directement `KEYCODE_HOME`
  (`flagRequestFilterKeyEvents` + `canRequestFilterKeyEvents`) au lieu d'attendre que l'accueil Sony s'affiche.
- **Appui long qui lançait l'appli** : le relâchement de OK après l'ouverture du menu « cliquait » le premier bouton.
  → le menu (`Panel` dans Dialogs.kt) ignore la touche OK tant qu'elle n'a pas été relâchée puis ré-appuyée,
  et l'appui long est géré à la main (`okPress`, via `repeatCount`). Ne pas revenir à un simple `combinedClickable`.
- **Favoris qui changeaient de place** après chaque lancement → favoris fixes enregistrés (`__pinned__`),
  remplis une seule fois au premier lancement (`__seeded__`).
- **Vignettes trop petites** dans les tuiles → bannière en `ContentScale.Crop`, plein cadre.
- **Écran de veille → retour sur l'accueil au lieu de Netflix** (v7) → `ResumeAfterDream` : on retient l'appli au
  début de l'écran de veille et on la rouvre (le service ignore tout pendant l'écran de veille).
  Une vraie mise en veille (SCREEN_OFF) ou la touche Home annulent cette reprise.
- Pièges généraux :
  - Le service d'accessibilité est le cœur du comportement Home : toute modification doit garder
    l'anti-rebond (`lastJump`) pour éviter les boucles d'ouverture.
  - `killBackgroundProcesses` ne ferme que les applis en arrière-plan ; « Désactiver » une appli système
    ne peut se faire que depuis sa fiche Android (on l'ouvre, l'utilisateur clique).
  - La liste des applis demande les `<queries>` du manifeste (Android 11+) : ne pas les retirer.
  - Les tailles sont en dp pour un écran TV (≈ 960×540 dp) : un texte « 22.sp » apparaît grand sur la TV.

## Historique des versions

| Version | Contenu |
|---|---|
| build-1 | Première version : accueil Zen, fond dynamique, favoris, Toutes les applis, mise à jour intégrée |
| build-2 | Service d'accessibilité touche Home, appui long (favoris), vignettes plein cadre |
| build-3 | Touche Home immédiate, appui long corrigé, accueil fixe + tuile « + », fond plus lumineux, horloge/date plus grandes |
| build-4 | Forcer l'arrêt, Désinstaller / Désactiver, Infos de l'appli, Tout fermer |
| build-5 | Météo du jour à côté de la date |
| build-6 | Icône réglages « curseurs » |
| build-7 | Retour à l'appli en cours après l'écran de veille |
| build-8 | Aucun changement d'appli (ajout de CLAUDE.md / AGENTS.md) |
| build-9 | Aucun changement d'appli : la fabrication ignore désormais les commits qui ne modifient que des `.md` |
| build-10 | Aucun changement d'appli : la fabrication ignore aussi `.claude/` et `.mcp.json` (ajout des automatisations) |

## Reste à faire / idées

- **À confirmer par le propriétaire** : le correctif écran de veille (build-7) n'a pas encore été validé sur la TV.
- La ville de la météo est codée en dur (`Weather.kt`) : la rendre réglable si besoin.
- Pas de page de réglages propre au lanceur (fond, transparence, ville…) : à envisager si demandé.
- Maquettes de design : dans l'artefact « Accueil TV épuré » (planche Claude Design) de la conversation d'origine.

## Sécurité

Le dépôt est **public** : ne jamais écrire de mot de passe, jeton ou clé dans ce fichier, le README ou les messages de commit.


## Graphify : explorer le code sans gaspiller de tokens
Avant grep, glob ou la lecture de fichiers entiers, utilise les outils Graphify :
- où est défini un symbole -> graphify_find, puis graphify_node
- comment marche une fonctionnalité -> query_graph
- qui appelle X -> graphify_callers
- impact d'une modification -> graphify_impact (include_risk=true)
- tests liés -> graphify_tests_for
- dépendances d'un fichier -> graphify_file_neighbors
Compare le commitSha renvoyé avec `git rev-parse HEAD` : identique = fais confiance au résultat, sinon relis seulement les fichiers modifiés.
Garde grep pour du texte précis, la config et les fichiers non indexés.
Ne lis que les lignes utiles d'un fichier, jamais un gros fichier en entier.
