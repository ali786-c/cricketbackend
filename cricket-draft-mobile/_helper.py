
import sys, os
path = sys.argv[1]
content = sys.stdin.read()
with open(path, 'w', newline='
') as f:
    f.write(content)
print(f'Written {len(content)} bytes to {path}')
