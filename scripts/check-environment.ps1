$ErrorActionPreference = "Stop"

$requiredJavaMajor = 21
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$wrapperPath = Join-Path $repositoryRoot "mvnw.cmd"

function Stop-EnvironmentCheck {
  param(
    [string] $Location,
    [string] $Reason,
    [string] $Fix
  )

  [Console]::Error.WriteLine(
    @"
StudyTrack environment check failed.
Location: $Location
Invariant: StudyTrack builds require JDK 21 and the Maven Wrapper from this repository.
Reason: $Reason
Fix: $Fix
Recheck: .\scripts\check-environment.ps1
Then verify: .\mvnw.cmd verify
Authority: docs/environment.md and AGENTS.md
"@
  )
  exit 1
}

if ($null -eq (Get-Command java -CommandType Application -ErrorAction SilentlyContinue)) {
  Stop-EnvironmentCheck `
    -Location "java on PATH" `
    -Reason "No java executable was found." `
    -Fix "Install or select JDK 21, then set JAVA_HOME and PATH for this shell."
}

$javaOutput = (& java --version 2>&1) -join [Environment]::NewLine
$javaExitCode = $LASTEXITCODE
if ($javaExitCode -ne 0) {
  Stop-EnvironmentCheck `
    -Location "java --version" `
    -Reason "The java command exited with code $javaExitCode. Output: $javaOutput" `
    -Fix "Select a working JDK 21 installation and update JAVA_HOME and PATH for this shell."
}

if ($javaOutput -notmatch "(?m)^(?:openjdk|java)\s+(?:version\s+`"?)?([0-9]+)") {
  Stop-EnvironmentCheck `
    -Location "java --version" `
    -Reason "The Java major version could not be parsed. Output: $javaOutput" `
    -Fix "Select a standard JDK 21 distribution and ensure its java command is first on PATH."
}

$detectedJavaMajor = [int] $Matches[1]
if ($detectedJavaMajor -ne $requiredJavaMajor) {
  Stop-EnvironmentCheck `
    -Location "java --version" `
    -Reason "Detected Java $detectedJavaMajor, but Java $requiredJavaMajor is required." `
    -Fix "Select JDK 21 and update JAVA_HOME and PATH for this shell; the script never changes them."
}

if (-not (Test-Path -LiteralPath $wrapperPath -PathType Leaf)) {
  Stop-EnvironmentCheck `
    -Location $wrapperPath `
    -Reason "The repository Maven Wrapper launcher is missing." `
    -Fix "Restore mvnw.cmd from the repository; do not substitute a system Maven installation."
}

$wrapperOutput = (& $wrapperPath --version 2>&1) -join [Environment]::NewLine
$wrapperExitCode = $LASTEXITCODE
if ($wrapperExitCode -ne 0) {
  Stop-EnvironmentCheck `
    -Location "mvnw.cmd --version" `
    -Reason "The Maven Wrapper exited with code $wrapperExitCode. Output: $wrapperOutput" `
    -Fix "Check the Wrapper files and the documented network, proxy, and certificate boundaries."
}

if ($wrapperOutput -notmatch "(?m)^Apache Maven\s+(\S+)") {
  Stop-EnvironmentCheck `
    -Location "mvnw.cmd --version" `
    -Reason "The Wrapper ran, but its Maven version could not be parsed. Output: $wrapperOutput" `
    -Fix "Restore the repository Wrapper configuration and rerun this check."
}
$mavenVersion = $Matches[1]

if ($wrapperOutput -notmatch "(?m)^Java version:\s*([0-9]+)") {
  Stop-EnvironmentCheck `
    -Location "mvnw.cmd --version" `
    -Reason "The Java version used by Maven could not be parsed. Output: $wrapperOutput" `
    -Fix "Set JAVA_HOME to JDK 21 and ensure the Wrapper starts with that runtime."
}

$wrapperJavaMajor = [int] $Matches[1]
if ($wrapperJavaMajor -ne $requiredJavaMajor) {
  Stop-EnvironmentCheck `
    -Location "mvnw.cmd --version" `
    -Reason "The Wrapper uses Java $wrapperJavaMajor, but Java $requiredJavaMajor is required." `
    -Fix "Set JAVA_HOME to JDK 21 for this shell, then rerun the environment check."
}

Write-Output "StudyTrack environment check passed."
Write-Output "Java: $requiredJavaMajor"
Write-Output "Maven Wrapper: Apache Maven $mavenVersion"
Write-Output "Next: .\mvnw.cmd verify"
