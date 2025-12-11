<#
.SYNOPSIS
    Runs the Top-K Benchmark.
.DESCRIPTION
    Compiles the Java source code and runs the TopKEvaluation benchmark.
    Allows configuring the event rate and duration.
.PARAMETER Rate
    Events per second (default: 5000).
.PARAMETER Duration
    Duration in seconds (default: 80).
.EXAMPLE
    .\run_benchmark.ps1
    Runs with default settings.
.EXAMPLE
    .\run_benchmark.ps1 -Rate 1000 -Duration 10
    Runs with 1000 events/sec for 10 seconds.
#>
param (
    [int]$Rate = 5000,
    [int]$Duration = 80
)

$ErrorActionPreference = "Stop"

Write-Host "Compiling..." -ForegroundColor Cyan
if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Force -Path "bin" | Out-Null
}

javac -d bin -sourcepath src src/topk/TopKEvaluation.java
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed."
    exit 1
}

Write-Host "Running Benchmark (Rate=$Rate, Duration=$Duration)..." -ForegroundColor Cyan
java -cp bin topk.TopKEvaluation $Rate $Duration
