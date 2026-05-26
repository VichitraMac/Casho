package com.vichitra.casho.ui.theme.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vichitra.casho.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

// ── Colours ───────────────────────────────────────────────────────────────────
private val HBlue       = Color(0xFF1565C0)
private val HBlueLight  = Color(0xFFDEEAFB)
private val HGreen      = Color(0xFF43A047)
private val HGreenLight = Color(0xFFE8F5E9)
private val HRed        = Color(0xFFE53935)
private val HRedLight   = Color(0xFFFFEBEE)
private val HGray       = Color(0xFF9E9E9E)
private val HTextDark   = Color(0xFF1A1A2E)
private val HPageBg     = Color(0xFFF0F3FA)
private val HCardBg     = Color.White

// ── Filter tabs ───────────────────────────────────────────────────────────────
private enum class TxFilter { ALL, EXPENSES, INCOME }

// ── Date bucket helpers ───────────────────────────────────────────────────────
private fun dateBucket(timestamp: Long): String {
    val cal   = Calendar.getInstance()
    val today = cal.clone() as Calendar
    cal.timeInMillis = timestamp

    val todayCal     = today
    val yesterdayCal = (today.clone() as Calendar).also { it.add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        isSameDay(cal, todayCal)     -> "TODAY"
        isSameDay(cal, yesterdayCal) -> "YESTERDAY"
        else                         -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            .format(Date(timestamp)).uppercase()
    }
}

private fun isSameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun bucketDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun timeLabel(timestamp: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun HistoryScreen(
    modifier     : Modifier = Modifier,
    transactions : List<TransactionEntity>
) {
    var filter      by remember { mutableStateOf(TxFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    // Apply filter + search
    val filtered = remember(transactions, filter, searchQuery) {
        transactions
            .filter { tx ->
                when (filter) {
                    TxFilter.ALL      -> true
                    TxFilter.EXPENSES -> tx.type == "DEBIT"
                    TxFilter.INCOME   -> tx.type == "CREDIT"
                }
            }
            .filter { tx ->
                searchQuery.isBlank() ||
                        tx.description.contains(searchQuery, ignoreCase = true)
            }
            // ✅ Hide split-children rows — they appear nested under their parent
            .filter { tx ->
                val cat = tx.description.split("|").getOrNull(1)?.trim() ?: ""
                !cat.startsWith("Split - ")
            }
    }

    // ✅ Map each split-parent transaction to its children (matched by parent name)
    val splitChildrenMap = remember(transactions) {
        transactions
            .filter { it.description.split("|").getOrNull(1)?.trim()?.startsWith("Split - ") == true }
            .groupBy { it.description.split("|").getOrNull(1)?.trim()?.removePrefix("Split - ") ?: "" }
    }

    // Group by date bucket, preserving order
    val grouped = remember(filtered) {
        filtered.groupBy { dateBucket(it.timestamp) }
            .entries.toList()
    }

    val totalExpenses = remember(transactions) {
        transactions.filter { it.type == "DEBIT" }.sumOf { it.amount }
    }
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "CREDIT" }.sumOf { it.amount }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HPageBg)
    ) {
        LazyColumn(
            modifier       = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start  = 20.dp,
                end    = 20.dp,
                top    = 12.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Title ─────────────────────────────────────────────────────────
            item {
                Text(
                    "Transactions",
                    color      = HTextDark,
                    fontWeight = FontWeight.Black,
                    fontSize   = 30.sp,
                    modifier   = Modifier.padding(bottom = 20.dp)
                )
            }

            // ── Filter tabs ───────────────────────────────────────────────────
            item {
                FilterTabRow(
                    selected  = filter,
                    onSelect  = { filter = it },
                    modifier  = Modifier.padding(bottom = 20.dp)
                )
            }

            // ── Search bar ────────────────────────────────────────────────────
            item {
                SearchBar(
                    query     = searchQuery,
                    onChange  = { searchQuery = it },
                    modifier  = Modifier.padding(bottom = 24.dp)
                )
            }

            // ── Grouped transactions ──────────────────────────────────────────
            if (grouped.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.ReceiptLong, null,
                                tint = HGray.copy(alpha = 0.4f), modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No transactions found", color = HGray, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                grouped.forEach { (bucket, txList) ->
                    // Date group header
                    item(key = "header_$bucket") {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(bucket,  color = HTextDark, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                letterSpacing = 0.5.sp)
                            Text(bucketDate(txList.first().timestamp),
                                color = HGray, fontSize = 12.sp)
                        }
                    }
                    // Transaction rows
                    items(txList, key = { it.id }) { tx ->
                        val parentName = tx.description.split("|").getOrNull(0)?.trim() ?: ""
                        val children   = splitChildrenMap[parentName] ?: emptyList()
                        TransactionRow(tx = tx, splitChildren = children)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        // ── Summary cards ─────────────────────────────────────────────────────
        SummaryBar(totalExpenses = totalExpenses, totalIncome = totalIncome)
    }
}

// ── Filter tab row ────────────────────────────────────────────────────────────
@Composable
private fun FilterTabRow(
    selected : TxFilter,
    onSelect : (TxFilter) -> Unit,
    modifier : Modifier = Modifier
) {
    Surface(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(50),
        color     = HCardBg,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TxFilter.entries.forEach { tab ->
                val isSelected = selected == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) HBlue else Color.Transparent)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        color      = if (isSelected) Color.White else HGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 14.sp
                    )
                }
            }
        }
    }
}

// ── Search bar ────────────────────────────────────────────────────────────────
@Composable
private fun SearchBar(
    query    : String,
    onChange : (String) -> Unit,
    modifier : Modifier = Modifier
) {
    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier  = Modifier.weight(1f),
            shape     = RoundedCornerShape(50),
            color     = HCardBg,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = HGray, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                TextField(
                    value         = query,
                    onValueChange = onChange,
                    placeholder   = { Text("Search transactions...", color = HGray, fontSize = 14.sp) },
                    singleLine    = true,
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor        = HTextDark,
                        unfocusedTextColor      = HTextDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Filter icon button
        Surface(
            shape          = CircleShape,
            color          = HCardBg,
            tonalElevation = 1.dp,
            modifier       = Modifier.size(48.dp)
        ) {
            Box(Modifier.fillMaxSize().clickable {}, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Tune, null, tint = HBlue, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ── Transaction row ───────────────────────────────────────────────────────────
@Composable
private fun TransactionRow(
    tx           : TransactionEntity,
    splitChildren: List<TransactionEntity> = emptyList()   // shares of this split parent
) {
    val isCredit   = tx.type == "CREDIT"
    val amtColor   = if (isCredit) HGreen else HTextDark
    val iconBg     = if (isCredit) HGreenLight else HBlueLight
    val iconTint   = if (isCredit) HGreen else HBlue
    val amtPrefix  = if (isCredit) "+" else "-"

    val parts    = tx.description.split("|")
    val dispName = parts.getOrElse(0) { tx.description }.trim()
        .ifBlank { if (isCredit) "Income" else "Expense" }
    val category = parts.getOrElse(1) { if (isCredit) "Income" else "Expense" }.trim()
    val isSplit  = category == "Split Payment" && splitChildren.isNotEmpty()
    val icon     = if (isSplit) Icons.Default.Group
    else if (isCredit) Icons.Default.ArrowDownward
    else Icons.Default.ArrowUpward

    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape          = RoundedCornerShape(16.dp),
        color          = HCardBg,
        tonalElevation = 1.dp,
        modifier       = Modifier
            .fillMaxWidth()
            .then(if (isSplit) Modifier.clickable { expanded = !expanded } else Modifier)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.size(46.dp).clip(CircleShape).background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(dispName, color = HTextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category, color = HGray, fontSize = 12.sp)
                        if (isSplit) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(HBlueLight)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("${splitChildren.size} people",
                                    color = HBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$amtPrefix₹${String.format("%,.2f", tx.amount)}",
                        color      = amtColor,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(timeLabel(tx.timestamp), color = HGray, fontSize = 12.sp)
                        if (isSplit) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                null, tint = HGray, modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // ── Expanded breakdown ────────────────────────────────────────────
            AnimatedVisibility(visible = isSplit && expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "SPLIT BREAKDOWN",
                        color         = HGray,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier      = Modifier.padding(bottom = 8.dp)
                    )
                    splitChildren.forEach { child ->
                        val cParts = child.description.split("|")
                        val person = cParts.getOrElse(0) { child.description }
                            .removeSuffix("'s share").trim()
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier         = Modifier.size(28.dp).clip(CircleShape)
                                        .background(HBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        person.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        color = HBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(person, color = HTextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                "₹${String.format("%,.2f", child.amount)}",
                                color = HBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Summary bar ───────────────────────────────────────────────────────────────
@Composable
private fun SummaryBar(totalExpenses: Double, totalIncome: Double) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(HPageBg)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            modifier    = Modifier.weight(1f),
            icon        = Icons.Default.TrendingDown,
            iconColor   = HRed,
            iconBg      = HRedLight,
            label       = "TOTAL EXPENSES",
            amount      = totalExpenses,
            amountColor = HTextDark
        )
        SummaryCard(
            modifier    = Modifier.weight(1f),
            icon        = Icons.Default.TrendingUp,
            iconColor   = HGreen,
            iconBg      = HGreenLight,
            label       = "TOTAL INCOME",
            amount      = totalIncome,
            amountColor = HGreen
        )
    }
}

@Composable
private fun SummaryCard(
    modifier    : Modifier,
    icon        : ImageVector,
    iconColor   : Color,
    iconBg      : Color,
    label       : String,
    amount      : Double,
    amountColor : Color
) {
    Surface(
        modifier       = modifier,
        shape          = RoundedCornerShape(16.dp),
        color          = HCardBg,
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = iconColor,
                modifier = Modifier.size(22.dp).clip(CircleShape).background(iconBg).padding(4.dp))
            Spacer(Modifier.height(10.dp))
            Text(label, color = HGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "₹${String.format("%,.2f", amount)}",
                color      = amountColor,
                fontWeight = FontWeight.Black,
                fontSize   = 18.sp
            )
        }
    }
}