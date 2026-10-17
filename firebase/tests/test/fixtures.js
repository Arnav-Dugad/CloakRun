import { doc, getDoc, runTransaction, serverTimestamp, writeBatch } from 'firebase/firestore';

export const MATCH = 'm_abcdefghijklmnop';
export const CODE = 'K7Q2XM';

export const config = (over = {}) => ({
  mode: 'classic',
  seekers: 1,
  headStartSec: 120,
  huntMin: 20,
  maxPlayers: 8,
  ...over,
});

export const circle = (over = {}) => ({ kind: 'circle', lat: 12.9763, lng: 77.5929, radiusM: 400, points: [], ...over });

export const player = (name, over = {}) => ({
  name,
  avatarSeed: 'seed' + name.toLowerCase().replace(/[^a-z0-9]/g, ''),
  role: 'unassigned',
  ready: false,
  briefed: false,
  joinedAt: serverTimestamp(),
  updatedAt: serverTimestamp(),
  ...over,
});

/** The host's create: match, own player document and join code in one batch, as the app does it. */
export function createMatchBatch(db, hostUid, { matchId = MATCH, code = CODE, match = {}, hostPlayer = {} } = {}) {
  const batch = writeBatch(db);
  batch.set(doc(db, 'matches', matchId), {
    hostUid,
    code,
    phase: 'lobby',
    config: config(),
    area: circle(),
    playerCount: 1,
    kicked: [],
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    phaseAt: serverTimestamp(),
    ...match,
  });
  batch.set(doc(db, 'matches', matchId, 'players', hostUid), player('Host Fox', hostPlayer));
  batch.set(doc(db, 'joinCodes', code), { matchId, hostUid, createdAt: serverTimestamp() });
  return batch;
}

/** Joining: read the match, bump the count and create your own player document atomically. */
export function joinTx(db, uid, name = 'Guest Owl', { matchId = MATCH, bump = 1, playerOver = {} } = {}) {
  return runTransaction(db, async (tx) => {
    const m = await tx.get(doc(db, 'matches', matchId));
    tx.update(doc(db, 'matches', matchId), { playerCount: m.data().playerCount + bump });
    tx.set(doc(db, 'matches', matchId, 'players', uid), player(name, playerOver));
  });
}

export function leaveTx(db, uid, { matchId = MATCH, bump = -1 } = {}) {
  return runTransaction(db, async (tx) => {
    const m = await tx.get(doc(db, 'matches', matchId));
    tx.update(doc(db, 'matches', matchId), { playerCount: m.data().playerCount + bump });
    tx.delete(doc(db, 'matches', matchId, 'players', uid));
  });
}

export async function readMatch(db, matchId = MATCH) {
  return (await getDoc(doc(db, 'matches', matchId))).data();
}
