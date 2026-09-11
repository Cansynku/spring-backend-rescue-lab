# Local WSL installation described in docs/local-postgres-recovery.md.
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$demoJava = 'java'
$portableJava = Get-ChildItem '.tools/java21' -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path (Join-Path $_.FullName 'bin/java.exe') } | Select-Object -First 1
if ($portableJava) { $demoJava = Join-Path $portableJava.FullName 'bin/java.exe' }
$demoJar = 'target/spring-backend-rescue-lab-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path $demoJar)) { throw 'Falta compilar la aplicacion. Consulta docs/local-postgres-recovery.md.' }
if (Get-NetTCPConnection -LocalPort 8084 -State Listen -ErrorAction SilentlyContinue) {
    throw 'El puerto 8084 ya esta ocupado. No se ha detenido ninguna aplicacion.'
}
# A systemd service alone does not keep WSL alive. This child lives with the demo.
$wslSession = Start-Process wsl.exe -ArgumentList '-d BackendRescue -u root -- sh -c "systemctl start postgresql@17-main && exec sleep infinity"' -WindowStyle Hidden -PassThru
try {
    $ready = $false
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        if ($wslSession.HasExited) { throw 'No se pudo iniciar PostgreSQL en BackendRescue.' }
        $probe = New-Object System.Net.Sockets.TcpClient
        try { $probe.Connect('127.0.0.1', 55433); $ready = $true } catch {} finally { $probe.Dispose() }
        if ($ready) { break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'PostgreSQL no responde en 127.0.0.1:55433.' }
    Write-Host 'Abre http://127.0.0.1:8084 cuando aparezca Started BackendRescueApplication.'
    Write-Host 'Esta demo utiliza una base PostgreSQL separada de la demo del puerto 8083.'
    Write-Host 'Deja esta ventana abierta. Ctrl+C detiene la demo.'
    & $demoJava -jar $demoJar '--server.port=8084' '--spring.datasource.url=jdbc:postgresql://127.0.0.1:55433/backend_rescue_ui' '--spring.datasource.username=backend_rescue' '--spring.datasource.password=backend_rescue' '--payment-provider.base-url=http://127.0.0.1:8084/sandbox-provider'
    if ($LASTEXITCODE -ne 0) { throw 'La aplicacion no termino correctamente; revisa el mensaje anterior.' }
} finally {
    if (-not $wslSession.HasExited) { Stop-Process -Id $wslSession.Id }
}
