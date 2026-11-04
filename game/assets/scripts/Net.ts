export type ServerMsg = { type: string; [key: string]: any };

// Preview (7456): connect straight to the backend. Embedded in Vue: same host, through Vite's proxy.
// 预览时直接连后端;嵌入 Vue 时连同一个网址,经过 Vite 代理
const WS_URL = window.location.port === '7456'
    ? 'ws://localhost:8080/ws'
    : `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`;

export class Net {
    private ws: WebSocket | null = null;
    private retries = 0;
    private lastPong = 0;
    private heartbeat: number | undefined;
    private stopped = false;

    constructor(
        private onMessage: (msg: ServerMsg) => void,
        private onStatus: (connected: boolean) => void,
    ) {}

    connect() {
        this.stopped = false;
        const ws = new WebSocket(WS_URL);
        this.ws = ws;

        ws.onopen = () => {
            this.retries = 0;                          // success → reset the backoff
            this.lastPong = Date.now();
            this.onStatus(true);
            this.heartbeat = window.setInterval(() => this.tick(), 10_000);
        };

        ws.onmessage = (e) => {
            const msg = JSON.parse(e.data as string) as ServerMsg;
            if (msg.type === 'PONG') this.lastPong = Date.now();
            this.onMessage(msg);
        };

        ws.onclose = () => {
            window.clearInterval(this.heartbeat);      // stop THIS connection's heartbeat
            this.onStatus(false);
            if (this.stopped) return;                  // we closed on purpose → don't reconnect
            // Rule 1: exponential backoff, 1s, 2s, 4s, 8s, 15s, 15s…  指数退避
            const wait = Math.min(1000 * 2 ** this.retries, 15_000);
            this.retries++;
            window.setTimeout(() => this.connect(), wait);
        };

        ws.onerror = () => ws.close();                 // onclose handles the reconnect
    }

    send(msg: object) {
        if (this.ws?.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(msg));
        }
    }

    /** Rule 3: heartbeat. No PONG for 25 s → the line is dead.  心跳:25 秒没回应 → 线路已死 */
    private tick() {
        if (Date.now() - this.lastPong > 25_000) {
            this.ws?.close();                          // triggers onclose → reconnect
            return;
        }
        this.send({ type: 'PING' });
    }

    /** Close for good (scene destroyed).  永久关闭 */
    close() {
        this.stopped = true;
        this.ws?.close();
    }
}