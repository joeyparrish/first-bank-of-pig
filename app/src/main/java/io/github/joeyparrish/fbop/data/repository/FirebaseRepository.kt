package io.github.joeyparrish.fbop.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.github.joeyparrish.fbop.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    // =========================================================================
    // Authentication
    // =========================================================================

    suspend fun signInAnonymously(): Result<FirebaseUser> = runCatching {
        auth.signInAnonymously().await().user
            ?: throw Exception("Anonymous sign-in returned null user")
    }

    fun signOut() {
        auth.signOut()
    }

    // =========================================================================
    // Family Operations
    // =========================================================================

    suspend fun createFamily(name: String): Result<Family> = runCatching {
        val user = currentUser ?: throw Exception("Not signed in")

        val familyRef = db.collection("families").document()
        val family = Family(
            id = familyRef.id,
            name = name,
            ownerUid = user.uid
        )

        val parent = Parent(
            uid = user.uid,
            email = user.email ?: ""
        )

        // Create family and add current user as parent in a batch
        db.runBatch { batch ->
            batch.set(familyRef, family)
            batch.set(familyRef.collection("parents").document(user.uid), parent)
        }.await()

        family.copy(id = familyRef.id)
    }

    suspend fun getFamily(familyId: String): Result<Family> = runCatching {
        db.collection("families")
            .document(familyId)
            .get()
            .await()
            .toObject(Family::class.java)
            ?: throw Exception("Family not found")
    }

    fun observeFamily(familyId: String): Flow<Family?> = callbackFlow {
        val listener = db.collection("families")
            .document(familyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Family::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateFamilyName(familyId: String, name: String): Result<Unit> = runCatching {
        db.collection("families")
            .document(familyId)
            .update("name", name)
            .await()
    }

    // =========================================================================
    // Parent Operations
    // =========================================================================

    fun observeParents(familyId: String): Flow<List<Parent>> = callbackFlow {
        val listener = db.collection("families")
            .document(familyId)
            .collection("parents")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val parents = snapshot?.documents?.mapNotNull {
                    it.toObject(Parent::class.java)
                } ?: emptyList()
                trySend(parents)
            }
        awaitClose { listener.remove() }
    }

    suspend fun removeParent(familyId: String, parentUid: String): Result<Unit> = runCatching {
        db.collection("families")
            .document(familyId)
            .collection("parents")
            .document(parentUid)
            .delete()
            .await()
    }

    // =========================================================================
    // Invite Operations
    // =========================================================================

    suspend fun createInvite(familyId: String): Result<Invite> = runCatching {
        val user = currentUser ?: throw Exception("Not signed in")

        // Generate a short, readable code
        val code = generateShortCode()
        val expiresAt = Timestamp(
            java.util.Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000) // 24 hours
        )

        val invite = Invite(
            code = code,
            familyId = familyId,
            createdBy = user.uid,
            expiresAt = expiresAt
        )

        // Create in both locations for lookup
        db.runBatch { batch ->
            val inviteRef = db.collection("families")
                .document(familyId)
                .collection("invites")
                .document()

            batch.set(inviteRef, invite)
            batch.set(
                db.collection("inviteCodes").document(code),
                mapOf(
                    "familyId" to familyId,
                    "expiresAt" to expiresAt
                )
            )
        }.await()

        invite
    }

    suspend fun lookupInviteCode(code: String): Result<String> = runCatching {
        val doc = db.collection("inviteCodes")
            .document(code.uppercase())
            .get()
            .await()

        if (!doc.exists()) {
            throw Exception("Invalid invite code")
        }

        val expiresAt = doc.getTimestamp("expiresAt")
        if (expiresAt != null && expiresAt.toDate().before(java.util.Date())) {
            throw Exception("Invite code has expired")
        }

        doc.getString("familyId") ?: throw Exception("Invalid invite code")
    }

    suspend fun joinFamily(familyId: String, inviteCode: String): Result<Unit> = runCatching {
        val user = currentUser ?: throw Exception("Not signed in")

        val parent = Parent(
            uid = user.uid,
            email = user.email ?: ""
        )

        // Add parent and delete the invite code
        db.runBatch { batch ->
            batch.set(
                db.collection("families")
                    .document(familyId)
                    .collection("parents")
                    .document(user.uid),
                parent
            )
            batch.delete(db.collection("inviteCodes").document(inviteCode.uppercase()))
        }.await()
    }

    private fun generateShortCode(): String {
        val chars = "ABCDEFGHJKMNPQRSTWXYZ23456789" // Excluding confusing characters
        return (1..8).map { chars.random() }.joinToString("")
    }

    // =========================================================================
    // Child Operations
    // =========================================================================

    suspend fun createChild(familyId: String, name: String): Result<Child> = runCatching {
        val childRef = db.collection("families")
            .document(familyId)
            .collection("children")
            .document()

        val child = Child(id = childRef.id, name = name)
        childRef.set(child).await()

        // Create lookup code for kid devices
        val lookupCode = generateShortCode()
        db.collection("childLookup")
            .document(lookupCode)
            .set(
                mapOf(
                    "familyId" to familyId,
                    "childId" to childRef.id
                )
            ).await()

        child.copy(id = childRef.id)
    }

    suspend fun updateChild(familyId: String, childId: String, name: String): Result<Unit> = runCatching {
        db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .update("name", name)
            .await()
    }

    suspend fun deleteChild(familyId: String, childId: String): Result<Unit> = runCatching {
        // Note: This doesn't delete transactions - consider a Cloud Function for cleanup
        db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .delete()
            .await()
    }

    fun observeChildren(familyId: String): Flow<List<Child>> = callbackFlow {
        val listener = db.collection("families")
            .document(familyId)
            .collection("children")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val children = snapshot?.documents?.mapNotNull {
                    it.toObject(Child::class.java)
                } ?: emptyList()
                trySend(children)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getChild(familyId: String, childId: String): Result<Child> = runCatching {
        db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .get()
            .await()
            .toObject(Child::class.java)
            ?: throw Exception("Child not found")
    }

    suspend fun getChildLookupCode(familyId: String, childId: String): Result<String> = runCatching {
        val snapshot = db.collection("childLookup")
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("childId", childId)
            .limit(1)
            .get()
            .await()

        if (snapshot.isEmpty) {
            // Create one if it doesn't exist
            val lookupCode = generateShortCode()
            db.collection("childLookup")
                .document(lookupCode)
                .set(
                    mapOf(
                        "familyId" to familyId,
                        "childId" to childId
                    )
                ).await()
            lookupCode
        } else {
            snapshot.documents.first().id
        }
    }

    suspend fun lookupChild(lookupCode: String): Result<ChildLookup> = runCatching {
        val doc = db.collection("childLookup")
            .document(lookupCode)
            .get()
            .await()

        if (!doc.exists()) {
            throw Exception("Invalid code")
        }

        ChildLookup(
            lookupCode = lookupCode,
            familyId = doc.getString("familyId") ?: throw Exception("Invalid code"),
            childId = doc.getString("childId") ?: throw Exception("Invalid code")
        )
    }

    // =========================================================================
    // Transaction Operations
    // =========================================================================

    suspend fun createTransaction(
        familyId: String,
        childId: String,
        amountCents: Long,
        description: String,
        date: Timestamp
    ): Result<Transaction> = runCatching {
        val txRef = db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("transactions")
            .document()

        val transaction = Transaction(
            id = txRef.id,
            amount = amountCents,
            description = description,
            date = date
        )

        txRef.set(transaction).await()
        transaction.copy(id = txRef.id)
    }

    suspend fun updateTransaction(
        familyId: String,
        childId: String,
        transactionId: String,
        amountCents: Long,
        description: String,
        date: Timestamp
    ): Result<Unit> = runCatching {
        db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("transactions")
            .document(transactionId)
            .update(
                mapOf(
                    "amount" to amountCents,
                    "description" to description,
                    "date" to date,
                    "modifiedAt" to Timestamp.now()
                )
            ).await()
    }

    suspend fun deleteTransaction(
        familyId: String,
        childId: String,
        transactionId: String
    ): Result<Unit> = runCatching {
        db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("transactions")
            .document(transactionId)
            .delete()
            .await()
    }

    fun observeTransactions(familyId: String, childId: String): Flow<List<Transaction>> = callbackFlow {
        val listener = db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents?.mapNotNull {
                    it.toObject(Transaction::class.java)
                } ?: emptyList()
                trySend(transactions)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getTransactions(familyId: String, childId: String): Result<List<Transaction>> = runCatching {
        db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Transaction::class.java) }
    }

    // =========================================================================
    // Device Access Operations
    // =========================================================================

    /**
     * Register this device for access to a child's data.
     * The device UID (anonymous auth) is used as the document ID.
     */
    suspend fun registerDevice(
        familyId: String,
        childId: String,
        deviceName: String
    ): Result<Unit> = runCatching {
        val user = currentUser ?: throw Exception("Not signed in")

        db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("devices")
            .document(user.uid)
            .set(
                mapOf(
                    "deviceName" to deviceName,
                    "registeredAt" to FieldValue.serverTimestamp(),
                    "lastAccessedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    /**
     * Check if this device has access to a child's data.
     * Returns true if the device is registered, false otherwise.
     */
    suspend fun checkDeviceAccess(familyId: String, childId: String): Result<Boolean> = runCatching {
        val user = currentUser ?: return@runCatching false

        val doc = db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("devices")
            .document(user.uid)
            .get()
            .await()

        doc.exists()
    }

    /**
     * Update the lastAccessedAt timestamp for this device.
     * Uses server timestamp to prevent client manipulation.
     */
    suspend fun updateLastAccessed(familyId: String, childId: String): Result<Unit> = runCatching {
        val user = currentUser ?: throw Exception("Not signed in")

        db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("devices")
            .document(user.uid)
            .update("lastAccessedAt", FieldValue.serverTimestamp())
            .await()
    }

    /**
     * Get all registered devices for a child (for parent management).
     */
    suspend fun getDevicesForChild(familyId: String, childId: String): Result<List<DeviceAccess>> = runCatching {
        db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("devices")
            .orderBy("registeredAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(DeviceAccess::class.java) }
    }

    /**
     * Observe registered devices for a child (for real-time updates in parent UI).
     */
    fun observeDevicesForChild(familyId: String, childId: String): Flow<List<DeviceAccess>> = callbackFlow {
        val listener = db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("devices")
            .orderBy("registeredAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val devices = snapshot?.documents?.mapNotNull {
                    it.toObject(DeviceAccess::class.java)
                } ?: emptyList()
                trySend(devices)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Delete a device's access (revoke access from parent UI).
     */
    suspend fun deleteDeviceAccess(
        familyId: String,
        childId: String,
        deviceUid: String
    ): Result<Unit> = runCatching {
        db.collection("families")
            .document(familyId)
            .collection("children")
            .document(childId)
            .collection("devices")
            .document(deviceUid)
            .delete()
            .await()
    }
}
