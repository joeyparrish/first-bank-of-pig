const { onSchedule } = require("firebase-functions/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

/**
 * Core cleanup logic. Deletes expired codes from all three collections.
 * Accepts a Firestore instance so callers can supply their own credentials.
 *
 * The Admin SDK bypasses security rules, so this function can
 * query and delete documents regardless of client-side restrictions.
 */
async function cleanupExpiredCodesImpl(db) {
  const now = new Date();

  // Clean up expired invite codes
  const expiredInvites = await db
    .collection("inviteCodes")
    .where("expiresAt", "<", now)
    .limit(500)
    .get();

  // Clean up expired child lookup codes
  const expiredLookups = await db
    .collection("childLookup")
    .where("expiresAt", "<", now)
    .limit(500)
    .get();

  // Clean up expired family invites subcollection (collection group)
  const expiredFamilyInvites = await db
    .collectionGroup("invites")
    .where("expiresAt", "<", now)
    .limit(500)
    .get();

  // Use separate batches per collection to stay under the 500-op batch limit
  const batch1 = db.batch();
  let count1 = 0;
  expiredInvites.forEach((doc) => { batch1.delete(doc.ref); count1++; });

  const batch2 = db.batch();
  let count2 = 0;
  expiredLookups.forEach((doc) => { batch2.delete(doc.ref); count2++; });

  const batch3 = db.batch();
  let count3 = 0;
  expiredFamilyInvites.forEach((doc) => { batch3.delete(doc.ref); count3++; });

  const count = count1 + count2 + count3;
  if (count1 > 0) await batch1.commit();
  if (count2 > 0) await batch2.commit();
  if (count3 > 0) await batch3.commit();

  if (count > 0) {
    console.log(`Deleted ${count} expired code(s)`);
  } else {
    console.log("No expired codes to clean up");
  }
}

exports.cleanupExpiredCodesImpl = cleanupExpiredCodesImpl;

/**
 * Scheduled function to clean up expired codes from Firestore.
 * Runs weekly.
 */
exports.cleanupExpiredCodes = onSchedule("every sunday 03:00", async (event) => {
  await cleanupExpiredCodesImpl(getFirestore());
});
