java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline `# This pipeline runs stanford ner` \
    annotators tokenize,ssplit,ner \
    tokenize.language es \
    ner.model edu/stanford/nlp/models/ner/spanish.ancora.distsim.s512.crf.ser.gz \
    ner.applyNumericClassifiers false \
    ner.useSUTime false \
    nthreads 1 \
    | java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline  `# This pipeline uses ner tags from previous steps and annotates entities` \
    preprocess.addDateEntities true \
    preprocess.addNamedEntities true \
    annotators tokenize,ssplit \
    tokenize.whitespace true \
    ssplit.eolonly true \
    nthreads 1 \
    | java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline `# This pipeline runs UD pos tagger and UD dependency parser` \
    preprocess.lowerCase true \
    annotators tokenize,ssplit,pos,depparse \
    tokenize.whitespace true \
    ssplit.eolonly true \
    languageCode es \
    posTagKey UD \
    pos.model lib_data/ud-models-v1.3/es/pos-tagger/utb-caseless-es-bidirectional-glove-distsim-lower.tagger \
    depparse.model lib_data/ud-models-v1.3/es/neural-parser/es-lowercase-glove50.lower.nndep.model.txt.gz \
    nthreads 1 \
    | java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline `# This pipeline runs semantic parser` \
    annotators tokenize,ssplit \
    tokenize.whitespace true \
    ssplit.eolonly true \
    languageCode es \
    deplambda true \
    deplambda.definedTypesFile lib_data/ud.types.txt \
    deplambda.treeTransformationsFile lib_data/ud-tree-transformation-rules.proto.txt \
    deplambda.relationPrioritiesFile lib_data/ud-relation-priorities.proto.txt \
    deplambda.lambdaAssignmentRulesFile lib_data/ud-lambda-assignment-rules.proto.txt \
    deplambda.lexicalizePredicates true \
    deplambda.debugToFile debug.txt \
    nthreads 1
