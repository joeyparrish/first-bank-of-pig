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
const { cleanupExpiredCodesImpl } = require("./index");

initializeApp({
  credential: applicationDefault(),
  projectId: "first-bank-of-pig",
});

cleanupExpiredCodesImpl(getFirestore())
  .then(() => process.exit(0))
  .catch((e) => { console.error(e); process.exit(1); });
