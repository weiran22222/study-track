#!/bin/sh

set -u

authority="docs/decisions/020-develop-production-branch-model.md and
docs/exec-plans/completed/017-develop-production-branch-model.md"

fail_branch_flow() {
  location=$1
  reason=$2
  fix=$3
  recheck=$4

  cat >&2 <<EOF
StudyTrack pull request branch flow check failed.
Location: $location
Invariant: Pull requests must follow one of the four branch flows authorized by decision 020.
Reason: $reason
Fix: $fix
Recheck: $recheck
Authority: $authority
EOF
  exit 1
}

if [ "$#" -ne 2 ]; then
  fail_branch_flow \
    "scripts/check-branch-flow.sh arguments" \
    "Expected exactly two arguments: the pull request base ref and head ref; received $#." \
    "Pass github.event.pull_request.base.ref and head.ref as separate arguments." \
    'sh ./scripts/check-branch-flow.sh "<base-ref>" "<head-ref>"'
fi

base=$1
head=$2

if [ -z "$base" ] || [ -z "$head" ]; then
  fail_branch_flow \
    "base=$base head=$head" \
    "The pull request base ref and head ref must both be non-empty." \
    "Pass the base.ref and head.ref values from the pull_request event." \
    'sh ./scripts/check-branch-flow.sh "<base-ref>" "<head-ref>"'
fi

case "$base:$head" in
  develop:codex/?*)
    route="ordinary integration: codex/* -> develop"
    ;;
  main:develop)
    route="release: develop -> main"
    ;;
  main:hotfix/?*)
    route="production hotfix: hotfix/* -> main"
    ;;
  develop:main)
    route="hotfix backflow: main -> develop"
    ;;
  *)
    fail_branch_flow \
      "base=$base head=$head" \
      "This base/head combination is not in the approved branch-flow matrix." \
      "Use codex/* -> develop, develop -> main, hotfix/* -> main, or main -> develop." \
      "sh ./scripts/check-branch-flow.sh \"$base\" \"$head\""
    ;;
esac

printf 'StudyTrack branch flow allowed: base=%s head=%s route=%s\n' "$base" "$head" "$route"
