package com.vichitra.casho.ui.theme.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vichitra.casho.MainViewModel
import com.vichitra.casho.data.ExpenseType
import com.vichitra.casho.data.TaskEntity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoalScreen (viewModel              : MainViewModel,
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
                onAddTransaction       : (Double, String, String) -> Unit = { _, _, _ -> }) {




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
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Achievable Goals", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("View All", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (taskList.isEmpty()) {
                item { EmptyGoalsHint { showAddGoalDialog = true } }
            } else {
                items(taskList, key = { it.id }) { task ->
                    val pct = goalProgress(totalAvailable, task.amount)
                    Box(modifier = Modifier.combinedClickable(
                        onClick     = {},
                        onLongClick = { showDeleteDialog = task }
                    )) {
                        GoalCard(
                            title          = task.name,
                            goalAmount     = task.amount,
                            totalAvailable = totalAvailable,
                            progressPct    = pct,
                            imageUri       = task.imageUri
                        )
                    }
                }
            }
        }

        // ── Speed-dial FAB ────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 60.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(visible = fabExpanded) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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