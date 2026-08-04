import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

export const options = {
    vus: 50,
    duration: '5s',
};

export default function () {
    const url = 'http://localhost:8080/api/v1/products/buy-safe';

    const payload = JSON.stringify({
        productId: 1,
        quantity: 1,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'status is 200 (Success)': (r) => r.status === 200,
        'status is 400 (Stock Out)': (r) => r.status === 400,
        'status is NOT 404/500': (r) => r.status !== 404 && r.status !== 500,
    });

    sleep(0.05);
}

export function handleSummary(data) {
return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true })
};
}