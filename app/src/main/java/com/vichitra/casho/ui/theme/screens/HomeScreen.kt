package com.vichitra.casho.ui.theme.screens

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vichitra.casho.MainViewModel
import com.vichitra.casho.data.ExpenseType
import com.vichitra.casho.data.MonthlySavingsPoint
import com.vichitra.casho.data.MonthlySpendData
import com.vichitra.casho.data.TaskEntity
import java.util.Locale

// ── Brand colours ─────────────────────────────────────────────────────────────
val AccentBlue      = Color(0xFF1565C0)
private val AccentBlueLight = Color(0xFFBBDEFB)
private val AccentOrange    = Color(0xFFFF8C00)
val AccentGreen     = Color(0xFF43A047)
private val GoldStart       = Color(0xFFFFD700)
private val GoldEnd         = Color(0xFFFFA500)
val PageBg          = Color(0xFFF4F6FB)
val CardBg          = Color.White
val LabelGray       = Color(0xFF9E9E9E)
private val RowBg           = Color(0xFFF2F4F8)
val TextDark        = Color(0xFF1A1A2E)

// ── Progress logic ────────────────────────────────────────────────────────────
fun goalProgress(totalAvailable: Double, goalAmount: Double): Float {
    if (goalAmount <= 0.0) return 0f
    return (totalAvailable / goalAmount).coerceIn(0.0, 1.0).toFloat()
}

// ── ExpenseType chip background colour ───────────────────────────────────────
fun ExpenseType.chipBg() = when (this) {
    ExpenseType.LIFESTYLE -> Color(0xFFFFF3E0)
    ExpenseType.HOUSING   -> Color(0xFFE3F2FD)
    ExpenseType.DINING    -> Color(0xFFE8F5E9)
    ExpenseType.OTHER     -> Color(0xFFF3E5F5)
}

// ── Formatters ────────────────────────────────────────────────────────────────
fun formatAmount(amount: Double): String =
    "₹${String.format(Locale.getDefault(), "%,.0f", amount)}"

@SuppressLint("DefaultLocale")
fun formatSavingsShort(amount: Double): String = when {
    amount >= 100_000 -> "₹${String.format("%.1f", amount / 100_000)}L"
    amount >= 1_000   -> "₹${String.format("%.1f", amount / 1_000)}K"
    amount > 0        -> "₹${amount.toInt()}"
    else              -> "₹0"
}

// ── Entry point ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel              : MainViewModel,
    modifier               : Modifier = Modifier,
    totalAvailable         : Double,
    onTotalAvailableChange : (Double) -> Unit,
    taskList               : List<TaskEntity>,
    onAddTask              : (String, Double, String) -> Unit,
    onDeleteTask           : (TaskEntity) -> Unit,
    onQrClick              : (TaskEntity) -> Unit,
    onProfileClick         : () -> Unit = {},
    savedGroups            : List<com.vichitra.casho.data.SplitGroupEntity> = emptyList(),
    loadMembers            : suspend (String) -> List<com.vichitra.casho.data.SplitMemberEntity> = { emptyList() },
    onSaveGroup            : (String, List<Pair<String, String>>) -> Unit = { _, _ -> },
    onAddTransaction       : (Double, String, String) -> Unit = { _, _, _ -> }
) {
    // ── Collect ViewModel data ────────────────────────────────────────────────
    val monthlySpend   by viewModel.monthlySpend.collectAsState()
    val monthlySavings by viewModel.monthlySavings.collectAsState()

    // ── UI state ──────────────────────────────────────────────────────────────
    var showDeleteDialog     by remember { mutableStateOf<TaskEntity?>(null) }
    var showAddGoalDialog    by remember { mutableStateOf(false) }
    var fabExpanded          by remember { mutableStateOf(false) }
    var showPayResultDialog  by remember { mutableStateOf(false) }
    var showPayDetailsDialog by remember { mutableStateOf(false) }
    var showSplitScreen      by remember { mutableStateOf(false) }
    var paymentSuccess       by remember { mutableStateOf(false) }
    var payAmount            by remember { mutableStateOf(0.0) }
    var payName              by remember { mutableStateOf("") }
    var payExpenseType       by remember { mutableStateOf(ExpenseType.OTHER) }   // ← NEW
    var currentTaskEntity    by remember { mutableStateOf<TaskEntity?>(null) }

    val qrLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { showPayResultDialog = true }

    // ── Split screen overlay ──────────────────────────────────────────────────
    if (showSplitScreen) {
        SplitScreen(
            totalAmount = payAmount,
            savedGroups = savedGroups,
            loadMembers = loadMembers,
            onSaveGroup = onSaveGroup,
            onDone = { splitMembers ->
                onAddTransaction(payAmount, "DEBIT", "$payName|Split Payment|${payExpenseType.name}")
                splitMembers.forEach { m ->
                    if (m.amount > 0.0) {
                        onAddTransaction(
                            m.amount, "DEBIT",
                            "${m.name}'s share|Split - $payName|${payExpenseType.name}"
                        )
                    }
                }
                currentTaskEntity?.let { viewModel.deleteTask(it) }
                showSplitScreen = false
            },
            onBack = { showSplitScreen = false }
        )
        return
    }

    // ── Goal options dialog (long-press) ──────────────────────────────────────
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor   = CardBg,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Goal Options", color = TextDark, fontWeight = FontWeight.Bold) },
            text  = { Text("What would you like to do with '${showDeleteDialog?.name}'?", color = LabelGray) },
            confirmButton = {
                TextButton(onClick = {
                    currentTaskEntity = showDeleteDialog
                    showDeleteDialog  = null
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("upi://pay")
                    }
                    try {
                        qrLauncher.launch(android.content.Intent.createChooser(intent, "Pay with..."))
                    } catch (e: Exception) {
                        showPayResultDialog = true
                    }
                }) {
                    Icon(Icons.Default.QrCode, null, tint = AccentBlue)
                    Spacer(Modifier.width(6.dp))
                    Text("Pay via QR", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { onDeleteTask(showDeleteDialog!!); showDeleteDialog = null }) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", color = Color.Red)
                }
            }
        )
    }

    // ── Add Goal dialog ───────────────────────────────────────────────────────
    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, amount, uri, type ->          // ← 4 params now
                onAddTask(name, amount, uri)
                showAddGoalDialog = false
            }
        )
    }

    // ── Payment result dialog ─────────────────────────────────────────────────
    if (showPayResultDialog) {
        AlertDialog(
            onDismissRequest = { showPayResultDialog = false },
            containerColor   = CardBg,
            shape            = RoundedCornerShape(24.dp),
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Payment Status", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Did the payment go through?", color = LabelGray, fontSize = 13.sp)
                }
            },
            text = { },
            confirmButton = {
                Button(
                    onClick = {
                        showPayResultDialog  = false
                        paymentSuccess       = true
                        showPayDetailsDialog = true
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Payment Successful", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showPayResultDialog = false },
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cancel, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Payment Failed", color = Color.Red)
                }
            }
        )
    }

    // ── Payment details dialog ────────────────────────────────────────────────
    if (showPayDetailsDialog) {
        PaymentDetailsDialog(
            currentTaskEntity = currentTaskEntity,
            onDismiss = { showPayDetailsDialog = false },
            onDone    = { name, amount, type ->               // ← 3 params now
                payName        = name
                payAmount      = amount
                payExpenseType = type
                onAddTransaction(amount, "DEBIT", "$name|QR Payment|${type.name}")
                showPayDetailsDialog = false
                currentTaskEntity?.let { viewModel.deleteTask(it) }
            },
            onSplit   = { name, amount, type ->               // ← 3 params now
                payName        = name
                payAmount      = amount
                payExpenseType = type
                showPayDetailsDialog = false
                showSplitScreen     = true
            }
        )
    }

    // ── Main content ──────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { LiquiditySection(totalAvailable) }
            item { ActionButtonsRow() }

            // ── Real data cards ───────────────────────────────────────────
            item { TotalSavingsCard(savingsPoints = monthlySavings) }   // ← pass data
            item { MonthlySpendCard(monthlySpend  = monthlySpend)   }   // ← pass data

//            item {
//                Row(
//                    Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment     = Alignment.CenterVertically
//                ) {
//                    Text("Achievable Goals", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
//                    Text("View All", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
//                }
//            }

//            if (taskList.isEmpty()) {
//                item { EmptyGoalsHint { showAddGoalDialog = true } }
//            } else {
//                items(taskList, key = { it.id }) { task ->
//                    val pct = goalProgress(totalAvailable, task.amount)
//                    Box(modifier = Modifier.combinedClickable(
//                        onClick     = {},
//                        onLongClick = { showDeleteDialog = task }
//                    )) {
//                        GoalCard(
//                            title          = task.name,
//                            goalAmount     = task.amount,
//                            totalAvailable = totalAvailable,
//                            progressPct    = pct,
//                            imageUri       = task.imageUri
//                        )
//                    }
//                }
//            }
        }

        // ── Speed-dial FAB ────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 88.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(visible = fabExpanded) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // QR Pay
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1A1A2E).copy(alpha = 0.85f)
                        ) {
                            Text(
                                "  QR Pay  ", color = Color.White, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("upi://pay")
                                }
                                try {
                                    qrLauncher.launch(android.content.Intent.createChooser(intent, "Pay with..."))
                                } catch (e: Exception) {
                                    showPayResultDialog = true
                                }
                            },
                            containerColor = AccentBlue,
                            contentColor   = Color.White,
                            shape          = CircleShape
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(22.dp))
                        }
                    }

                    // Add Goal
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1A1A2E).copy(alpha = 0.85f)
                        ) {
                            Text(
                                "  Add Goal  ", color = Color.White, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        SmallFloatingActionButton(
                            onClick        = { fabExpanded = false; showAddGoalDialog = true },
                            containerColor = AccentBlue,
                            contentColor   = Color.White,
                            shape          = CircleShape
                        ) {
                            Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick        = { fabExpanded = !fabExpanded },
                containerColor = AccentBlue,
                contentColor   = Color.White,
                shape          = CircleShape
            ) {
                Icon(
                    if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Menu"
                )
            }
        }
    }
}

// ── Shared: ExpenseTypeChips ──────────────────────────────────────────────────
@Composable
fun ExpenseTypeChips(
    selected : ExpenseType,
    onSelect : (ExpenseType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Expense Type", color = LabelGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpenseType.values().forEach { type ->
                val isSelected = selected == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentBlue else type.chipBg())
                        .border(
                            width = if (isSelected) 0.dp else 1.dp,
                            color = if (isSelected) Color.Transparent else Color(0xFFDDE2EC),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(type) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(type.emoji, fontSize = 18.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            type.label,
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isSelected) Color.White else TextDark,
                            maxLines   = 1,
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ── PaymentDetailsDialog ──────────────────────────────────────────────────────
@Composable
fun PaymentDetailsDialog(
    currentTaskEntity : TaskEntity? = null,
    onDismiss : () -> Unit,
    onDone    : (name: String, amount: Double, type: ExpenseType) -> Unit,
    onSplit   : (name: String, amount: Double, type: ExpenseType) -> Unit
) {
    var name        by remember { mutableStateOf(currentTaskEntity?.name ?: "") }
    var amount      by remember { mutableStateOf(currentTaskEntity?.amount?.toString() ?: "") }
    var expenseType by remember { mutableStateOf(ExpenseType.OTHER) }
    val isValid = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardBg,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("Payment Successful!", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 18.sp)
                Text("Enter payment details", color = LabelGray, fontSize = 12.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Paid to / Description") },
                    placeholder   = { Text("e.g. Zomato, Movie tickets") },
                    leadingIcon   = { Icon(Icons.Default.Receipt, null, tint = AccentBlue) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue, focusedLabelColor = AccentBlue)
                )
                OutlinedTextField(
                    value           = amount,
                    onValueChange   = { amount = it },
                    label           = { Text("Amount") },
                    prefix          = { Text("₹ ", color = AccentBlue, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    shape           = RoundedCornerShape(12.dp),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue, focusedLabelColor = AccentBlue)
                )
                ExpenseTypeChips(selected = expenseType, onSelect = { expenseType = it })
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = { if (isValid) onSplit(name.trim(), amount.toDouble(), expenseType) },
                    enabled  = isValid,
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, if (isValid) AccentBlue else LabelGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.GroupAdd, null,
                        tint = if (isValid) AccentBlue else LabelGray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Split", color = if (isValid) AccentBlue else LabelGray, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick  = { if (isValid) onDone(name.trim(), amount.toDouble(), expenseType) },
                    enabled  = isValid,
                    colors   = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

// ── AddGoalDialog ─────────────────────────────────────────────────────────────
@Composable
fun AddGoalDialog(
    onDismiss : () -> Unit,
    onConfirm : (name: String, amount: Double, imageUri: String, type: ExpenseType) -> Unit
) {
    var name        by remember { mutableStateOf("") }
    var amount      by remember { mutableStateOf("") }
    var imageUri    by remember { mutableStateOf<Uri?>(null) }
    var expenseType by remember { mutableStateOf(ExpenseType.OTHER) }
    val isValid = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardBg,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(AccentBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, null, tint = AccentBlue, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("New Achievable Goal", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Set a target and track your progress", color = LabelGray, fontSize = 12.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Goal Name") },
                    placeholder   = { Text("e.g. New Car, Vacation") },
                    leadingIcon   = { Icon(Icons.Default.Flag, null, tint = AccentBlue) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentBlue,
                        unfocusedBorderColor = Color(0xFFDDE2EC),
                        focusedLabelColor    = AccentBlue
                    )
                )
                OutlinedTextField(
                    value           = amount,
                    onValueChange   = { amount = it },
                    label           = { Text("Target Amount") },
                    prefix          = { Text("₹ ", color = AccentBlue, fontWeight = FontWeight.Bold) },
                    placeholder     = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    shape           = RoundedCornerShape(12.dp),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentBlue,
                        unfocusedBorderColor = Color(0xFFDDE2EC),
                        focusedLabelColor    = AccentBlue
                    )
                )
                if (imageUri != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model             = ImageRequest.Builder(LocalContext.current)
                                .data(imageUri).crossfade(true).build(),
                            contentDescription = "Goal image",
                            contentScale      = ContentScale.Crop,
                            modifier          = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd).padding(8.dp)
                                .size(32.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick  = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, Color(0xFFDDE2EC))
                    ) {
                        Icon(Icons.Default.Image, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Add Goal Image (optional)", color = LabelGray)
                    }
                }
                ExpenseTypeChips(selected = expenseType, onSelect = { expenseType = it })
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(name.trim(), amt, imageUri?.toString() ?: "", expenseType)
                },
                enabled  = isValid,
                colors   = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Goal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick  = onDismiss,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                border   = BorderStroke(1.dp, Color(0xFFDDE2EC))
            ) { Text("Cancel", color = LabelGray) }
        }
    )
}

// ── EmptyGoalsHint ────────────────────────────────────────────────────────────
@Composable
fun EmptyGoalsHint(onAddClick: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.EmojiEvents, null, tint = AccentBlueLight, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("No goals yet", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
        Text("Tap + to add your first goal", color = LabelGray, fontSize = 13.sp)
    }
}

// ── WfHeader ──────────────────────────────────────────────────────────────────
@Composable
fun WfHeader(onProfileClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(AccentBlueLight).clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("Casho", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, null, tint = TextDark, modifier = Modifier.size(22.dp))
        }
    }
}

// ── LiquiditySection ─────────────────────────────────────────────────────────
@Composable
fun LiquiditySection(amount: Double) {
    val whole          = amount.toLong()
    val cents          = ((amount - whole) * 100).toInt()
    val wholeFormatted = String.format(Locale.getDefault(), "%,.0f", whole.toDouble())

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            "CURRENT LIQUIDITY",
            color = LabelGray, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = TextDark, fontWeight = FontWeight.Black, fontSize = 44.sp)) {
                    append("₹$wholeFormatted.")
                }
                withStyle(SpanStyle(color = AccentBlue, fontWeight = FontWeight.Black, fontSize = 44.sp)) {
                    append(String.format("%02d", cents))
                }
            }
        )
    }
}

// ── ActionButtonsRow ──────────────────────────────────────────────────────────
@Composable
fun ActionButtonsRow() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick        = {},
            shape          = RoundedCornerShape(50),
            colors         = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.SwapHoriz, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Transfer Funds", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(16.dp))
        Text("Details", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            modifier = Modifier.clickable {})
    }
}

// ── TotalSavingsCard (functional, scrollable) ─────────────────────────────────
@Composable
fun TotalSavingsCard(savingsPoints: List<MonthlySavingsPoint> = emptyList()) {
    val maxSavings = savingsPoints.maxOfOrNull { it.savings }?.takeIf { it > 0 } ?: 1.0
    val totalSaved = savingsPoints.lastOrNull()?.savings ?: 0.0

    Card(
        Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column {
                    Text("Total Savings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (totalSaved > 0) "Saved so far: ${formatSavingsShort(totalSaved)}"
                        else "Start transacting to see savings",
                        color      = if (totalSaved > 0) AccentGreen else LabelGray,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(Icons.Default.TrendingUp, null, tint = AccentGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(20.dp))

            if (savingsPoints.isEmpty()) {
                Row(
                    Modifier.fillMaxWidth().height(110.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.Bottom
                ) {
                    listOf(0.3f, 0.5f, 0.4f, 0.7f, 0.6f, 0.8f, 0.65f).forEach { h ->
                        Box(
                            modifier = Modifier.width(28.dp).fillMaxHeight(h)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(Color(0xFFE0E0E0))
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Add balance & make payments to track savings",
                    color     = LabelGray,
                    fontSize  = 11.sp,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding        = PaddingValues(horizontal = 2.dp)
                ) {
                    items(savingsPoints) { point ->
                        val fraction     = (point.savings / maxSavings).toFloat().coerceIn(0f, 1f)
                        val animFraction by animateFloatAsState(fraction, tween(800), label = "bar_${point.month}")
                        val isHighest    = point.savings == savingsPoints.maxOf { it.savings }

                        Column(
                            modifier            = Modifier.width(56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                formatSavingsShort(point.savings),
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (isHighest) AccentBlue else LabelGray,
                                textAlign  = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier         = Modifier.width(36.dp).height(90.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                        .background(Color(0xFFEEF2F7))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animFraction.coerceAtLeast(0.04f))
                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                        .background(
                                            if (isHighest)
                                                Brush.verticalGradient(listOf(AccentBlue, AccentBlueLight))
                                            else
                                                Brush.verticalGradient(listOf(AccentBlueLight, Color(0xFFDEECFA)))
                                        )
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                point.month,
                                fontSize   = 10.sp,
                                color      = if (isHighest) TextDark else LabelGray,
                                fontWeight = if (isHighest) FontWeight.Bold else FontWeight.Normal,
                                textAlign  = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── MonthlySpendCard (functional with real data) ──────────────────────────────
@Composable
fun MonthlySpendCard(monthlySpend: MonthlySpendData = MonthlySpendData()) {
    data class SpendRow(val type: ExpenseType, val amount: Double)

    val rows = listOf(
        SpendRow(ExpenseType.LIFESTYLE, monthlySpend.lifestyle),
        SpendRow(ExpenseType.HOUSING,   monthlySpend.housing),
        SpendRow(ExpenseType.DINING,    monthlySpend.dining),
        SpendRow(ExpenseType.OTHER,     monthlySpend.other),
    )
    val total = monthlySpend.lifestyle + monthlySpend.housing + monthlySpend.dining + monthlySpend.other

    Card(
        Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Monthly Spend", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Text(
                        "Total: ${formatAmount(total)}",
                        color      = if (total > 0) AccentBlue else LabelGray,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(Icons.Default.PieChart, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(14.dp))

            rows.forEachIndexed { i, row ->
                val fraction = if (total > 0) (row.amount / total).toFloat().coerceIn(0f, 1f) else 0f
                val animFrac by animateFloatAsState(fraction, tween(700), label = "spend_${row.type.name}")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RowBg)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(row.type.chipBg()),
                            contentAlignment = Alignment.Center
                        ) { Text(row.type.emoji, fontSize = 17.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(row.type.label, fontWeight = FontWeight.Medium, color = TextDark, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                    Text(
                        if (row.amount > 0) formatAmount(row.amount) else "₹0",
                        fontWeight = FontWeight.SemiBold,
                        color      = if (row.amount > 0) TextDark else LabelGray,
                        fontSize   = 13.sp
                    )
                }
                if (i < rows.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── TransactionCategoryIcon ───────────────────────────────────────────────────
@Composable
fun TransactionCategoryIcon(type: ExpenseType, size: androidx.compose.ui.unit.Dp = 42.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(size / 3))
            .background(type.chipBg()),
        contentAlignment = Alignment.Center
    ) {
        Text(type.emoji, fontSize = (size.value * 0.45f).sp)
    }
}

// ── GoalCard ──────────────────────────────────────────────────────────────────
@Composable
fun GoalCard(
    title          : String,
    goalAmount     : Double,
    totalAvailable : Double,
    progressPct    : Float,
    imageUri       : String = ""
) {
    val isAchieved       = progressPct >= 1.0f
    val animatedProgress by animateFloatAsState(
        targetValue   = progressPct,
        animationSpec = tween(durationMillis = 800),
        label         = "goalProgress"
    )
    val pctInt     = (progressPct * 100).toInt().coerceAtMost(100)
    val statusText = if (isAchieved) "🎉 Achieved!" else "$pctInt% Complete"
    val barColor   = when {
        isAchieved         -> AccentGreen
        progressPct > 0.6f -> AccentBlue
        progressPct > 0.3f -> AccentOrange
        else               -> Color(0xFFEF5350)
    }
    val goalFmt = String.format(Locale.getDefault(), "%,.0f", goalAmount)
    val currFmt = String.format(Locale.getDefault(), "%,.0f", totalAvailable.coerceAtMost(goalAmount))

    val cardModifier = if (isAchieved) {
        Modifier.fillMaxWidth()
            .border(2.dp, Brush.linearGradient(listOf(GoldStart, GoldEnd, GoldStart)), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier  = cardModifier,
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = if (isAchieved) Color(0xFFFFFDE7) else CardBg),
        elevation = CardDefaults.cardElevation(if (isAchieved) 6.dp else 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                if (imageUri.isNotEmpty()) {
                    AsyncImage(
                        model             = ImageRequest.Builder(LocalContext.current)
                            .data(Uri.parse(imageUri)).crossfade(true).build(),
                        contentDescription = title,
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFFB0C4DE), Color(0xFF7EB8F7)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EmojiEvents, null,
                            tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                    }
                }
                if (isAchieved) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(listOf(GoldStart, GoldEnd)))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("✨ Achieved", color = Color(0xFF5D4000), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(7.dp)
                        .clip(RoundedCornerShape(50)).background(Color(0xFFE0E0E0))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isAchieved) Brush.linearGradient(listOf(GoldStart, GoldEnd))
                                else Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.7f), barColor))
                            )
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        statusText,
                        color      = if (isAchieved) Color(0xFFB8860B) else LabelGray,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "₹$currFmt / ₹$goalFmt",
                        color      = if (isAchieved) Color(0xFFB8860B) else AccentBlue,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}