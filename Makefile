lint:
	./gradlew ktlintCheck

lint_format:
	./gradlew ktlintFormat

test:
	./gradlew test

test_generated_code:
	cd generatedCodeTests && bash run_all.sh
