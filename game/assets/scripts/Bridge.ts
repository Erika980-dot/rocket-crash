import { setToken } from './Api';

let tokenWaiters: Array<() => void> = [];

/** Start listening to Vue. Call once, at game start.  开始监听 Vue 的消息 */
export function initBridge(onToken: () => void) {
    window.addEventListener('message', (e: MessageEvent) => {
        // Only trust messages from our own site  只相信来自自己网站的消息
        if (e.origin !== window.location.origin) return;

        const msg = e.data;
        if (msg?.type === 'TOKEN' && typeof msg.token === 'string') {
            setToken(msg.token);
            tokenWaiters.forEach((resolve) => resolve());   // wake up anyone waiting
            tokenWaiters = [];
            onToken();
        }
    });
    sendToParent({ type: 'GAME_READY' });   // "I'm loaded, who's playing?"
}

/** Send a message to the Vue page (does nothing when not embedded).  发消息给 Vue */
export function sendToParent(msg: object) {
    if (window.parent !== window) {
        window.parent.postMessage(msg, window.location.origin);
    }
}

/** Ask Vue for a fresh token and wait for it (max 5 s).  向 Vue 要新 token 并等待 */
export function requestNewToken(): Promise<void> {
    return new Promise((resolve, reject) => {
        tokenWaiters.push(resolve);
        sendToParent({ type: 'TOKEN_EXPIRED' });
        setTimeout(() => reject(new Error('No new token')), 5000);
    });
}