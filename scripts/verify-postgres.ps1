# Uses only the disposable test database; never target application data.
param(
    [ValidateRange(1, 65535)][int]$Port = 55432,
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9_.-]*$')][string]$WslDistribution
)
$ErrorActionPreference = 'Stop'
$projectPom = Join-Path (Split-Path $PSScriptRoot -Parent) 'pom.xml'
$wslSession = $null
try {
    if ($WslDistribution) {
        # A systemd service alone does not keep WSL alive. Own only this child session.
        $wslSession = Start-Process wsl.exe -ArgumentList @('-d', $WslDistribution, '--', 'sleep', 'infinity') -WindowStyle Hidden -PassThru
        $ready = $false
        for ($attempt = 0; $attempt -lt 20; $attempt++) {
            if ($wslSession.HasExited) { throw 'The WSL session exited before PostgreSQL became available.' }
            $probe = [Net.Sockets.TcpClient]::new()
            try {
                $connection = $probe.ConnectAsync('127.0.0.1', $Port)
                $ready = $connection.Wait(1000) -and $probe.Connected
            } catch {} finally { $probe.Dispose() }
            if ($ready) { break }
            Start-Sleep -Seconds 1
        }
        if (-not $ready) { throw "PostgreSQL is not reachable on localhost:$Port. Check the existing service in $WslDistribution." }
    }
    & mvn -f $projectPom -B -ntp "-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:$Port/backend_rescue_test" '-Dspring.datasource.driver-class-name=org.postgresql.Driver' '-Dspring.datasource.username=backend_rescue' '-Dspring.datasource.password=backend_rescue' clean verify
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL verification failed' }
} finally {
    if ($wslSession -and -not $wslSession.HasExited) { Stop-Process -Id $wslSession.Id }
}
