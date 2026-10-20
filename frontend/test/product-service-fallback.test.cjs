const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const Module = require('node:module');
const test = require('node:test');
const ts = require('typescript');
const rxjs = require('rxjs');

const sourcePath = path.join(__dirname, '../src/app/core/services/product.service.ts');
const source = fs.readFileSync(sourcePath, 'utf8');
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, experimentalDecorators: true }
}).outputText;

const angularCoreStub = { Injectable: () => target => target };
const angularHttpStub = { HttpClient: class HttpClient {} };
const loadProductService = () => {
  const module = { exports: {} };
  const localRequire = name => {
    if (name === '@angular/core') return angularCoreStub;
    if (name === '@angular/common/http') return angularHttpStub;
    if (name === 'rxjs') return rxjs;
    return require(name);
  };
  new Function('require', 'module', 'exports', compiled)(localRequire, module, module.exports);
  return module.exports.ProductService;
};

test('API refresh failure restores cached products into the observable and localStorage', () => {
  const cached = [{ id: 47, name: 'Cached product', price: 8, stock: 2 }];
  const storage = new Map([['sport_vision_products', JSON.stringify(cached)]]);
  global.localStorage = {
    getItem: key => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value)
  };
  const http = { get: () => rxjs.throwError(() => new Error('API unavailable')) };

  const service = new (loadProductService())(http);

  assert.deepEqual(service.productsSubject.value, cached);
  assert.deepEqual(JSON.parse(storage.get('sport_vision_products')), cached);
});

test('failed product creation still emits a local success and persists it in localStorage', async () => {
  const storage = new Map();
  global.localStorage = {
    getItem: key => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value)
  };
  const http = {
    get: () => rxjs.throwError(() => new Error('API unavailable')),
    post: () => rxjs.throwError(() => new Error('403 Forbidden'))
  };
  const service = new (loadProductService())(http);
  const form = {
    name: 'Offline product', description: 'Local only', price: 15, stock: 3,
    categoryId: 1, imageUrl: '', featured: false
  };

  const emitted = await new Promise((resolve, reject) => {
    service.createProduct(form).subscribe({ next: resolve, error: reject });
  });

  assert.equal(emitted.name, 'Offline product');
  assert.ok(JSON.parse(storage.get('sport_vision_products')).some(product => product.name === 'Offline product'));
});
