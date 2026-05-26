package com.vichitra.casho.data

// ── Monthly spend totals (current month) ─────────────────────────────────────
data class MonthlySpendData(
    val lifestyle : Double = 0.0,
    val housing   : Double = 0.0,
    val dining    : Double = 0.0,
    val other     : Double = 0.0
)

// ── One bar in the Total Savings chart ───────────────────────────────────────
// savings = carryover from previous month + credits this month - debits this month
data class MonthlySavingsPoint(
    val month   : String,   // "Jan", "Feb", …
    val year    : Int,      // 2025
    val savings : Double    // ≥ 0
) {
    val label get() = "$month '${year.toString().takeLast(2)}"
}