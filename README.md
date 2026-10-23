# Samoth Home — lanceur épuré pour Android TV

Écran d'accueil minimaliste pour Android TV / Google TV, aux couleurs de Samoth TV (noir et rouge).

- **Allumage** : grande horloge, date, et « SAMOTH HOME » en fond, gras et transparent
- **Navigation** : le fond affiche le nom de l'appli sélectionnée dans sa couleur, petite signature « SAMOTH HOME » en bas à droite
- **5 favoris** fixes, dans l'ordre choisi, en tuiles de verre avec leur vraie icône et un halo lumineux à la sélection
- **Toutes les applis** : grille qui défile, triée par utilisation
- **Roue crantée** : accès direct aux réglages de la TV
- **Mise à jour intégrée** : une pastille « Mise à jour disponible » apparaît à côté de la roue crantée dès qu'une nouvelle version est publiée ici

## Télécharger

Lien fixe vers la dernière version :
**https://github.com/BDBOFF/SamothHOME/releases/latest/download/SamothHome.apk**

Chaque modification envoyée sur ce dépôt est compilée automatiquement (onglet *Actions*) et publiée dans *Releases*.

## Installer sur la TV (Sony KE-65A8)

1. Sur la TV, installez l'appli **Downloader** depuis le Play Store.
2. Dans Downloader, saisissez le lien ci-dessus (ou son code court), puis **Installer**.
   Si la TV le demande, autorisez Downloader à installer des applis inconnues.
3. Ouvrez Samoth Home une première fois depuis la liste des applis.

Alternative : copiez `SamothHome.apk` sur une clé USB et ouvrez-le avec un gestionnaire de fichiers.

## En faire l'écran d'accueil (une seule fois)

**Méthode recommandée (sans ordinateur)** — au premier lancement, Samoth Home affiche un message
« Faire de Samoth Home votre accueil ». Choisissez **Ouvrir l'accessibilité**, puis
**Samoth Home – touche Home → Activer**.
Ce service ne lit rien à l'écran : il détecte seulement l'ouverture de l'accueil Sony et ramène aussitôt sur Samoth Home
(touche Home, rallumage, retour d'une appli). Pour revenir à l'accueil Sony, désactivez-le au même endroit.

Chemin manuel sur Sony : Paramètres → Préférences de l'appareil → Accessibilité → Samoth Home – touche Home.

**Méthode ADB (optionnelle, sans le bref passage par l'accueil Sony)** — depuis votre téléphone :

1. TV : Paramètres → Préférences de l'appareil → À propos → appuyez 7 fois sur **Version** (active les options pour les développeurs).
2. TV : Paramètres → Préférences de l'appareil → Options pour les développeurs → activez **Débogage USB / réseau**. Notez l'adresse IP de la TV (Paramètres → Réseau).
3. Téléphone : installez une appli ADB (par exemple *Remote ADB Shell* sur Android, ou *ADB* depuis un ordinateur), connectez-vous à `IP-de-la-TV:5555` et acceptez la demande sur la TV.
4. Tapez :

```
cmd package set-home-activity com.samoth.home/.MainActivity
pm disable-user --user 0 com.google.android.tvlauncher
```

La deuxième ligne met en sommeil l'accueil d'origine (sans le supprimer). Pour revenir en arrière :

```
pm enable com.google.android.tvlauncher
```

Ensuite, touche Home, rallumage après la veille et redémarrage ramènent toujours sur Samoth Home.

## Choisir les applis de l'accueil

- **Toutes les applis** → placez-vous sur une appli → **maintenez OK** → *Mettre sur l'accueil*.
  Si l'accueil est plein, choisissez quelle appli remplacer.
- Sur l'accueil, **maintenez OK** sur une tuile → *Déplacer à gauche / à droite* ou *Retirer de l'accueil*.
- L'accueil ne bouge jamais tout seul : les tuiles restent dans l'ordre que vous avez choisi. Une place libre affiche un « + » qui ouvre Toutes les applis.

## Gérer les applis

Maintenez OK sur n'importe quelle tuile :
- **Forcer l'arrêt** : ferme l'appli pour qu'elle reparte à neuf au prochain lancement (utile si elle rame ou bloque après la veille).
- **Désinstaller** : pour les applis que vous avez installées (confirmation d'Android).
- **Désactiver** : pour les applis intégrées par Sony/Google, ouvre leur fiche où se trouve le bouton *Désactiver*.
- **Infos de l'appli** : fiche complète (Forcer l'arrêt, Vider le cache, Stockage…).

Dans **Toutes les applis**, le bouton **Tout fermer** ferme d'un coup toutes les applis restées en mémoire.

## Mise à jour

Quand une nouvelle version est publiée, la pastille rouge **Mise à jour disponible** apparaît en haut à droite.
Sélectionnez-la : l'APK se télécharge et l'installation se lance, il suffit de confirmer.
La première fois, la TV peut demander d'autoriser Samoth Home à installer des applis : autorisez, puis resélectionnez la pastille.

## Technique

Kotlin 2.1 · Jetpack Compose · minSdk 26 (Android 8). Polices Unbounded et Inter (licence OFL) téléchargées à la compilation.
APK signé avec `app/samoth-home.jks` (toujours la même clé, pour que les mises à jour s'installent par-dessus).
