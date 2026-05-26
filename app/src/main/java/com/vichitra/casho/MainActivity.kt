package com.vichitra.casho

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vichitra.casho.ui.theme.screens.GoalScreen
import com.vichitra.casho.ui.theme.screens.HistoryScreen
import com.vichitra.casho.ui.theme.screens.HomeScreen
import com.vichitra.casho.ui.theme.screens.ProfileScreen
import com.vichitra.casho.ui.theme.screens.WfHeader

private val NavAccent = Color(0xFF1565C0)
private val NavBg     = Color.White
private val NavGray   = Color(0xFF9E9E9E)
private val AppPageBg = Color(0xFFF0F3FA)
private val NavPillBg = Color(0xFFDEE8F8)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme { MainScreen(viewModel) }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context     = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val totalAvailable by viewModel.totalAvailable.collectAsState()
    val taskList       by viewModel.allTasks.collectAsState()
    val transactions   by viewModel.allTransactions.collectAsState()
    val allGroups      by viewModel.allSplitGroups.collectAsState()

    // Permissions
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (!perms.values.all { it })
            Toast.makeText(context, "Permissions required to track SMS", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        val needed = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            needed += Manifest.permission.POST_NOTIFICATIONS
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }

    Scaffold(
        containerColor = AppPageBg,
        topBar = {
            Surface(
                color          = Color.White,
                tonalElevation = 0.dp,
                modifier       = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    WfHeader(onProfileClick = { selectedTab = 3 })
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                }
            }
        },
        bottomBar = {
            CashoBottomBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }
    ) { innerPadding ->
        when (selectedTab) {
            // ── HOME ──────────────────────────────────────────────────────────
            0 -> HomeScreen(
                viewModel              = viewModel,
                modifier               = Modifier.padding(innerPadding),
                totalAvailable         = totalAvailable,
                onTotalAvailableChange = { viewModel.updateTotalAvailableWithLog(it) },
                taskList               = taskList,
                onAddTask              = { name, amount, imageUri ->
                    viewModel.addTask(name, amount, imageUri)
                },
                onDeleteTask           = { viewModel.deleteTask(it) },
                savedGroups            = allGroups,
                loadMembers            = { groupId -> viewModel.getMembersOfGroup(groupId) },
                onSaveGroup            = { groupName, members ->
                    viewModel.createSplitGroup(groupName, members)
                },
                onAddTransaction       = { amount, type, desc ->
                    // ✅ Deduct from balance (only if > 0 — handled in VM)
                    viewModel.addTransaction(amount, type, desc)
                },
                onQrClick              = { task ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("upi://pay?am=${task.amount}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Pay for ${task.name}…"))
                }
            )

            // ── TRANSACTIONS ──────────────────────────────────────────────────
            1 -> HistoryScreen(
                modifier     = Modifier.padding(innerPadding),
                transactions = transactions
            )

            // ── GOALS (placeholder) ───────────────────────────────────────────
            2 ->  GoalScreen(
            viewModel              = viewModel,
            modifier               = Modifier.padding(innerPadding),
            totalAvailable         = totalAvailable,
            onTotalAvailableChange = { viewModel.updateTotalAvailableWithLog(it) },
            taskList               = taskList,
            onAddTask              = { name, amount, imageUri ->
                viewModel.addTask(name, amount, imageUri)
            },
            onDeleteTask           = { viewModel.deleteTask(it) },
            savedGroups            = allGroups,
            loadMembers            = { groupId -> viewModel.getMembersOfGroup(groupId) },
            onSaveGroup            = { groupName, members ->
                viewModel.createSplitGroup(groupName, members)
            },
            onAddTransaction       = { amount, type, desc ->
                // ✅ Deduct from balance (only if > 0 — handled in VM)
                viewModel.addTransaction(amount, type, desc)
            },
            onQrClick              = { task ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("upi://pay?am=${task.amount}")
                }
                context.startActivity(Intent.createChooser(intent, "Pay for ${task.name}…"))
            })

            // ── PROFILE ───────────────────────────────────────────────────────
            3 -> ProfileScreen(
                modifier        = Modifier.padding(innerPadding),
                totalAvailable  = totalAvailable,
                // ✅ Use updateTotalAvailableWithLog so top-ups appear in Transactions as Income
                onBalanceUpdate = { viewModel.updateTotalAvailableWithLog(it) }
            )
        }
    }
}

// ── Bottom bar ────────────────────────────────────────────────────────────────
@Composable
fun CashoBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    data class Tab(val outline: ImageVector, val filled: ImageVector, val label: String)

    val tabs = listOf(
        Tab(Icons.Outlined.Home,         Icons.Filled.Home,         "HOME"),
        Tab(Icons.Outlined.SwapHoriz,    Icons.Filled.SwapHoriz,    "TRANSACTIONS"),
        Tab(Icons.Outlined.TrackChanges, Icons.Filled.TrackChanges, "GOALS"),
        Tab(Icons.Outlined.Person,       Icons.Filled.Person,       "PROFILE"),
    )

    Surface(
        color    = NavBg,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 16.dp,
                shape        = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ambientColor = Color.Black.copy(0.07f),
                spotColor    = Color.Black.copy(0.07f)
            ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, tab ->
                CashoNavItem(
                    icon       = tab.outline,
                    iconFilled = tab.filled,
                    label      = tab.label,
                    isSelected = selectedTab == i,
                    onClick    = { onTabSelected(i) }
                )
            }
        }
    }
}

@Composable
private fun CashoNavItem(
    icon       : ImageVector,
    iconFilled : ImageVector,
    label      : String,
    isSelected : Boolean,
    onClick    : () -> Unit
) {
    val pillWidth  by animateDpAsState(
        targetValue   = if (isSelected) 80.dp else 0.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label         = "pillWidth"
    )
    val pillAlpha  by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = tween(300),
        label         = "pillAlpha"
    )
    val iconTint   by animateColorAsState(
        targetValue   = if (isSelected) NavAccent else NavGray,
        animationSpec = tween(250),
        label         = "iconTint"
    )
    val labelAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0.7f,
        animationSpec = tween(250),
        label         = "labelAlpha"
    )
    val scale      by animateFloatAsState(
        targetValue   = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .scale(scale)
    ) {
        Box(
            modifier         = Modifier.size(width = 56.dp, height = 34.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .height(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NavPillBg.copy(alpha = pillAlpha))
            )
            Icon(
                imageVector        = if (isSelected) iconFilled else icon,
                contentDescription = label,
                tint               = iconTint,
                modifier           = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text       = label,
            color      = iconTint.copy(alpha = labelAlpha),
            fontSize   = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}