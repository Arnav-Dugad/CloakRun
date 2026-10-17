import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';

const here = dirname(fileURLToPath(import.meta.url));
const rulesDir = join(here, '..', '..');

export const PROJECT_ID = 'demo-cloakrun';

/** Creates a test environment bound to the emulators started by `firebase emulators:exec`. */
export async function createEnv() {
  return initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(join(rulesDir, 'firestore.rules'), 'utf8'),
    },
    database: {
      rules: readFileSync(join(rulesDir, 'database.rules.json'), 'utf8'),
    },
  });
}
