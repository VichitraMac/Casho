package com.vichitra.casho.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// ── Colours ───────────────────────────────────────────────────────────────────
private val PBlue        = Color(0xFF1565C0)
private val PLightBlue   = Color(0xFFBBDEFB)
private val PPageBg      = Color(0xFFF0F3FA)
private val PCardBg      = Color.White
private val PGray        = Color(0xFF9E9E9E)
private val PTextDark    = Color(0xFF1A1A2E)
private val PIconBg      = Color(0xFFE8EFF9)
private val PRedBg       = Color(0xFFFFEBEB)
private val PBadgeBg     = Color(0xFFF5E6D3)
private val PBadgeText   = Color(0xFF8B6914)
private val PBarLight    = Color(0xFFCCDFF5)

@Composable
fun ProfileScreen(
    modifier        : Modifier = Modifier,
    totalAvailable  : Double,
    onBalanceUpdate : (Double) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditBalanceDialog(
            currentBalance = totalAvailable,
            onDismiss      = { showEditDialog = false },
            onConfirm      = { newAmount ->
                onBalanceUpdate(newAmount)
                showEditDialog = false
            }
        )
    }

    LazyColumn(
        modifier        = modifier
            .fillMaxSize()
            .background(PPageBg),
        contentPadding  = PaddingValues(bottom = 100.dp)
    ) {
        // ── Hero gradient + avatar + name + badge ────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFDDE6F8), Color(0xFFF0F3FA))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar with edit badge
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2C3E50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint     = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PBlue)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Alex Rivera",
                        color      = PTextDark,
                        fontWeight = FontWeight.Black,
                        fontSize   = 26.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    // PRO MEMBER badge
                    Surface(
                        color  = PBadgeBg,
                        shape  = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Star, null, tint = PBadgeText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "PRO MEMBER",
                                color         = PBadgeText,
                                fontSize      = 12.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Stats row ─────────────────────────────────────────────────────────
        item {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Balance card — tappable to edit
                Card(
                    modifier  = Modifier
                        .weight(1f)
                        .clickable { showEditDialog = true },
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = PCardBg),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "TOTAL BALANCE",
                            color         = PGray,
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "₹${String.format(Locale.getDefault(), "%,.2f", totalAvailable)}",
                            color      = PTextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        // Mini bar chart
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment     = Alignment.Bottom
                        ) {
                            listOf(0.4f, 0.6f, 0.75f, 0.55f, 0.9f).forEachIndexed { i, h ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height((40 * h).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (i == 4) PBlue else PLightBlue)
                                )
                            }
                        }
                    }
                }

                // Monthly Savings card
                Card(
                    modifier  = Modifier.weight(1f),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = PCardBg),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "MONTHLY\nSAVINGS",
                            color         = PGray,
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "₹1,250.00",
                            color      = PTextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(PBarLight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(PBlue)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(28.dp)) }

        // ── ACCOUNT SETTINGS ─────────────────────────────────────────────────
        item {
            SectionLabel("ACCOUNT SETTINGS")
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            SettingsRow(icon = Icons.Default.Person,           label = "Personal Info",    iconBg = PIconBg, iconTint = PBlue)
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            SettingsRow(icon = Icons.Default.Shield,           label = "Security",         iconBg = PIconBg, iconTint = PBlue)
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            SettingsRow(icon = Icons.Default.AccountBalance,   label = "Linked Accounts",  iconBg = PIconBg, iconTint = PBlue)
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            SettingsRow(icon = Icons.Default.NotificationsNone, label = "Notifications",   iconBg = PIconBg, iconTint = PBlue)
        }

        item { Spacer(Modifier.height(28.dp)) }

        // ── SUPPORT & ACTIONS ─────────────────────────────────────────────────
        item {
            SectionLabel("SUPPORT & ACTIONS")
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            SettingsRow(icon = Icons.Outlined.HelpOutline, label = "Help & Support", iconBg = Color(0xFFF0F0F0), iconTint = PGray)
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            SettingsRow(icon = Icons.Default.ExitToApp, label = "Logout", iconBg = PRedBg, iconTint = Color(0xFFD32F2F), labelColor = Color(0xFFD32F2F), showChevron = false)
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        color         = PGray,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier      = Modifier.padding(horizontal = 20.dp)
    )
}

// ── Single settings row ───────────────────────────────────────────────────────
@Composable
private fun SettingsRow(
    icon        : ImageVector,
    label       : String,
    iconBg      : Color,
    iconTint    : Color,
    labelColor  : Color  = PTextDark,
    showChevron : Boolean = true
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable {},
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = PCardBg),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(label, color = labelColor, fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (showChevron) {
                Icon(Icons.Default.ChevronRight, null, tint = PGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Edit Balance Dialog ───────────────────────────────────────────────────────
@Composable
fun EditBalanceDialog(
    currentBalance : Double,
    onDismiss      : () -> Unit,
    onConfirm      : (Double) -> Unit
) {
    var input by remember { mutableStateOf(currentBalance.toInt().toString()) }
    val isValid = input.toDoubleOrNull()?.let { it > 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier         = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE8EFF9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, null, tint = PBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("Edit Balance", color = PTextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Update your current liquidity", color = PGray, fontSize = 12.sp)
            }
        },
        text = {
            Column {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value         = input,
                    onValueChange = { input = it },
                    modifier      = Modifier.fillMaxWidth(),
                    prefix        = { Text("₹ ", color = PBlue, fontWeight = FontWeight.Bold) },
                    label         = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PBlue,
                        unfocusedBorderColor = Color(0xFFDDE2EC),
                        focusedLabelColor    = PBlue,
                        cursorColor          = PBlue
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { input.toDoubleOrNull()?.let { onConfirm(it) } },
                enabled  = isValid,
                colors   = ButtonDefaults.buttonColors(containerColor = PBlue),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Balance", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick  = onDismiss,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                border   = BorderStroke(1.dp, Color(0xFFDDE2EC))
            ) {
                Text("Cancel", color = PGray)
            }
        }
    )
}