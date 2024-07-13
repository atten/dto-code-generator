lint:
	./gradlew ktlintCheck

lint_format:
	./gradlew ktlintFormat

test:
	./gradlew test

test_generated_code:
	cd generatedCodeTests && ./run_all.sh
