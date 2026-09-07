# Pantalla de demostración

La app ya incluye una pantalla en español para crear pedidos, simular pagos y comprobar que repetir una petición no duplica el pago. Se sirve desde el propio backend; no necesita una aplicación frontend ni dependencias instaladas aparte.

## Abrirla en este equipo

1. Ejecuta `scripts/abrir-demo.cmd` con doble clic y deja la ventana abierta.
2. Cuando termine de arrancar, abre [la demo local](http://127.0.0.1:8083/).
3. Usa el correo de prueba y el importe propuestos. Pulsa **Crear pedido**, después **Simular pago** y finalmente **Repetir sin duplicar**. El pedido debe seguir mostrando un solo intento de pago.

La base `backend_rescue_ui` ya se creó en el PostgreSQL local del proyecto. Está separada de las bases anteriores. No se ha migrado ni alterado la baseline. El control automático rechazó el arranque persistente de la nueva instancia, sin indicar motivo: por eso el primer arranque queda en manos del usuario. Esta URL no se presenta como disponible hasta que la aplicación esté arrancada. El lanzador tampoco se ha ejecutado ni validado como proceso persistente.

El paquete compilado está en `.local/ui-validation/target/` en este equipo. El lanzador utiliza ese paquete si existe; si no, utiliza `target/`. En otro equipo hay que tener Java 21, PostgreSQL y una base vacía `backend_rescue_ui`, y compilar con `mvn clean package`. El script no instala programas ni crea bases por su cuenta. Si el puerto 8083 ya está ocupado, revisar el proceso existente; no lo detiene automáticamente.

## Qué verás

- Un formulario con correo e importe y un listado de pedidos conservados en PostgreSQL.
- Estado de conexión, recuentos y estado de cada pedido.
- Confirmación de pago, repetición sin duplicados y mensajes de resultado pendiente.
- Una referencia de operación para localizar sus eventos cuando la API la proporciona.

La pantalla limita los importes a 999.999,99 para esta demo. La API mantiene su contrato de 17 dígitos enteros; valores antiguos fuera del rango de la pantalla no se redondean ni se envían a pagar. No se asigna moneda porque el dominio todavía no la define.

Las claves de pago se conservan en el almacenamiento local de este navegador, por pedido, para reutilizarlas tras un fallo o una recarga. No se almacena el correo. Si ese almacenamiento está bloqueado, el pago no se envía. Si se borra o se cambia de navegador, un intento ya registrado continúa protegido por el backend; la pantalla no inventa una recuperación del resultado. No hay reintentos automáticos ni promesa de recuperación de pagos inciertos.

## Validación y límites

`DemoHttpTest` arranca una instancia de prueba en un puerto aleatorio: comprueba la pantalla y sus recursos y recorre creación, pago, repetición y lectura usando HTTP real con un proveedor simulado. Esa prueba sí ha arrancado y probado la aplicación; no equivale al arranque persistente del lanzador sobre `backend_rescue_ui`.

`node --test scripts/test-demo-ui.cjs` prueba la lógica JavaScript con una superficie DOM simulada: reutilización de clave tras perder una respuesta y recargar, bloqueo preventivo cuando el almacenamiento falla, y validación/conversión del importe. No se ha realizado inspección visual ni interacción automatizada en un navegador real.

La aplicación sigue siendo una demo local, sin autenticación ni dinero real. No se ha desplegado públicamente. Para cerrar la entrega visible falta que el usuario abra el lanzador y confirme que puede ver y utilizar la pantalla.
