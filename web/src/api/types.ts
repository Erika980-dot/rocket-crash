export type RoundStatus = 'BETTING' | 'RUNNING' | 'CRASHED' | 'CANCELLED'

export interface Round {
  id: number
  status: RoundStatus
  seedHash: string
  multiplier: number
  crashPoint: number | null
  serverSeed: string | null
}

export type BetStatus = 'PLACED' | 'WON' | 'LOST' | 'REFUNDED'

export interface Bet {
  id: number
  roundId: number
  amount: number
  status: BetStatus
  cashoutAt: number | null
  payout: number
}

export interface Me { id: number; username: string }
export interface Wallet { userId: number; balance: number }

/** 250 → "2.50x" */
export function fmtX(hundredths: number): string {
  return (hundredths / 100).toFixed(2) + 'x'
}