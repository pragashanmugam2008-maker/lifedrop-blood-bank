$conns = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($conns) {
    foreach ($c in $conns) {
        $procId = $c.OwningProcess
        if ($procId -gt 0) {
            Write-Host "Stopping process on port 8080 (PID: $procId)"
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
    }
    Start-Sleep -Seconds 1
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:JAVA_HOME = "$ScriptDir\.tools\jdk\jdk21.0.12_9"
$env:PATH = "$env:JAVA_HOME\bin;$ScriptDir\.tools\mvn\apache-maven-3.9.9\bin;$env:PATH"

mvn spring-boot:run
