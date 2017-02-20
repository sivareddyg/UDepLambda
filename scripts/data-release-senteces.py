import json
import sys

for line in sys.stdin:
    sent = json.loads(line)
    print "%d\t%s" %(sent['index'], sent['sentence'].encode('utf-8'))
