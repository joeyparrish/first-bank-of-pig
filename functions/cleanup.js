/**
 * Run expired code cleanup locally against the production database.
 *
 * Prerequisites:
 *   - npm install (in this directory)
 *   - gcloud auth application-default login
 *
 * Usage:
 *   node cleanup.js
 */

const { initializeApp, applicationDefault } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp({
  credential: applicationDefault(),
  projectId: "first-bank-of-pig",
});
const db = getFirestore();

async function cleanup() {
  const now = new Date();
  let count = 0;

  const expiredInvites = await db
    .collection("inviteCodes")
    .where("expiresAt", "<", now)
    .limit(500)
    .get();
  const expiredLookups = await db
    .collection("childLookup")
    .where("expiresAt", "<", now)
    .limit(500)
    .get();

  const batch = db.batch();
  expiredInvites.forEach((doc) => { batch.delete(doc.ref); count++; });
  expiredLookups.forEach((doc) => { batch.delete(doc.ref); count++; });

  if (count > 0) {
    await batch.commit();
    console.log(`Deleted ${count} expired code(s)`);
  } else {
    console.log("No expired codes to clean up");
  }
}

cleanup()
  .then(() => process.exit(0))
  .catch((e) => { console.error(e); process.exit(1); });
