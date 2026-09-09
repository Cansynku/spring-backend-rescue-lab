'use strict';
const $ = id => document.getElementById(id);
const money = new Intl.NumberFormat('es-ES', {minimumFractionDigits: 2, maximumFractionDigits: 2});
let orders = [], busy = false, connected = false;
const storageKey = id => `backend-rescue-payment:${id}`;
function knownKey(id) { try { return localStorage.getItem(storageKey(id)); } catch { return null; } }
function show(title, message, reference = '', error = false) {
  $('result').hidden = false; $('result').classList.toggle('error', error);
  $('result-title').textContent = title; $('result-message').textContent = message;
  $('request-reference').textContent = reference ? `Referencia de la operación: ${reference}` : '';
}
function lock(value) { busy = value; $('create').disabled = value || !connected; $('refresh').disabled = value; render(); }
async function request(path, options) {
  const response = await fetch(path, {...options, headers: {'Content-Type': 'application/json', ...options?.headers}});
  const data = await response.json();
  return {response, data, reference: response.headers.get('X-Request-ID') || ''};
}
function node(tag, className, value) { const element = document.createElement(tag); element.className = className; element.textContent = value; return element; }
function render() {
  $('total').textContent = connected ? orders.length : '—';
  $('paid').textContent = connected ? orders.filter(o => o.status === 'PAID').length : '—';
  $('unpaid').textContent = connected ? orders.filter(o => o.status !== 'PAID').length : '—';
  const list = $('orders'); list.replaceChildren();
  if (!orders.length) { list.append(node('p', 'empty', connected ? 'Aún no hay pedidos. Crea el primero para probar un pago.' : 'No se pueden cargar los pedidos. Pulsa Actualizar para intentarlo de nuevo.')); return; }
  for (const order of orders) {
    const row = node('article', 'order', ''); const detail = node('div', '', '');
    detail.append(node('p', 'email', order.customerEmail || 'Pedido antiguo sin correo'));
    detail.append(node('p', 'order-meta order-id', `Pedido ${order.id.slice(0, 8)}`));
    const paid = order.status === 'PAID', review = !paid && order.paymentCount > 0;
    detail.append(node('span', `badge ${paid ? 'paid' : review ? 'review' : ''}`, paid ? 'Pagado' : review ? 'Necesita revisión' : order.status === 'CANCELLED' ? 'Cancelado' : 'Sin pagar'));
    detail.append(node('p', 'order-meta', `${order.paymentCount} ${order.paymentCount === 1 ? 'intento de pago' : 'intentos de pago'}`));
    const actions = node('div', 'order-actions', '');
    const supported = order.totalAmount > 0 && order.totalAmount <= 999999.99;
    actions.append(node('strong', 'money', supported ? money.format(order.totalAmount) : 'Importe fuera de la demo'));
    const key = knownKey(order.id);
    if (supported && (key || (order.status === 'CREATED' && order.paymentCount === 0))) {
      const button = node('button', paid ? 'secondary' : '', paid ? 'Repetir sin duplicar' : key ? 'Reintentar con seguridad' : 'Simular pago');
      button.type = 'button'; button.disabled = busy || !connected; button.addEventListener('click', () => pay(order)); actions.append(button);
    } else if (review) { actions.append(node('small', '', 'No inicies otro pago. El resultado requiere revisión.')); }
    row.append(detail, actions); list.append(row);
  }
}
async function refresh() {
  try {
    const {response, data} = await request('/api/orders');
    if (!response.ok || !Array.isArray(data)) throw new Error('unavailable');
    orders = data; connected = true; $('connection').textContent = '● Conectado'; $('connection').classList.add('ready');
  } catch {
    connected = false; $('connection').textContent = 'Sin conexión · datos no actualizados'; $('connection').classList.remove('ready');
  }
  $('create').disabled = busy || !connected; render();
}
$('create-form').addEventListener('submit', async event => {
  event.preventDefault(); if (busy || !connected) return;
  const amount = $('amount').value.trim().replace(',', '.');
  if (!/^\d{1,6}(\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) {
    show('Revisa el importe', 'Escribe un importe entre 0,01 y 999.999,99 con hasta dos decimales.', '', true); return;
  }
  lock(true);
  try {
    const {response, reference} = await request('/api/orders', {method: 'POST', body: JSON.stringify({customerEmail: $('email').value.trim(), totalAmount: Number(amount)})});
    show(response.ok ? 'Pedido creado' : 'No se ha creado el pedido', response.ok ? 'Ya puedes simular su pago en la lista.' : 'Comprueba el correo y el importe e inténtalo de nuevo.', reference, !response.ok);
  } catch { show('No hemos podido confirmar la creación', 'Actualiza la lista antes de volver a crear el pedido: puede haberse guardado.', '', true); }
  await refresh(); lock(false);
});
async function pay(order) {
  if (busy || !connected) return;
  lock(true);
  try {
    let key = knownKey(order.id);
    if (!key) { key = crypto.randomUUID(); localStorage.setItem(storageKey(order.id), key); }
    const {response, data, reference} = await request(`/api/orders/${order.id}/payments`, {method: 'POST', headers: {'Idempotency-Key': key}, body: JSON.stringify({amount: Number(order.totalAmount.toFixed(2))})});
    if (response.status === 201) show('Pago simulado completado', 'El pedido está pagado. Puedes repetir la petición y comprobar que sigue habiendo un solo pago.', reference);
    else if (response.status === 200) show('Mismo pago, sin duplicados', 'Se ha recuperado el pago original. No se ha creado otro cobro.', reference);
    else if (response.status === 202) show('El pago sigue pendiente', 'No inicies otro pago. Puedes volver a consultar usando esta misma referencia.', reference);
    else show('El pago no se ha confirmado', data.code === 'AMOUNT_MISMATCH' ? 'El importe no coincide con el pedido. Actualiza la lista.' : 'No crees otro intento. Conservamos la misma referencia para comprobar el resultado; puede requerir revisión.', reference, true);
  } catch { show('Resultado sin confirmar', 'No inicies un pago nuevo. La referencia guardada permite reintentar esta misma petición sin duplicarla. Si el navegador bloquea el almacenamiento, habilítalo antes de pagar.', '', true); }
  await refresh(); lock(false);
}
$('refresh').addEventListener('click', async () => { if (busy) return; lock(true); await refresh(); lock(false); });
refresh();
