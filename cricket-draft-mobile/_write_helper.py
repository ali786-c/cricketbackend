
import sys, json

def write_file(path, content):
    with open(path, "w", newline="
") as f:
        f.write(content)
    print(f"Written {len(content)} chars to {path}")

# Read the JSON-encoded content from stdin
data = json.loads(sys.stdin.read())
write_file(data["path"], data["content"])
