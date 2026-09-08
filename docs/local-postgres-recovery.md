# PostgreSQL local recuperado mediante WSL

Estado verificado el 07/09/2026: nueva instancia PostgreSQL 17.11 en Debian 13,
distribucion WSL2 `BackendRescue`, accesible desde Java en Windows por
`127.0.0.1:55433`. Las 66 pruebas del proyecto pasan sin errores ni omisiones,
incluido el recorrido HTTP de pedidos, pagos y repeticion. Una fila sintetica
tambien se conserva tras reiniciar PostgreSQL; su tabla de prueba se retiro.

## Uso en este equipo

Ejecutar con doble clic `scripts/abrir-demo-postgresql.cmd` y mantener la ventana
abierta. Cuando aparezca `Started BackendRescueApplication`, abrir
http://127.0.0.1:8084/. El lanzador mantiene una sesion WSL mientras vive la app;
los servicios systemd por si solos no mantienen WSL activo. No requiere cambiar
la politica de ejecucion de PowerShell ni la seguridad de Windows.

La sintaxis del lanzador y sus operaciones de base de datos se han comprobado.
El usuario abrió la demo PostgreSQL en 8084 el 07/09/2026: GET / devolvió 200 y
`scripts/smoke.ps1 -BaseUrl http://127.0.0.1:8084` terminó PASS: creación 201,
pago 201, repetición 200, PAID y un pago. Evidencia comunicada en el punto de
continuidad; no se ha vuelto a ejecutar el smoke en este cierre documental.
Los informes de la suite previa en `.local/pg-wsl-validation/target/surefire-reports` se han releído: 66 tests, cero fallos, errores y omisiones; no es una nueva ejecución.

El paquete verificado esta en `.local/pg-wsl-validation/target/`. El lanzador usa
ese paquete y, si no existe, `target/`. Para recompilar desde el repositorio,
usar Java 21 y `mvn clean verify`; para verificar PostgreSQL usar
`scripts/verify-postgres.ps1 -Port 55433` con WSL activo. Esa verificacion utiliza
exclusivamente `backend_rescue_test`, que contiene datos desechables.

## Bases separadas

| Uso | Ubicacion | Estado |
| --- | --- | --- |
| PostgreSQL nuevo | WSL BackendRescue, puerto 55433, `backend_rescue_ui` | Demo arrancada y smoke PASS el 07/09/2026; contiene datos sintéticos que deben conservarse |
| Pruebas PostgreSQL | Misma instancia, `backend_rescue_test` | 66 pruebas aprobadas |
| Demo autonoma abierta | Puerto 8083, `.local/standalone-demo/orders` | Responde HTTP 200; conserva sus propios datos H2 |
| PostgreSQL antiguo | Puerto 55432, `.local/pgdata` | Archivos conservados; ejecutable Windows bloqueado; datos no recuperados ni migrados |

Los datos nuevos de PostgreSQL estan en el disco de la distribucion WSL, en
`/var/lib/postgresql/17/main`. No borrar ni desregistrar esa distribucion: contiene
la base. No se han trasladado pedidos de H2 ni del cluster antiguo. El rol
`backend_rescue` solo tiene LOGIN y es propietario de estas dos bases; utiliza
los valores desechables de la demo. PostgreSQL escucha solo en localhost.

## Causa y solucion aplicada

Windows Code Integrity registro bloqueos 3077/3033 para el servidor portable y
el Control inteligente de aplicaciones esta activo. El instalador oficial EDB
17.11-3 tiene firma valida y se verifico su SHA-256 publicado, pero su
`postgres.exe` extraido es el mismo binario sin firma que ya estaba bloqueado.
Reinstalar ese binario no resolvia la causa. No se cambio ninguna proteccion.

Se instalo Debian mediante `wsl --install Debian --name BackendRescue --no-launch
--version 2` y PostgreSQL mediante los paquetes oficiales de Debian. Se configuro
el puerto 55433 y escucha localhost, preservando el cluster Windows anterior.
La primera prueba revelo el cierre de WSL por inactividad; manteniendo una sesion
WSL explicita, la verificacion completa termino correctamente.

Referencias: [instalacion WSL](https://learn.microsoft.com/en-us/windows/wsl/basic-commands),
[duracion de servicios systemd en WSL](https://learn.microsoft.com/en-us/windows/wsl/systemd),
[instalador PostgreSQL Windows](https://www.postgresql.org/download/windows/).
