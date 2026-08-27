import base64, sys
b64data = open(sys.argv[1]).read()
content = base64.b64decode(b64data).decode("utf-8")
with open(sys.argv[2], "w", newline="
") as out:
    out.write(content)
print(f"Written {len(content)} chars")
