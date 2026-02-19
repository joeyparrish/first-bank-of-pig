const { onSchedule } = require("firebase-functions/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

/**
 * Scheduled function to clean up expired codes from Firestore.
 * Runs weekly. Deletes expired invite codes and child lookup codes.
 *
 * The Admin SDK bypasses security rules, so this function can
 * query and delete documents regardless of client-side restrictions.
 */
exports.cleanupExpiredCodes = onSchedule("every sunday 03:00", async (event) => {
  const db = getFirestore();
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

  const batch = db.batch();
  let count = 0;

  expiredInvites.forEach((doc) => {
    batch.delete(doc.ref);
    count++;
  });

  expiredLookups.forEach((doc) => {
    batch.delete(doc.ref);
    count++;
  });

  if (count > 0) {
    await batch.commit();
    console.log(`Deleted ${count} expired code(s)`);
  } else {
    console.log("No expired codes to clean up");
  }
});
