@echo off
setlocal
cd /d "%~dp0.."
set "DEMO_JAVA=java"
for /d %%J in (".tools\java21\*") do if exist "%%~fJ\bin\java.exe" set "DEMO_JAVA=%%~fJ\bin\java.exe"
set "DEMO_JAR=.local\ui-validation\target\spring-backend-rescue-lab-0.0.1-SNAPSHOT.jar"
if not exist "%DEMO_JAR%" set "DEMO_JAR=target\spring-backend-rescue-lab-0.0.1-SNAPSHOT.jar"
if not exist "%DEMO_JAR%" (
  echo Falta compilar la aplicacion. Consulta docs/demo-screen.md.
  pause
  exit /b 1
)
echo Abre http://127.0.0.1:8083 cuando la aplicacion termine de arrancar.
echo Deja esta ventana abierta. Ctrl+C detiene la demo.
"%DEMO_JAVA%" -jar "%DEMO_JAR%" --server.port=8083 --spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/backend_rescue_ui --payment-provider.base-url=http://127.0.0.1:8083/sandbox-provider
pause
