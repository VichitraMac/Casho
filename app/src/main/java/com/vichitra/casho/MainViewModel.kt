package com.vichitra.casho

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vichitra.casho.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map           // ← ADD THIS
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat             // ← ADD THIS
import java.util.Calendar                     // ← ADD THIS
import java.util.Date                         // ← ADD THIS
import java.util.Locale                       // ← ADD THIS
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CashoRepository

    val allTasks: StateFlow<List<TaskEntity>>
    val allTransactions: StateFlow<List<TransactionEntity>>
    val allSplitGroups: StateFlow<List<SplitGroupEntity>>

    private val _totalAvailable = MutableStateFlow(0.0)
    val totalAvailable: StateFlow<Double> = _totalAvailable.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = CashoRepository(db)

        allTasks = repository.allTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allTransactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allSplitGroups = repository.allSplitGroups
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            _totalAvailable.value = repository.getTotalAvailable()
        }
    }

    // ── Balance ───────────────────────────────────────────────────────────────
    fun setTotalAvailable(amount: Double) {
        viewModelScope.launch {
            _totalAvailable.value = amount
            repository.setTotalAvailable(amount)
        }
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────
    fun addTask(name: String, amount: Double, imageUri: String = "") {
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    id       = UUID.randomUUID().toString(),
                    name     = name,
                    amount   = amount,
                    imageUri = imageUri
                )
            )
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    // ── Transactions ──────────────────────────────────────────────────────────
    fun addTransaction(
        amount: Double,
        type: String,
        description: String,
        updateBalance: Boolean = true
    ) {
        viewModelScope.launch {
            val current  = _totalAvailable.value
            val newTotal = when (type) {
                "CREDIT" -> current + amount
                "DEBIT"  -> if (current > 0.0) (current - amount).coerceAtLeast(0.0) else current
                else     -> current
            }
            if (newTotal != current) setTotalAvailable(newTotal)

            repository.insertTransaction(
                TransactionEntity(
                    amount      = amount,
                    type        = type,
                    timestamp   = System.currentTimeMillis(),
                    description = description
                )
            )
        }
    }

    fun updateTotalAvailableWithLog(newAmount: Double) {
        viewModelScope.launch {
            val current = _totalAvailable.value
            val diff    = newAmount - current
            setTotalAvailable(newAmount)
            if (diff > 0.0) {
                repository.insertTransaction(
                    TransactionEntity(
                        amount      = diff,
                        type        = "CREDIT",
                        timestamp   = System.currentTimeMillis(),
                        description = "Balance Top-up|Income"
                    )
                )
            }
        }
    }

    fun handleSms(amount: Double, isCredit: Boolean, body: String) {
        addTransaction(amount, if (isCredit) "CREDIT" else "DEBIT", body)
    }

    // ── Split groups ──────────────────────────────────────────────────────────
    fun createSplitGroup(name: String, members: List<Pair<String, String>>): String {
        val groupId = UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.insertSplitGroup(
                SplitGroupEntity(groupId, name, System.currentTimeMillis())
            )
            members.forEach { (memberName, imageUri) ->
                repository.insertSplitMember(
                    SplitMemberEntity(
                        id       = UUID.randomUUID().toString(),
                        groupId  = groupId,
                        name     = memberName,
                        imageUri = imageUri
                    )
                )
            }
        }
        return groupId
    }

    suspend fun getMembersOfGroup(groupId: String): List<SplitMemberEntity> =
        repository.getMembersOfGroup(groupId)

    fun deleteSplitGroup(group: SplitGroupEntity) {
        viewModelScope.launch { repository.deleteSplitGroup(group) }
    }

    // ── Monthly Spend (current month, DEBIT only, by ExpenseType) ─────────────
    val monthlySpend: StateFlow<MonthlySpendData> = allTransactions  // ← was: transactions
        .map { txns -> computeMonthlySpend(txns) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlySpendData()
        )

    // ── Monthly Savings chart (last 12 months, cumulative) ────────────────────
    val monthlySavings: StateFlow<List<MonthlySavingsPoint>> = allTransactions  // ← was: transactions
        .map { txns -> computeMonthlySavings(txns) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ── Private: compute current-month spend per category ────────────────────
    private fun computeMonthlySpend(
        transactions: List<TransactionEntity>
    ): MonthlySpendData {
        val cal          = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear  = cal.get(Calendar.YEAR)

        val debits = transactions.filter { txn ->
            txn.type == "DEBIT" && Calendar.getInstance().run {
                timeInMillis = txn.timestamp                          // ← was: txn.date
                get(Calendar.MONTH) == currentMonth &&
                        get(Calendar.YEAR)  == currentYear
            }
        }

        fun sumFor(type: ExpenseType) =
            debits
                .filter { ExpenseType.fromNote(it.description) == type }  // ← was: it.note
                .sumOf { it.amount }

        return MonthlySpendData(
            lifestyle = sumFor(ExpenseType.LIFESTYLE),
            housing   = sumFor(ExpenseType.HOUSING),
            dining    = sumFor(ExpenseType.DINING),
            other     = sumFor(ExpenseType.OTHER)
        )
    }

    // ── Private: compute cumulative monthly savings for chart ─────────────────
    // Jan: added ₹10K, spent ₹5K  → carryover = ₹5K  (bar shows ₹5K)
    // Feb: carryover ₹5K + added ₹10K − spent ₹8K = ₹7K  (bar shows ₹7K)
    private fun computeMonthlySavings(
        transactions: List<TransactionEntity>
    ): List<MonthlySavingsPoint> {
        if (transactions.isEmpty()) return emptyList()

        val keyFmt   = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthMap = mutableMapOf<String, Pair<Double, Double>>() // "yyyy-MM" → (credits, debits)

        transactions.forEach { txn ->
            val key       = keyFmt.format(Date(txn.timestamp))       // ← was: txn.date
            val (c, d)    = monthMap[key] ?: (0.0 to 0.0)
            monthMap[key] = if (txn.type == "CREDIT")
                (c + txn.amount) to d
            else
                c to (d + txn.amount)
        }

        val monthNameFmt = SimpleDateFormat("MMM", Locale.getDefault())
        val yearFmt      = SimpleDateFormat("yyyy", Locale.getDefault())
        var carryover    = 0.0

        return monthMap.keys
            .sorted()
            .map { key ->
                val (credits, debits) = monthMap[key]!!
                carryover = (carryover + credits - debits).coerceAtLeast(0.0)
                val date  = keyFmt.parse(key)!!
                MonthlySavingsPoint(
                    month   = monthNameFmt.format(date),
                    year    = yearFmt.format(date).toInt(),
                    savings = carryover
                )
            }
            .takeLast(12)
    }
}