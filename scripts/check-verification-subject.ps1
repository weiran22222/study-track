param(
  [string] $ExpectedSha,
  [string] $ExpectedBranch
)

$authority = "docs/decisions/021-generator-evaluator-role-separation.md and " +
  "docs/exec-plans/018-generator-evaluator-role-separation.md"

function Stop-VerificationSubjectCheck {
  param(
    [string] $Location,
    [string] $Invariant,
    [string] $Reason,
    [string] $Fix
  )

  [Console]::Error.WriteLine(
    @"
StudyTrack verification subject check failed.
Location: $Location
Invariant: $Invariant
Reason: $Reason
Fix: $Fix
Recheck: .\scripts\check-verification-subject.ps1 "<subject-sha>" "<source-branch>"
Authority: $authority
"@
  )
  exit 1
}

if ($PSBoundParameters.Count -ne 2 -or $args.Count -ne 0) {
  Stop-VerificationSubjectCheck `
    -Location "scripts/check-verification-subject.ps1 arguments" `
    -Invariant "Serial shared verification requires exactly one immutable Subject SHA and one source branch." `
    -Reason "Expected exactly two arguments: Subject SHA and source branch." `
    -Fix "Pass the full commit SHA and exact branch from the handoff manifest as separate arguments."
}

if ([string]::IsNullOrWhiteSpace($ExpectedSha) -or
    [string]::IsNullOrWhiteSpace($ExpectedBranch)) {
  Stop-VerificationSubjectCheck `
    -Location "expected SHA or branch argument" `
    -Invariant "The handoff Subject SHA and source branch must both be non-empty." `
    -Reason "At least one required handoff value is empty." `
    -Fix "Copy both non-empty values directly from the coordinator's handoff manifest."
}

if ($ExpectedSha -notmatch "^(?:[0-9a-fA-F]{40}|[0-9a-fA-F]{64})$") {
  Stop-VerificationSubjectCheck `
    -Location "expected SHA: $ExpectedSha" `
    -Invariant "The verification subject must be identified by a full immutable Git object ID." `
    -Reason "The expected SHA is not a full 40- or 64-hexadecimal-character object ID." `
    -Fix "Use the full Subject SHA recorded by the coordinator; do not pass a ref or abbreviation."
}

if ($null -eq (Get-Command git -CommandType Application -ErrorAction SilentlyContinue)) {
  Stop-VerificationSubjectCheck `
    -Location "git on PATH" `
    -Invariant "The verification subject guard must inspect the repository with Git." `
    -Reason "No git executable was found on PATH." `
    -Fix "Select an environment with Git available, then rerun the same read-only guard."
}

$insideWorkTree = (& git rev-parse --is-inside-work-tree 2>&1) -join [Environment]::NewLine
$insideExitCode = $LASTEXITCODE
if ($insideExitCode -ne 0 -or $insideWorkTree.Trim() -ne "true") {
  Stop-VerificationSubjectCheck `
    -Location "current directory" `
    -Invariant "The guard must run inside the StudyTrack Git working tree being handed off." `
    -Reason "Git did not identify the current directory as a working tree (exit $insideExitCode): $insideWorkTree" `
    -Fix "Change to the handed-off repository root without changing branches or files, then rerun."
}

$resolvedExpectedSha = (& git rev-parse --verify --quiet "$ExpectedSha`^{commit}" 2>&1) -join `
  [Environment]::NewLine
$expectedExitCode = $LASTEXITCODE
if ($expectedExitCode -ne 0) {
  Stop-VerificationSubjectCheck `
    -Location "expected SHA: $ExpectedSha" `
    -Invariant "The manifest Subject SHA must resolve to a commit in the handed-off repository." `
    -Reason "Git could not resolve the expected SHA to a commit (exit $expectedExitCode): $resolvedExpectedSha" `
    -Fix "Fetch or restore the handed-off history outside evaluator execution, then provide the exact commit."
}

if ($resolvedExpectedSha.Trim().ToLowerInvariant() -ne $ExpectedSha.ToLowerInvariant()) {
  Stop-VerificationSubjectCheck `
    -Location "expected SHA: $ExpectedSha" `
    -Invariant "The supplied full Subject SHA must name the exact commit Git resolves." `
    -Reason "Git resolved the expected value to $($resolvedExpectedSha.Trim())." `
    -Fix "Copy the canonical full Subject SHA from the coordinator's frozen handoff."
}

$currentBranch = (& git symbolic-ref --quiet --short HEAD 2>&1) -join [Environment]::NewLine
$branchExitCode = $LASTEXITCODE
if ($branchExitCode -ne 0) {
  Stop-VerificationSubjectCheck `
    -Location "current branch" `
    -Invariant "Serial shared verification must remain attached to the manifest source branch." `
    -Reason "Git could not read an attached branch (exit $branchExitCode); detached mode is not supported by this entry point." `
    -Fix "Ask the coordinator to restore the serial shared handoff on the expected branch, then rerun."
}

$currentBranch = $currentBranch.Trim()
if ($currentBranch -ne $ExpectedBranch) {
  Stop-VerificationSubjectCheck `
    -Location "current branch: $currentBranch" `
    -Invariant "The checked-out branch must exactly equal the handoff source branch $ExpectedBranch." `
    -Reason "The current branch does not match the expected source branch." `
    -Fix "Stop verification and ask the coordinator to provide the correct serial shared checkout."
}

$currentHead = (& git rev-parse --verify HEAD 2>&1) -join [Environment]::NewLine
$headExitCode = $LASTEXITCODE
if ($headExitCode -ne 0) {
  Stop-VerificationSubjectCheck `
    -Location "HEAD" `
    -Invariant "HEAD must resolve to the exact immutable Subject SHA." `
    -Reason "Git could not resolve HEAD (exit $headExitCode): $currentHead" `
    -Fix "Ask the coordinator to repair and refreeze the handoff before verification."
}

$currentHead = $currentHead.Trim()
if ($currentHead.ToLowerInvariant() -ne $ExpectedSha.ToLowerInvariant()) {
  Stop-VerificationSubjectCheck `
    -Location "HEAD: $currentHead" `
    -Invariant "HEAD must exactly equal the handoff Subject SHA $ExpectedSha." `
    -Reason "The checked-out commit differs from the frozen verification subject." `
    -Fix "Stop verification; the coordinator must freeze and hand off the intended commit again."
}

& git diff --cached --quiet --exit-code
$indexExitCode = $LASTEXITCODE
if ($indexExitCode -ne 0) {
  Stop-VerificationSubjectCheck `
    -Location "Git index" `
    -Invariant "The evaluator handoff index must be empty." `
    -Reason "git diff --cached --quiet exited with code $indexExitCode, indicating staged changes or an index error." `
    -Fix "Stop verification and ask the coordinator to provide a clean handoff with no staged changes."
}

$statusOutput = (& git status --porcelain 2>&1) -join [Environment]::NewLine
$statusExitCode = $LASTEXITCODE
if ($statusExitCode -ne 0) {
  Stop-VerificationSubjectCheck `
    -Location "Git working tree status" `
    -Invariant "Git status must be readable before the verification subject can be trusted." `
    -Reason "git status --porcelain exited with code ${statusExitCode}: $statusOutput" `
    -Fix "Stop verification and ask the coordinator to repair the handoff repository."
}

if (-not [string]::IsNullOrEmpty($statusOutput)) {
  Stop-VerificationSubjectCheck `
    -Location "Git working tree status: $statusOutput" `
    -Invariant "The evaluator handoff working tree must be clean, including untracked files." `
    -Reason "git status --porcelain reported repository changes." `
    -Fix "Stop verification and ask the coordinator to provide a clean handoff; do not auto-fix it."
}

Write-Output "StudyTrack verification subject check passed."
Write-Output "Mode: serial shared"
Write-Output "Expected branch: $ExpectedBranch"
Write-Output "Current branch: $currentBranch"
Write-Output "Expected SHA: $ExpectedSha"
Write-Output "HEAD: $currentHead"
Write-Output "Worktree: clean (git status --porcelain is empty)"
Write-Output "Index: empty (git diff --cached --quiet succeeded)"
