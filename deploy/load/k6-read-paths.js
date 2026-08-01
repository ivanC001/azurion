import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');
const token = __ENV.ACCESS_TOKEN || '';
const tenantId = __ENV.TENANT_ID || '';

export const options = {
  scenarios: {
    authenticated_reads: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 25),
      duration: __ENV.DURATION || '2m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<750', 'p(99)<1500'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  if (!token || !tenantId) {
    throw new Error('ACCESS_TOKEN and TENANT_ID are required; use a dedicated load-test user.');
  }
}

const endpoints = [
  '/v1/saas/ventas/summary',
  '/v1/saas/ventas/page?page=0&size=20',
  '/v1/saas/inventory/productos/summary',
  '/v1/saas/inventory/productos/page?page=0&size=20',
  '/v1/saas/crm/prospectos/page?page=0&size=20',
  '/v1/saas/crm/oportunidades/page?page=0&size=20',
  '/v1/saas/crm/actividades/page?page=0&size=20',
];

export default function () {
  const endpoint = endpoints[(__VU + __ITER) % endpoints.length];
  const response = http.get(`${baseUrl}${endpoint}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Tenant-Id': tenantId,
      Accept: 'application/json',
    },
    tags: { endpoint },
    timeout: '10s',
  });

  check(response, {
    'status is 200': (result) => result.status === 200,
    'response is json': (result) =>
      String(result.headers['Content-Type'] || '').includes('application/json'),
  });
  sleep(Number(__ENV.THINK_TIME_SECONDS || 1));
}
