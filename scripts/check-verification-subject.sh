#!/bin/sh

set -u

authority="docs/decisions/022-simplify-agent-handoff.md and
docs/exec-plans/completed/019-simplify-agent-handoff.md"

fail_verification_subject_check() {
  location=$1
  invariant=$2
  reason=$3
  fix=$4

  cat >&2 <<EOF
StudyTrack verification subject check failed.
Location: $location
Invariant: $invariant
Reason: $reason
Fix: $fix
Recheck: sh ./scripts/check-verification-subject.sh "<subject-sha>"
Authority: $authority
EOF
  exit 1
}

if [ "$#" -ne 1 ]; then
  fail_verification_subject_check \
    "scripts/check-verification-subject.sh arguments" \
    "Verification requires exactly one immutable Subject SHA." \
    "Expected exactly one argument, the full Subject SHA; received $#." \
    "Pass the full commit SHA from the evaluator handoff as the only argument."
fi

subject_sha=$1

if [ -z "$subject_sha" ]; then
  fail_verification_subject_check \
    "Subject SHA argument" \
    "The handoff Subject SHA must be non-empty." \
    "The required Subject SHA is empty." \
    "Copy the non-empty full Subject SHA directly from the evaluator handoff."
fi

case "$subject_sha" in
  *[!0-9a-fA-F]*)
    valid_sha=false
    ;;
  *)
    valid_sha=true
    ;;
esac

if { [ "${#subject_sha}" -ne 40 ] && [ "${#subject_sha}" -ne 64 ]; } ||
    [ "$valid_sha" != "true" ]; then
  fail_verification_subject_check \
    "Subject SHA: $subject_sha" \
    "The verification subject must be identified by a full immutable Git object ID." \
    "The Subject SHA is not a full 40- or 64-hexadecimal-character object ID." \
    "Use the full Subject SHA recorded by the coordinator; do not pass a ref or abbreviation."
fi

if ! command -v git >/dev/null 2>&1; then
  fail_verification_subject_check \
    "git on PATH" \
    "The verification subject guard must inspect the repository with Git." \
    "No git executable was found on PATH." \
    "Select an environment with Git available, then rerun the same read-only guard."
fi

inside_work_tree=$(git rev-parse --is-inside-work-tree 2>&1)
inside_exit_code=$?
if [ "$inside_exit_code" -ne 0 ] || [ "$inside_work_tree" != "true" ]; then
  fail_verification_subject_check \
    "current directory" \
    "The guard must run inside the StudyTrack Git repository being handed off." \
    "Git did not identify the current directory as a working tree (exit $inside_exit_code): $inside_work_tree" \
    "Change to the handed-off repository root without changing files, then rerun."
fi

resolved_subject_sha=$(git rev-parse --verify --quiet "$subject_sha^{commit}" 2>&1)
subject_exit_code=$?
if [ "$subject_exit_code" -ne 0 ]; then
  fail_verification_subject_check \
    "Subject SHA: $subject_sha" \
    "The handoff Subject SHA must resolve to a commit in the handed-off repository." \
    "Git could not resolve the Subject SHA to a commit (exit $subject_exit_code): $resolved_subject_sha" \
    "Ask the coordinator to provide a repository containing the exact frozen commit."
fi

normalized_subject_sha=$(printf '%s' "$subject_sha" | tr 'A-F' 'a-f')
if [ "$resolved_subject_sha" != "$normalized_subject_sha" ]; then
  fail_verification_subject_check \
    "Subject SHA: $subject_sha" \
    "The supplied full Subject SHA must name the exact commit Git resolves." \
    "Git resolved the supplied value to $resolved_subject_sha." \
    "Copy the canonical full Subject SHA from the coordinator's frozen handoff."
fi

current_head=$(git rev-parse --verify HEAD 2>&1)
head_exit_code=$?
if [ "$head_exit_code" -ne 0 ]; then
  fail_verification_subject_check \
    "HEAD" \
    "HEAD must resolve to the exact immutable Subject SHA." \
    "Git could not resolve HEAD (exit $head_exit_code): $current_head" \
    "Ask the coordinator to repair and refreeze the handoff before verification."
fi

if [ "$current_head" != "$normalized_subject_sha" ]; then
  fail_verification_subject_check \
    "HEAD: $current_head" \
    "HEAD must exactly equal the handoff Subject SHA $subject_sha." \
    "The checked-out commit differs from the frozen verification subject." \
    "Stop verification; the coordinator must freeze and hand off the intended commit again."
fi

git diff --cached --quiet --exit-code
index_exit_code=$?
if [ "$index_exit_code" -ne 0 ]; then
  fail_verification_subject_check \
    "Git index" \
    "The evaluator handoff index must be empty." \
    "git diff --cached --quiet exited with code $index_exit_code, indicating staged changes or an index error." \
    "Stop verification and ask the coordinator to provide a clean handoff with no staged changes."
fi

status_output=$(git status --porcelain 2>&1)
status_exit_code=$?
if [ "$status_exit_code" -ne 0 ]; then
  fail_verification_subject_check \
    "Git working tree status" \
    "Git status must be readable before the verification subject can be trusted." \
    "git status --porcelain exited with code $status_exit_code: $status_output" \
    "Stop verification and ask the coordinator to repair the handoff repository."
fi

if [ -n "$status_output" ]; then
  fail_verification_subject_check \
    "Git working tree status: $status_output" \
    "The evaluator handoff working tree must be clean, including untracked files." \
    "git status --porcelain reported repository changes." \
    "Stop verification and ask the coordinator to provide a clean handoff; do not auto-fix it."
fi

printf '%s\n' \
  "StudyTrack verification subject check passed." \
  "Subject SHA: $subject_sha" \
  "HEAD: $current_head" \
  "Working tree: clean (git status --porcelain is empty)" \
  "Index: empty (git diff --cached --quiet succeeded)"
