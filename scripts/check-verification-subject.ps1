param(
  [string] $SubjectSha
)

$authority = "docs/decisions/022-simplify-agent-handoff.md and " +
  "docs/exec-plans/completed/019-simplify-agent-handoff.md"

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
Recheck: .\scripts\check-verification-subject.ps1 "<subject-sha>"
Authority: $authority
"@
  )
  exit 1
}

if ($PSBoundParameters.Count -ne 1 -or $args.Count -ne 0) {
  Stop-VerificationSubjectCheck `
    -Location "scripts/check-verification-subject.ps1 arguments" `
    -Invariant "Verification requires exactly one immutable Subject SHA." `
    -Reason "Expected exactly one argument: the full Subject SHA." `
    -Fix "Pass the full commit SHA from the evaluator handoff as the only argument."
}

if ([string]::IsNullOrWhiteSpace($SubjectSha)) {
  Stop-VerificationSubjectCheck `
    -Location "Subject SHA argument" `
    -Invariant "The handoff Subject SHA must be non-empty." `
    -Reason "The required Subject SHA is empty." `
    -Fix "Copy the non-empty full Subject SHA directly from the evaluator handoff."
}

if ($SubjectSha -notmatch "^(?:[0-9a-fA-F]{40}|[0-9a-fA-F]{64})$") {
  Stop-VerificationSubjectCheck `
    -Location "Subject SHA: $SubjectSha" `
    -Invariant "The verification subject must be identified by a full immutable Git object ID." `
    -Reason "The Subject SHA is not a full 40- or 64-hexadecimal-character object ID." `
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
    -Invariant "The guard must run inside the StudyTrack Git repository being handed off." `
    -Reason "Git did not identify the current directory as a working tree (exit $insideExitCode): $insideWorkTree" `
    -Fix "Change to the handed-off repository root without changing files, then rerun."
}

$resolvedSubjectSha = (& git rev-parse --verify --quiet "$SubjectSha`^{commit}" 2>&1) -join `
  [Environment]::NewLine
$subjectExitCode = $LASTEXITCODE
if ($subjectExitCode -ne 0) {
  Stop-VerificationSubjectCheck `
    -Location "Subject SHA: $SubjectSha" `
    -Invariant "The handoff Subject SHA must resolve to a commit in the handed-off repository." `
    -Reason "Git could not resolve the Subject SHA to a commit (exit $subjectExitCode): $resolvedSubjectSha" `
    -Fix "Ask the coordinator to provide a repository containing the exact frozen commit."
}

if ($resolvedSubjectSha.Trim().ToLowerInvariant() -ne $SubjectSha.ToLowerInvariant()) {
  Stop-VerificationSubjectCheck `
    -Location "Subject SHA: $SubjectSha" `
    -Invariant "The supplied full Subject SHA must name the exact commit Git resolves." `
    -Reason "Git resolved the supplied value to $($resolvedSubjectSha.Trim())." `
    -Fix "Copy the canonical full Subject SHA from the coordinator's frozen handoff."
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
if ($currentHead.ToLowerInvariant() -ne $SubjectSha.ToLowerInvariant()) {
  Stop-VerificationSubjectCheck `
    -Location "HEAD: $currentHead" `
    -Invariant "HEAD must exactly equal the handoff Subject SHA $SubjectSha." `
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
Write-Output "Subject SHA: $SubjectSha"
Write-Output "HEAD: $currentHead"
Write-Output "Working tree: clean (git status --porcelain is empty)"
Write-Output "Index: empty (git diff --cached --quiet succeeded)"
