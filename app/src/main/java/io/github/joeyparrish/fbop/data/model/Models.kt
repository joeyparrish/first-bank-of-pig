// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a family unit in First Bank of Pig.
 * A family has one owner, potentially multiple parents, and multiple children.
 */
data class Family(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val ownerUid: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

/**
 * Represents a parent who has access to the family.
 * Stored as a subcollection under the family document.
 */
data class Parent(
    @DocumentId
    val uid: String = "",
    val email: String = "",
    val inviteCode: String? = null, // Required for joining parents, validated server-side
    @ServerTimestamp
    val joinedAt: Timestamp? = null
)

/**
 * Represents a child's account in the family.
 * Balance is computed from transactions, not stored.
 */
data class Child(
    @DocumentId
    val id: String = "",
    val name: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    companion object {
        fun create(name: String): Child = Child(name = name)
    }
}

/**
 * Represents a single transaction (deposit or withdrawal).
 * Amount is stored in cents to avoid floating-point issues.
 * Positive = deposit, Negative = withdrawal.
 */
data class Transaction(
    @DocumentId
    val id: String = "",
    val amount: Long = 0, // In cents, positive or negative
    val description: String = "",
    val date: Timestamp = Timestamp.now(), // User-specified date
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val modifiedAt: Timestamp? = null
) {
    companion object {
        fun create(
            amountCents: Long,
            description: String,
            date: Timestamp = Timestamp.now()
        ): Transaction = Transaction(
            amount = amountCents,
            description = description,
            date = date
        )
    }

    val isDeposit: Boolean get() = amount >= 0
}

/**
 * Represents an invite code for adding a new parent to the family.
 * Stored in both /families/{id}/invites and /inviteCodes/{code} for lookup.
 */
data class Invite(
    @DocumentId
    val id: String = "",
    val code: String = "",
    val familyId: String = "",
    val createdBy: String = "",
    val expiresAt: Timestamp = Timestamp.now()
)

/**
 * Lookup record for kid devices to find their data.
 * The lookupCode is encoded in the QR code shown to kids.
 */
data class ChildLookup(
    @DocumentId
    val lookupCode: String = "",
    val familyId: String = "",
    val childId: String = ""
)

/**
 * Represents a registered kid device with access to a child's data.
 * The document ID is the device's anonymous auth UID.
 */
data class DeviceAccess(
    @DocumentId
    val uid: String = "",
    val deviceName: String = "",
    val lookupCode: String = "", // Validated server-side against childLookup collection
    val registeredAt: Timestamp? = null,
    val lastAccessedAt: Timestamp? = null
)

/**
 * Local app configuration stored in SharedPreferences.
 * Determines how the app behaves on this device.
 */
data class AppConfig(
    val mode: AppMode = AppMode.NOT_CONFIGURED,
    val familyId: String? = null,
    val childId: String? = null, // Only set for kid mode
    val lookupCode: String? = null // Only set for kid mode
)

enum class AppMode {
    NOT_CONFIGURED,
    PARENT,
    KID
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Child with computed balance, used for display.
 */
data class ChildWithBalance(
    val child: Child,
    val balanceCents: Long,
    val transactions: List<Transaction>
) {
    val formattedBalance: String
        get() = formatCurrency(balanceCents)
}

/**
 * Format cents as a currency string (USD).
 */
fun formatCurrency(cents: Long): String {
    val dollars = cents / 100
    val remainingCents = kotlin.math.abs(cents % 100)
    val sign = if (cents < 0) "-" else ""
    return "$sign\$${kotlin.math.abs(dollars)}.${remainingCents.toString().padStart(2, '0')}"
}

/**
 * Parse a dollar amount string into cents.
 * Handles formats like "12.34", "12", ".50", etc.
 */
fun parseCurrency(input: String): Long? {
    val cleaned = input.trim().removePrefix("$").removePrefix("-")
    val isNegative = input.trim().startsWith("-")

    return try {
        val parts = cleaned.split(".")
        val dollars = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val cents = when {
            parts.size < 2 -> 0L
            parts[1].isEmpty() -> 0L
            parts[1].length == 1 -> parts[1].toLong() * 10
            else -> parts[1].take(2).toLong()
        }
        val total = dollars * 100 + cents
        if (isNegative) -total else total
    } catch (e: NumberFormatException) {
        null
    }
}

/**
 * Convert UTC millis from DatePicker to a local Date.
 * DatePicker returns midnight UTC, but we want to preserve the calendar date
 * in the local timezone.
 */
fun datePickerMillisToLocalDate(utcMillis: Long): java.util.Date {
    // Parse the UTC millis as a calendar date in UTC
    val utcCalendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    utcCalendar.timeInMillis = utcMillis

    // Create a new date at noon local time for the same calendar date
    // (noon avoids edge cases with DST transitions)
    val localCalendar = java.util.Calendar.getInstance()
    localCalendar.set(
        utcCalendar.get(java.util.Calendar.YEAR),
        utcCalendar.get(java.util.Calendar.MONTH),
        utcCalendar.get(java.util.Calendar.DAY_OF_MONTH),
        12, 0, 0
    )
    localCalendar.set(java.util.Calendar.MILLISECOND, 0)

    return localCalendar.time
}

/**
 * Convert a local Date to UTC millis for DatePicker.
 */
fun localDateToDatePickerMillis(date: java.util.Date): Long {
    val localCalendar = java.util.Calendar.getInstance()
    localCalendar.time = date

    // Create UTC midnight for the same calendar date
    val utcCalendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    utcCalendar.set(
        localCalendar.get(java.util.Calendar.YEAR),
        localCalendar.get(java.util.Calendar.MONTH),
        localCalendar.get(java.util.Calendar.DAY_OF_MONTH),
        0, 0, 0
    )
    utcCalendar.set(java.util.Calendar.MILLISECOND, 0)

    return utcCalendar.timeInMillis
}
