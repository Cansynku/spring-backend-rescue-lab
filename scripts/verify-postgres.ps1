# Uses a dedicated disposable database. Never point create-drop tests at application data.
$ErrorActionPreference = 'Stop'
mvn -B -ntp '-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:55432/backend_rescue_test' '-Dspring.datasource.driver-class-name=org.postgresql.Driver' '-Dspring.datasource.username=backend_rescue' '-Dspring.datasource.password=backend_rescue' clean verify
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL verification failed' }
