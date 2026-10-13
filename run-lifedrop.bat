@echo off
echo ===================================================================
echo  Starting LifeDrop Blood Bank Full-Stack Application
echo ===================================================================

set "JAVA_HOME=%~dp0.tools\jdk\jdk21.0.12_9"
set "PATH=%JAVA_HOME%\bin;%~dp0.tools\mvn\apache-maven-3.9.9\bin;%PATH%"

echo Java Version:
java -version
echo.
echo Starting Spring Boot Server on http://localhost:8080 ...
echo Swagger UI Documentation: http://localhost:8080/swagger-ui.html
echo LifeDrop Web UI:          http://localhost:8080/
echo.

mvn spring-boot:run
