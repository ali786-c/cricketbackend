import base64, sys
b64 = sys.stdin.read().strip()
content = base64.b64decode(b64).decode("utf-8")
path = "app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/TournamentSetupViewModel.kt"
with open(path, "w") as fh:
    fh.write(content)
print(f"Written {len(content)} chars")
