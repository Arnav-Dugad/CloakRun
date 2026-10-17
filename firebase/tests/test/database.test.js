import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { get, ref, remove, serverTimestamp, set, update } from 'firebase/database';
import { createEnv } from './setup.js';

const M = 'm_abcdefghijklmnop';
const HOST = 'host';
const ALICE = 'alice';
const MALLORY = 'mallory';

describe('Realtime Database rules', () => {
  let env;
  const as = (uid) => env.authenticatedContext(uid).database();
  const zone = (over = {}) => ({ lat: 12.97, lng: 77.59, radiusM: 120, status: 'hiding', at: serverTimestamp(), ...over });

  async function lobby(...members) {
    await assertSucceeds(set(ref(as(HOST), `lobbies/${M}/host`), HOST));
    await assertSucceeds(set(ref(as(HOST), `lobbies/${M}/members/${HOST}`), true));
    for (const uid of members) await assertSucceeds(set(ref(as(uid), `lobbies/${M}/members/${uid}`), true));
  }

  before(async () => {
    env = await createEnv();
  });

  afterEach(async () => {
    await env.clearDatabase();
  });

  after(async () => {
    await env.cleanup();
  });

  it('denies everything outside the known paths', async () => {
    await assertFails(get(ref(as(ALICE), '/')));
    await assertFails(set(ref(as(ALICE), 'anything'), 1));
    await assertFails(get(ref(env.unauthenticatedContext().database(), `lobbies/${M}`)));
  });

  it('lets the first claimant become host, once', async () => {
    await lobby();
    await assertFails(set(ref(as(MALLORY), `lobbies/${M}/host`), MALLORY));
    await assertFails(set(ref(as(ALICE), `lobbies/m_other_match_id_0/host`), HOST));
    await assertFails(set(ref(as(ALICE), `lobbies/short/host`), ALICE));
  });

  it('lets players add and remove only themselves as members', async () => {
    await lobby(ALICE);
    await assertFails(set(ref(as(ALICE), `lobbies/${M}/members/${MALLORY}`), true));
    await assertFails(set(ref(as(ALICE), `lobbies/${M}/members/${ALICE}`), 'yes'));
    await assertSucceeds(get(ref(as(ALICE), `lobbies/${M}`)));
    await assertFails(get(ref(as(MALLORY), `lobbies/${M}`)));
    await assertSucceeds(remove(ref(as(ALICE), `lobbies/${M}/members/${ALICE}`)));
  });

  it('cannot join a lobby that has no host', async () => {
    await assertFails(set(ref(as(ALICE), `lobbies/${M}/members/${ALICE}`), true));
  });

  it('keeps kicked players out', async () => {
    await lobby(ALICE);
    await assertFails(set(ref(as(ALICE), `lobbies/${M}/kicked/${MALLORY}`), true));
    await assertSucceeds(update(ref(as(HOST), `lobbies/${M}`), { [`kicked/${ALICE}`]: true, [`members/${ALICE}`]: null }));
    await assertFails(set(ref(as(ALICE), `lobbies/${M}/members/${ALICE}`), true));
  });

  it('shares presence with members only', async () => {
    await lobby(ALICE);
    await assertSucceeds(set(ref(as(ALICE), `presence/${M}/${ALICE}`), { online: true, at: serverTimestamp() }));
    await assertFails(set(ref(as(ALICE), `presence/${M}/${ALICE}`), { online: true, at: 5 }));
    await assertFails(set(ref(as(ALICE), `presence/${M}/${HOST}`), { online: true, at: serverTimestamp() }));
    await assertFails(set(ref(as(MALLORY), `presence/${M}/${MALLORY}`), { online: true, at: serverTimestamp() }));
    await assertSucceeds(get(ref(as(HOST), `presence/${M}`)));
    await assertFails(get(ref(as(MALLORY), `presence/${M}`)));
  });

  it('accepts only fuzzed zones: a radius of at least 25 m and no extra fields', async () => {
    await lobby(ALICE);
    await assertFails(set(ref(as(ALICE), `public/${M}/${ALICE}`), zone({ radiusM: 3 })));
    await assertFails(set(ref(as(ALICE), `public/${M}/${ALICE}`), zone({ exactLat: 12.97001 })));
    await assertFails(set(ref(as(ALICE), `public/${M}/${ALICE}`), zone({ status: 'teleporting' })));
    await assertFails(set(ref(as(ALICE), `public/${M}/${ALICE}`), zone({ lat: 120 })));
    await assertSucceeds(set(ref(as(ALICE), `public/${M}/${ALICE}`), zone()));
    await assertFails(set(ref(as(ALICE), `public/${M}/${HOST}`), zone()));
    await assertFails(set(ref(as(MALLORY), `public/${M}/${MALLORY}`), zone()));
    await assertFails(get(ref(as(MALLORY), `public/${M}`)));
  });

  it('throttles zone updates to one per two seconds', async () => {
    await lobby(ALICE);
    await assertSucceeds(set(ref(as(ALICE), `public/${M}/${ALICE}`), zone()));
    await assertFails(set(ref(as(ALICE), `public/${M}/${ALICE}`), zone({ lat: 12.971 })));
  });

  it('lets the host clear the whole match', async () => {
    await lobby(ALICE);
    await assertSucceeds(set(ref(as(ALICE), `public/${M}/${ALICE}`), zone()));
    await assertFails(remove(ref(as(ALICE), `lobbies/${M}`)));
    await assertFails(remove(ref(as(ALICE), `public/${M}`)));
    await assertSucceeds(remove(ref(as(HOST), `public/${M}`)));
    await assertSucceeds(remove(ref(as(HOST), `presence/${M}`)));
    await assertSucceeds(remove(ref(as(HOST), `lobbies/${M}`)));
  });
});
