import sys
import json
import random
import os
import sqlite3
import re

conn = sqlite3.connect(sys.argv[1])
c = conn.cursor()

# Create table
results = c.execute('''select qid, sentence, translation FROM sentences''')

for result in results:
    print "%d\t%s\t%s" %(result[0], result[1].encode("utf-8").replace("\n", " "), result[2].encode("utf-8").replace("\n", " "))
conn.close()  
