#!/usr/bin/env python
# -*- coding: utf-8 -*-

'''
Created on 12 Jul 2016

@author: siva
'''

import re
import sys

for line in sys.stdin:
    line = line.decode("utf-8", "ignore")
    line = line.strip().lower()
    line = line.strip(u"¿")
    line = re.sub("[\,\"\:]", "", line)
    print line.encode("utf-8")