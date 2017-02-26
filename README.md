# UDepLambda

UDepLambda is a framework to convert Universal Dependencies to Logical Forms. 

For more details, please refer to our paper:

[Universal Semantic Parsing](https://arxiv.org/pdf/1702.03196)  
Siva Reddy, Oscar Täckström, Slav Petrov, Mark Steedman, Mirella Lapata  
arXiv:1702.03196, 2017.

## Install

> git clone https://github.com/sivareddyg/UDepLambda.git

> cd UDepLambda

> git submodule update --init --recursive lib

> ant build

## Usage:

> cat input-english.txt | sh run-english.sh

> cat input-spanish.txt | sh run-spanish.sh

> cat input-german.txt | sh run-german.sh

Check debug.txt to see step by step derivation.

## Web Demo (based on old version of rules)

> http://sivareddy.in/deplambda.html

## Important files

* [Type System](lib_data/ud.types.txt)
* [Enhancement Step Rules](lib_data/ud-enhancement-rules.proto)
* [Binarization Step Rules](lib_data/ud-obliqueness-hierarchy.proto)
* [Substitution Step Rules](lib_data/ud-substitution-rules.proto)

## Documentation
* See [FreebaseExperiments.md](doc/FreebaseExperiments.md) for Freebase semantic parsing experiments.
* See [SimplifiedLogicForm.md](doc/SimplifiedLogicForm.md) for output formats.
* See [doc](doc/) for additional documentation.

## Reference

```
@article{reddy2017universal,
  title={Universal Semantic Parsing},
  author={Reddy, Siva and T{\"a}ckstr{\"o}m, Oscar and Petrov, Slav and Steedman, Mark and Lapata, Mirella},
  journal={arXiv preprint arXiv:1702.03196},
  year={2017}
  url = {https://arxiv.org/pdf/1702.03196.pdf}
}

```
