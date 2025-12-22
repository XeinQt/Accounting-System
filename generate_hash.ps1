# PowerShell script to generate hashed password SQL
$ErrorActionPreference = "Stop"

# Compile and run Java
javac GenerateHash.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed"
    exit 1
}

java GenerateHash
if ($LASTEXITCODE -ne 0) {
    Write-Host "Execution failed"
    exit 1
}

# Display the generated file
if (Test-Path "admin_insert_hashed.sql") {
    Write-Host "`n========================================"
    Write-Host "Generated SQL file:"
    Write-Host "========================================"
    Get-Content "admin_insert_hashed.sql"
} else {
    Write-Host "File was not created"
}




