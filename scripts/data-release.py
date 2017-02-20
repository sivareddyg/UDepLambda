# {"function": "count", "sparql_query": "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX : <http://rdf.freebase.com/ns/> \nSELECT (COUNT(?x0) AS ?value) WHERE {\nSELECT DISTINCT ?x0  WHERE { \n?x0 :type.object.type :broadcast.tv_station . \n?x1 :type.object.type :broadcast.tv_station_owner . \nVALUES ?x2 { :en.msnbc } \n?x0 :broadcast.tv_station.owner ?x1 . \n?x1 :broadcast.tv_station_owner.tv_stations ?x2 . \nFILTER ( ?x0 != ?x1 && ?x0 != ?x2 && ?x1 != ?x2  )\n}\n}", "sentence": "\u00bfCu\u00e1ntas estaciones de televisi\u00f3n posee el due\u00f1o de msnbc?", "original": "how many tv stations does the owner of msnbc possess?", "answerF1": ["29"], "goldMids": ["m.0152x_"], "id": 216000100}
# webq: {"index": 2, "sentence": "en que zona horaria estoy en cleveland ohio?", "url": "http://www.freebase.com/view/en/cleveland_ohio", "targetValue": "(list (description \"North American Eastern Time Zone\"))", "goldMid": "m.01sn3", "original": "what time zone am i in cleveland ohio?"}

import json
import sys

for line in sys.stdin:
    sent = json.loads(line)
    if 'index' in sent:
        del sent['index']
    if 'goldRelations' in sent:
        del sent['goldRelations']
    if 'goldMid' in sent:
        del sent['goldMid']
    if 'goldMids' in sent:
        del sent['goldMids']
    if 'function' in sent:
        del sent['function']
    print json.dumps(sent)
