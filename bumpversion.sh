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

# publish new docker image on gitlab
git push --set-upstream origin "release_$NEW_VERSION"
git push --set-upstream github "release_$NEW_VERSION"

# publish new archive on gitlab
git checkout master
git push origin
git push github

echo "Publish new release on github manually!"