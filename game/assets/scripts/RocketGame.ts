import { _decorator, Component, Label, Node, Graphics, Vec3, Color, tween, UIOpacity,
         Prefab, instantiate, Sprite, Button, EditBox } from 'cc';
import { ApiError, getJson, hasToken, request } from './Api';
import { initBridge, requestNewToken, sendToParent } from './Bridge';
import { fmtX, multiplierAt, GROWTH_PER_MS } from './GameMath';
const { ccclass, property } = _decorator;

/** Same shape as the backend's RoundView  对应后端的 RoundView */
interface RoundView {
    id: number;
    status: 'BETTING' | 'RUNNING' | 'CRASHED' | 'CANCELLED';
    multiplier: number;
    bettingEndsAtMs: number;
    launchedAtMs: number | null;
    serverTimeMs: number;
    crashPoint: number | null;
}

interface Bet {
    id: number;
    roundId: number;
    amount: number;
    status: 'PLACED' | 'WON' | 'LOST' | 'REFUNDED';
    cashoutAt: number | null;
    payout: number;
}

// Where the flight path sits on screen (Canvas centre = 0,0)  飞行路线在画面的位置
const START = new Vec3(-560, -220, 0);   // was -280
const WIDTH = 1000;                      // px for 20 s of flight      20 秒飞过的宽度
const HEIGHT = 430;                      // was 500
const SECONDS_ACROSS = 20;

const COLOR_BETTING = new Color(252, 211, 77);    // amber  琥珀色
const COLOR_RUNNING = new Color(52, 211, 153);    // green  绿色
const COLOR_CRASHED = new Color(248, 113, 113);   // red    红色

@ccclass('RocketGame')
export class RocketGame extends Component {

    // These 4 appear as slots in the Inspector. Drag nodes into them.
    // 这 4 个会出现在检查器里,把节点拖进去绑定。
    @property(Label) multiplierLabel: Label = null!;
    @property(Label) statusLabel: Label = null!;
    @property(Node) rocket: Node = null!;
    @property(Graphics) curve: Graphics = null!;
    @property(Prefab) pillPrefab: Prefab = null!;
    @property(Node) recentBar: Node = null!; 
    @property(EditBox) amountInput: EditBox = null!;
    @property(Button) betButton: Button = null!;
    @property(Button) cashOutButton: Button = null!;
    @property(Label) messageLabel: Label = null!;

    private round: RoundView | null = null;
    private myBet: Bet | null = null;   // my bet in the CURRENT round  当前回合的注单
    private busy = false;               // a request is in progress  正在请求中
    private clockOffset = 0;     // serverTime − localTime
    private polling = false;

    // ---------- lifecycle  生命周期 ----------

    /** Runs once, when the scene starts.  场景开始时执行一次 */
    start() {
    // Button clicks → our methods  按钮点击 → 我们的方法
    this.betButton.node.on(Button.EventType.CLICK, this.onBetClick, this);
    this.cashOutButton.node.on(Button.EventType.CLICK, this.onCashOutClick, this);

    // Listen to Vue; when we get a token, check whether we already bet this round
    initBridge(() => {
        this.messageLabel.string = '';
        this.loadMyBet();
    });

    this.loadRecent();
    this.poll();
    this.schedule(this.poll, 0.5);
    }

    /** Runs EVERY FRAME (about 60×/s). dt = seconds since last frame.  每一帧执行 */
    update(dt: number) {
        const r = this.round;
        if (!r) return;

        if (r.status === 'BETTING') {
            const left = Math.max(0, (r.bettingEndsAtMs - this.serverNow()) / 1000);
            this.multiplierLabel.string = '1.00x';
            this.multiplierLabel.color = COLOR_BETTING;
            this.statusLabel.string = `Place your bets!  ${left.toFixed(1)}s`;

        } else if (r.status === 'RUNNING' && r.launchedAtMs !== null) {
            // Compute locally from the launch time: smooth at 60 fps
            // 用起飞时间在本地计算:60 帧流畅动画
            const elapsed = this.serverNow() - r.launchedAtMs;
            this.multiplierLabel.string = fmtX(multiplierAt(elapsed));
            this.multiplierLabel.color = COLOR_RUNNING;
            this.statusLabel.string = 'Flying…';

            const scale = this.viewScale(elapsed);              // ONE zoom for this frame  这一帧用同一个缩放
            const now = this.pointAt(elapsed, scale);
            const before = this.pointAt(Math.max(0, elapsed - 100), scale);

            this.rocket.setPosition(now);
            this.rocket.angle = Math.atan2(now.y - before.y, now.x - before.x) * 180 / Math.PI - 45;
            this.drawCurve(elapsed, scale);
        }
        // CRASHED: nothing to animate; playCrash() already set the labels
        this.updateControls();
    }

    // ---------- talking to the server  和服务器沟通 ----------

    private serverNow(): number {
        return Date.now() + this.clockOffset;
    }

    private async poll() {
        if (this.polling) return;          // don't start a 2nd poll while one is running
        this.polling = true;
        try {
            const r = await getJson<RoundView>('/rounds/current');
            this.clockOffset = r.serverTimeMs - Date.now();
            this.onRound(r);
        } catch (e) {
            this.statusLabel.string = 'Connecting…';
        } finally {
            this.polling = false;
        }
    }

    /** React only to CHANGES: new round, or just crashed.  只对"变化"做反应 */
    private onRound(r: RoundView) {
    const prev = this.round;
    this.round = r;

    const isNewRound = !prev || prev.id !== r.id;
    if (isNewRound && r.status === 'BETTING') {
        this.resetScene();
        this.myBet = null;                 // new round, no bet yet
        this.messageLabel.string = '';
    }

    if (r.status === 'CRASHED' && prev?.status !== 'CRASHED') {
        this.playCrash(r.crashPoint ?? r.multiplier);
        this.loadRecent();
        if (this.myBet?.status === 'PLACED') {
            // Didn't cash out in time: get the REAL result from the server
            this.messageLabel.string = `💥 Lost ${this.myBet.amount}`;
            this.loadMyBet();
            sendToParent({ type: 'BALANCE_CHANGED' });
        }
    }
}

    // ---------- drawing  画面 ----------

    /** Screen position after elapsedMs: time → x, multiplier → y.  时间决定 x,倍数决定 y */
    /** How big the axes are right now: always ~15% bigger than the current values.
 *  坐标轴现在有多大:永远比当前数值大 15% 左右 */
private viewScale(elapsedMs: number) {
    const seconds = elapsedMs / 1000;
    const m = Math.exp(GROWTH_PER_MS * elapsedMs);
    return {
        maxSeconds: Math.max(10, seconds * 1.15),   // at least 10 s wide
        maxMultiplier: Math.max(2, m * 1.15),       // at least up to 2x
    };
}

/** Screen position for a moment of the flight, using the CURRENT zoom.  用当前缩放计算位置 */
private pointAt(elapsedMs: number, scale: { maxSeconds: number; maxMultiplier: number }): Vec3 {
    const seconds = elapsedMs / 1000;
    const m = Math.exp(GROWTH_PER_MS * elapsedMs);   // smooth, not rounded  不取整,曲线才平滑
    const x = START.x + (seconds / scale.maxSeconds) * WIDTH;
    const y = START.y + ((m - 1) / (scale.maxMultiplier - 1)) * HEIGHT;
    return new Vec3(x, y, 0);
}

private drawCurve(elapsedMs: number, scale: { maxSeconds: number; maxMultiplier: number }) {
    const g = this.curve;
    g.clear();
    g.moveTo(START.x, START.y);
    const steps = 60;
    for (let i = 1; i <= steps; i++) {
        const p = this.pointAt((elapsedMs * i) / steps, scale);
        g.lineTo(p.x, p.y);
    }
    g.stroke();
}

    private playCrash(crashPoint: number) {
        this.multiplierLabel.string = fmtX(crashPoint);
        this.multiplierLabel.color = COLOR_CRASHED;
        this.statusLabel.string = `💥 Crashed at ${fmtX(crashPoint)}`;

        // Explosion: grow + fade out over 0.3 s  爆炸:0.3 秒内放大并淡出
        const opacity = this.rocket.getComponent(UIOpacity)!;
        tween(this.rocket).to(0.3, { scale: new Vec3(2.5, 2.5, 1) }).start();
        tween(opacity).to(0.3, { opacity: 0 }).start();
    }

    /** New round: rocket back to the launch pad.  新回合:火箭回到起点 */
    private resetScene() {
        this.curve.clear();
        this.rocket.setPosition(START);
        this.rocket.setScale(1, 1, 1);
        this.rocket.getComponent(UIOpacity)!.opacity = 255;
        this.rocket.angle = 0;
    }

    private async loadRecent() {
       try {
        const list = await getJson<{ crashPoint: number }[]>('/rounds/recent');

        // Remove the old pills, and destroy them to free memory
        // 移除旧标签,并销毁以释放内存
        const old = [...this.recentBar.children];
        this.recentBar.removeAllChildren();
        old.forEach((n) => n.destroy());

        for (const r of list) {
            const pill = instantiate(this.pillPrefab);          // copy the template  复制模板
            const good = r.crashPoint >= 200;                    // 2.00x or more
            pill.getComponent(Sprite)!.color = good ? new Color(16, 185, 129) : new Color(220, 38, 38);
            pill.getComponentInChildren(Label)!.string = fmtX(r.crashPoint);
            this.recentBar.addChild(pill);                       // Layout places it  Layout 自动排列
        }
       } catch {
        // not important if it fails once; it reloads at the next crash
      }
    }

    // ---------- actions  动作 ----------

private async onBetClick() {
    const amount = parseInt(this.amountInput.string, 10);
    if (!Number.isFinite(amount)) {
        this.messageLabel.string = 'Enter an amount';
        return;
    }
    await this.act(async () => {
        this.myBet = await request<Bet>('POST', '/bets', {
            amount,
            requestId: crypto.randomUUID(),   // new id per click → idempotent  每次点击新 ID
        });
        this.messageLabel.string = `Bet ${amount} placed. Good luck! 🍀`;
    });
}

private async onCashOutClick() {
    await this.act(async () => {
        this.myBet = await request<Bet>('POST', '/bets/cashout');
        this.messageLabel.string = `✅ Cashed out at ${fmtX(this.myBet.cashoutAt ?? 0)}: +${this.myBet.payout}`;
    });
}

/** Run an action with: busy flag, one silent token refresh, friendly errors.
 *  执行动作:防连点、自动刷新 token 一次、友善错误提示 */
private async act(fn: () => Promise<void>) {
    if (this.busy) return;
    this.busy = true;
    try {
        try {
            await fn();
        } catch (e) {
            if (e instanceof ApiError && e.code === 'TOKEN_EXPIRED') {
                await requestNewToken();   // ask Vue for a new one, then try once more
                await fn();
            } else {
                throw e;
            }
        }
        sendToParent({ type: 'BALANCE_CHANGED' });   // tell Vue to refresh the balance
    } catch (e) {
        if (e instanceof ApiError && e.status === 401) {
            sendToParent({ type: 'AUTH_FAILED', code: e.code });   // e.g. KICKED → Vue goes to login
        }
        this.messageLabel.string = e instanceof Error ? e.message : 'Something went wrong';
    } finally {
        this.busy = false;
    }
}

/** Ask the server whether I already have a bet this round (after a reload).
 *  问服务器这局我是否已下注(重新加载后) */
private async loadMyBet() {
    if (!hasToken() || !this.round) return;
    try {
        const bets = await request<Bet[]>('GET', '/bets/me');
        this.myBet = bets.find((b) => b.roundId === this.round!.id) ?? null;
    } catch {
        /* not critical */
    }
}

/** Enable/disable buttons and update their text, every frame.  每一帧更新按钮状态 */
private updateControls() {
    const r = this.round;
    const loggedIn = hasToken();

    this.betButton.interactable =
        loggedIn && !this.busy && r?.status === 'BETTING' && !this.myBet;

    const canCashOut =
        loggedIn && !this.busy && r?.status === 'RUNNING' && this.myBet?.status === 'PLACED';
    this.cashOutButton.interactable = canCashOut;

    const cashLabel = this.cashOutButton.node.getComponentInChildren(Label)!;
    if (canCashOut && r?.launchedAtMs) {
        const m = multiplierAt(this.serverNow() - r.launchedAtMs);
        cashLabel.string = `Cash out  +${Math.floor((this.myBet!.amount * m) / 100)}`;
    } else {
        cashLabel.string = 'Cash out';
    }

    if (!loggedIn && !this.messageLabel.string) {
        this.messageLabel.string = 'Watching. Log in from the lobby to play';
    }
}
}