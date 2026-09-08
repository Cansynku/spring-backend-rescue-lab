# Backend Health Check: revisión reutilizable

Objetivo: revisar un flujo de una API Java/Spring y entregar problemas demostrables
con acciones verificables. No es una certificación ni un escáner automático.

## Antes de revisar

- Acordar repositorio, commit, flujo, comportamiento esperado y destinatario del informe.
- Registrar qué acceso y ejecuciones están autorizados; empezar por lectura.
- Identificar instrucciones del repositorio y cambios locales que deben conservarse.
- Confirmar Java, Spring, base de datos, forma de arranque y dependencias externas.
- Usar un entorno aislado y datos sintéticos para reproducciones con escritura.
- Registrar límites: código no disponible, pruebas no ejecutables, servicios simulados.

## Comprobaciones

Para cada fila registrar **Verificado**, **Problema demostrado**, **Parcial**,
**No verificado** o **No aplica**, con evidencia y fecha. No aplica requiere motivo.
Una prueba verde cubre sus casos concretos; no demuestra ausencia de fallos.

| ID | Pregunta | Evidencia mínima / verificación propuesta |
| --- | --- | --- |
| HC-01 | ¿Puede arrancarse el flujo de forma reproducible? | Versiones, configuración sanitizada, arranque y petición de comprobación |
| HC-02 | ¿Se rechazan entradas inválidas antes de persistir o llamar fuera? | Contrato y casos nulo, formato, límites y discordancia entre campos |
| HC-03 | ¿Cada usuario accede solo a lo autorizado? | Modelo de identidad/propiedad y casos anónimo, otro propietario y permiso insuficiente |
| HC-04 | ¿Repetir o concurrir puede duplicar un efecto? | Contrato de claves, restricciones persistidas y pruebas de repetición/concurrencia |
| HC-05 | ¿Las transacciones protegen la consistencia sin retener recursos durante HTTP? | Recorrido real y pruebas de rollback, proveedor lento y finalización fallida |
| HC-06 | ¿Los fallos externos tienen límites y recuperación definida? | Timeouts, política de reintento y tratamiento de resultado incierto/crash |
| HC-07 | ¿El esquema evoluciona conservando datos? | Migraciones de base vacía y copia aislada de cada formato admitido; valores antes/después |
| HC-08 | ¿Las consultas y respuestas crecen de forma controlada? | Número de consultas con diferentes tamaños, límites de página; carga medida si procede |
| HC-09 | ¿Los errores son consistentes y no revelan datos internos? | Estado, código y cuerpo ante errores esperados; revisar también errores de infraestructura |
| HC-10 | ¿Podemos seguir una operación sin registrar información sensible? | Correlación, lista de campos permitidos y prueba de ausencia de marcadores sintéticos |
| HC-11 | ¿Las pruebas protegen los riesgos del flujo en su base real? | Casos negativos/concurrentes, resultados, omisiones y commit de CI |
| HC-12 | ¿Se conocen las condiciones de entrega y recuperación? | Configuración de exposición, secretos, copia/restauración ensayada y procedimiento de vuelta atrás |

## Convertir observaciones en un informe

Usar la [plantilla](backend-health-check-report-template.md). Cada hallazgo debe
tener ubicación, condición de fallo, resultado observado y esperado, impacto,
acción mínima y criterio de aceptación. Si solo se inspeccionó código, indicarlo;
si falta evidencia, proponer la comprobación pendiente sin presentar una hipótesis
como un fallo reproducido.

Prioridades: **P1** bloquea el uso previsto por impacto demostrado o riesgo concreto;
**P2** corregir después por un fallo acotado; **P3** mejora de mantenimiento.
Valorar exposición y efecto real: una limitación educativa local no tiene el mismo
impacto que un fallo en un sistema con clientes. No asignar prioridad por intuición
ni usar un porcentaje de aprobado como garantía de preparación para producción.

## Cierre del piloto

- Revisar que todas las afirmaciones tengan evidencia o estén marcadas como pendientes.
- Elegir una primera corrección pequeña y sus criterios de aceptación.
- Separar revisión e implementación; acordar el alcance de cualquier cambio posterior.
- Registrar tiempo real invertido, bloqueos y utilidad del informe para el destinatario.
- Conservar solo evidencias sanitizadas autorizadas. No publicar ni contactar a terceros automáticamente.

Ejemplo propio: [evaluación del laboratorio](backend-health-check-lab-example.md).
