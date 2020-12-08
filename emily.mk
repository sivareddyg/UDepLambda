process_data_%:
	cat data/en_mcd$*-ud-test.conllu \
	| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.ConvertConllToGraphParserSentence \
	| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline `# This pipeline runs stanford default postagger, lemmatizer, ner and dependency parser` \
    annotators tokenize,ssplit,pos,lemma,ner \
    languageCode en \
    tokenize.whitespace true \
    ssplit.eolonly true \
    nthreads 1 \
    | java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline  `# This pipeline uses ner tags from previous steps and annotates entities` \
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
	| python scripts/fix_errors_mcd.py \
	> data/en_mcd$*-ud-test.json

print_conll:
	cat data/en_mcd2-ud-test.json \
	| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.ConvertGraphParserSentenceToConll \
	> data/en_mcd2-ud-test.parsed.conllu

run_deplambda:
	cat data/en_mcd2-ud-test.json \
    	| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline `# This pipeline runs semantic parser` \
    annotators tokenize,ssplit \
    tokenize.whitespace true \
    ssplit.eolonly true \
    languageCode en \
    deplambda true \
    deplambda.definedTypesFile lib_data/ud.types.txt \
    deplambda.treeTransformationsFile lib_data/udv2-enhancement-rules.proto \
    deplambda.relationPrioritiesFile lib_data/udv2-obliqueness-hierarchy.proto  \
    deplambda.lambdaAssignmentRulesFile lib_data/udv2-substitution-rules.proto \
    deplambda.lexicalizePredicates true \
    deplambda.debugToFile debug.txt \
    nthreads 1 \
	> data/en_mcd2-ud-test.deplambda.json
