# Pantalla de demostración

La app ya incluye una pantalla en español para crear pedidos, simular pagos y comprobar que repetir una petición no duplica el pago. Se sirve desde el propio backend; no necesita una aplicación frontend ni dependencias instaladas aparte.

## Abrirla en este equipo

**PostgreSQL recuperado en una instancia nueva:** usa `scripts/abrir-demo-postgresql.cmd`
y abre http://127.0.0.1:8084/ cuando arranque. El usuario ya abrió esta variante el
07/09/2026: GET / 200 y smoke PASS (201/201/200, PAID, un pago). La suite previa
registró 66 tests sin fallos, errores ni omisiones. No se han repetido en este cierre documental. [Detalle y separacion de datos](local-postgres-recovery.md).
La demo autonoma de 8083 ya responde HTTP 200 y puede seguir utilizandose.

**Actualización 07/09:** Windows bloquea el PostgreSQL portable (eventos Code Integrity 3077/3033 para `postgres.exe` y bloqueo explícito de `pg_ctl.exe`). El doble clic del usuario sí lanzó Java; falló la conexión JDBC con `Connection reset`. No es un error de copiar comandos ni de migraciones. No se ha cambiado la política de Windows.

Para probar la pantalla sin ese proceso bloqueado, utiliza **`scripts/abrir-demo-autonoma.cmd`**. El paquete `standalone-demo` incluye H2 y guarda sus datos en `.local/standalone-demo/orders` dentro del proyecto. Se abre igualmente en http://127.0.0.1:8083/. Es una base separada: no importa ni modifica datos de PostgreSQL. La configuración normal de la API continúa usando PostgreSQL. Solo puede abrirse una instancia de esta demo a la vez.

No pegues el contenido del archivo en CMD. Puedes ejecutarlo con doble clic o pegar únicamente su ruta completa entre comillas. Espera el mensaje `Started BackendRescueApplication` antes de abrir la página. La apertura de PostgreSQL en 8084 ya se confirmó el 07/09/2026; si sigue abierta, no lances otra instancia. H2 en 8083 conserva datos separados.

Para reconstruir el paquete autónomo en este equipo: `mvn -f .local/standalone-build/pom.xml -Pstandalone-demo clean verify` después de sincronizar allí el código. Desde un clon normal: `mvn -Pstandalone-demo clean verify` genera el paquete en `target/`; ejecútalo con `--spring.profiles.active=standalone-demo`. La prueba `StandaloneStorageTest` verifica migraciones y conservación del pedido tras cerrar y volver a abrir la base de archivo.

### Referencia histórica: arranque original bloqueado (no ejecutar en este equipo)

1. Ejecuta `scripts/abrir-demo.cmd` con doble clic y deja la ventana abierta.
2. Cuando termine de arrancar, abre [la demo local](http://127.0.0.1:8083/).
3. Usa el correo de prueba y el importe propuestos. Pulsa **Crear pedido**, después **Simular pago** y finalmente **Repetir sin duplicar**. El pedido debe seguir mostrando un solo intento de pago.

La base `backend_rescue_ui` ya se creó en el PostgreSQL local del proyecto. Está separada de las bases anteriores. No se ha migrado ni alterado la baseline. El control automático rechazó el arranque persistente de la nueva instancia, sin indicar motivo: por eso el primer arranque queda en manos del usuario. Esta URL no se presenta como disponible hasta que la aplicación esté arrancada. El lanzador tampoco se ha ejecutado ni validado como proceso persistente.

El paquete compilado está en `.local/ui-validation/target/` en este equipo. El lanzador utiliza ese paquete si existe; si no, utiliza `target/`. En otro equipo hay que tener Java 21, PostgreSQL y una base vacía `backend_rescue_ui`, y compilar con `mvn clean package`. El script no instala programas ni crea bases por su cuenta. Si el puerto 8083 ya está ocupado, revisar el proceso existente; no lo detiene automáticamente.

## Qué verás

- Un formulario con correo e importe y un listado de pedidos conservados en la base elegida: PostgreSQL o H2 en la demo autonoma.
- Estado de conexión, recuentos y estado de cada pedido.
- Confirmación de pago, repetición sin duplicados y mensajes de resultado pendiente.
- Una referencia de operación para localizar sus eventos cuando la API la proporciona.

La pantalla limita los importes a 999.999,99 para esta demo. La API mantiene su contrato de 17 dígitos enteros; valores antiguos fuera del rango de la pantalla no se redondean ni se envían a pagar. No se asigna moneda porque el dominio todavía no la define.

Las claves de pago se conservan en el almacenamiento local de este navegador, por pedido, para reutilizarlas tras un fallo o una recarga. No se almacena el correo. Si ese almacenamiento está bloqueado, el pago no se envía. Si se borra o se cambia de navegador, un intento ya registrado continúa protegido por el backend; la pantalla no inventa una recuperación del resultado. No hay reintentos automáticos ni promesa de recuperación de pagos inciertos.

## Validación y límites

`DemoHttpTest` arranca una instancia de prueba en un puerto aleatorio: comprueba la pantalla y sus recursos y recorre creación, pago, repetición y lectura usando HTTP real con un proveedor simulado. Esa prueba sí ha arrancado y probado la aplicación; no equivale al arranque persistente del lanzador sobre `backend_rescue_ui`.

`node --test scripts/test-demo-ui.cjs` prueba la lógica JavaScript con una superficie DOM simulada: reutilización de clave tras perder una respuesta y recargar, bloqueo preventivo cuando el almacenamiento falla, y validación/conversión del importe. El 07/09/2026 se comprobó además el navegador integrado sobre 8084: crear un pedido sintético de 25,50, simular el pago y repetir. El pedido d94f825a pasó de cero a un intento y permaneció en uno, con el mensaje «Mismo pago, sin duplicados». La captura revisada muestra formulario y listado legibles; no es una revisión multidispositivo ni de accesibilidad completa.

La aplicación sigue siendo una demo local, sin autenticación ni dinero real. No se ha desplegado públicamente. La apertura de la variante PostgreSQL en 8084 y su smoke HTTP se confirmaron el 07/09/2026. El recorrido visual local está verificado; véase el [guion y cierre de demo](demo-presentation.md).
