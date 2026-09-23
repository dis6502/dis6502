<#
.SYNOPSIS
    Triggers the "Release" GitHub Actions workflow (.github/workflows/release.yml)
    manually via workflow_dispatch, instead of pushing a v* tag.

.DESCRIPTION
    The Release workflow builds portable app-images for Windows/Linux/macOS and,
    when triggered by a v* tag push, publishes them to a GitHub Release. It can
    also be run manually in one of two modes, matching the two workflow_dispatch
    behaviors documented in the workflow file's own header comment:

      - Plain test build (no -ReleaseTag): builds and uploads artifacts to the
        workflow run only; no release is created or touched.
      - Republish (-ReleaseTag <existing tag>, e.g. v4.0.0): rebuilds whatever
        -Ref currently is and republishes those binaries as assets on that
        EXISTING release, without moving the release's git tag.

    Running this script immediately triggers a real workflow run on GitHub -
    there is no dry-run mode.

    Requires the GitHub CLI (`gh`), already authenticated (`gh auth status`).

.PARAMETER ReleaseTag
    An existing release's tag (e.g. v4.0.0) to rebuild and republish assets to.
    Omit for a plain test build - no release is touched.

.PARAMETER Ref
    Branch, tag, or commit to run the workflow against. Defaults to the current
    branch.

.EXAMPLE
    .\build\trigger-release-workflow.ps1
    Triggers a plain test build on the current branch - builds artifacts, publishes nothing.

.EXAMPLE
    .\build\trigger-release-workflow.ps1 -ReleaseTag v4.0.0
    Rebuilds the current branch and republishes its binaries onto the existing v4.0.0 release.
#>
[CmdletBinding()]
param(
    [string]$ReleaseTag = "",
    [string]$Ref = ""
)

$RepoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $RepoRoot
try {
    if (-not $Ref) {
        $Ref = (git rev-parse --abbrev-ref HEAD).Trim()
    }

    $ghArgs = @("workflow", "run", "release.yml", "--ref", $Ref)
    if ($ReleaseTag) {
        $ghArgs += @("-f", "release_tag=$ReleaseTag")
    }

    $description = if ($ReleaseTag) { "republishing assets onto existing release '$ReleaseTag'" } else { "plain test build, nothing published" }
    Write-Host "gh $($ghArgs -join ' ')"
    Write-Host "($description, ref '$Ref')"

    & gh @ghArgs
    Write-Host "`nTriggered. Watch it with: gh run watch (pick the latest 'Release' run), or gh run list --workflow=release.yml"
}
finally {
    Pop-Location
}
