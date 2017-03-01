java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline `# This pipeline runs stanford default postagger, lemmatizer, ner and dependency parser` \
    annotators tokenize,ssplit,pos,lemma,ner,depparse \
    tokenize.language en \
    ssplit.eolonly true \
    nthreads 1 \
    | java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline  `# This pipeline uses ner tags from previous steps and annotates entities` \
    preprocess.addDateEntities true \
    preprocess.addNamedEntities true \
    annotators tokenize,ssplit \
    tokenize.whitespace true \
    ssplit.eolonly true \
    nthreads 1 \
    | java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline `# This pipeline runs UD pos tagger` \
    annotators tokenize,ssplit,pos \
    tokenize.whitespace true \
    ssplit.eolonly true \
    languageCode en \
    posTagKey UD \
    pos.model lib_data/ud-models-v1.2/en/pos-tagger/utb-caseless-en-bidirectional-glove-distsim-lower.full.tagger \
    nthreads 1 \
    | java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline `# This pipeline runs semantic parser` \
    annotators tokenize,ssplit \
    tokenize.whitespace true \
    ssplit.eolonly true \
    languageCode en \
    deplambda true \
    deplambda.definedTypesFile lib_data/ud.types.txt \
    deplambda.treeTransformationsFile lib_data/ud-enhancement-rules.proto \
    deplambda.relationPrioritiesFile lib_data/ud-obliqueness-hierarchy.proto  \
    deplambda.lambdaAssignmentRulesFile lib_data/ud-substitution-rules.proto \
    deplambda.lexicalizePredicates true \
    deplambda.debugToFile debug.txt \
    nthreads 1 
