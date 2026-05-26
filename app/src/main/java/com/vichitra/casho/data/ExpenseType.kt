package com.vichitra.casho.data

// ── Expense category enum ─────────────────────────────────────────────────────
// Used in PaymentDetailsDialog, AddGoalDialog, MonthlySpendCard, TransactionItem
// Stored in transaction note as: "$name|$channel|${type.name}"
// e.g. "Zomato|QR Payment|DINING"

enum class ExpenseType(
    val label : String,
    val emoji : String
) {
    LIFESTYLE("Lifestyle", "🛍️"),
    HOUSING  ("Housing",   "🏠"),
    DINING   ("Dining",    "🍽️"),
    OTHER    ("Other",     "💼");

    companion object {
        fun fromName(name: String?) =
            values().find { it.name == name } ?: OTHER

        // Parse from transaction note string: "name|channel|TYPE"
        fun fromNote(note: String): ExpenseType {
            val parts = note.split("|")
            return fromName(parts.getOrNull(2))
        }
    }
}