$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot

function Stop-WorktreeAudit {
  param(
    [string] $Location,
    [string] $Reason,
    [string] $Fix
  )

  [Console]::Error.WriteLine(
    @"
StudyTrack worktree audit failed.
Location: $Location
Invariant: Worktree discovery must remain read-only and classify complete Git state reliably.
Reason: $Reason
Fix: $Fix
Recheck: .\scripts\audit-worktrees.ps1
Authority: AGENTS.md and docs/decisions/018-worktree-hygiene-audit.md
"@
  )
  exit 1
}

function Invoke-AuditGit {
  param(
    [string[]] $Arguments,
    [string] $Location,
    [string] $Fix
  )

  $output = @(& git @Arguments 2>&1 | ForEach-Object { $_.ToString() })
  $exitCode = $LASTEXITCODE
  if ($exitCode -ne 0) {
    Stop-WorktreeAudit `
      -Location $Location `
      -Reason "Git exited with code $exitCode. Output: $(($output -join ' ').Trim())" `
      -Fix $Fix
  }
  return $output
}

function Convert-WorktreeRecord {
  param([hashtable] $Record)

  return [pscustomobject] @{
    Path = $Record.Path
    Head = $Record.Head
    Branch = $Record.Branch
    Detached = $Record.Detached
  }
}

try {
  if ($null -eq (Get-Command git -CommandType Application -ErrorAction SilentlyContinue)) {
    Stop-WorktreeAudit `
      -Location "git on PATH" `
      -Reason "No git executable was found." `
      -Fix "Install or select Git, then ensure git is available on PATH."
  }

  $worktreeLines = Invoke-AuditGit `
    -Arguments @("-C", $repositoryRoot, "worktree", "list", "--porcelain") `
    -Location "git worktree list --porcelain" `
    -Fix "Repair the repository worktree metadata, then rerun the audit."

  $records = @()
  $current = $null
  foreach ($lineValue in $worktreeLines) {
    $line = [string] $lineValue
    if ([string]::IsNullOrWhiteSpace($line)) {
      if ($null -ne $current) {
        $records += Convert-WorktreeRecord -Record $current
        $current = $null
      }
      continue
    }

    if ($line.StartsWith("worktree ")) {
      if ($null -ne $current) {
        $records += Convert-WorktreeRecord -Record $current
      }
      $current = @{
        Path = $line.Substring("worktree ".Length)
        Head = $null
        Branch = $null
        Detached = $false
      }
      continue
    }

    if ($null -eq $current) {
      Stop-WorktreeAudit `
        -Location "git worktree list --porcelain output" `
        -Reason "A field appeared before a worktree path: $line" `
        -Fix "Inspect the installed Git porcelain format and update the parser deliberately."
    }

    if ($line.StartsWith("HEAD ")) {
      $current.Head = $line.Substring("HEAD ".Length)
    } elseif ($line.StartsWith("branch ")) {
      $current.Branch = $line.Substring("branch ".Length)
    } elseif ($line -eq "detached") {
      $current.Detached = $true
    } elseif (
      ($line -eq "bare") -or
      $line.StartsWith("locked") -or
      $line.StartsWith("prunable")
    ) {
      continue
    } else {
      Stop-WorktreeAudit `
        -Location "git worktree list --porcelain output" `
        -Reason "An unknown worktree field was returned: $line" `
        -Fix "Inspect the installed Git porcelain format and update the parser deliberately."
    }
  }

  if ($null -ne $current) {
    $records += Convert-WorktreeRecord -Record $current
  }
  if ($records.Count -lt 1) {
    Stop-WorktreeAudit `
      -Location "git worktree list --porcelain output" `
      -Reason "Git returned no worktree records." `
      -Fix "Run the script from a valid StudyTrack checkout with intact worktree metadata."
  }

  for ($index = 0; $index -lt $records.Count; $index++) {
    $record = $records[$index]
    $branchDisplay = if ([string]::IsNullOrWhiteSpace($record.Branch)) {
      "DETACHED"
    } else {
      $record.Branch.Replace("refs/heads/", "")
    }

    if ($index -eq 0) {
      Write-Output (
        "PRIMARY path=`"$($record.Path)`" branch=`"$branchDisplay`" " +
        "note=`"Primary worktree is never an automatic cleanup candidate.`""
      )
      continue
    }

    $reasons = New-Object System.Collections.Generic.List[string]
    $statusLines = Invoke-AuditGit `
      -Arguments @(
        "-C",
        $record.Path,
        "status",
        "--porcelain=v1",
        "--untracked-files=all"
      ) `
      -Location "git status for $($record.Path)" `
      -Fix "Repair access to this linked worktree, then rerun the audit."
    if ($statusLines.Count -gt 0) {
      $reasons.Add("dirty worktree") | Out-Null
    }

    $ahead = $null
    $behind = $null
    $upstreamRef = $null
    if ($record.Detached) {
      $reasons.Add("detached HEAD") | Out-Null
    } elseif (
      [string]::IsNullOrWhiteSpace($record.Branch) -or
      (-not $record.Branch.StartsWith("refs/heads/"))
    ) {
      $reasons.Add("no local branch") | Out-Null
    } else {
      $localBranch = (
        Invoke-AuditGit `
          -Arguments @(
            "-C",
            $record.Path,
            "for-each-ref",
            "--format=%(refname)",
            $record.Branch
          ) `
          -Location "local branch for $($record.Path)" `
          -Fix "Repair the local branch reference, then rerun the audit."
      ) -join ""

      if ($localBranch.Trim() -ne $record.Branch) {
        $reasons.Add("no local branch") | Out-Null
      } else {
        $upstreamRef = (
          Invoke-AuditGit `
            -Arguments @(
              "-C",
              $record.Path,
              "for-each-ref",
              "--format=%(upstream)",
              $record.Branch
            ) `
            -Location "upstream configuration for $($record.Path)" `
            -Fix "Repair the local branch metadata, then rerun the audit."
        ) -join ""
        $upstreamRef = $upstreamRef.Trim()

        if ([string]::IsNullOrWhiteSpace($upstreamRef)) {
          $reasons.Add("no upstream") | Out-Null
        } else {
          $upstreamObject = (
            Invoke-AuditGit `
              -Arguments @(
                "-C",
                $record.Path,
                "for-each-ref",
                "--format=%(objectname)",
                $upstreamRef
              ) `
              -Location "upstream reference for $($record.Path)" `
              -Fix "Fetch or repair the configured upstream reference, then rerun the audit."
          ) -join ""

          if ([string]::IsNullOrWhiteSpace($upstreamObject)) {
            $reasons.Add("upstream reference is missing") | Out-Null
          } else {
            $counts = (
              Invoke-AuditGit `
                -Arguments @(
                  "-C",
                  $record.Path,
                  "rev-list",
                  "--left-right",
                  "--count",
                  "HEAD...$upstreamRef"
                ) `
                -Location "ahead/behind counts for $($record.Path)" `
                -Fix "Repair the branch history or upstream reference, then rerun the audit."
            ) -join ""
            if ($counts.Trim() -notmatch "^([0-9]+)\s+([0-9]+)$") {
              Stop-WorktreeAudit `
                -Location "git rev-list output for $($record.Path)" `
                -Reason "Ahead/behind counts could not be parsed: $counts" `
                -Fix "Inspect the installed Git output and update the parser deliberately."
            }
            $ahead = [int] $Matches[1]
            $behind = [int] $Matches[2]
            if (($ahead -ne 0) -or ($behind -ne 0)) {
              $reasons.Add("out of sync ahead=$ahead behind=$behind") | Out-Null
            }
          }
        }
      }
    }

    if (($reasons.Count -eq 0) -and ($ahead -eq 0) -and ($behind -eq 0)) {
      $upstreamDisplay = $upstreamRef.Replace("refs/remotes/", "").Replace("refs/heads/", "")
      Write-Output (
        "REVIEW-CANDIDATE path=`"$($record.Path)`" branch=`"$branchDisplay`" " +
        "upstream=`"$upstreamDisplay`" ahead=0 behind=0 " +
        "manual-confirmation=`"Confirm the task and PR are complete before removal.`""
      )
    } else {
      Write-Output (
        "ATTENTION path=`"$($record.Path)`" branch=`"$branchDisplay`" " +
        "reason=`"$($reasons -join '; ')`""
      )
    }
  }
} catch {
  Stop-WorktreeAudit `
    -Location "scripts/audit-worktrees.ps1" `
    -Reason "Unexpected PowerShell failure: $($_.Exception.Message)" `
    -Fix "Inspect the reported failure without changing worktree or branch state, then rerun."
}
