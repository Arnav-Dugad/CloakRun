import { assertFails } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { ref, get, set } from 'firebase/database';
import { createEnv } from './setup.js';

describe('Phase 0 baseline: deny by default', () => {
  let env;

  before(async () => {
    env = await createEnv();
  });

  after(async () => {
    await env.cleanup();
  });

  it('denies unauthenticated Firestore reads and writes', async () => {
    const db = env.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, 'matches/any')));
    await assertFails(setDoc(doc(db, 'matches/any'), { x: 1 }));
  });

  it('denies authenticated Firestore reads and writes', async () => {
    const db = env.authenticatedContext('alice').firestore();
    await assertFails(getDoc(doc(db, 'users/alice')));
    await assertFails(setDoc(doc(db, 'users/alice'), { name: 'Alice' }));
  });

  it('denies Realtime Database reads and writes', async () => {
    const db = env.authenticatedContext('alice').database();
    await assertFails(get(ref(db, 'public/m1/alice')));
    await assertFails(set(ref(db, 'public/m1/alice'), { lat: 0 }));
  });
});
