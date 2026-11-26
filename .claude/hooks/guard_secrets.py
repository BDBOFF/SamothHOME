#!/usr/bin/env python3
"""
Garde-fou anti-secrets et anti-casse (hook PreToolUse de Claude Code) — Samoth Home.

Le dépôt est PUBLIC. On bloque :
- l'écriture ou le commit d'un nouveau secret (jeton, clé privée, clé API, e-mail perso) ;
- toute modification ou suppression du fichier de signature app/samoth-home.jks
  (le changer casserait les mises à jour sur la TV : il faudrait désinstaller et tout perdre).

Note : la config de signature de app/build.gradle.kts est déjà dans le dépôt (décision assumée).
On ne regarde donc que les lignes AJOUTÉES, pour ne pas bloquer chaque commit sur l'existant.
Code de sortie 2 = action bloquée, le message est renvoyé à l'assistant.
"""
import hashlib
import json
import os
import re
import subprocess
import sys

# Empreintes SHA-256 de valeurs à ne jamais écrire (les valeurs elles-mêmes ne figurent pas ici)
KNOWN_SECRET_HASHES = {
    "75219cf0f705ce2bce7f7ac1394f6650f131b80153e88fafdc4c49b0fb63e802",  # adresse e-mail personnelle du propriétaire
    "1807d49789777a833ab07b1ce86752347ebb53b1b20e4f4be7e0b13ffe6f03a6",  # clé TMDB de Samoth TV
}

PATTERNS = [
    (r"-----BEGIN [A-Z ]*PRIVATE KEY-----", "clé privée"),
    (r"\bgh[pousr]_[A-Za-z0-9]{30,}\b|\bgithub_pat_[A-Za-z0-9_]{30,}", "jeton GitHub"),
    (r"\bAIza[0-9A-Za-z_\-]{30,}", "clé API Google"),
    (r"\bsk_(live|test)_[A-Za-z0-9]{16,}", "clé secrète de paiement"),
    (r"(?i)(api[_-]?key|token|secret)\s*[:=]\s*\"[A-Za-z0-9_\-]{20,}\"", "clé ou jeton écrit en clair"),
    (r"SAMOTH-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}", "code de licence Samoth TV"),
]

KEYSTORE = "app/samoth-home.jks"
OTHER_SECRET_FILE = re.compile(r"(?i)\.(keystore|p12|pfx|pem)$|(^|/)(local\.properties|\.env)$")
ALLOWED_FILES = (".claude/hooks/guard_secrets.py",)


def find_problems(text: str) -> list:
    problems = [label for rx, label in PATTERNS if re.search(rx, text)]
    tokens = set(re.findall(r"[A-Za-z0-9_\-]{6,64}", text)) | set(re.findall(r"[\w.+-]+@[\w-]+\.[\w.]+", text))
    if any(hashlib.sha256(t.encode()).hexdigest() in KNOWN_SECRET_HASHES for t in tokens):
        problems.append("valeur privée connue (e-mail ou clé)")
    return problems


def git(cwd, *args) -> str:
    try:
        return subprocess.run(["git", "-C", cwd, *args], capture_output=True, text=True, timeout=20).stdout
    except Exception:
        return ""


def added_lines(diff: str) -> str:
    """Garde seulement les lignes ajoutées d'un diff, hors fichier de garde lui-même."""
    out, skip = [], False
    for line in diff.splitlines():
        if line.startswith("diff --git "):
            skip = "guard_secrets.py" in line
            continue
        if not skip and line.startswith("+") and not line.startswith("+++"):
            out.append(line[1:])
    return "\n".join(out)


def pending(cwd: str):
    """Ce qu'un `git add -A && git commit` enverrait."""
    text = added_lines(git(cwd, "diff", "HEAD"))
    bad = []
    for f in git(cwd, "ls-files", "--others", "--exclude-standard").splitlines():
        if f.endswith(ALLOWED_FILES):
            continue
        if OTHER_SECRET_FILE.search(f) or (f.endswith(".jks") and f != KEYSTORE):
            bad.append(f)
            continue
        try:
            p = os.path.join(cwd, f)
            if os.path.getsize(p) < 1_000_000:
                with open(p, encoding="utf-8", errors="ignore") as fh:
                    text += "\n" + fh.read()
        except OSError:
            pass
    # Le keystore ne doit jamais changer ni disparaître
    status = git(cwd, "status", "--porcelain", "--", KEYSTORE).strip()
    if status:
        bad.append(f"{KEYSTORE} modifié ou supprimé")
    return text, bad


def block(reasons):
    print(
        "BLOQUÉ par le garde-fou de Samoth Home (dépôt public) : " + ", ".join(sorted(set(reasons))) + ".\n"
        "Retire la valeur sensible, ou remets le fichier de signature tel quel "
        f"(`git checkout -- {KEYSTORE}`) : le changer empêcherait les mises à jour sur la TV.",
        file=sys.stderr,
    )
    sys.exit(2)


def main():
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)
    tool = data.get("tool_name", "")
    ti = data.get("tool_input", {}) or {}
    cwd = data.get("cwd") or os.getcwd()

    if tool in ("Write", "Edit", "MultiEdit"):
        path = ti.get("file_path", "")
        if path.endswith(ALLOWED_FILES):
            sys.exit(0)
        if path.endswith(KEYSTORE):
            block(["modification du fichier de signature"])
        text = ti.get("content", "") + ti.get("new_string", "")
        for e in ti.get("edits", []) or []:
            text += "\n" + e.get("new_string", "")
        problems = find_problems(text)
        if problems:
            block(problems)

    elif tool == "Bash":
        cmd = ti.get("command", "")
        if re.search(r"samoth-home\.jks", cmd) and re.search(r"\b(rm|mv|keytool|truncate|shred)\b|git\s+rm|>\s*\S*samoth-home\.jks", cmd):
            block(["commande qui toucherait au fichier de signature"])
        if re.search(r"git\s+add\b[^\n;&|]*\s-f\b", cmd):
            block(["ajout forcé d'un fichier ignoré"])
        if re.search(r"\bgit\b[^\n]*\b(commit|push)\b", cmd):
            text, bad = pending(cwd)
            if "push" in cmd:
                text += "\n" + added_lines(git(cwd, "log", "-p", "@{u}..HEAD"))
            reasons = find_problems(text) + bad
            if reasons:
                block(reasons)
    sys.exit(0)


if __name__ == "__main__":
    main()
