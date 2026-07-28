#!/bin/sh

set -u

authority="docs/decisions/016-ci-pr-diff-whitespace-gate.md and
https://git-scm.com/docs/git-diff#Documentation/git-diff.txt---check"

fail_diff_guard() {
  location=$1
  reason=$2
  fix=$3
  recheck=$4

  cat >&2 <<EOF
StudyTrack pull request diff check failed.
Location: $location
Invariant: The complete pull request base...head diff must contain no Git whitespace errors.
Reason: $reason
Fix: $fix
Recheck: $recheck
Authority: $authority
EOF
  exit 1
}

if [ "$#" -ne 2 ]; then
  fail_diff_guard \
    "scripts/check-pr-diff.sh arguments" \
    "Expected exactly two arguments: the pull request base SHA and head SHA; received $#." \
    "Pass the immutable base and head SHAs from the pull_request event as separate arguments." \
    'sh ./scripts/check-pr-diff.sh "<base-sha>" "<head-sha>"'
fi

base=$1
head=$2

if ! git rev-parse --verify --quiet "$base^{commit}" >/dev/null; then
  fail_diff_guard \
    "base SHA: $base" \
    "Git cannot resolve the base argument to a commit in the checked-out history." \
    "Fetch complete history and pass github.event.pull_request.base.sha." \
    "sh ./scripts/check-pr-diff.sh \"$base\" \"$head\""
fi

if ! git rev-parse --verify --quiet "$head^{commit}" >/dev/null; then
  fail_diff_guard \
    "head SHA: $head" \
    "Git cannot resolve the head argument to a commit in the checked-out history." \
    "Fetch complete history and pass github.event.pull_request.head.sha." \
    "sh ./scripts/check-pr-diff.sh \"$base\" \"$head\""
fi

if ! git merge-base "$base" "$head" >/dev/null 2>&1; then
  fail_diff_guard \
    "range: $base...$head" \
    "Git cannot find a merge base for the pull request base and head commits." \
    "Fetch complete history and verify that the event base and head SHAs belong to the PR." \
    "sh ./scripts/check-pr-diff.sh \"$base\" \"$head\""
fi

if diff_output=$(git diff --check "$base...$head" 2>&1); then
  exit 0
else
  diff_exit_code=$?
fi

fail_diff_guard \
  "$diff_output" \
  "git diff --check exited with code $diff_exit_code after detecting a whitespace error." \
  "Remove the reported trailing whitespace or extra blank line at end of file." \
  "sh ./scripts/check-pr-diff.sh \"$base\" \"$head\""
