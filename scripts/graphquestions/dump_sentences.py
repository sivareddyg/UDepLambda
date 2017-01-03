import json
import sys

for line in sys.stdin:
    sent = json.loads(line)
    print "%s\t%s" %(sent['id'], sent['sentence'].encode("utf-8"))
