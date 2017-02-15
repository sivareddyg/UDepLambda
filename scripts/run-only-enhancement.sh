java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline `# This pipeline runs semantic parser` \
    annotators tokenize,ssplit \
    tokenize.whitespace true \
    ssplit.eolonly true \
    deplambda true \
    deplambda.definedTypesFile lib_data/ud.types.txt \
    deplambda.treeTransformationsFile lib_data/ud-lang-model-enhancement.proto.txt \
    deplambda.relationPrioritiesFile lib_data/ud-relation-priorities.proto.txt \
    deplambda.lambdaAssignmentRulesFile lib_data/ud-lambda-assignment-rules.dummy.txt \
    deplambda.lexicalizePredicates true \
    deplambda.debugToFile debug.txt \
    nthreads 1
