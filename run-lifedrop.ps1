# LifeDrop Spring Boot Full-Stack Application Launcher
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$env:JAVA_HOME = "$ScriptDir\.tools\jdk\jdk21.0.12_9"
$env:PATH = "$env:JAVA_HOME\bin;$ScriptDir\.tools\mvn\apache-maven-3.9.9\bin;$env:PATH"

Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host " Starting LifeDrop Blood Bank Full-Stack Application" -ForegroundColor Green
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host "Java Version:" -ForegroundColor Yellow
java -version

Write-Host "`nWeb Application URL:       http://localhost:8080/" -ForegroundColor Green
Write-Host "Swagger OpenAPI Docs:      http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Write-Host "H2 Web Console (fallback): http://localhost:8080/h2-console`n" -ForegroundColor Green

mvn spring-boot:run
