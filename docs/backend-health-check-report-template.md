# Backend Health Check — [proyecto / flujo]

Fecha: [fecha]. Revisor: [nombre]. Destinatario: [destinatario acordado].
Repositorio y commit: [referencia]. Cambios locales incluidos: [lista o ninguno].
Objetivo del flujo: [qué debe poder hacer el usuario y con qué garantías].

## Resultado ejecutivo

[Qué se ha demostrado, qué necesita corregirse primero y qué no se pudo verificar.
Evitar declarar el sistema seguro o listo para producción por haber pasado pruebas.]

## Alcance y condiciones

- Incluido: [operaciones, componentes y contratos concretos].
- Excluido: [entornos, operaciones o servicios fuera de alcance].
- Entorno y datos: [versiones, aislamiento y datos sintéticos].
- Autorización: [lectura, ejecución o cambios permitidos].
- Limitaciones: [dependencias simuladas, pruebas omitidas o accesos no disponibles].

## Flujo revisado

[Entrada → validación/autorización → persistencia → integración → respuesta.
Indicar límites transaccionales y qué ocurre cuando una dependencia falla.]

## Evidencias

| ID | Comprobación | Referencia reproducible | Observado | Estado / límite |
| --- | --- | --- | --- | --- |
| E-01 | [pregunta de la checklist] | [archivo y línea o prueba/comando sanitizado] | [resultado real] | [estado y cobertura] |

## Hallazgos

### H-01 — [problema concreto]

- Prioridad y motivo: [P1/P2/P3, exposición e impacto].
- Evidencia: [E-xx; código inspeccionado o reproducción ejecutada].
- Condición: [cuándo ocurre].
- Esperado / observado: [diferencia verificable].
- Acción mínima: [cambio propuesto, sin implementarlo por defecto].
- Aceptación: [prueba o resultado que demostraría la corrección].
- Incertidumbres: [suposiciones o información pendiente].

## Plan propuesto

| Orden | Acción | Dependencia / decisión necesaria | Criterio de aceptación |
| --- | --- | --- | --- |
| 1 | [primera corrección] | [contrato o dato que falta] | [resultado verificable] |

## Entrega y seguimiento

[Qué archivos se entregan, qué se ejecutó realmente y qué queda pendiente.
Registrar aprobación del alcance de implementación por separado. No inventar
estimaciones comerciales, aceptación del cliente ni resultados futuros.]
