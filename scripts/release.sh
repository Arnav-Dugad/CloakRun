#!/usr/bin/env bash
# Cut a CloakRun release.
#
#   scripts/release.sh patch|minor|major
#
# Bumps VERSION_NAME and VERSION_CODE in version.properties, moves the "Unreleased"
# section of CHANGELOG.md under the new version heading, commits, tags vX.Y.Z and
# pushes main and the tag. The tag triggers .github/workflows/release.yml, which
# builds, signs and publishes the GitHub Release.
set -euo pipefail

bump="${1:-}"
case "$bump" in
  patch|minor|major) ;;
  *) echo "usage: scripts/release.sh patch|minor|major" >&2; exit 64 ;;
esac

root="$(git rev-parse --show-toplevel)"
cd "$root"

branch="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$branch" != "main" ]]; then
  echo "Releases are cut from main (current: $branch)." >&2
  exit 1
fi
if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree is not clean. Commit or stash first." >&2
  exit 1
fi
git pull --ff-only --quiet

props="version.properties"
current_name="$(sed -n 's/^VERSION_NAME=//p' "$props" | tr -d '\r')"
current_code="$(sed -n 's/^VERSION_CODE=//p' "$props" | tr -d '\r')"
IFS=. read -r major minor patch <<<"$current_name"

case "$bump" in
  major) major=$((major + 1)); minor=0; patch=0 ;;
  minor) minor=$((minor + 1)); patch=0 ;;
  patch) patch=$((patch + 1)) ;;
esac

new_name="$major.$minor.$patch"
new_code=$((current_code + 1))
tag="v$new_name"

if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
  echo "Tag $tag already exists." >&2
  exit 1
fi

if ! grep -q '^## \[Unreleased\]' CHANGELOG.md; then
  echo "CHANGELOG.md has no '## [Unreleased]' section." >&2
  exit 1
fi

unreleased_body="$(awk '/^## \[Unreleased\]/{flag=1; next} /^## \[/{flag=0} flag' CHANGELOG.md | sed '/^[[:space:]]*$/d')"
if [[ -z "$unreleased_body" ]]; then
  echo "The Unreleased section of CHANGELOG.md is empty. Document the release first." >&2
  exit 1
fi

today="$(date -u +%Y-%m-%d)"
tmp="$(mktemp)"
awk -v heading="## [$new_name] - $today" '
  /^## \[Unreleased\]/ { print; print ""; print heading; next }
  { print }
' CHANGELOG.md >"$tmp"
mv "$tmp" CHANGELOG.md

sed -i.bak -e "s/^VERSION_NAME=.*/VERSION_NAME=$new_name/" -e "s/^VERSION_CODE=.*/VERSION_CODE=$new_code/" "$props"
rm -f "$props.bak"

git add CHANGELOG.md "$props"
git commit --quiet -m "chore(release): $tag"
git tag -a "$tag" -m "CloakRun $tag"
git push --quiet origin main
git push --quiet origin "$tag"

echo "Released $tag (versionCode $new_code). The release workflow is now building it."
