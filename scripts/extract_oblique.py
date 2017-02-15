import json
import sys

for line in sys.stdin:
    sent = json.loads(line)
    print sent['deplambda_oblique_tree']
