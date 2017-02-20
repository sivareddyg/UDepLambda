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
