// In the Cocos preview (port 7456) call the backend directly.
// Embedded in Vue, use "/api" so it goes through Vite's proxy (same as Vue itself).
// 在 Cocos 预览(7456)时直接连后端;嵌入 Vue 时用 /api 走 Vite 代理。
const API_BASE = window.location.port === '7456' ? 'http://localhost:8080/api' : '/api';

let token = '';
export function setToken(t: string) { token = t; }
export function hasToken(): boolean { return token !== ''; }

/** An error with the backend's { code, message }  带有后端错误码的错误 */
export class ApiError extends Error {
    constructor(public status: number, public code: string, message: string) {
        super(message);
    }
}

export async function request<T>(
    method: 'GET' | 'POST',
    path: string,
    body?: unknown,
    auth = true,                     // false for public endpoints  公开接口不带 token
): Promise<T> {
    const headers: Record<string, string> = {};
    if (auth && token) headers['Authorization'] = `Bearer ${token}`;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    const res = await fetch(API_BASE + path, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
    });

    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
        throw new ApiError(res.status, data?.code ?? 'ERROR', data?.message ?? `HTTP ${res.status}`);
    }
    return data as T;
}

/** Public GET, no token: used for polling the round.  公开的 GET,不带 token(轮询用) */
export function getJson<T>(path: string): Promise<T> {
    return request<T>('GET', path, undefined, false);
}