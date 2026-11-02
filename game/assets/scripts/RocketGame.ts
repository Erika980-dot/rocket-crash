import { _decorator, Component, Label, Node, Graphics, Vec3, Color, tween, UIOpacity, Prefab, instantiate, Sprite } from 'cc';
import { getJson } from './Api';
import { fmtX, multiplierAt } from './GameMath';
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

// Where the flight path sits on screen (Canvas centre = 0,0)  飞行路线在画面的位置
const START = new Vec3(-560, -280, 0);   // launch point, bottom-left  起点(左下)
const WIDTH = 1000;                      // px for 20 s of flight      20 秒飞过的宽度
const HEIGHT = 500;                      // px from 1x to 10x          1x 到 10x 的高度
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

    private round: RoundView | null = null;
    private clockOffset = 0;     // serverTime − localTime
    private polling = false;

    // ---------- lifecycle  生命周期 ----------

    /** Runs once, when the scene starts.  场景开始时执行一次 */
    start() {
        this.loadRecent(); 
        this.poll();
        this.schedule(this.poll, 0.5);   // Cocos timer: every 0.5 s  每 0.5 秒
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
            this.rocket.setPosition(this.pointAt(elapsed));
            // Direction of flight = from where we were 0.1 s ago to where we are now
            const before = this.pointAt(Math.max(0, elapsed - 100));
            const now = this.pointAt(elapsed);
            const degrees = Math.atan2(now.y - before.y, now.x - before.x) * 180 / Math.PI;

            // The 🚀 emoji is already drawn pointing up-right (45°), so subtract 45
            this.rocket.angle = degrees - 45;
            this.drawCurve(elapsed);
        }
        // CRASHED: nothing to animate; playCrash() already set the labels
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
        }
        if (r.status === 'CRASHED' && prev?.status !== 'CRASHED') {
            this.playCrash(r.crashPoint ?? r.multiplier);
            this.loadRecent();
        }
    }

    // ---------- drawing  画面 ----------

    /** Screen position after elapsedMs: time → x, multiplier → y.  时间决定 x,倍数决定 y */
    private pointAt(elapsedMs: number): Vec3 {
        const seconds = elapsedMs / 1000;
        const x = START.x + Math.min(seconds / SECONDS_ACROSS, 1) * WIDTH;

        const m = multiplierAt(elapsedMs) / 100;               // e.g. 2.5
        const y = START.y + Math.min(Math.log10(m), 1) * HEIGHT; // 1x → 0, 10x → top

        return new Vec3(x, y, 0);
    }

    /** Draw the path from launch until now.  画出从起飞到现在的轨迹 */
    private drawCurve(elapsedMs: number) {
        const g = this.curve;
        g.clear();
        g.moveTo(START.x, START.y);
        const steps = 40;
        for (let i = 1; i <= steps; i++) {
            const p = this.pointAt((elapsedMs * i) / steps);
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
}