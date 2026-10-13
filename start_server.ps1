$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:JAVA_HOME = "$ScriptDir\.tools\jdk\jdk21.0.12_9"
$env:PATH = "$env:JAVA_HOME\bin;$ScriptDir\.tools\mvn\apache-maven-3.9.9\bin;$env:PATH"

mvn spring-boot:run "-Dspring-boot.run.profiles=h2"
