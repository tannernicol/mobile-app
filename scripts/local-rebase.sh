#!/usr/bin/env bash
# Personal-fork maintenance: keep the two personal branches current on top of
# upstream main, then prove they still build. Run this monthly (or whenever
# upstream ships something you want) instead of hand-rebasing.
#
#   feat/radio-favorite-current-song  -> rebased onto origin/main
#   local/remote-volume-keys          -> rebased onto the branch above
#
# Both stay on the fork remote; this script never pushes and never merges.
set -euo pipefail

repo_root=$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)
cd "$repo_root"

radio_branch="feat/radio-favorite-current-song"
volume_branch="local/remote-volume-keys"
check_cmd=(./gradlew detektAll testAndroidHostTest :androidApp:testDebug)
apk_cmd=(./gradlew :androidApp:assembleDebug)

if [[ -n "$(git status --porcelain)" ]]; then
    echo "local-rebase: working tree is dirty — commit or stash first." >&2
    exit 1
fi

echo "local-rebase: fetching upstream main..."
git fetch origin main

abort_and_die() {
    local branch="$1"
    echo "local-rebase: conflict rebasing $branch — aborting. Resolve by hand." >&2
    git rebase --abort || true
    exit 1
}

echo "local-rebase: rebasing $radio_branch onto origin/main..."
git checkout "$radio_branch"
git rebase origin/main || abort_and_die "$radio_branch"

echo "local-rebase: rebasing $volume_branch onto $radio_branch..."
git checkout "$volume_branch"
git rebase "$radio_branch" || abort_and_die "$volume_branch"

echo "local-rebase: running the check suite..."
"${check_cmd[@]}"

echo "local-rebase: assembling the debug APK..."
"${apk_cmd[@]}"

apk=$(find androidApp/build/outputs/apk/debug -name '*.apk' | head -1)
echo "local-rebase: done. APK: $apk"
echo "local-rebase: push manually once you've smoke-tested it:"
echo "  git push fork $radio_branch $volume_branch"
