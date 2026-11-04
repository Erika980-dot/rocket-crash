export type ServerMsg = { type: string; [key: string]: any }

const WS_URL = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`

export class Net {
  private ws: WebSocket | null = null
  private retries = 0
  private lastPong = 0
  private heartbeat: number | undefined
  private stopped = false

  constructor(
    private onMessage: (msg: ServerMsg) => void,
    private onStatus: (connected: boolean) => void = () => {},
  ) {}

  connect() {
    this.stopped = false
    const ws = new WebSocket(WS_URL)
    this.ws = ws
    ws.onopen = () => {
      this.retries = 0
      this.lastPong = Date.now()
      this.onStatus(true)
      this.heartbeat = window.setInterval(() => this.tick(), 10_000)
    }
    ws.onmessage = (e) => {
      const msg = JSON.parse(e.data) as ServerMsg
      if (msg.type === 'PONG') this.lastPong = Date.now()
      this.onMessage(msg)
    }
    ws.onclose = () => {
      window.clearInterval(this.heartbeat)
      this.onStatus(false)
      if (this.stopped) return
      const wait = Math.min(1000 * 2 ** this.retries, 15_000)
      this.retries++
      window.setTimeout(() => this.connect(), wait)
    }
    ws.onerror = () => ws.close()
  }

  send(msg: object) {
    if (this.ws?.readyState === WebSocket.OPEN) this.ws.send(JSON.stringify(msg))
  }

  private tick() {
    if (Date.now() - this.lastPong > 25_000) { this.ws?.close(); return }
    this.send({ type: 'PING' })
  }

  close() {
    this.stopped = true
    this.ws?.close()
  }
}