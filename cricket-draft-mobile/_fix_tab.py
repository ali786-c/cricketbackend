import os
path = "app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/TournamentHomeTab.kt"
with open(path, "r") as fh:
    c = fh.read()
print(f"Read {len(c)} chars")
