# Backend Health Check · Borrador de oferta

**Revisión acotada de una API Java/Spring para detectar fallos y convertirlos en acciones verificables.**

Pensado para un equipo que ya tiene un backend y quiere una segunda revisión de un flujo concreto: creación de pedidos, pagos, integración HTTP o acceso a datos. El caso Backend Rescue sirve como muestra del método, no como referencia de un cliente real.

## Entregables

- Informe breve con problemas priorizados, evidencia, impacto y límites de la revisión.
- Mapa del flujo revisado y sus dependencias relevantes.
- Plan de corrección con criterios de aceptación y pruebas propuestas.
- Sesión de explicación del informe y dudas del equipo.

Una implementación o PR de corrección se presupuesta como una fase separada. El informe distingue fallos demostrados, observaciones de código y cuestiones que necesitan información adicional.

## Alcance de un primer piloto

Un repositorio Java/Spring y un flujo de negocio acordado. Revisión de código, pruebas y configuración necesarias para ese flujo, con reproducción local cuando sea viable. Datos sintéticos y acceso mínimo; el cliente conserva sus credenciales y decide sobre cualquier cambio remoto.

No incluye pentest exhaustivo, acceso a producción, cambios de infraestructura, certificación, guardias ni garantías de ausencia de errores. Los bloqueos de entorno y la falta de acceso se documentan; no se sustituyen por conclusiones inventadas.

## Información para concretar una propuesta

1. Qué operación falla o preocupa, y qué resultado se busca.
2. Versiones de Java/Spring, dependencias externas y tamaño aproximado del flujo.
3. Cómo se reproduce localmente y qué pruebas existen.
4. Qué acceso está permitido y quién valida las conclusiones.
5. Disponibilidad para revisar el resultado y fecha objetivo.

Precio y plazo quedan por acordar después de ese alcance. No se han validado demanda, capacidad comercial ni disposición a pagar. Este documento no implica una oferta enviada, un contrato aceptado ni ingresos.

## Evidencia para acompañarla

[Caso práctico Backend Rescue](project-progress.md): baseline conservada, pagos idempotentes, migraciones verificadas, eliminación de N+1, errores definidos y pruebas de redacción. Mostrar los checks y las limitaciones junto a los resultados.

## Método reutilizable (BRL-011)

[Checklist de revisión](backend-health-check-checklist.md), [plantilla de informe](backend-health-check-report-template.md) y [ejemplo aplicado al laboratorio](backend-health-check-lab-example.md). Revisados localmente el 07/09/2026. El siguiente paso es un piloto sobre un segundo repositorio propio o autorizado y un único flujo; su selección, utilidad y tiempo real siguen pendientes. No es un escáner automático ni una certificación.
