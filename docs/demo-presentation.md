# Backend Rescue: demo para recruiters

Laboratorio propio de pedidos y pagos simulados, desarrollado con asistencia de IA. No procesa dinero real ni representa trabajo de clientes. Backend Health Check es el método de revisión preparado a partir del laboratorio, no un escáner automático.

## Explicación en 30 segundos

«Partí de un backend con fallos intencionados y fui corrigiéndolos con pruebas. El ejemplo principal es evitar que repetir una petición de pago genere otro cobro. También mejoré la validación, las consultas y la navegación. En GitHub se puede comparar el punto de partida con los cambios y sus verificaciones. Es un laboratorio; puedo explicar lo demostrado y lo que todavía falta para producción».

## Recorrido de cinco minutos

1. Abre la pantalla local en http://127.0.0.1:8084 y explica que todos los datos y pagos son de demostración.
2. Crea un pedido con demo@example.com e importe 25,50. Cada demostración conserva un nuevo pedido sintético.
3. Pulsa Simular pago y después Repetir sin duplicar. Muestra que permanece pagado con un solo intento.
4. Enseña la navegación de pedidos y el último pedido creado, que sigue visible aunque esté en otra página.
5. Abre el [resumen de cambios](project-progress.md), la [baseline](https://github.com/Cansynku/spring-backend-rescue-lab/tree/baseline-v1) y las [PRs integradas](https://github.com/Cansynku/spring-backend-rescue-lab/pulls?q=is%3Apr+is%3Amerged). Explica un fallo, su prueba y su corrección.

## Antes de la entrevista

- Usa una copia actualizada de main, Java 21 y Maven. Compila con `mvn clean verify`.
- En el entorno Windows/WSL ya preparado, abre `scripts/abrir-demo-postgresql.cmd`. El lanzador usa exclusivamente el JAR de `target` de esa copia; ya no selecciona la compilación histórica de `.local`. Mantén abierta su ventana.
- Si 8084 está ocupado, comprueba qué demo está abierta. El lanzador no detiene aplicaciones existentes.
- Para repetir las pruebas PostgreSQL: `scripts/verify-postgres.ps1 -Port 55433 -WslDistribution BackendRescue`. Solo utiliza la base desechable backend_rescue_test.
- La pantalla es local: un recruiter remoto la verá al compartir pantalla. El código y las evidencias están disponibles en GitHub.

## Evidencia y límites

La suite local PostgreSQL ejecutada para PR #9 pasó 73 casos, sin fallos, errores ni omisiones. Hay siete pruebas de lógica de pantalla. La CI verifica H2 y PostgreSQL; cada ejecución tiene su fecha y commit. Esto no equivale a una revisión independiente ni a cobertura visual de todos los navegadores.

Para producción faltan identidad/propiedad de pedidos, reconciliación con un proveedor real y decisiones operativas y de volumen. El endpoint antiguo de listado sigue sin límite por compatibilidad. La migración del paquete sobre una copia de baseline continúa pendiente por bloqueo automático; no confundirla con las pruebas de migración ni con el flujo de demo.

El [piloto de Alf.io](alfio-review-pilot.md) es una revisión estática de código abierto: no se atribuye su autoría, una corrección upstream ni una reproducción dinámica. No hay clientes o ingresos validados.

Comprobación del 11/09/2026: lanzador PostgreSQL ejecutado en 8084, página HTTP 200 y JavaScript servido idéntico al código actual. Smoke PASS: creación 201, pago 201, repetición 200, estado PAID y un pago. Se añadió un pedido sintético. La inspección visual nueva no pudo completarse porque la herramienta de navegador falló al inicializarse; no se atribuye una validación visual a esta ejecución.
