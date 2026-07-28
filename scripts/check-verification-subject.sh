#!/bin/sh

set -u

authority="docs/decisions/021-generator-evaluator-role-separation.md and
docs/exec-plans/018-generator-evaluator-role-separation.md"

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
Recheck: sh ./scripts/check-verification-subject.sh "<subject-sha>" "<source-branch>"
Authority: $authority
EOF
  exit 1
}

if [ "$#" -ne 2 ]; then
  fail_verification_subject_check \
    "scripts/check-verification-subject.sh arguments" \
    "Serial shared verification requires exactly one immutable Subject SHA and one source branch." \
    "Expected exactly two arguments: Subject SHA and source branch; received $#." \
    "Pass the full commit SHA and exact branch from the handoff manifest as separate arguments."
fi

expected_sha=$1
expected_branch=$2

if [ -z "$expected_sha" ] || [ -z "$expected_branch" ]; then
  fail_verification_subject_check \
    "expected SHA or branch argument" \
    "The handoff Subject SHA and source branch must both be non-empty." \
    "At least one required handoff value is empty." \
    "Copy both non-empty values directly from the coordinator's handoff manifest."
fi

case "$expected_sha" in
  *[!0-9a-fA-F]*)
    valid_sha=false
    ;;
  *)
    valid_sha=true
    ;;
esac

if { [ "${#expected_sha}" -ne 40 ] && [ "${#expected_sha}" -ne 64 ]; } ||
    [ "$valid_sha" != "true" ]; then
  fail_verification_subject_check \
    "expected SHA: $expected_sha" \
    "The verification subject must be identified by a full immutable Git object ID." \
    "The expected SHA is not a full 40- or 64-hexadecimal-character object ID." \
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
    "The guard must run inside the StudyTrack Git working tree being handed off." \
    "Git did not identify the current directory as a working tree (exit $inside_exit_code): $inside_work_tree" \
    "Change to the handed-off repository root without changing branches or files, then rerun."
fi

resolved_expected_sha=$(git rev-parse --verify --quiet "$expected_sha^{commit}" 2>&1)
expected_exit_code=$?
if [ "$expected_exit_code" -ne 0 ]; then
  fail_verification_subject_check \
    "expected SHA: $expected_sha" \
    "The manifest Subject SHA must resolve to a commit in the handed-off repository." \
    "Git could not resolve the expected SHA to a commit (exit $expected_exit_code): $resolved_expected_sha" \
    "Fetch or restore the handed-off history outside evaluator execution, then provide the exact commit."
fi

normalized_expected_sha=$(printf '%s' "$expected_sha" | tr 'A-F' 'a-f')
if [ "$resolved_expected_sha" != "$normalized_expected_sha" ]; then
  fail_verification_subject_check \
    "expected SHA: $expected_sha" \
    "The supplied full Subject SHA must name the exact commit Git resolves." \
    "Git resolved the expected value to $resolved_expected_sha." \
    "Copy the canonical full Subject SHA from the coordinator's frozen handoff."
fi

current_branch=$(git symbolic-ref --quiet --short HEAD 2>&1)
branch_exit_code=$?
if [ "$branch_exit_code" -ne 0 ]; then
  fail_verification_subject_check \
    "current branch" \
    "Serial shared verification must remain attached to the manifest source branch." \
    "Git could not read an attached branch (exit $branch_exit_code); detached mode is not supported by this entry point." \
    "Ask the coordinator to restore the serial shared handoff on the expected branch, then rerun."
fi

if [ "$current_branch" != "$expected_branch" ]; then
  fail_verification_subject_check \
    "current branch: $current_branch" \
    "The checked-out branch must exactly equal the handoff source branch $expected_branch." \
    "The current branch does not match the expected source branch." \
    "Stop verification and ask the coordinator to provide the correct serial shared checkout."
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

if [ "$current_head" != "$normalized_expected_sha" ]; then
  fail_verification_subject_check \
    "HEAD: $current_head" \
    "HEAD must exactly equal the handoff Subject SHA $expected_sha." \
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
  "Mode: serial shared" \
  "Expected branch: $expected_branch" \
  "Current branch: $current_branch" \
  "Expected SHA: $expected_sha" \
  "HEAD: $current_head" \
  "Worktree: clean (git status --porcelain is empty)" \
  "Index: empty (git diff --cached --quiet succeeded)"
