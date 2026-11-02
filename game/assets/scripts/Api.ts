// Where the backend lives  后端地址
export const API_BASE = 'http://localhost:8080/api';

export async function getJson<T>(path: string): Promise<T> {
    const res = await fetch(API_BASE + path);
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }
    return (await res.json()) as T;
}