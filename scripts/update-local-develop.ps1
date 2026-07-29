param(
  [string] $VerifiedDevelopSha
)

$authority = "docs/decisions/023-local-develop-fast-forward-policy.md and " +
  "docs/exec-plans/completed/020-local-develop-fast-forward-policy.md"
$remoteDevelopRef = "refs/remotes/origin/develop"
$developFetchRefspec = "refs/heads/develop:refs/remotes/origin/develop"

function Stop-LocalDevelopUpdate {
  param(
    [string] $Location,
    [string] $Invariant,
    [string] $Reason,
    [string] $Fix
  )

  [Console]::Error.WriteLine(
    @"
StudyTrack local develop update failed.
Location: $Location
Invariant: $Invariant
Reason: $Reason
Fix: $Fix
Recheck: .\scripts\update-local-develop.ps1 "<verified-develop-sha>"
Authority: $authority
"@
  )
  exit 1
}

if ($PSBoundParameters.Count -ne 1 -or $args.Count -ne 0) {
  Stop-LocalDevelopUpdate `
    -Location "scripts/update-local-develop.ps1 arguments" `
    -Invariant "Local develop update requires exactly one verified full SHA." `
    -Reason "Expected one bound SHA argument and no extras." `
    -Fix "Pass the exact final origin/develop SHA whose required push verify succeeded."
}

if ([string]::IsNullOrWhiteSpace($VerifiedDevelopSha) -or
    $VerifiedDevelopSha -notmatch "^[0-9a-fA-F]{40}$") {
  Stop-LocalDevelopUpdate `
    -Location "Verified develop SHA: $VerifiedDevelopSha" `
    -Invariant "The verified develop SHA must be one full 40-character object ID." `
    -Reason "The supplied value is empty, abbreviated, non-hexadecimal, or not 40 characters." `
    -Fix "Copy the exact 40-character final develop SHA from the successful GitHub push verify."
}

if ($null -eq (Get-Command git -CommandType Application -ErrorAction SilentlyContinue)) {
  Stop-LocalDevelopUpdate `
    -Location "git on PATH" `
    -Invariant "The updater must inspect and fast-forward the repository with Git." `
    -Reason "No git executable was found on PATH." `
    -Fix "Use an environment with Git available, then rerun the same update command."
}

$insideWorkTree = (& git rev-parse --is-inside-work-tree 2>&1) -join `
  [Environment]::NewLine
$insideExitCode = $LASTEXITCODE
if ($insideExitCode -ne 0 -or $insideWorkTree.Trim() -cne "true") {
  Stop-LocalDevelopUpdate `
    -Location "current directory" `
    -Invariant "The updater must run inside the intended Git working tree." `
    -Reason "Git did not identify the current directory as a working tree (exit ${insideExitCode}): $insideWorkTree" `
    -Fix "Change to the repository root without modifying files, then rerun."
}

$currentBranch = (& git symbolic-ref --quiet --short HEAD 2>&1) -join `
  [Environment]::NewLine
$branchExitCode = $LASTEXITCODE
if ($branchExitCode -ne 0) {
  Stop-LocalDevelopUpdate `
    -Location "current branch" `
    -Invariant "The updater may run only on the exact local develop branch." `
    -Reason "Git could not resolve the current symbolic branch (exit ${branchExitCode}): $currentBranch" `
    -Fix "Stop and ask the human to select the intended local develop branch."
}

$currentBranch = $currentBranch.Trim()
if ($currentBranch -cne "develop") {
  Stop-LocalDevelopUpdate `
    -Location "current branch: $currentBranch" `
    -Invariant "The updater may run only when the current branch is exactly develop." `
    -Reason "The checked-out branch is not the exact local develop branch." `
    -Fix "Stop; do not use this updater on feature, main, detached, or differently cased branches."
}

& git diff --cached --quiet --exit-code
$indexExitCode = $LASTEXITCODE
if ($indexExitCode -ne 0) {
  Stop-LocalDevelopUpdate `
    -Location "Git index" `
    -Invariant "The local develop index must be empty before fetching or updating." `
    -Reason "git diff --cached --quiet exited with code ${indexExitCode}." `
    -Fix "Review and resolve your staged work without changing local develop through this script."
}

$statusOutput = (& git status --porcelain 2>&1) -join [Environment]::NewLine
$statusExitCode = $LASTEXITCODE
if ($statusExitCode -ne 0) {
  Stop-LocalDevelopUpdate `
    -Location "Git working tree status" `
    -Invariant "Git status must be readable before local develop can change." `
    -Reason "git status --porcelain exited with code ${statusExitCode}: $statusOutput" `
    -Fix "Stop and repair the repository state manually, then rerun."
}

if (-not [string]::IsNullOrEmpty($statusOutput)) {
  Stop-LocalDevelopUpdate `
    -Location "Git working tree status: $statusOutput" `
    -Invariant "The local develop worktree must be clean, including untracked files." `
    -Reason "git status --porcelain reported repository changes." `
    -Fix "Review your local files and reach a clean state without automatic cleanup."
}

$fetchOutput = (& git fetch --no-tags origin $developFetchRefspec 2>&1) -join `
  [Environment]::NewLine
$fetchExitCode = $LASTEXITCODE
if ($fetchExitCode -ne 0) {
  Stop-LocalDevelopUpdate `
    -Location "origin/develop fetch" `
    -Invariant "The only permitted update source is the fetched origin/develop ref." `
    -Reason "The exact origin develop fetch failed (exit ${fetchExitCode}): $fetchOutput" `
    -Fix "Check origin connectivity and the remote develop ref, then rerun with the same verified SHA."
}

$fetchedDevelopSha = (& git rev-parse --verify "$remoteDevelopRef`^{commit}" 2>&1) -join `
  [Environment]::NewLine
$resolveExitCode = $LASTEXITCODE
if ($resolveExitCode -ne 0) {
  Stop-LocalDevelopUpdate `
    -Location "fetched origin/develop ref" `
    -Invariant "The fetched origin/develop ref must resolve to one exact commit." `
    -Reason "Git could not resolve $remoteDevelopRef (exit ${resolveExitCode}): $fetchedDevelopSha" `
    -Fix "Stop and inspect the configured origin and its develop branch."
}

$fetchedDevelopSha = $fetchedDevelopSha.Trim()
if ($fetchedDevelopSha.ToLowerInvariant() -ne $VerifiedDevelopSha.ToLowerInvariant()) {
  Stop-LocalDevelopUpdate `
    -Location "fetched origin/develop SHA: $fetchedDevelopSha" `
    -Invariant "Fetched origin/develop must equal the exact SHA verified by GitHub." `
    -Reason "The fetched SHA differs from verified SHA $VerifiedDevelopSha." `
    -Fix "Stop; verify the current final origin/develop push check before retrying with its exact SHA."
}

$relationship = (& git rev-list --left-right --count "HEAD...$remoteDevelopRef" 2>&1) -join `
  [Environment]::NewLine
$relationshipExitCode = $LASTEXITCODE
if ($relationshipExitCode -ne 0) {
  Stop-LocalDevelopUpdate `
    -Location "local develop relationship" `
    -Invariant "Local develop must be equal to or strictly behind origin/develop." `
    -Reason "Git could not compare HEAD with $remoteDevelopRef (exit ${relationshipExitCode}): $relationship" `
    -Fix "Stop and inspect both refs without rewriting local develop."
}

$relationshipParts = $relationship.Trim() -split "\s+"
$leftCount = 0
$rightCount = 0
if ($relationshipParts.Count -ne 2 -or
    -not [int]::TryParse($relationshipParts[0], [ref] $leftCount) -or
    -not [int]::TryParse($relationshipParts[1], [ref] $rightCount)) {
  Stop-LocalDevelopUpdate `
    -Location "local develop relationship: $relationship" `
    -Invariant "The ahead/behind relationship must be parsed without ambiguity." `
    -Reason "git rev-list did not return two integer counts." `
    -Fix "Stop and inspect the Git installation and repository refs."
}

if ($leftCount -ne 0) {
  Stop-LocalDevelopUpdate `
    -Location "local develop relationship: ahead=$leftCount, remote-ahead=$rightCount" `
    -Invariant "Local develop must not be ahead of or diverged from origin/develop." `
    -Reason "Local develop contains commits not present in the verified remote develop ref." `
    -Fix "Stop and ask the human how to preserve and recover the local commits; do not rewrite them."
}

if ($rightCount -gt 0) {
  $updateOutput = (& git merge --ff-only refs/remotes/origin/develop 2>&1) -join `
    [Environment]::NewLine
  $updateExitCode = $LASTEXITCODE
  if ($updateExitCode -ne 0) {
    Stop-LocalDevelopUpdate `
      -Location "fast-forward update" `
      -Invariant "Local develop may change only by fast-forwarding from origin/develop." `
      -Reason "git merge --ff-only failed (exit ${updateExitCode}): $updateOutput" `
      -Fix "Stop and inspect the repository; do not repair the failure by rewriting local develop."
  }
}

$finalBranch = (& git symbolic-ref --quiet --short HEAD 2>&1) -join [Environment]::NewLine
$finalBranchExitCode = $LASTEXITCODE
if ($finalBranchExitCode -ne 0 -or $finalBranch.Trim() -cne "develop") {
  Stop-LocalDevelopUpdate `
    -Location "post-update branch" `
    -Invariant "The current branch must remain exactly develop after the update." `
    -Reason "The final branch check failed (exit ${finalBranchExitCode}): $finalBranch" `
    -Fix "Stop and inspect the repository; do not perform additional automated changes."
}

$finalHead = (& git rev-parse --verify HEAD 2>&1) -join [Environment]::NewLine
$finalHeadExitCode = $LASTEXITCODE
$finalHead = $finalHead.Trim()
if ($finalHeadExitCode -ne 0 -or
    $finalHead.ToLowerInvariant() -ne $VerifiedDevelopSha.ToLowerInvariant()) {
  Stop-LocalDevelopUpdate `
    -Location "post-update HEAD: $finalHead" `
    -Invariant "Final local develop HEAD must equal the exact verified origin/develop SHA." `
    -Reason "The final HEAD check failed or differs from $VerifiedDevelopSha (exit ${finalHeadExitCode})." `
    -Fix "Stop and inspect local and remote refs without rewriting history."
}

& git diff --cached --quiet --exit-code
$finalIndexExitCode = $LASTEXITCODE
$finalStatus = (& git status --porcelain 2>&1) -join [Environment]::NewLine
$finalStatusExitCode = $LASTEXITCODE
if ($finalIndexExitCode -ne 0 -or
    $finalStatusExitCode -ne 0 -or
    -not [string]::IsNullOrEmpty($finalStatus)) {
  Stop-LocalDevelopUpdate `
    -Location "post-update Git state" `
    -Invariant "The index and worktree must remain clean after updating local develop." `
    -Reason "Final index exit=${finalIndexExitCode}, status exit=${finalStatusExitCode}, status=$finalStatus" `
    -Fix "Stop and inspect hook or filesystem side effects; do not auto-clean them."
}

Write-Output "StudyTrack local develop update passed."
Write-Output "Verified origin/develop SHA: $VerifiedDevelopSha"
Write-Output "HEAD: $finalHead"
Write-Output "Relationship before update: ahead=$leftCount, remote-ahead=$rightCount"
Write-Output "Working tree: clean"
Write-Output "Index: empty"
