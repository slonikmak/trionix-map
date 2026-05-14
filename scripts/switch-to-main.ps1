<#
PowerShell script to fully switch the repository default branch from `master` to `main`.

What it does:
- Commits local changes (the script will stage changes in the working tree)
- Renames local branch `master` -> `main` (if necessary)
- Pushes `main` to origin and sets upstream
- Updates origin HEAD and sets local main upstream
- Sets repository default branch on GitHub (via gh CLI if available, otherwise via PAT+API)
- Optionally deletes remote `master`

Usage:
  Open PowerShell in the repository root and run:
    .\scripts\switch-to-main.ps1

You will be prompted for confirmation and (only if gh is not available) for a GitHub Personal Access Token (PAT) with `repo` or `admin:repo_hook` privileges.

Note: This script MUST be executed by a user with admin rights on the GitHub repository to change default branch and branch protection.
#>

function Abort([string]$msg){ Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }

Write-Host "Preparing to switch repository default branch to 'main'..." -ForegroundColor Cyan

Push-Location (Get-Location)

# Ensure we're in a git repository
if (-not (Test-Path .git)) { Abort "This script must be run from the repository root (where .git is located)." }

# Helper: run-exit
function Run([string]$cmd) {
    Write-Host "> $cmd"
    $rv = & cmd /c "$cmd"
    return $LASTEXITCODE
}

# Check for required binaries
$haveGit = (Get-Command git -ErrorAction SilentlyContinue) -ne $null
$haveGh  = (Get-Command gh  -ErrorAction SilentlyContinue) -ne $null

if (-not $haveGit) { Abort "git is not installed or not available in PATH." }

# Detect repo root via git
$repoRoot = git rev-parse --show-toplevel 2>$null
if ($LASTEXITCODE -ne 0) { Abort "Not inside a git repository." }

Write-Host "Repository root: $repoRoot"

# Show current branch
$currentBranch = git rev-parse --abbrev-ref HEAD
Write-Host "Current branch: $currentBranch"

# Ask user to continue
$confirm = Read-Host "Proceed with switching default branch to 'main' in this repository? (y/N)"
if ($confirm.ToLower() -ne 'y') { Write-Host "Aborted by user."; exit 0 }

# Stage and commit any local changes that are present in this working tree.
Write-Host "Staging updated files in working tree (if any)..." -ForegroundColor Yellow
git add .github/workflows/test.yml pom.xml docs/RELEASING.md 2>$null

# If there are other staged changes or unstaged changes, show status and give choice
$status = git status --porcelain
if ($status) {
    Write-Host "Git status (uncommitted changes):`n$status`n" -ForegroundColor Yellow
    $commitChoice = Read-Host "Commit these changes with message 'Switch default branch to main' now? (y/N)"
    if ($commitChoice.ToLower() -eq 'y') {
        git commit -m "Switch default branch to main: update workflows and docs" || Abort "git commit failed"
        Write-Host "Committed changes." -ForegroundColor Green
    } else {
        Write-Host "Leaving uncommitted changes as-is." -ForegroundColor Yellow
    }
} else {
    Write-Host "Working tree is clean." -ForegroundColor Green
}

# Determine origin URL and owner/repo
$remoteUrl = git config --get remote.origin.url
if (-not $remoteUrl) { Abort "No remote 'origin' configured." }

Write-Host "Remote origin URL: $remoteUrl"

function Parse-GitHubRemote([string]$url) {
    # support: git@github.com:owner/repo.git or https://github.com/owner/repo.git
    if ($url -match "git@github.com:(.+)/(.+)(\.git)?$") { return @{ owner=$matches[1]; repo=$matches[2].TrimEnd('.git') } }
    if ($url -match "https?://github.com/(.+)/(.+)(\.git)?$") { return @{ owner=$matches[1]; repo=$matches[2].TrimEnd('.git') } }
    return $null
}

$parsed = Parse-GitHubRemote $remoteUrl
if (-not $parsed) { Write-Host "Warning: cannot parse remote origin as GitHub URL. Will still perform git operations but cannot set default branch on GitHub without repo info." -ForegroundColor Yellow }
else { Write-Host "Detected GitHub repository: $($parsed.owner)/$($parsed.repo)" }

# Rename local branch if needed
if ($currentBranch -eq 'main') {
    Write-Host "Local branch already 'main'." -ForegroundColor Green
} elseif ($currentBranch -eq 'master') {
    Write-Host "Renaming local branch 'master' -> 'main'..."
    git branch -m main || Abort "Failed to rename branch"
    $currentBranch = 'main'
    Write-Host "Renamed."
} else {
    # If current branch is something else, ensure main exists or create from current
    $hasMain = git show-ref --verify --quiet refs/heads/main; if ($LASTEXITCODE -eq 0) { $hasMain = $true } else { $hasMain = $false }
    if (-not $hasMain) {
        $create = Read-Host "Local 'main' branch not found. Create 'main' from current branch '$currentBranch'? (y/N)"
        if ($create.ToLower() -eq 'y') {
            git branch main || Abort "Failed to create main"
            Write-Host "Created local branch 'main'."
        } else {
            Write-Host "Skipping creation of local 'main'. You can create it manually later." -ForegroundColor Yellow
        }
    } else {
        Write-Host "Local 'main' branch already exists." -ForegroundColor Green
    }
}

# Push main to origin and set upstream
Write-Host "Pushing 'main' to origin and setting upstream..."
git push -u origin main || Abort "Failed to push 'main' to origin. Resolve remote permissions or conflicts and retry."

# Update origin HEAD
Write-Host "Updating origin/HEAD to point to origin/main..."
git remote set-head origin -a || Write-Host "Warning: git remote set-head failed. You can run 'git remote set-head origin -a' manually." -ForegroundColor Yellow

# Ensure local main tracks origin/main
git fetch origin main:main 2>$null | Out-Null
git branch -u origin/main main 2>$null

# Set default branch on GitHub
if ($parsed) {
    if ($haveGh) {
        Write-Host "Setting default branch on GitHub using 'gh' CLI..."
        gh repo edit --default-branch main || Write-Host "gh repo edit failed. You can run: gh repo edit --default-branch main" -ForegroundColor Yellow
    } else {
        Write-Host "gh CLI not found. Will attempt to set default branch via GitHub API using a PAT." -ForegroundColor Yellow
        $pat = Read-Host "Enter a GitHub Personal Access Token (PAT) with repo admin rights (input hidden)" -AsSecureString
        $patPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($pat))
        if (-not $patPlain) { Write-Host "No token provided; skipping automatic GitHub default branch update." -ForegroundColor Yellow }
        else {
            $body = @{ default_branch = 'main' }
            $uri = "https://api.github.com/repos/$($parsed.owner)/$($parsed.repo)"
            try {
                $hdrs = @{ Authorization = "token $patPlain"; Accept = 'application/vnd.github+json'; 'User-Agent' = 'switch-to-main-script' }
                $resp = Invoke-RestMethod -Uri $uri -Method Patch -Headers $hdrs -Body ($body | ConvertTo-Json -Depth 10)
                Write-Host "GitHub default branch updated to 'main'." -ForegroundColor Green
            } catch {
                Write-Host "Failed to update repository default via API: $_" -ForegroundColor Red
            }
            # Clear PAT from memory
            $patPlain = $null
        }
    }
} else {
    Write-Host "Repository origin does not appear to be GitHub; skipping GitHub default-branch update." -ForegroundColor Yellow
}

# Optionally delete remote master
$deleteMaster = Read-Host "Do you want to delete remote 'master' branch now? This is irreversible for others. (y/N)"
if ($deleteMaster.ToLower() -eq 'y') {
    Write-Host "Deleting remote 'master'..."
    git push origin --delete master || Write-Host "Failed to delete remote master. You may not have permission or branch may not exist." -ForegroundColor Yellow
} else { Write-Host "Skipping deletion of remote 'master'." }

Write-Host "Done. Please verify branch protection rules and CI settings on GitHub (Settings → Branches)." -ForegroundColor Cyan

Pop-Location

