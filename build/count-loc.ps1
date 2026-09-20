<#
.SYNOPSIS
    Counts *.java files, lines of code, and file size, split into "src" (production code, under
    src\, recursively) and "test" (test code, under test\, recursively), plus an overall total
    across both categories.

.DESCRIPTION
    Mirrors the C++ project's build\count-loc.ps1, but for the Java port: instead of splitting
    .h/.cpp by ui vs. rest, this splits the single .java extension by production vs. test code,
    matching pom.xml's <sourceDirectory>src</sourceDirectory>/<testSourceDirectory>test</testSourceDirectory>.
    target\ (Maven build output) and test-resources\ (data, not code) are not under src\/test\,
    so they are excluded automatically by only scanning those two directories.

.EXAMPLE
    .\build\count-loc.ps1
#>
[CmdletBinding()]
param()

$RepoRoot = Split-Path -Parent $PSScriptRoot
$SrcDir = Join-Path $RepoRoot "src"
$TestDir = Join-Path $RepoRoot "test"

function Get-Stats {
    param([System.IO.FileInfo[]]$Files)

    $lines = 0
    $bytes = 0
    foreach ($f in $Files) {
        $lines += (Get-Content -LiteralPath $f.FullName).Count
        $bytes += $f.Length
    }
    [PSCustomObject]@{
        Files = $Files.Count
        Lines = $lines
        Bytes = $bytes
    }
}

function Format-Size {
    param([long]$Bytes)
    $ic = [System.Globalization.CultureInfo]::InvariantCulture
    if ($Bytes -ge 1MB) { return [string]::Format($ic, "{0:N2} MB", ($Bytes / 1MB)) }
    if ($Bytes -ge 1KB) { return [string]::Format($ic, "{0:N2} KB", ($Bytes / 1KB)) }
    return "$Bytes bytes"
}

function Get-CategoryRow {
    param([string]$Category, [System.IO.FileInfo[]]$Files)

    $stats = Get-Stats $Files
    [PSCustomObject]@{
        Category = $Category
        Files    = $stats.Files
        Lines    = $stats.Lines
        Size     = Format-Size $stats.Bytes
    }
}

$srcFiles = @(Get-ChildItem -Path $SrcDir -Recurse -File -Include "*.java" -ErrorAction SilentlyContinue)
$testFiles = @(Get-ChildItem -Path $TestDir -Recurse -File -Include "*.java" -ErrorAction SilentlyContinue)
$allFiles = $srcFiles + $testFiles

$rows = @(Get-CategoryRow "src" $srcFiles) + @(Get-CategoryRow "test" $testFiles) + @(Get-CategoryRow "overall" $allFiles)

$rows | Format-Table -AutoSize
