#!/bin/sh

set -u

authority="docs/decisions/023-local-develop-fast-forward-policy.md and
docs/exec-plans/completed/020-local-develop-fast-forward-policy.md"
remote_develop_ref="refs/remotes/origin/develop"
develop_fetch_refspec="refs/heads/develop:refs/remotes/origin/develop"

fail_local_develop_update() {
  location=$1
  invariant=$2
  reason=$3
  fix=$4

  cat >&2 <<EOF
StudyTrack local develop update failed.
Location: $location
Invariant: $invariant
Reason: $reason
Fix: $fix
Recheck: sh ./scripts/update-local-develop.sh "<verified-develop-sha>"
Authority: $authority
EOF
  exit 1
}

if [ "$#" -ne 1 ]; then
  fail_local_develop_update \
    "scripts/update-local-develop.sh arguments" \
    "Local develop update requires exactly one verified full SHA." \
    "Expected one SHA argument and no extras; received $#." \
    "Pass the exact final origin/develop SHA whose required push verify succeeded."
fi

verified_develop_sha=$1
if [ "${#verified_develop_sha}" -ne 40 ]; then
  fail_local_develop_update \
    "Verified develop SHA: $verified_develop_sha" \
    "The verified develop SHA must be one full 40-character object ID." \
    "The supplied value is empty, abbreviated, or not 40 characters." \
    "Copy the exact 40-character final develop SHA from the successful GitHub push verify."
fi

case "$verified_develop_sha" in
  *[!0-9a-fA-F]*)
    fail_local_develop_update \
      "Verified develop SHA: $verified_develop_sha" \
      "The verified develop SHA must be one full 40-character object ID." \
      "The supplied value contains non-hexadecimal characters." \
      "Copy the exact 40-character final develop SHA from the successful GitHub push verify."
    ;;
esac

if ! command -v git >/dev/null 2>&1; then
  fail_local_develop_update \
    "git on PATH" \
    "The updater must inspect and fast-forward the repository with Git." \
    "No git executable was found on PATH." \
    "Use an environment with Git available, then rerun the same update command."
fi

inside_work_tree=$(git rev-parse --is-inside-work-tree 2>&1)
inside_exit_code=$?
if [ "$inside_exit_code" -ne 0 ] || [ "$inside_work_tree" != "true" ]; then
  fail_local_develop_update \
    "current directory" \
    "The updater must run inside the intended Git working tree." \
    "Git did not identify the current directory as a working tree (exit $inside_exit_code): $inside_work_tree" \
    "Change to the repository root without modifying files, then rerun."
fi

current_branch=$(git symbolic-ref --quiet --short HEAD 2>&1)
branch_exit_code=$?
if [ "$branch_exit_code" -ne 0 ]; then
  fail_local_develop_update \
    "current branch" \
    "The updater may run only on the exact local develop branch." \
    "Git could not resolve the current symbolic branch (exit $branch_exit_code): $current_branch" \
    "Stop and ask the human to select the intended local develop branch."
fi

if [ "$current_branch" != "develop" ]; then
  fail_local_develop_update \
    "current branch: $current_branch" \
    "The updater may run only when the current branch is exactly develop." \
    "The checked-out branch is not the exact local develop branch." \
    "Stop; do not use this updater on feature, main, detached, or differently cased branches."
fi

git diff --cached --quiet --exit-code
index_exit_code=$?
if [ "$index_exit_code" -ne 0 ]; then
  fail_local_develop_update \
    "Git index" \
    "The local develop index must be empty before fetching or updating." \
    "git diff --cached --quiet exited with code $index_exit_code." \
    "Review and resolve your staged work without changing local develop through this script."
fi

status_output=$(git status --porcelain 2>&1)
status_exit_code=$?
if [ "$status_exit_code" -ne 0 ]; then
  fail_local_develop_update \
    "Git working tree status" \
    "Git status must be readable before local develop can change." \
    "git status --porcelain exited with code $status_exit_code: $status_output" \
    "Stop and repair the repository state manually, then rerun."
fi

if [ -n "$status_output" ]; then
  fail_local_develop_update \
    "Git working tree status: $status_output" \
    "The local develop worktree must be clean, including untracked files." \
    "git status --porcelain reported repository changes." \
    "Review your local files and reach a clean state without automatic cleanup."
fi

fetch_output=$(git fetch --no-tags origin "$develop_fetch_refspec" 2>&1)
fetch_exit_code=$?
if [ "$fetch_exit_code" -ne 0 ]; then
  fail_local_develop_update \
    "origin/develop fetch" \
    "The only permitted update source is the fetched origin/develop ref." \
    "The exact origin develop fetch failed (exit $fetch_exit_code): $fetch_output" \
    "Check origin connectivity and the remote develop ref, then rerun with the same verified SHA."
fi

fetched_develop_sha=$(git rev-parse --verify "$remote_develop_ref^{commit}" 2>&1)
resolve_exit_code=$?
if [ "$resolve_exit_code" -ne 0 ]; then
  fail_local_develop_update \
    "fetched origin/develop ref" \
    "The fetched origin/develop ref must resolve to one exact commit." \
    "Git could not resolve $remote_develop_ref (exit $resolve_exit_code): $fetched_develop_sha" \
    "Stop and inspect the configured origin and its develop branch."
fi

lower_verified_sha=$(printf '%s' "$verified_develop_sha" | tr 'A-F' 'a-f')
if [ "$fetched_develop_sha" != "$lower_verified_sha" ]; then
  fail_local_develop_update \
    "fetched origin/develop SHA: $fetched_develop_sha" \
    "Fetched origin/develop must equal the exact SHA verified by GitHub." \
    "The fetched SHA differs from verified SHA $verified_develop_sha." \
    "Stop; verify the current final origin/develop push check before retrying with its exact SHA."
fi

relationship=$(git rev-list --left-right --count "HEAD...$remote_develop_ref" 2>&1)
relationship_exit_code=$?
if [ "$relationship_exit_code" -ne 0 ]; then
  fail_local_develop_update \
    "local develop relationship" \
    "Local develop must be equal to or strictly behind origin/develop." \
    "Git could not compare HEAD with $remote_develop_ref (exit $relationship_exit_code): $relationship" \
    "Stop and inspect both refs without rewriting local develop."
fi

set -- $relationship
if [ "$#" -ne 2 ]; then
  fail_local_develop_update \
    "local develop relationship: $relationship" \
    "The ahead/behind relationship must be parsed without ambiguity." \
    "git rev-list did not return two integer counts." \
    "Stop and inspect the Git installation and repository refs."
fi

left_count=$1
right_count=$2
case "$left_count:$right_count" in
  *[!0-9:]* | :* | *:)
    fail_local_develop_update \
      "local develop relationship: $relationship" \
      "The ahead/behind relationship must contain two nonnegative integer counts." \
      "git rev-list returned an unexpected relationship." \
      "Stop and inspect the Git installation and repository refs."
    ;;
esac

if [ "$left_count" -ne 0 ]; then
  fail_local_develop_update \
    "local develop relationship: ahead=$left_count, remote-ahead=$right_count" \
    "Local develop must not be ahead of or diverged from origin/develop." \
    "Local develop contains commits not present in the verified remote develop ref." \
    "Stop and ask the human how to preserve and recover the local commits; do not rewrite them."
fi

if [ "$right_count" -gt 0 ]; then
  update_output=$(git merge --ff-only refs/remotes/origin/develop 2>&1)
  update_exit_code=$?
  if [ "$update_exit_code" -ne 0 ]; then
    fail_local_develop_update \
      "fast-forward update" \
      "Local develop may change only by fast-forwarding from origin/develop." \
      "git merge --ff-only failed (exit $update_exit_code): $update_output" \
      "Stop and inspect the repository; do not repair the failure by rewriting local develop."
  fi
fi

final_branch=$(git symbolic-ref --quiet --short HEAD 2>&1)
final_branch_exit_code=$?
if [ "$final_branch_exit_code" -ne 0 ] || [ "$final_branch" != "develop" ]; then
  fail_local_develop_update \
    "post-update branch" \
    "The current branch must remain exactly develop after the update." \
    "The final branch check failed (exit $final_branch_exit_code): $final_branch" \
    "Stop and inspect the repository; do not perform additional automated changes."
fi

final_head=$(git rev-parse --verify HEAD 2>&1)
final_head_exit_code=$?
if [ "$final_head_exit_code" -ne 0 ] || [ "$final_head" != "$lower_verified_sha" ]; then
  fail_local_develop_update \
    "post-update HEAD: $final_head" \
    "Final local develop HEAD must equal the exact verified origin/develop SHA." \
    "The final HEAD check failed or differs from $verified_develop_sha (exit $final_head_exit_code)." \
    "Stop and inspect local and remote refs without rewriting history."
fi

git diff --cached --quiet --exit-code
final_index_exit_code=$?
final_status=$(git status --porcelain 2>&1)
final_status_exit_code=$?
if [ "$final_index_exit_code" -ne 0 ] ||
  [ "$final_status_exit_code" -ne 0 ] ||
  [ -n "$final_status" ]; then
  fail_local_develop_update \
    "post-update Git state" \
    "The index and worktree must remain clean after updating local develop." \
    "Final index exit=$final_index_exit_code, status exit=$final_status_exit_code, status=$final_status" \
    "Stop and inspect hook or filesystem side effects; do not auto-clean them."
fi

printf '%s\n' "StudyTrack local develop update passed."
printf '%s\n' "Verified origin/develop SHA: $verified_develop_sha"
printf '%s\n' "HEAD: $final_head"
printf '%s\n' "Relationship before update: ahead=$left_count, remote-ahead=$right_count"
printf '%s\n' "Working tree: clean"
printf '%s\n' "Index: empty"
