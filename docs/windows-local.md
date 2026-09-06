# This Windows workspace

This checkout has a project-local JDK 21 and PostgreSQL under ignored `.tools/`, with database data under ignored `.local/`. These binaries are not part of the Git repository. A fresh clone should use the standard README prerequisites.

From a PowerShell terminal opened in this directory, select the portable JDK for **that terminal only**:

```powershell
$env:JAVA_HOME = (Get-ChildItem .tools/java21 -Directory | Select-Object -First 1).FullName
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
mvn clean verify
```

Start the already initialized local PostgreSQL cluster if stopped:

```powershell
& .tools/postgres/pgsql/bin/pg_ctl.exe -D .local/pgdata -l .local/postgres.log -w start
```

Start the app in the foreground (if port 8080 is free):

```powershell
java -jar target/spring-backend-rescue-lab-0.0.1-SNAPSHOT.jar
```

Run `./scripts/smoke.ps1` from a second terminal. Ctrl+C stops a foreground app.

The initial verification session may have left the app running in the background, with its process ID in `.local/app.pid`. The following checks the command before stopping **that exact process**:

```powershell
$labProcessId = [int](Get-Content .local/app.pid)
$labProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $labProcessId"
if ($labProcess.Name -eq 'java.exe' -and $labProcess.CommandLine -like '*spring-backend-rescue-lab-0.0.1-SNAPSHOT.jar*') {
    Stop-Process -Id $labProcessId
}
```

After stopping the application, stop the isolated database without deleting its data:

```powershell
& .tools/postgres/pgsql/bin/pg_ctl.exe -D .local/pgdata -m fast -w stop
```

Local diagnostic logs are in `.local/`; build results are in `target/surefire-reports/`. Logs should be redacted before sharing.
