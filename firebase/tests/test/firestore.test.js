import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  collection, deleteDoc, doc, getDoc, getDocs, serverTimestamp, setDoc, Timestamp, updateDoc, writeBatch,
} from 'firebase/firestore';
import { createEnv } from './setup.js';
import { CODE, MATCH, config, createMatchBatch, joinTx, leaveTx, player, readMatch } from './fixtures.js';

const HOST = 'host';
const ALICE = 'alice';
const BOB = 'bob';
const MALLORY = 'mallory';

describe('Firestore rules', () => {
  let env;
  const as = (uid) => env.authenticatedContext(uid).firestore();
  const anon = () => env.unauthenticatedContext().firestore();

  /** Moves a document's timestamps into the past, so rate limits and expiry can be tested. */
  async function age(path, fields, ms = 60_000) {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const past = Timestamp.fromMillis(Date.now() - ms);
      await updateDoc(doc(ctx.firestore(), path), Object.fromEntries(fields.map((f) => [f, past])));
    });
  }

  async function openLobby(...guests) {
    await assertSucceeds(createMatchBatch(as(HOST), HOST).commit());
    for (const g of guests) await assertSucceeds(joinTx(as(g), g, 'Guest ' + g));
    await age(`matches/${MATCH}`, ['updatedAt']);
    for (const uid of [HOST, ...guests]) await age(`matches/${MATCH}/players/${uid}`, ['updatedAt']);
  }

  before(async () => {
    env = await createEnv();
  });

  afterEach(async () => {
    await env.clearFirestore();
  });

  after(async () => {
    await env.cleanup();
  });

  describe('defaults', () => {
    it('denies signed-out access everywhere', async () => {
      await assertFails(getDoc(doc(anon(), 'matches/any')));
      await assertFails(setDoc(doc(anon(), 'users/x'), { name: 'Ana' }));
      await assertFails(getDoc(doc(anon(), 'joinCodes/K7Q2XM')));
    });

    it('denies unknown collections', async () => {
      await assertFails(setDoc(doc(as(ALICE), 'secrets/x'), { a: 1 }));
      await assertFails(getDoc(doc(as(ALICE), 'secrets/x')));
    });

    it('never allows listing matches or join codes', async () => {
      await openLobby();
      await assertFails(getDocs(collection(as(ALICE), 'matches')));
      await assertFails(getDocs(collection(as(ALICE), 'joinCodes')));
    });
  });

  describe('users', () => {
    const profile = (over = {}) => ({
      name: 'Silent Fox 42', avatarSeed: 'abc123def456', createdAt: serverTimestamp(), updatedAt: serverTimestamp(), ...over,
    });

    it('lets you create, read and delete only your own profile', async () => {
      await assertSucceeds(setDoc(doc(as(ALICE), 'users', ALICE), profile()));
      await assertSucceeds(getDoc(doc(as(ALICE), 'users', ALICE)));
      await assertFails(getDoc(doc(as(BOB), 'users', ALICE)));
      await assertFails(setDoc(doc(as(BOB), 'users', ALICE), profile()));
      await assertFails(deleteDoc(doc(as(BOB), 'users', ALICE)));
      await assertSucceeds(deleteDoc(doc(as(ALICE), 'users', ALICE)));
    });

    it('validates names, seeds and extra fields', async () => {
      await assertFails(setDoc(doc(as(ALICE), 'users', ALICE), profile({ name: 'A' })));
      await assertFails(setDoc(doc(as(ALICE), 'users', ALICE), profile({ name: 'x'.repeat(21) })));
      await assertFails(setDoc(doc(as(ALICE), 'users', ALICE), profile({ name: '<b>hi</b>' })));
      await assertFails(setDoc(doc(as(ALICE), 'users', ALICE), profile({ avatarSeed: 'NOT VALID' })));
      await assertFails(setDoc(doc(as(ALICE), 'users', ALICE), profile({ lat: 12.9 })));
      await assertFails(setDoc(doc(as(ALICE), 'users', ALICE), profile({ createdAt: Timestamp.fromMillis(0) })));
    });

    it('rate limits profile edits', async () => {
      await assertSucceeds(setDoc(doc(as(ALICE), 'users', ALICE), profile()));
      await assertFails(updateDoc(doc(as(ALICE), 'users', ALICE), { name: 'Quick Edit', updatedAt: serverTimestamp() }));
      await age(`users/${ALICE}`, ['updatedAt']);
      await assertSucceeds(updateDoc(doc(as(ALICE), 'users', ALICE), { name: 'Slow Edit', updatedAt: serverTimestamp() }));
      await assertFails(updateDoc(doc(as(ALICE), 'users', ALICE), { createdAt: serverTimestamp() }));
    });
  });

  describe('creating a match', () => {
    it('lets a host create a match, player document and join code together', async () => {
      await assertSucceeds(createMatchBatch(as(HOST), HOST).commit());
      const m = await readMatch(as(ALICE));
      if (m.phase !== 'lobby' || m.playerCount !== 1) throw new Error('unexpected match');
      await assertSucceeds(getDoc(doc(as(ALICE), 'joinCodes', CODE)));
    });

    it('rejects a match without its join code or host player', async () => {
      const db = as(HOST);
      const lone = writeBatch(db);
      lone.set(doc(db, 'matches', MATCH), {
        hostUid: HOST, code: CODE, phase: 'lobby', config: config(), area: { kind: 'circle', lat: 1, lng: 1, radiusM: 400, points: [] },
        playerCount: 1, kicked: [], createdAt: serverTimestamp(), updatedAt: serverTimestamp(), phaseAt: serverTimestamp(),
      });
      await assertFails(lone.commit());
    });

    it('rejects impersonation, bad settings, bad areas and pre-started phases', async () => {
      await assertFails(createMatchBatch(as(MALLORY), HOST).commit());
      await assertFails(createMatchBatch(as(HOST), HOST, { match: { phase: 'hunt' } }).commit());
      await assertFails(createMatchBatch(as(HOST), HOST, { match: { playerCount: 5 } }).commit());
      await assertFails(createMatchBatch(as(HOST), HOST, { match: { config: config({ maxPlayers: 50 }) } }).commit());
      await assertFails(createMatchBatch(as(HOST), HOST, { match: { config: config({ seekers: 8 }) } }).commit());
      await assertFails(createMatchBatch(as(HOST), HOST, { match: { config: config({ mode: 'battle' }) } }).commit());
      await assertFails(createMatchBatch(as(HOST), HOST, {
        match: { area: { kind: 'circle', lat: 1, lng: 1, radiusM: 50_000, points: [] } },
      }).commit());
      await assertFails(createMatchBatch(as(HOST), HOST, {
        match: { area: { kind: 'polygon', lat: 1, lng: 1, radiusM: 0, points: [1, 2, 3, 4] } },
      }).commit());
      await assertFails(createMatchBatch(as(HOST), HOST, { code: 'K7Q2X0' }).commit());
      await assertFails(createMatchBatch(as(HOST), HOST, { hostPlayer: { role: 'seeker' } }).commit());
    });

    it('accepts a valid polygon area', async () => {
      const points = [12.97, 77.59, 12.98, 77.59, 12.98, 77.60, 12.97, 77.60];
      await assertSucceeds(createMatchBatch(as(HOST), HOST, {
        match: { area: { kind: 'polygon', lat: 12.975, lng: 77.595, radiusM: 0, points } },
      }).commit());
    });

    it('does not let a code be taken over', async () => {
      await openLobby();
      await assertFails(createMatchBatch(as(MALLORY), MALLORY, { matchId: 'm_zzzzzzzzzzzzzzzz' }).commit());
    });
  });

  describe('joining and leaving', () => {
    it('lets players join with the count moving in the same transaction', async () => {
      await openLobby();
      await assertSucceeds(joinTx(as(ALICE), ALICE));
      const m = await readMatch(as(ALICE));
      if (m.playerCount !== 2) throw new Error('count did not move');
      await assertSucceeds(getDocs(collection(as(ALICE), 'matches', MATCH, 'players')));
    });

    it('rejects joins that skip or fake the count', async () => {
      await openLobby();
      await assertFails(setDoc(doc(as(ALICE), 'matches', MATCH, 'players', ALICE), player('Alice')));
      await assertFails(joinTx(as(ALICE), ALICE, 'Alice', { bump: 2 }));
      await assertFails(updateDoc(doc(as(ALICE), 'matches', MATCH), { playerCount: 2 }));
    });

    it('rejects joining as someone else, with a role, or ready', async () => {
      await openLobby();
      await assertFails(joinTx(as(MALLORY), ALICE));
      await assertFails(joinTx(as(ALICE), ALICE, 'Alice', { playerOver: { role: 'seeker' } }));
      await assertFails(joinTx(as(ALICE), ALICE, 'Alice', { playerOver: { ready: true } }));
    });

    it('enforces the player cap', async () => {
      await assertSucceeds(createMatchBatch(as(HOST), HOST, { match: { config: config({ maxPlayers: 2 }) } }).commit());
      await assertSucceeds(joinTx(as(ALICE), ALICE));
      await assertFails(joinTx(as(BOB), BOB));
    });

    it("lets a player check their own document before joining, but no one else's", async () => {
      await openLobby(ALICE);
      await assertSucceeds(getDoc(doc(as(BOB), 'matches', MATCH, 'players', BOB)));
      await assertFails(getDoc(doc(as(BOB), 'matches', MATCH, 'players', ALICE)));
    });

    it('keeps outsiders out of the player list', async () => {
      await openLobby(ALICE);
      await assertFails(getDocs(collection(as(MALLORY), 'matches', MATCH, 'players')));
      await assertFails(getDoc(doc(as(MALLORY), 'matches', MATCH, 'players', ALICE)));
    });

    it('only allows joining in the lobby phase and before expiry', async () => {
      await openLobby(ALICE);
      await assertSucceeds(updateDoc(doc(as(HOST), 'matches', MATCH), {
        phase: 'briefing', phaseAt: serverTimestamp(), updatedAt: serverTimestamp(),
      }));
      await assertFails(joinTx(as(BOB), BOB));
      await env.clearFirestore();
      await openLobby();
      await age(`matches/${MATCH}`, ['createdAt'], 7 * 3600_000);
      await assertFails(joinTx(as(BOB), BOB));
    });

    it('lets players leave, moving the count', async () => {
      await openLobby(ALICE);
      await assertFails(deleteDoc(doc(as(ALICE), 'matches', MATCH, 'players', ALICE)));
      await assertSucceeds(leaveTx(as(ALICE), ALICE));
      const m = await readMatch(as(BOB));
      if (m.playerCount !== 1) throw new Error('count did not move');
      await assertFails(leaveTx(as(MALLORY), ALICE));
    });
  });

  describe('inside the lobby', () => {
    it('lets players toggle ready and edit their own name only', async () => {
      await openLobby(ALICE, BOB);
      const mine = doc(as(ALICE), 'matches', MATCH, 'players', ALICE);
      await assertSucceeds(updateDoc(mine, { ready: true, updatedAt: serverTimestamp() }));
      await assertFails(updateDoc(mine, { ready: false, updatedAt: serverTimestamp() })); // too soon
      await assertFails(updateDoc(doc(as(ALICE), 'matches', MATCH, 'players', BOB), { ready: true, updatedAt: serverTimestamp() }));
      await age(`matches/${MATCH}/players/${ALICE}`, ['updatedAt']);
      await assertFails(updateDoc(mine, { role: 'seeker', updatedAt: serverTimestamp() }));
      await assertFails(updateDoc(mine, { joinedAt: serverTimestamp(), updatedAt: serverTimestamp() }));
    });

    it('lets only the host assign roles, and only in the lobby', async () => {
      await openLobby(ALICE);
      const alice = (db) => doc(db, 'matches', MATCH, 'players', ALICE);
      await assertFails(updateDoc(alice(as(BOB)), { role: 'seeker', updatedAt: serverTimestamp() }));
      await assertFails(updateDoc(alice(as(HOST)), { role: 'king', updatedAt: serverTimestamp() }));
      await assertFails(updateDoc(alice(as(HOST)), { name: 'Renamed', updatedAt: serverTimestamp() }));
      await assertFails(updateDoc(alice(as(HOST)), { ready: true, updatedAt: serverTimestamp() }));
      await assertSucceeds(updateDoc(alice(as(HOST)), { role: 'seeker', updatedAt: serverTimestamp() }));
      await assertSucceeds(updateDoc(doc(as(HOST), 'matches', MATCH), {
        phase: 'briefing', phaseAt: serverTimestamp(), updatedAt: serverTimestamp(),
      }));
      await assertFails(updateDoc(alice(as(HOST)), { role: 'hider', updatedAt: serverTimestamp() }));
    });

    it('lets players confirm the briefing only during the briefing', async () => {
      await openLobby(ALICE);
      const mine = doc(as(ALICE), 'matches', MATCH, 'players', ALICE);
      await assertFails(updateDoc(mine, { briefed: true, updatedAt: serverTimestamp() }));
      await assertSucceeds(updateDoc(doc(as(HOST), 'matches', MATCH), {
        phase: 'briefing', phaseAt: serverTimestamp(), updatedAt: serverTimestamp(),
      }));
      await assertSucceeds(updateDoc(mine, { briefed: true, updatedAt: serverTimestamp() }));
      await age(`matches/${MATCH}/players/${ALICE}`, ['updatedAt']);
      await assertFails(updateDoc(mine, { ready: true, updatedAt: serverTimestamp() }));
    });
  });

  describe('host controls', () => {
    it('allows only legal phase changes, by the host', async () => {
      await openLobby(ALICE);
      const match = (db) => doc(db, 'matches', MATCH);
      const to = (phase) => ({ phase, phaseAt: serverTimestamp(), updatedAt: serverTimestamp() });
      await assertFails(updateDoc(match(as(ALICE)), to('briefing')));
      await assertFails(updateDoc(match(as(HOST)), to('hunt')));
      await assertFails(updateDoc(match(as(HOST)), to('nonsense')));
      await assertSucceeds(updateDoc(match(as(HOST)), to('briefing')));
      await age(`matches/${MATCH}`, ['updatedAt']);
      await assertFails(updateDoc(match(as(HOST)), { config: config({ seekers: 2 }), updatedAt: serverTimestamp() }));
      await assertSucceeds(updateDoc(match(as(HOST)), to('headstart')));
      await age(`matches/${MATCH}`, ['updatedAt']);
      await assertFails(updateDoc(match(as(HOST)), to('lobby')));
      await assertSucceeds(updateDoc(match(as(HOST)), to('ended')));
    });

    it('requires phaseAt to move with the phase', async () => {
      await openLobby();
      await assertFails(updateDoc(doc(as(HOST), 'matches', MATCH), { phase: 'briefing', updatedAt: serverTimestamp() }));
    });

    it('lets the host change settings in the lobby, rate limited', async () => {
      await openLobby();
      const match = doc(as(HOST), 'matches', MATCH);
      await assertSucceeds(updateDoc(match, { config: config({ seekers: 2 }), updatedAt: serverTimestamp() }));
      await assertFails(updateDoc(match, { config: config({ seekers: 3 }), updatedAt: serverTimestamp() }));
      await assertFails(updateDoc(match, { hostUid: ALICE }));
    });

    it('kicks a player for good', async () => {
      await openLobby(ALICE, BOB);
      const db = as(HOST);
      const kick = writeBatch(db);
      kick.update(doc(db, 'matches', MATCH), { kicked: [ALICE], playerCount: 2, updatedAt: serverTimestamp() });
      kick.delete(doc(db, 'matches', MATCH, 'players', ALICE));
      await assertSucceeds(kick.commit());
      await assertFails(joinTx(as(ALICE), ALICE));
    });

    it('rejects kicks that do not remove the player, and deletes without a kick', async () => {
      await openLobby(ALICE, BOB);
      await assertFails(updateDoc(doc(as(HOST), 'matches', MATCH), { kicked: [ALICE], playerCount: 2, updatedAt: serverTimestamp() }));
      await assertFails(deleteDoc(doc(as(HOST), 'matches', MATCH, 'players', ALICE)));
      await assertFails(updateDoc(doc(as(ALICE), 'matches', MATCH), { kicked: [BOB] }));
    });

    it('lets the host end and clean up everything in one batch', async () => {
      await openLobby(ALICE);
      const db = as(HOST);
      const end = writeBatch(db);
      end.delete(doc(db, 'matches', MATCH, 'players', ALICE));
      end.delete(doc(db, 'matches', MATCH, 'players', HOST));
      end.delete(doc(db, 'joinCodes', CODE));
      end.delete(doc(db, 'matches', MATCH));
      await assertSucceeds(end.commit());
    });

    it('lets anyone clear an expired match, but not a live one', async () => {
      await openLobby(ALICE);
      await assertFails(deleteDoc(doc(as(MALLORY), 'matches', MATCH)));
      await assertFails(deleteDoc(doc(as(MALLORY), 'joinCodes', CODE)));
      await age(`matches/${MATCH}`, ['createdAt'], 7 * 3600_000);
      await age(`joinCodes/${CODE}`, ['createdAt'], 7 * 3600_000);
      await assertSucceeds(deleteDoc(doc(as(MALLORY), 'matches', MATCH, 'players', ALICE)));
      await assertSucceeds(deleteDoc(doc(as(MALLORY), 'joinCodes', CODE)));
      await assertSucceeds(deleteDoc(doc(as(MALLORY), 'matches', MATCH)));
    });
  });
});
