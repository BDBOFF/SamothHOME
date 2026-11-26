---
name: regles-lanceur
description: Conventions de Samoth Home (lanceur Android TV) - télécommande, focus, appui long, tuiles verre + halo, couleurs, tailles TV, touche Home. À appliquer dès qu'on crée ou modifie un écran, une tuile, un bouton, un menu ou le service d'accessibilité dans /home/claude/samothhome.
user-invocable: false
---

# Règles de Samoth Home

## Télécommande et focus
- Tout se pilote aux flèches + OK + Retour. Chaque élément cliquable doit montrer clairement son focus (agrandissement ~1.08–1.12, fond blanc ou halo).
- Au changement d'écran ou à la fermeture d'un menu, **redonner le focus** à un élément précis (`FocusRequester`, `vm.focusPkg`, `rememberTileFocus`).
- **Appui long sur OK** : géré par `Modifier.okPress(onClick, onLongClick)` (via `repeatCount`). Ne jamais revenir au seul `combinedClickable`.
- Tout menu ouvert par un appui long passe par `Panel` (Dialogs.kt), qui ignore OK jusqu'au relâchement. Le premier bouton = l'action utile, pas « Ouvrir ».
- Retour : ferme le menu, puis revient à l'accueil ; sur l'accueil, il ne fait rien (on reste sur le lanceur).

## Visuel
- Couleurs : `Samoth.Bg` #07070A, `Samoth.Red` #D10A20, `Samoth.Text`, `Samoth.Muted`, `Samoth.Glass` / `GlassLine`. Pas de couleur en dur hors couleurs propres à une appli.
- Polices : **Unbounded** (fond, signature), **Inter** (tout le reste). Horloge entièrement blanche, pas de point rouge, pas de « Bonsoir ».
- Tuiles : verre transparent, bannière d'origine en plein cadre (`ContentScale.Crop`), halo à la couleur de l'appli au focus.
- Écran TV ≈ 960×540 dp : un texte de 22.sp est déjà grand. Vérifier qu'un ajout tient dans la hauteur de l'accueil.
- Textes visibles en **français**.

## Comportements validés (ne pas casser)
- 5 favoris **fixes** dans l'ordre choisi ; place libre = tuile « + ». Seule « Toutes les applis » est triée par usage.
- Fond : « SAMOTH HOME » à l'allumage, nom de l'appli sélectionnée dès la première flèche (délai ~220 ms), signature en bas à droite.
- `HomeGuardService` : intercepte `KEYCODE_HOME`, ramène sur Samoth Home quand l'accueil Sony s'ouvre (anti-rebond `lastJump`), ne fait rien pendant l'écran de veille, et `ResumeAfterDream` rouvre l'appli en cours à la fin de l'écran de veille.
- Une nouvelle capacité du service d'accessibilité (fichier `res/xml/home_guard.xml`) oblige le propriétaire à le désactiver/réactiver : le prévenir.

## Changements visuels
Avant un changement visuel important, proposer **2–3 maquettes** au propriétaire et attendre son choix.
