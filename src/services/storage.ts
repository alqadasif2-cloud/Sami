import { Challenge, Milestone, Trade } from '../types';

const CHALLENGE_KEY = 'trading_challenge_state_v1';
const TRADES_KEY = 'trading_challenge_trades_v1';
const MILESTONES_KEY = 'trading_challenge_milestones_v1';

export const TOTAL_MILESTONES = 150;

export function createInitialMilestones(challengeId: string): Milestone[] {
  const milestones: Milestone[] = [];
  for (let i = 1; i <= TOTAL_MILESTONES; i++) {
    milestones.push({
      id: `milestone_${i}`,
      challengeId,
      milestoneNumber: i,
      profitCents: null,
      balanceCents: null,
      status: 'PENDING',
      tradeId: null,
      timestamp: null,
      attachmentPath: null,
    });
  }
  return milestones;
}

export function createInitialChallenge(): Challenge {
  const now = Date.now();
  return {
    id: `challenge_${now}`,
    userName: '',
    initialCapitalCents: 10000, // $100.00
    currentBalanceCents: 10000, // $100.00
    targetBalanceCents: 300000, // $3,000.00
    tradeCount: 0,
    challengeStarted: false,
    createdAt: now,
    updatedAt: now,
  };
}

export class StorageService {
  private static isSubmitting = false;

  public static getChallenge(): Challenge {
    try {
      const data = localStorage.getItem(CHALLENGE_KEY);
      if (data) {
        return JSON.parse(data);
      }
    } catch (e) {
      console.error('Failed to load challenge from localStorage', e);
    }
    const initial = createInitialChallenge();
    this.saveChallenge(initial);
    return initial;
  }

  public static saveChallenge(challenge: Challenge): void {
    try {
      challenge.updatedAt = Date.now();
      localStorage.setItem(CHALLENGE_KEY, JSON.stringify(challenge));
    } catch (e) {
      console.error('Failed to save challenge to localStorage', e);
    }
  }

  public static getTrades(): Trade[] {
    try {
      const data = localStorage.getItem(TRADES_KEY);
      if (data) {
        return JSON.parse(data);
      }
    } catch (e) {
      console.error('Failed to load trades from localStorage', e);
    }
    return [];
  }

  public static saveTrades(trades: Trade[]): void {
    try {
      localStorage.setItem(TRADES_KEY, JSON.stringify(trades));
    } catch (e) {
      console.error('Failed to save trades to localStorage', e);
    }
  }

  public static getMilestones(challengeId?: string): Milestone[] {
    try {
      const data = localStorage.getItem(MILESTONES_KEY);
      if (data) {
        const parsed: Milestone[] = JSON.parse(data);
        if (parsed && parsed.length === TOTAL_MILESTONES) {
          return parsed;
        }
      }
    } catch (e) {
      console.error('Failed to load milestones from localStorage', e);
    }
    const initial = createInitialMilestones(challengeId || 'active_challenge');
    this.saveMilestones(initial);
    return initial;
  }

  public static saveMilestones(milestones: Milestone[]): void {
    try {
      localStorage.setItem(MILESTONES_KEY, JSON.stringify(milestones));
    } catch (e) {
      console.error('Failed to save milestones to localStorage', e);
    }
  }

  public static updateSettings(
    userName: string,
    initialCapitalCents: number,
    targetBalanceCents: number
  ): Challenge {
    const challenge = this.getChallenge();
    challenge.userName = userName.trim();
    challenge.targetBalanceCents = targetBalanceCents;

    // If challenge has not started yet, user can edit initial capital directly
    if (!challenge.challengeStarted) {
      challenge.initialCapitalCents = initialCapitalCents;
      challenge.currentBalanceCents = initialCapitalCents;
    }

    this.saveChallenge(challenge);
    return challenge;
  }

  public static startChallenge(initialCapitalCents?: number): Challenge {
    const challenge = this.getChallenge();
    const capital = initialCapitalCents ?? challenge.initialCapitalCents;

    challenge.initialCapitalCents = capital;
    challenge.currentBalanceCents = capital;
    challenge.tradeCount = 0;
    challenge.challengeStarted = true;
    challenge.updatedAt = Date.now();

    // Reset trades and milestones
    this.saveChallenge(challenge);
    this.saveTrades([]);
    this.saveMilestones(createInitialMilestones(challenge.id));

    return challenge;
  }

  public static recordTrade(
    resultCents: number,
    attachmentPath: string | null = null,
    note?: string
  ): { challenge: Challenge; trade: Trade; milestone: Milestone } {
    if (this.isSubmitting) {
      throw new Error('عملية التسجيل قيد التنفيذ، يرجى الانتظار.');
    }

    this.isSubmitting = true;

    try {
      const challenge = this.getChallenge();
      if (!challenge.challengeStarted) {
        throw new Error('يجب بدء التحدي أولاً.');
      }

      if (challenge.tradeCount >= TOTAL_MILESTONES) {
        throw new Error('تم إكمال جميع محطات التحدي الـ150.');
      }

      if (resultCents === 0) {
        throw new Error('لا يمكن أن تكون نتيجة الصفقة صفراً.');
      }

      const trades = this.getTrades();
      const milestones = this.getMilestones(challenge.id);

      const oldBalance = challenge.currentBalanceCents;
      const newBalance = oldBalance + resultCents;
      const tradeNumber = challenge.tradeCount + 1;
      const type = resultCents > 0 ? 'WIN' : 'LOSS';
      const timestamp = Date.now();
      const tradeId = `trade_${tradeNumber}_${timestamp}`;

      const newTrade: Trade = {
        id: tradeId,
        challengeId: challenge.id,
        tradeNumber,
        resultCents,
        oldBalanceCents: oldBalance,
        newBalanceCents: newBalance,
        type,
        attachmentPath,
        timestamp,
        note,
      };

      // Milestone index (0-indexed for tradeNumber 1..150)
      const milestoneIndex = tradeNumber - 1;
      if (milestones[milestoneIndex]) {
        milestones[milestoneIndex] = {
          ...milestones[milestoneIndex],
          profitCents: resultCents,
          balanceCents: newBalance,
          status: type,
          tradeId,
          timestamp,
          attachmentPath,
        };
      }

      // Update challenge
      challenge.currentBalanceCents = newBalance;
      challenge.tradeCount = tradeNumber;
      challenge.updatedAt = timestamp;

      // Atomic commit
      trades.push(newTrade);
      this.saveTrades(trades);
      this.saveMilestones(milestones);
      this.saveChallenge(challenge);

      return {
        challenge,
        trade: newTrade,
        milestone: milestones[milestoneIndex],
      };
    } finally {
      this.isSubmitting = false;
    }
  }

  public static resetChallenge(): Challenge {
    const challenge = this.getChallenge();
    challenge.currentBalanceCents = challenge.initialCapitalCents;
    challenge.tradeCount = 0;
    challenge.updatedAt = Date.now();

    this.saveChallenge(challenge);
    this.saveTrades([]);
    this.saveMilestones(createInitialMilestones(challenge.id));

    return challenge;
  }

  public static startNewChallenge(newInitialCapitalCents: number): Challenge {
    const challenge = this.getChallenge();
    const now = Date.now();

    challenge.id = `challenge_${now}`;
    challenge.initialCapitalCents = newInitialCapitalCents;
    challenge.currentBalanceCents = newInitialCapitalCents;
    challenge.tradeCount = 0;
    challenge.challengeStarted = true;
    challenge.createdAt = now;
    challenge.updatedAt = now;

    this.saveChallenge(challenge);
    this.saveTrades([]);
    this.saveMilestones(createInitialMilestones(challenge.id));

    return challenge;
  }
}
