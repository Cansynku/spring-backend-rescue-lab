const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');
const {randomUUID} = require('node:crypto');
const source = readFileSync(join(__dirname, '../src/main/resources/static/demo.js'), 'utf8');

async function screen({stored = new Map(), failPayment = false, storageBlocked = false} = {}) {
  const elements = new Map(), calls = [];
  const element = () => ({textContent: '', value: '', children: [], handlers: {},
    classList: {toggle() {}, add() {}, remove() {}},
    append(...children) { this.children.push(...children); }, replaceChildren() { this.children = []; },
    addEventListener(event, handler) { this.handlers[event] = handler; }});
  const get = id => { if (!elements.has(id)) elements.set(id, element()); return elements.get(id); };
  const order = {id: '00000000-0000-0000-0000-000000000001', customerEmail: 'demo@example.com', totalAmount: 25.5, status: 'CREATED', paymentCount: 0};
  const context = vm.createContext({Intl, crypto: {randomUUID},
    document: {getElementById: get, createElement: element},
    localStorage: {getItem: key => stored.get(key) || null, setItem: (key, value) => {
      if (storageBlocked) throw new Error('blocked'); stored.set(key, value);
    }},
    fetch: async (path, options) => {
      calls.push({path, options});
      let status = 200, data = [order];
      if (path.endsWith('/payments')) {
        if (failPayment) { failPayment = false; throw new Error('lost response'); }
        order.status = 'PAID'; order.paymentCount = 1; data = {status: 'AUTHORIZED'};
      } else if (options.method === 'POST') { status = 201; data = order; }
      return {ok: status < 400, status, json: async () => data, headers: {get: () => 'test-request-id'}};
    }});
  vm.runInContext(source, context);
  await new Promise(resolve => setImmediate(resolve));
  return {context, calls, get, stored, pay: () => vm.runInContext('pay(orders[0])', context)};
}

test('payment retries and reloaded page reuse the persisted key after a lost response', async () => {
  const first = await screen({failPayment: true});
  await first.pay();
  assert.match(first.get('result-title').textContent, /sin confirmar/);
  await first.pay();
  const payments = first.calls.filter(c => c.path.endsWith('/payments'));
  assert.equal(payments.length, 2);
  assert.equal(payments[0].options.headers['Idempotency-Key'], payments[1].options.headers['Idempotency-Key']);
  const reloaded = await screen({stored: first.stored});
  await reloaded.pay();
  assert.equal(reloaded.calls.at(-2).options.headers['Idempotency-Key'], payments[0].options.headers['Idempotency-Key']);
});

test('blocked storage prevents submitting a payment without a durable browser key', async () => {
  const ui = await screen({storageBlocked: true}); await ui.pay();
  assert.equal(ui.calls.filter(c => c.path.endsWith('/payments')).length, 0);
});

test('invalid amount is rejected locally and a comma amount is sent correctly', async () => {
  const ui = await screen(); ui.get('email').value = 'demo@example.com';
  ui.get('amount').value = '25,501';
  await ui.get('create-form').handlers.submit({preventDefault() {}});
  assert.equal(ui.calls.filter(c => c.options.method === 'POST').length, 0);
  ui.get('amount').value = '25,50';
  await ui.get('create-form').handlers.submit({preventDefault() {}});
  const posted = ui.calls.find(c => c.options.method === 'POST');
  assert.equal(JSON.parse(posted.options.body).totalAmount, 25.5);
  assert.equal(ui.get('result-title').textContent, 'Pedido creado');
});
