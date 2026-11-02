// ⚠️ MUST be the same value as GROWTH_PER_MS in CrashMath.java!
// ⚠️ 必须和后端 CrashMath.java 的数值完全一样!
export const GROWTH_PER_MS = 0.00006;

/** Multiplier in hundredths after elapsedMs of flight.  飞行 elapsedMs 毫秒后的倍数 */
export function multiplierAt(elapsedMs: number): number {
    return Math.floor(100 * Math.exp(GROWTH_PER_MS * Math.max(0, elapsedMs)));
}

/** 250 → "2.50x" */
export function fmtX(hundredths: number): string {
    return (hundredths / 100).toFixed(2) + 'x';
}