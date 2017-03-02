webq_%:
	mkdir -p webquestions-translations
	python scripts/data-release.py \
		< data/webquestions/$*/webquestions.dev.json \
		> webquestions-translations/$*.dev.json
	python scripts/data-release.py \
		< data/webquestions/$*/webquestions.train.json \
		> webquestions-translations/$*.train.json
	python scripts/data-release.py \
		< data/webquestions/$*/webquestions.test.json \
		> webquestions-translations/$*.test.json

sentences_webq_%:
	mkdir -p working/webquestions-translations
	python scripts/data-release-senteces.py \
		< data/webquestions/$*/webquestions.train.json \
		| sort -n \
		| cut -f2 \
		> working/webquestions-translations/$*.train.txt
	python scripts/data-release-senteces.py \
		< data/webquestions/$*/webquestions.dev.json \
		| sort -n \
		| cut -f2 \
		> working/webquestions-translations/$*.dev.txt
	python scripts/data-release-senteces.py \
		< data/webquestions/$*/webquestions.test.json \
		| sort -n \
		| cut -f2 \
		> working/webquestions-translations/$*.test.txt

sentence_ids:
	python scripts/data-release-sentece-ids.py data/WebQuestions/original/webquestions.examples.train.json working/webquestions-translations/en.train.txt > working/webquestions-translations/en.train.ids.txt
	python scripts/data-release-sentece-ids.py data/WebQuestions/original/webquestions.examples.test.json working/webquestions-translations/en.test.txt > working/webquestions-translations/en.test.ids.txt
	python scripts/data-release-sentece-ids.py data/WebQuestions/original/webquestions.examples.train.json working/webquestions-translations/en.dev.txt > working/webquestions-translations/en.dev.ids.txt
