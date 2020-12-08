import json
import sys

pos_correct = {"influence": "VERB", "edit": "VERB", "star": "VERB"}

for line in sys.stdin:
	sent = json.loads(line)
	for word in sent['words']:
		word['dep'] = word['dep'].lower()
		word['pos'] = pos_correct.get(word['word'], word['pos'])
	print(json.dumps(sent))
