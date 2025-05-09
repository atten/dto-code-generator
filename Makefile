# ===================== BUILD =====================

build:
	./gradlew build


build_image:
	./gradlew jibDockerBuild --image=ez-code-generator:local


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


# ===================== RELEASE ===================

git_set_remotes:
	git remote show | grep github || git remote add github git@github.com:atten/ez-code-generator.git
	git remote show | grep gitlab || git remote add gitlab git@gitlab.com:atten0/ez-code-generator.git

git_clear_remotes:
	git remote show | grep gitlab && git remote remove gitlab || echo
	git remote show | grep github && git remote remove github || echo

git_reset_remotes: git_clear_remotes git_set_remotes

release_major: git_set_remotes
	./bumpversion.sh major

release_minor: git_set_remotes
	./bumpversion.sh minor

release_patch: git_set_remotes
	./bumpversion.sh patch
