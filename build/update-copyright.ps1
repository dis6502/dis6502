<#
.SYNOPSIS
    Updates the year in every "Copyright (C) <year> ... Peter Dell ..." header comment under
    src\ and test\ to the current year (or a year given via -Year).

.DESCRIPTION
    Every dis6502 Java source file starts with a header comment like:

        /*
         * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
         *
         * This file is part of dis6502.
         */

    This script finds every file under src\ and test\ (recursively, any extension) containing a
    "Copyright (C) <4-digit-year>" line and rewrites just the year, leaving everything else in the
    file - including line endings and the absence of a BOM - untouched. Files whose year already
    matches the target are left alone (and not reported as changed).

.PARAMETER Year
    The year to set. Defaults to the current year.

.PARAMETER Apply
    Without -Apply (the default), the script only lists the files it would change (safe, read-only).
    With -Apply, it rewrites them in place.

.EXAMPLE
    .\build\update-copyright.ps1
    Dry run: lists files whose copyright year is not the current year.

.EXAMPLE
    .\build\update-copyright.ps1 -Apply
    Updates every such file to the current year.

.EXAMPLE
    .\build\update-copyright.ps1 -Year 2027 -Apply
    Updates every such file to a specific year.
#>
[CmdletBinding()]
param(
    [int]$Year = (Get-Date).Year,
    [switch]$Apply
)

$RepoRoot = Split-Path -Parent $PSScriptRoot
$Dirs = @("src", "test") | ForEach-Object { Join-Path $RepoRoot $_ } | Where-Object { Test-Path $_ }

$Pattern = "Copyright \(C\) (\d{4})"
$Utf8NoBom = New-Object System.Text.UTF8Encoding $false

$changedCount = 0
$files = Get-ChildItem -Path $Dirs -Recurse -File

foreach ($f in $files) {
    $text = [System.IO.File]::ReadAllText($f.FullName)
    $match = [regex]::Match($text, $Pattern)
    if (-not $match.Success) {
        continue
    }
    $currentYear = $match.Groups[1].Value
    if ($currentYear -eq "$Year") {
        continue
    }

    $relativePath = $f.FullName.Substring($RepoRoot.Length + 1)
    if ($Apply) {
        $newText = [regex]::Replace($text, $Pattern, "Copyright (C) $Year")
        [System.IO.File]::WriteAllText($f.FullName, $newText, $Utf8NoBom)
        Write-Host "Updated $relativePath ($currentYear -> $Year)"
    }
    else {
        Write-Host "Would update $relativePath ($currentYear -> $Year)"
    }
    $changedCount++
}

if ($changedCount -eq 0) {
    Write-Host "No files need updating - all copyright years already $Year."
}
elseif (-not $Apply) {
    Write-Host "`n$changedCount file(s) would be updated. Re-run with -Apply to write the changes."
}
else {
    Write-Host "`n$changedCount file(s) updated."
}
