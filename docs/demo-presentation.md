# Demo Backend Rescue — guion de cinco minutos

Estado local al 07/09/2026: preparada para presentar pedidos y pagos simulados.
Referencia de la prueba visual: rescue/demo-interface, HEAD 53c5d10 más los cambios locales de aquel paso. El cierre documental se publicó en 7012519 y las PRs #1–#5 se integraron en main el 09/09/2026.
La integración fue autorizada por el propietario y no equivale a preparación para producción ni a review independiente.

## Recorrido

| Tiempo | Qué mostrar | Qué explicar |
| --- | --- | --- |
| 0:00–0:45 | Pantalla de http://127.0.0.1:8084/ | Laboratorio propio con datos sintéticos; ningún pago real. Backend Health Check es el método de revisión, no un escáner propio. |
| 0:45–1:30 | Crear un pedido con correo sintético e importe 25,50 | Entrada validada; el pedido aparece sin pagar y con cero intentos. Cada demostración añade datos que se conservan. |
| 1:30–2:30 | Simular pago y pulsar Repetir sin duplicar | Pasa a pagado con un intento. La repetición recupera el pago original; comprobar mensaje y recuento. |
| 2:30–3:30 | Comparación documentada en project-progress.md y payment-contract.md | Problema inicial, intento persistido, llamada HTTP fuera de transacción y protección ante repetición. No simular ahora un fallo destructivo. |
| 3:30–4:15 | Evidencia de tests y migraciones | Suite PostgreSQL previa: 66 tests, cero fallos/errores/omisiones. Migraciones verificadas por tests; arranque del paquete sobre copia de baseline aún no verificado. |
| 4:15–5:00 | Checklist y ejemplo Health Check | Evidencia → impacto → acción → aceptación. Próximo paso: aplicar el método a un flujo de otro repositorio existente propio o autorizado. Sin clientes ni ingresos validados. |

## Verificación de este cierre

- Navegador integrado en 8084: creación del pedido d94f825a por 25,50, pago simulado y repetición. Estado final: pagado, un intento, «Mismo pago, sin duplicados».
- Había dos pedidos; se añadió uno sintético y se conservan los tres. No se modificaron datos de H2 ni del PostgreSQL antiguo.
- Inspección de una captura: formulario, listado y controles legibles en el tamaño observado. No cubre móvil, todos los navegadores ni accesibilidad completa.
- Revisión del diff local y archivos nuevos de documentación/lanzamiento. Los dos scripts PowerShell pasan el análisis sintáctico. La revisión confirma puerto de demo 8084, base backend_rescue_ui, guardia de puerto ocupado y verificación dirigida a backend_rescue_test.
- No se ejecutaron los lanzadores ni su cierre durante esta revisión, ni se volvió a ejecutar Maven o smoke.ps1. La ejecución visual es nueva; los 66 tests son evidencia anterior.
- Límite del lanzador: prefiere el JAR de .local/pg-wsl-validation/target sobre target; después de cambios de código hay que comprobar qué paquete se está usando. El sondeo TCP no prueba identidad ni disponibilidad SQL. No se cambió su implementación.

## Pendientes separados

Para esta presentación local no se ha observado un bloqueo en el recorrido comprobado. Antes de otra sesión, comprobar que 8084 sigue disponible; no lanzar otra instancia si ya está abierta.

Integración completada el 09/09/2026: PRs #1–#5 hacia main, diffs sin cambios al recolocar las bases y CI correcto tras cada incremento. Se verificó que el árbol integrado coincide con 7012519 antes de este ajuste documental. No hay reviews independientes registradas en GitHub.

Para afirmar migración empaquetada completa: verificar arranque, valores conservados y smoke sobre una copia autorizada de la baseline. No confundirlo con el flujo de la base de demo.

Para un uso real: definir identidad/propiedad, reconciliación del proveedor, volumen y compatibilidad de la lista antigua sin límite y requisitos operativos de exposición, secretos y recuperación. Son trabajo posterior, no nuevas funcionalidades de este cierre. Testcontainers, benchmarks y despliegue público no son requisitos de la demo.

## Piloto mínimo propuesto

Elegir un repositorio existente propio o expresamente autorizado, un commit y un flujo con resultado esperado conocido. Excluir material del empleador. Aplicar la checklist en lectura y reproducir solo lo necesario con datos sintéticos y permisos acordados.

Entregar un informe breve con evidencias, incertidumbres y una primera acción con criterio de aceptación si procede. Registrar tiempo real, bloqueos y si el destinatario puede entender/verificar las conclusiones. No exigir encontrar defectos ni implementar arreglos en el piloto. Esto valida transferencia del método, no demanda comercial. No se ha elegido ni creado un segundo repositorio.
