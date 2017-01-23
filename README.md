# UDepLambda

## Install


> wget https://www.dropbox.com/s/43zjcsgs64h3gse/udeplambda-lib.tgz?dl=0

> tar -xvzf udeplambda-lib.tgz

> ant build

## Usage:

> cat input-english.txt | sh run-english.sh

> cat input-spanish.txt | sh run-spanish.sh

> cat input-german.txt | sh run-german.sh

Check debug.txt to see step by step derivation.

## Web Demo (based on old version of rules)

> http://sivareddy.in/deplambda.html

## Important files

* Type System: lib_data/ud.types.txt 
* Refinement Step: lib_data/ud-tree-transformation-rules.proto.txt
* Binarization Step: lib_data/ud-relation-priorities.proto.txt 
* Substitution Step: lib_data/ud-lambda-assignment-rules.proto.txt

## Documentation
See SimplifiedLogicForm.md(doc/SimplifiedLogicForm.md) for output formats.
