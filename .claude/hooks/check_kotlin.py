#!/usr/bin/env python3
"""
Vérificateur Kotlin (hook PostToolUse) — Samoth Home, lancé après chaque modification d'un .kt.

On ne compile pas en local : ce contrôle attrape avant le push des erreurs qui feraient
échouer la fabrication GitHub (≈ 5 min perdues à chaque fois).
- `by mutableStateOf` / `by remember`… sans les imports getValue / setValue ;
- API Android plus récentes que minSdk 26 utilisées sans test de version ;
- retour à un simple combinedClickable sur les tuiles (bug d'appui long déjà corrigé).
Ajouter `// check-ok` en fin de ligne pour ignorer un faux positif.
Code de sortie 2 = le message est renvoyé à l'assistant pour qu'il corrige.
"""
import json
import re
import sys

# API > 26 fréquentes sur TV : à entourer d'un `if (Build.VERSION.SDK_INT >= …)`
NEWER_API = [
    (r"\.getLongVersionCode\(|\blongVersionCode\b", "versionCode long (API 28)"),
    (r"\bsetShowWhenLocked\(|\bsetTurnScreenOn\(", "setShowWhenLocked / setTurnScreenOn (API 27)"),
    (r"\bPackageManager\.PackageInfoFlags\b|\bResolveInfoFlags\b", "PackageManager.*Flags (API 33)"),
    (r"\bWindowInsets\.Type\b|\bWindowInsetsController\b", "WindowInsetsController (API 30)"),
]


def main():
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)
    path = (data.get("tool_input", {}) or {}).get("file_path", "")
    if not path.endswith(".kt"):
        sys.exit(0)
    try:
        with open(path, encoding="utf-8") as fh:
            src = fh.read()
    except OSError:
        sys.exit(0)
    lines = src.splitlines()
    issues = []

    guarded = "SDK_INT" in src
    for n, line in enumerate(lines, 1):
        if "check-ok" in line:
            continue
        code = line.split("//")[0]
        for rx, msg in NEWER_API:
            if re.search(rx, code) and not guarded:
                issues.append(f"  ligne {n} : {msg} — ajouter un test Build.VERSION.SDK_INT (minSdk 26)")
        if re.search(r"\bContext\.RECEIVER_(NOT_)?EXPORTED\b", code):
            issues.append(f"  ligne {n} : Context.RECEIVER_* (API 33) — utiliser ContextCompat.RECEIVER_* avec ContextCompat.registerReceiver")

    if re.search(r"\bby\s+(mutable\w*StateOf|remember|derivedStateOf|produceState|collectAsState|animate\w+AsState)", src):
        if not re.search(r"import androidx\.compose\.runtime\.(getValue|\*)\b", src):
            issues.append("  import manquant : androidx.compose.runtime.getValue (requis par `by …`)")
        if re.search(r"\bvar\s+\w+(\s*:\s*[\w<>?, ]+)?\s+by\b", src) and not re.search(r"import androidx\.compose\.runtime\.(setValue|\*)\b", src):
            issues.append("  import manquant : androidx.compose.runtime.setValue (requis par `var … by`)")

    # Les tuiles doivent garder okPress (appui long fiable sur TV)
    if path.endswith("ui/Components.kt") and "fun AppTile" in src and ".okPress(" not in src:
        issues.append("  AppTile n'utilise plus .okPress(...) : l'appui long sur OK lancerait l'appli (bug déjà corrigé)")

    if issues:
        short = path.split("/app/src/main/java/")[-1]
        print(f"Vérificateur Kotlin — {short} : à corriger avant le push :\n" + "\n".join(issues), file=sys.stderr)
        sys.exit(2)
    sys.exit(0)


if __name__ == "__main__":
    main()
