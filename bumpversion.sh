#!/usr/bin/env sh

if [ -z $1 ]; then
  echo "usage: $0 major|minor|patch";
  exit 1
fi

git checkout master --quiet

CURRENT_VERSION=$(bump-my-version show --format json | jq '.current_version' --raw-output)
bump-my-version bump $1
NEW_VERSION=$(bump-my-version show --format json | jq '.current_version' --raw-output)

echo "Bump version: $CURRENT_VERSION → $NEW_VERSION"

# create release branch
git branch "release_$NEW_VERSION"
git checkout "release_$NEW_VERSION"

# push release branch and trigger CI
git push --set-upstream gitlab "release_$NEW_VERSION"
git push --set-upstream github "release_$NEW_VERSION"

# push master branch and tags
git checkout master
git push --tags gitlab
git push --tags github
