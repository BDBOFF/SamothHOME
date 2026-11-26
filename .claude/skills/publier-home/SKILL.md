---
name: publier-home
description: Publie une modification de Samoth Home de bout en bout - relecture, commit, push sur main, surveillance de la fabrication GitHub jusqu'au vert, vérification de la release, mise à jour de CLAUDE.md, message au propriétaire. À utiliser à la fin de chaque modification, sans attendre qu'on le demande.
---

# /publier-home — envoyer une modification jusqu'à l'APK

C'est la règle permanente de CLAUDE.md, faite toujours de la même façon. Dépôt : `/home/claude/samothhome`, GitHub `BDBOFF/SamothHOME`.

## 1. Relire
- Lance le subagent **relecteur-home** sur les changements et corrige tout ce qu'il signale comme bloquant.
- Le hook `check_kotlin.py` doit être muet sur les `.kt` modifiés.

## 2. Commit et push sur main
```bash
cd /home/claude/samothhome && git pull -q --rebase origin main
git add -A
git -c user.name="BDBOFF" -c user.email="BDBOFF@users.noreply.github.com" commit -q -m "<résumé en français>

<lignes d'attribution demandées par la session>"
git push -q origin main
```
Le message « This repository moved » au push est sans conséquence (casse du nom).
Le hook `guard_secrets.py` bloque s'il voit un secret ou une modification du keystore : retirer la cause, ne jamais le contourner.

## 3. Surveiller la fabrication
Un push qui ne touche que des `.md` ou le dossier `.claude/` ne lance **pas** de fabrication : passer à l'étape 6.
```bash
sleep 10
id=$(gh api "repos/BDBOFF/SamothHOME/actions/runs?branch=main&per_page=1" --jq '.workflow_runs[0].id')
gh api repos/BDBOFF/SamothHOME/actions/runs/$id --jq '"\(.head_sha[0:7]) \(.run_number)"'   # doit être ton commit
gh run watch $id -R BDBOFF/SamothHOME --exit-status --interval 20 > /dev/null 2>&1; echo "exit=$?"
```
(GraphQL est bloqué ici : `gh run watch` marche, `gh release download` non.)

## 4. Si ça échoue
```bash
gh api repos/BDBOFF/SamothHOME/actions/runs/$id/jobs --jq '.jobs[0].steps[] | "\(.name): \(.conclusion)"'
j=$(gh api repos/BDBOFF/SamothHOME/actions/runs/$id/jobs --jq '.jobs[0].id')
gh api repos/BDBOFF/SamothHOME/check-runs/$j/annotations --jq '.[] | "\(.path):\(.start_line) \(.message)"' | grep -iv "node\|deprecat\|ubuntu"
```
- « Télécharger les polices » en échec : souci réseau passager → relancer (`gh api -X POST repos/BDBOFF/SamothHOME/actions/runs/$id/rerun`).
- « Compiler l'APK » en échec : erreur Kotlin → relire le diff (imports, smart cast, signatures), corriger, revenir à l'étape 2. Jusqu'au vert.

## 5. Vérifier la release
```bash
gh api repos/BDBOFF/SamothHOME/releases/latest --jq '"\(.tag_name) \(.assets[0].name) \(.assets[0].size)"'
```
Attendu : `build-<numéro du run>` avec `SamothHome.apk`.

## 6. Mettre à jour CLAUDE.md
Historique des versions (ligne `build-N`), décisions et leur raison, bugs corrigés, pièges, reste à faire. Commit + push (pas de fabrication pour un `.md`).

## 7. Prévenir le propriétaire
En français simple et court : ce qui change pour lui, le numéro `build-N`, et qu'il verra la pastille « Mise à jour disponible » sur l'accueil. S'il faut réactiver le service d'accessibilité après la mise à jour, le dire clairement.
