# Backend Health Check — ejemplo del laboratorio

Fecha: 07/09/2026. Caso propio, sin cliente ni pagos reales.
Referencia: `rescue/demo-interface`, commit `53c5d10`, con los cambios locales
posteriores de lanzadores y documentación descritos en la recuperación de PostgreSQL.
Alcance: pedidos, pago simulado, repetición y límites conocidos del laboratorio.
Es un ejemplo acotado de informe, no una auditoría completa de la pila de PRs.

## Resultado

La demo empaquetada que responde en `http://127.0.0.1:8084` completa el recorrido:
crear pedido → pagar → repetir la petición → consultar un único pago. Puede
enseñarse como caso práctico local. Siguen pendientes contratos de identidad,
propiedad y reconciliación para plantear un uso real.

## Evidencias

E-01 y E-02 proceden del punto de continuidad del 07/09/2026 aportado por el
usuario; no se repitieron en este cierre documental. E-03 se contrastó releyendo
los informes existentes y E-04/E-05 mediante lectura del código actual.

| ID | Comprobación | Resultado y fuente | Límite |
| --- | --- | --- | --- |
| E-01 | Arranque visible | GET `/` devuelve 200 en 8084 | No equivale a inspección visual del navegador |
| E-02 | Flujo real de la demo | `scripts/smoke.ps1 -BaseUrl http://127.0.0.1:8084`: creación 201, pago 201, repetición 200, PAID, un pago, pedido en listado | Proveedor local simulado; se añadió un pedido sintético |
| E-03 | Regresión con PostgreSQL | Informes locales de `.local/pg-wsl-validation/target/surefire-reports`: 66 pruebas, cero fallos/errores/omisiones | Ejecución del paso anterior, verificada leyendo los informes; no repetida en este incremento documental |
| E-04 | Identidad y acceso | `SecurityConfiguration.securityFilterChain` permite todas las peticiones; E-02 funciona sin credenciales | Observación y ejecución local; no prueba contra un entorno externo |
| E-05 | Volumen del listado | `OrderService.list` devuelve `List` mediante `findSummaries`, sin límite/paginación | Inspección de código; no benchmark de memoria ni latencia |
| E-06 | Pago incierto | `docs/payment-contract.md` define PENDING/UNKNOWN sin reconciliación automática; pruebas de fallo de finalización y timeout en `PaymentReliabilityTest` | Límite documentado y cubierto por pruebas previas; no nuevo experimento de caída del proceso |

## Verificación posterior en navegador — 07/09/2026

En 8084 se creó el pedido sintético d94f825a por 25,50, se simuló el pago y se
repitió desde la pantalla. Quedó pagado con un intento y el mensaje «Mismo pago,
sin duplicados». Se inspeccionó una captura del formulario y listado. Esta prueba
visual nueva complementa E-01/E-02; no reejecuta la suite E-03 ni cubre otros dispositivos.

## Decisiones pendientes para ampliar el uso

**H-01 — Acceso sin identidad ni propiedad.** Bloquea una exposición multiusuario
con datos reales (P1 para ese uso; limitación deliberada de esta demo). Definir
primero quién crea/consulta/paga cada pedido y cómo se identifica. Aceptación:
anónimos y otros propietarios no pueden leer ni modificar el pedido; el propietario
autorizado conserva su flujo. No añadir usuarios ficticios como sustituto del contrato.

**H-02 — Un pago incierto puede quedarse bloqueado.** Bloquea una operación real
que requiera recuperación (P1 para ese uso). Definir con el proveedor cómo consultar
un resultado y resolver una discrepancia. Aceptación: recuperar un resultado incierto
sin reenviar el cobro ni permitir una segunda clave como atajo. El simulador actual
no ofrece ese contrato; no prometer recuperación automática.

**H-03 — El listado no tiene límite de tamaño.** P2 si el volumen previsto justifica
esa corrección. Definir orden estable, tamaño máximo y compatibilidad del contrato.
Aceptación: límites y navegación verificables, sin omisiones/duplicados bajo las
condiciones acordadas. No se ha demostrado una degradación de rendimiento concreta.

## Próximo piloto

Seleccionar un segundo repositorio propio o con permiso de revisión y un único
flujo. Aplicar la [checklist](backend-health-check-checklist.md), entregar el informe
y registrar utilidad y tiempo real. Con esa evidencia se podrá decidir qué partes
merece automatizar. Todavía no hay un escáner propio ni validación comercial.
