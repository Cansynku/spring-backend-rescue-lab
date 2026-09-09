@echo off
setlocal
cd /d "%~dp0.."
set "DEMO_JAVA=java"
for /d %%J in (".tools\java21\*") do if exist "%%~fJ\bin\java.exe" set "DEMO_JAVA=%%~fJ\bin\java.exe"
set "DEMO_JAR=.local\standalone-build\target\spring-backend-rescue-lab-0.0.1-SNAPSHOT.jar"
if not exist "%DEMO_JAR%" (
  echo Falta el paquete de demo autonoma. Consulta docs/demo-screen.md.
  pause
  exit /b 1
)
echo DEMO AUTONOMA - No utiliza PostgreSQL ni dinero real.
echo Abre http://127.0.0.1:8083 cuando aparezca Started BackendRescueApplication.
echo Deja esta ventana abierta. Ctrl+C detiene la demo.
"%DEMO_JAVA%" -jar "%DEMO_JAR%" --spring.profiles.active=standalone-demo
pause
