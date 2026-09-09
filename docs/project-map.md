# Las dos lineas del proyecto

## 1. Backend Rescue Lab: el caso practico

Es la aplicacion de pedidos y pagos simulados que ya podemos abrir. Se construyo
con problemas deliberados y se fueron corrigiendo con pruebas y cambios
revisables. Sirve para aprender, demostrar resultados y ensenar el metodo a
posibles clientes. No es un sistema que analiza otros repositorios.

## 2. Backend Health Check: lo que se aplicaria a otros proyectos

Es el servicio de revision de una API Java/Spring existente: localizar problemas,
demostrarlos, priorizarlos y proponer correcciones verificables. El laboratorio
es la muestra de ese trabajo. Tenemos una [oferta en borrador](backend-health-check-offer.md)
y experiencia documentada con el metodo de revision, apoyado por las herramientas
de desarrollo y revision disponibles.

No existe todavia una segunda aplicacion propia que reciba un repositorio y
genere automaticamente un diagnostico. La idea inicial era validar primero el
servicio con un piloto acotado y, despues, automatizar tareas repetidas que
demuestren utilidad. No hay clientes, ingresos ni demanda validados en la
documentacion del proyecto.

El siguiente resultado de esa segunda linea seria una revision piloto autorizada
de otro repositorio, con un informe reproducible y alcance concreto. Construir
un escaner automatico propio es una fase posterior, aun pendiente.
