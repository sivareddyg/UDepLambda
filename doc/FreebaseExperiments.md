# Freebase Experiments

## Setting up Freebase

Let's setup Freebase server first.

1. Install virtuso-6 not virtuoso-7. See http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSUbuntuNotes
2. Download our Freebase version at https://www.dropbox.com/sh/zxv2mos2ujjyxnu/AAACCR4AJ1MMTCe8ElfBN39Ha?dl=0
3. In a terminal, cd to the folder which you just downloaded
4. Run "pwd"
5. Replace /dev/shm/vdb/ in virtuoso.ini with the output of Step 4.
6. Run "virtuoso-t -f"

## Multiligual WebQuestions and GraphQuestions Data

1. cd UDepLambda
2. git submodule update --init --recursive data/WebQuestions
3. git submodule update --init --recursive data/GraphQuestions

### Data Format

#### WebQuestions

The main files are in `data/WebQuestions/<LANG>/<nlpPipline>-webquestions.<split>.deplambda.json`. For example, consider the training split `data/WebQuestions/en/en-bilty-bist-webquestions.train.deplambda.json`. This file is the end product of several processing steps such as tokenization, pos tagging, entity annotation, entity disambiguation, dependency parsing, UDepLambda ungrounded semantic parsing.

Each line in the data is of the json format. 

    {sentence:"where does luke skywalker live in star wars?", "forest": [hypothesis1, hypothesis2, hypothesis3]"} 

Each hypothesis represents a different entity disambiguation possibilities. In one hypothesis "luke skywalker" and "star wars" may be treated as entities with thier Freebase ids m.0f1bg and m.0dtfn, and in the other hypothesis "luke skywalker" and "family guy" may have m.abc and m.123, and yet in the one hypothesis "star wars" may not be treated as an entity, but only "skywalker" as m.iamskywakler. 

Now lets consider hypothesis1. This is of the format

```
    "forest": [
       {"words": [wordObject1, wordObject2, ...], 
         "entities": [entityObj1, entityObj2],
         "dependency_lambda": "....."
       }
      ]
 ```
    
Words are of the form:

``` 
    "words": [
       {"index" : 1, "head": "4", "word": "where", "dep": "advmod", "pos": "ADV", "lemma": "where"}, 
       {"index": 2, "head": "4", "word": "does", "dep": "aux", "pos": "AUX", "lemma": "do"}, 
       {"index": 3, "head": "4", "word": "lukeskywalker", "dep": "nsubj", "pos": "PROPN", "lemma": "LukeSkywalker"}, 
       {"index": 4, "head": "0", "word": "live", "dep": "root", "pos": "VERB", "lemma": "live"}, 
       {"index": 5, "head": "6", "word": "in", "dep": "case", "pos": "ADP", "lemma": "in"}, 
       {"index": 6, "head": "4", "word": "starwars", "dep": "nmod", "pos": "PROPN", "lemma": "StarWars"}, 
       {"index": 7, "head": "4", "word": "?", "dep": "punct", "pos": "PUNCT", "sentEnd": true, "lemma": "?"}
   ]
```
* Note here LukeSkywalker is combined as single word, and so is StarWars.

Entities are of the form:

```
    "entities": [
          {"index": 2, "name": "Luke Skywalker", "entity": "m.0f1bg", "score": 70.13603026346405, "phrase": "luke skywalker"}, 
          {"index": 5, "name": "Star Wars", "entity": "m.0dtfn", "start": 5, "score": 51.616535960759244, "phrase": "star wars"}
     ]
```

* Here index corresponds to the word position in the words list above.

The `dependency_lambda` key is used for storing the ungrounded logical form from UDepLamda. For this sentence, the parse is
```
   "dependency_lambda": [
          ["live.advmod(3:e , 0:x)", "QUESTION(0:x)", "live.arg1(3:e , 2:m.lukeskywalker)", "where(0:s , 0:x)", "live.nmod.in(3:e , 5:m.starwars)"]
      ]
```

## Paper Results replication

### WebQuestions

#### Single Event results
This is the simplest model to run. So let's start with this.

Get Oracle Graphs

    make extract_gold_graphs_bow_en-bilty-bist
     
Check the oracle score.
        
    python scripts/evaluation.py data/gold_graphs/en-bilty-bist_bow_without_merge_without_expand.full.answers.txt

* You should get a score around 0.7254 for `Average f1 over questions`.

Train the model.

    make bow_supervised_without_merge_without_expand_en-bilty-bist

* This will store all the output files in the directory `../working/webquestions/en-bilty-bist/bow_supervised_without_merge_without_expand`. 

* You can see the scores of each iteration in `../working/wq/en-bilty-bist/bow_supervised_without_merge_without_expand/all.log.txt.eval.iteration<N>`. 

* The best iteration scores are stored in `../working/wq/en-bilty-bist/bow_supervised_without_merge_without_expand/all.log.txt.eval.bestIteration`.

* If you want the answers of your best iteration model, you can see them in `../working/wq/en-bilty-bist/bow_supervised_without_merge_without_expand/all.log.txt.eval.dev.bestIteration.1best.answers.txt` and corresponding test file is in `../working/wq/en-bilty-bist/bow_supervised_without_merge_without_expand/all.log.txt.eval.test.bestIteration.1best.answers.txt`

To evaluate the resulsts yourself, run

    python scripts/evaluation.py ../working/wq/en-bilty-bist/bow_supervised_without_merge_without_expand/all.log.txt.eval.test.bestIteration.1best.answers.txt
    
You should see numbers close to the ones reported in Table 3 of the [paper](https://arxiv.org/pdf/1702.03196.pdf)

For results in other langauges, replace "en" in all of the above commands with "es" and "de". For results with stanford pos tagger and dependency parser, replace "bilty" with "stanford" and "bist" with "stanford".

### UDepLambda model

TODO: commands

TODO: share output files.

## Running your own pipeline

### Entity Annotation

### Running UDepLambda

### Running Oracle
