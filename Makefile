# ===================== BUILD =====================

build:
	./gradlew build


build_image:
	./gradlew jibDockerBuild --image=dto-codegen:local


# ===================== LOCAL ENV =================

run_sidecars_for_local_tests_of_generated_code:
	cd generatedCodeTests/sidecars && docker compose up -d --build

logs_sidecars:
	cd generatedCodeTests/sidecars && docker compose logs -f --tail=100


# ===================== LINT ======================

lint:
	./gradlew ktlintCheck

lint_format:
	./gradlew ktlintFormat


# ===================== TESTS =====================

test:
	./gradlew test

test_generated_code:
	cd generatedCodeTests && ./run_all.sh

test_all: test test_generated_code


# ===================== PUBLISH ===================

release_major:
	./bumpversion.sh major

release_minor:
	./bumpversion.sh minor

release_patch:
	./bumpversion.sh patch
