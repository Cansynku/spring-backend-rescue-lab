# Backend Rescue · Resultados para enseñar

**Caso práctico propio: mejorar una API de pedidos y pagos con pruebas de antes/después.**

No es un encargo de un cliente ni mueve dinero real. La baseline permanece congelada para poder comparar el punto de partida con cada mejora. Las PRs #1–#5 se integraron secuencialmente en main el 09/09/2026 mediante commits de merge y CI verificado. Se conservaron las ramas originales. La revisión fue asistida por IA y no consta una aprobación independiente en GitHub.

## Qué ha cambiado

| Problema de partida | Resultado demostrable | Evidencia |
| --- | --- | --- |
| Repetir una petición podía repetir el cobro simulado | La misma clave devuelve el mismo pago; una petición concurrente no lo duplica | Pruebas de repetición y concurrencia con proveedor HTTP simulado |
| La llamada HTTP mantenía abierta la transacción | Se guarda el intento antes de llamar al proveedor; la finalización es independiente | Pruebas de límites transaccionales, fallo y rollback |
| Un timeout dejaba el resultado sin un tratamiento seguro | El intento incierto bloquea otra carga y exige reconciliación | Estados PENDING/UNKNOWN y pruebas de errores del proveedor |
| El esquema dependía de actualizaciones automáticas de Hibernate | Flyway versiona el esquema y verifica los dos formatos antiguos conocidos | Migración sobre PostgreSQL con comprobación de valores conservados |
| Se guardaban pedidos inválidos o imposibles de pagar | Correo e importe se validan; pedidos y pagos comparten máximo | Crear/pagar/repetir el máximo; rechazar el siguiente céntimo |
| Listar pedidos disparaba consultas adicionales | Una consulta para obtener pedidos y recuentos | Medición con uno y once pedidos, incluidos pedidos sin pagos |
| Errores parecidos tenían respuestas distintas | Códigos y ProblemDetail para los casos esperados | Pruebas HTTP de validación, 404, 409 y almacenamiento |
| Un conflicto SQL escribía la clave en el log | Un aviso genérico conserva la señal sin el detalle filtrado | Violación real de unicidad y captura de logs con marcadores sintéticos |
| Faltaba una referencia para buscar una petición | La respuesta devuelve X-Request-ID y los eventos lo incluyen | Pruebas de concurrencia, excepción y limpieza del contexto |

## Recorrido de demostración

La nueva [pantalla de pedidos y pagos](demo-screen.md) permite seguir el recorrido con botones. La demo autonoma responde en 8083. La variante PostgreSQL en 8084 fue abierta por el usuario el 07/09/2026: GET / 200 y smoke PASS (201/201/200, PAID, un pago). La suite PostgreSQL previa registró 66 tests sin fallos, errores ni omisiones; no se repitió para este cierre documental. Ver [recuperacion y bases separadas](local-postgres-recovery.md).

**Validación histórica del incremento de observabilidad:** 64 pruebas aprobadas en PostgreSQL. H2 descubre 64, con 60 aplicables y 4 casos de migración exclusivos de PostgreSQL. Los checks de la PR permiten comprobar el resultado sobre el commit publicado. No se adjuntan logs brutos con datos de las pruebas.

1. Mostrar [la baseline](https://github.com/Cansynku/spring-backend-rescue-lab/tree/baseline) y el inventario de diez problemas deliberados.
2. Abrir [la PR de pagos](https://github.com/Cansynku/spring-backend-rescue-lab/pull/1): comparar el intento persistido y la llamada fuera de transacción.
3. Abrir [la PR de migraciones](https://github.com/Cansynku/spring-backend-rescue-lab/pull/2): explicar cómo se conservan valores antiguos sin adivinar el esquema.
4. Abrir [la PR de pedidos y errores](https://github.com/Cansynku/spring-backend-rescue-lab/pull/3): enseñar el límite compartido y la consulta con recuentos.
5. Mostrar los checks de la rama `rescue/safe-observability` y [la política de logs](observability.md). Explicar qué datos se permiten y cómo se verifica que los marcadores no aparecen.

El recorrido es reproducible con las pruebas del repositorio. La referencia a una demo antigua corresponde al paso anterior; la demo PostgreSQL de 8084 ya tiene el smoke confirmado descrito arriba. El arranque de la aplicación empaquetada sobre una copia de la baseline sigue pendiente porque el control automático rechazó aquella acción; no se ha intentado eludirlo.

## Qué podemos afirmar y qué no

Podemos enseñar código propio, fallos reproducidos, cambios revisables y pruebas contra H2/PostgreSQL. Una consulta no equivale a una mejora de latencia medida: todavía no hay benchmark. Tampoco hay pagos reales, clientes conseguidos ni ingresos demostrados.

La demo local tiene el recorrido en navegador verificado y un [guion de presentación](demo-presentation.md). El propietario autorizó la integración de las PRs #1–#5, completada el 09/09/2026 con comprobación de diffs, dependencias y CI; no es una aceptación para producción. El arranque empaquetado sobre una copia de la baseline sigue sin verificarse: es una comprobación de migración distinta del smoke de la demo, y no bloquea presentar pedidos y pagos con ese límite declarado. Autenticación, propiedad de los pedidos y reconciliación necesitan contratos propios. La aplicación continúa siendo un laboratorio local, no un servicio listo para producción.

## Uso profesional

Este material sirve como caso de portfolio: explicar un problema, reproducirlo, aplicar una corrección pequeña y mostrar su prueba. Ya hay un [borrador de oferta Backend Health Check](backend-health-check-offer.md) con alcance, entregables y preguntas para un primer piloto. No se ha enviado a nadie; precio, plazo y demanda siguen pendientes de validar.

## Paginación — 10/09/2026

Incremento de navegación: [contrato y límites](order-pagination.md). La API anterior conserva su formato; la pantalla consulta páginas de 20 pedidos. Validación local H2: 73 casos, cero fallos/errores y cuatro omisiones exclusivas de PostgreSQL; cinco pruebas de pantalla correctas. La ejecución PostgreSQL local falló por conexión rechazada en 55433; no constituye un resultado de regresión. La CI de la [PR #7](https://github.com/Cansynku/spring-backend-rescue-lab/pull/7) pasó en H2 y PostgreSQL; el estado de integración se consulta en esa PR.
