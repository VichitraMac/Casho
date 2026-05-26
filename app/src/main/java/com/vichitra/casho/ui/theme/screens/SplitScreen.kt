package com.vichitra.casho.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vichitra.casho.data.SplitGroupEntity
import com.vichitra.casho.data.SplitMemberEntity
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

// ── Colours ───────────────────────────────────────────────────────────────────
private val SBlue      = Color(0xFF1565C0)
private val SBlueLight = Color(0xFFDEEAFB)
private val SGray      = Color(0xFF9E9E9E)
private val STextDark  = Color(0xFF1A1A2E)
private val SCardBg    = Color.White
private val SGold      = Color(0xFFFFD700)
private val SPageBg    = Color(0xFFF0F3FA)
private val SGreen     = Color(0xFF43A047)

// ── Runtime member state (in-memory for the current split session) ─────────────
data class SplitMemberState(
    val id       : String,
    val name     : String,
    val imageUri : String  = "",
    val amount   : Double  = 0.0,
    val isFixed  : Boolean = false   // true = user manually edited this person's share
)

// ── Split logic ────────────────────────────────────────────────────────────────
// Fixed members keep their amount.
// Remaining = totalAmount - sum(fixed), divided evenly among non-fixed members.
fun recalcSplit(members: List<SplitMemberState>, total: Double): List<SplitMemberState> {
    val fixedTotal  = members.filter { it.isFixed }.sumOf { it.amount }
    val remaining   = (total - fixedTotal).coerceAtLeast(0.0)
    val freeCount   = members.count { !it.isFixed }
    val autoShare   = if (freeCount > 0) remaining / freeCount else 0.0
    return members.map { m -> if (m.isFixed) m else m.copy(amount = autoShare) }
}

// ── Main SplitScreen ──────────────────────────────────────────────────────────
@Composable
fun SplitScreen(
    totalAmount   : Double,
    savedGroups   : List<SplitGroupEntity>,
    loadMembers   : suspend (groupId: String) -> List<SplitMemberEntity>,
    onSaveGroup   : (groupName: String, members: List<Pair<String, String>>) -> Unit,
    onDone        : (members: List<SplitMemberState>) -> Unit,
    onBack        : () -> Unit
) {
    // Step: "pick" = choose/create group, "split" = adjust amounts
    var step by remember { mutableStateOf("pick") }

    // Current members being split
    var members by remember { mutableStateOf<List<SplitMemberState>>(emptyList()) }

    if (step == "pick") {
        GroupPickerScreen(
            totalAmount  = totalAmount,
            savedGroups  = savedGroups,
            loadMembers  = loadMembers,
            onGroupSelected = { loaded ->
                members = recalcSplit(
                    loaded.map { SplitMemberState(it.id, it.name, it.imageUri) },
                    totalAmount
                )
                step = "split"
            },
            // ✅ Quick Done — equal split, save directly to transactions
            onQuickDone = { equalSplit ->
                onDone(equalSplit)
            },
            onNewGroup = { newMembers ->
                members = recalcSplit(newMembers, totalAmount)
                step = "split"
            },
            onSaveGroup = onSaveGroup,
            onBack = onBack
        )
    } else {
        SplitAdjustScreen(
            totalAmount = totalAmount,
            members     = members,
            onMemberAmountChange = { idx, newAmt ->
                // Lock this member's amount, recalculate others
                val updated = members.mapIndexed { i, m ->
                    if (i == idx) m.copy(amount = newAmt, isFixed = true) else m
                }
                members = recalcSplit(updated, totalAmount)
            },
            onDone = { onDone(members) },
            onBack = { step = "pick" }
        )
    }
}

// ── Step 1: Pick or create a group ────────────────────────────────────────────
@Composable
private fun GroupPickerScreen(
    totalAmount    : Double,
    savedGroups    : List<SplitGroupEntity>,
    loadMembers    : suspend (String) -> List<SplitMemberEntity>,
    onGroupSelected: (List<SplitMemberEntity>) -> Unit,
    onQuickDone    : (List<SplitMemberState>) -> Unit,
    onNewGroup     : (List<SplitMemberState>) -> Unit,
    onSaveGroup    : (String, List<Pair<String, String>>) -> Unit,
    onBack         : () -> Unit
) {
    var showCreateGroup by remember { mutableStateOf(false) }

    // ✅ Intercept system back: close create-group dialog first, otherwise go back to home
    BackHandler {
        if (showCreateGroup) showCreateGroup = false else onBack()
    }

    if (showCreateGroup) {
        CreateGroupDialog(
            onDismiss = { showCreateGroup = false },
            onConfirm = { groupName, members ->
                onSaveGroup(groupName, members.map { it.name to it.imageUri })
                onNewGroup(members)
                showCreateGroup = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(SPageBg)) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = STextDark)
            }
            Column(Modifier.weight(1f)) {
                Text("Split Bill", color = STextDark, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(
                    "Total: ₹${String.format(Locale.getDefault(), "%,.2f", totalAmount)}",
                    color    = SGray,
                    fontSize = 13.sp
                )
            }
        }

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Create new group button
            item {
                Surface(
                    modifier  = Modifier.fillMaxWidth().clickable { showCreateGroup = true },
                    shape     = RoundedCornerShape(16.dp),
                    color     = SBlue
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GroupAdd, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Create New Group", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Add members to split with", color = Color.White.copy(0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }

            if (savedGroups.isNotEmpty()) {
                item {
                    Text("SAVED GROUPS", color = SGray, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 8.dp))
                }
                itemsIndexed(savedGroups) { _, group ->
                    SavedGroupRow(
                        group       = group,
                        totalAmount = totalAmount,
                        loadMembers = loadMembers,
                        onAdjust    = {
                            kotlinx.coroutines.MainScope().launch {
                                val loaded = loadMembers(group.id)
                                onGroupSelected(loaded)
                            }
                        },
                        onQuickDone = onQuickDone
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedGroupRow(
    group         : SplitGroupEntity,
    totalAmount   : Double,
    loadMembers   : suspend (String) -> List<SplitMemberEntity>,
    onAdjust      : () -> Unit,
    onQuickDone   : (List<SplitMemberState>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var memberCount by remember { mutableStateOf(0) }
    var showSuccess by remember { mutableStateOf(false) }

    // Load member count on first composition
    LaunchedEffect(group.id) {
        memberCount = loadMembers(group.id).size
    }

    // After success message, auto-clear
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(1500)
            showSuccess = false
        }
    }

    val perPerson = if (memberCount > 0) totalAmount / memberCount else 0.0

    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(16.dp),
        color          = SCardBg,
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            // Group info row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(SBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Group, null, tint = SBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(group.name, color = STextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$memberCount members · ₹${String.format(Locale.getDefault(), "%,.2f", perPerson)} each",
                        color    = SGray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (showSuccess) {
                // ✅ Success state inline
                Row(
                    Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(SGreen),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Added to Transactions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                // ── Adjust + Done buttons ─────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onAdjust,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, SBlue)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = SBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Adjust", color = SBlue, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick  = {
                            scope.launch {
                                val loaded = loadMembers(group.id)
                                val equalSplit = loaded.map { m ->
                                    SplitMemberState(
                                        id       = m.id,
                                        name     = m.name,
                                        imageUri = m.imageUri,
                                        amount   = perPerson,
                                        isFixed  = false
                                    )
                                }
                                showSuccess = true
                                kotlinx.coroutines.delay(1500)
                                onQuickDone(equalSplit)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = SBlue),
                        enabled  = memberCount > 0
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Create Group Dialog ───────────────────────────────────────────────────────
@Composable
private fun CreateGroupDialog(
    onDismiss : () -> Unit,
    onConfirm : (groupName: String, members: List<SplitMemberState>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var members   by remember { mutableStateOf(listOf(
        SplitMemberState(UUID.randomUUID().toString(), ""),
        SplitMemberState(UUID.randomUUID().toString(), "")
    )) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SCardBg,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Text("Create Split Group", color = STextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = groupName,
                    onValueChange = { groupName = it },
                    label         = { Text("Group Name") },
                    placeholder   = { Text("e.g. Trip to Goa") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SBlue, focusedLabelColor = SBlue)
                )

                Text("MEMBERS", color = SGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                members.forEachIndexed { idx, member ->
                    MemberInputRow(
                        member   = member,
                        onNameChange = { name ->
                            members = members.toMutableList().also { it[idx] = member.copy(name = name) }
                        },
                        onImageChange = { uri ->
                            members = members.toMutableList().also { it[idx] = member.copy(imageUri = uri) }
                        },
                        onRemove = if (members.size > 2) ({
                            members = members.toMutableList().also { it.removeAt(idx) }
                        }) else null
                    )
                }

                // Add member
                TextButton(
                    onClick = {
                        members = members + SplitMemberState(UUID.randomUUID().toString(), "")
                    }
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = SBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Member", color = SBlue)
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    if (groupName.isNotBlank() && members.all { it.name.isNotBlank() })
                        onConfirm(groupName, members)
                },
                colors   = ButtonDefaults.buttonColors(containerColor = SBlue),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create Group", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick  = onDismiss,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                border   = BorderStroke(1.dp, Color(0xFFDDE2EC))
            ) { Text("Cancel", color = SGray) }
        }
    )
}

@Composable
private fun MemberInputRow(
    member       : SplitMemberState,
    onNameChange : (String) -> Unit,
    onImageChange: (String) -> Unit,
    onRemove     : (() -> Unit)?
) {
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            uri: Uri? -> uri?.let { onImageChange(it.toString()) }
    }

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar / image picker
        Box(
            modifier         = Modifier.size(44.dp).clip(CircleShape)
                .background(SBlueLight).clickable { imagePicker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (member.imageUri.isNotEmpty()) {
                AsyncImage(
                    model            = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(member.imageUri)).crossfade(true).build(),
                    contentDescription = member.name,
                    contentScale     = ContentScale.Crop,
                    modifier         = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(Icons.Default.Person, null, tint = SBlue, modifier = Modifier.size(22.dp))
            }
        }

        OutlinedTextField(
            value         = member.name,
            onValueChange = onNameChange,
            placeholder   = { Text("Name", color = SGray) },
            modifier      = Modifier.weight(1f),
            singleLine    = true,
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SBlue, unfocusedBorderColor = Color(0xFFDDE2EC))
        )

        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.Red.copy(0.7f))
            }
        }
    }
}

// ── Step 2: Adjust split amounts ──────────────────────────────────────────────
@Composable
private fun SplitAdjustScreen(
    totalAmount          : Double,
    members              : List<SplitMemberState>,
    onMemberAmountChange : (index: Int, amount: Double) -> Unit,
    onDone               : () -> Unit,
    onBack               : () -> Unit
) {
    val currentTotal = members.sumOf { it.amount }
    val isBalanced   = Math.abs(currentTotal - totalAmount) < 0.01

    // ✅ Show success state when Done is tapped
    var showSuccess by remember { mutableStateOf(false) }

    // ✅ Intercept system back: route to onBack (returns to group picker)
    BackHandler { onBack() }

    // After showing success briefly, fire onDone
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(1200)
            onDone()
        }
    }

    Column(Modifier.fillMaxSize().background(SPageBg)) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = STextDark)
            }
            Column(Modifier.weight(1f)) {
                Text("Adjust Split", color = STextDark, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Total: ₹${String.format(Locale.getDefault(), "%,.2f", totalAmount)}",
                    color = SGray, fontSize = 13.sp)
            }
        }

        // Members list
        LazyColumn(
            modifier       = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(members) { idx, member ->
                SplitMemberAmountRow(
                    member  = member,
                    onAmountChange = { newAmt -> onMemberAmountChange(idx, newAmt) }
                )
            }

            // Remaining indicator
            item {
                val remaining = totalAmount - members.filter { it.isFixed }.sumOf { it.amount }
                if (!isBalanced) {
                    Surface(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(12.dp),
                        color     = if (remaining < 0) Color(0xFFFFEBEE) else Color(0xFFF3F7FF)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (remaining < 0) Icons.Default.Warning else Icons.Default.Info,
                                null,
                                tint = if (remaining < 0) Color.Red else SBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (remaining < 0) "Over by ₹${String.format("%.2f", -remaining)}"
                                else "Auto-split ₹${String.format("%.2f", remaining)} among unlocked members",
                                color    = if (remaining < 0) Color.Red else SGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Done button OR success message ────────────────────────────────────
        Surface(color = SCardBg, modifier = Modifier.fillMaxWidth()) {
            if (showSuccess) {
                // ✅ Success message
                Row(
                    Modifier.fillMaxWidth().padding(20.dp).height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SGreen),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "✅  Split Added Successfully",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
            } else {
                Button(
                    onClick  = { showSuccess = true },
                    modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = SBlue)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isBalanced) "Done — Confirm Split" else "Done & Save",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitMemberAmountRow(
    member        : SplitMemberState,
    onAmountChange: (Double) -> Unit
) {
    var editing     by remember { mutableStateOf(false) }
    var inputText   by remember(member.amount) {
        mutableStateOf(String.format(Locale.getDefault(), "%.2f", member.amount))
    }

    Surface(
        shape     = RoundedCornerShape(16.dp),
        color     = SCardBg,
        tonalElevation = 2.dp,
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Box(
                modifier         = Modifier.size(46.dp).clip(CircleShape)
                    .background(if (member.isFixed) Color(0xFFFFF3E0) else SBlueLight),
                contentAlignment = Alignment.Center
            ) {
                if (member.imageUri.isNotEmpty()) {
                    AsyncImage(
                        model            = ImageRequest.Builder(LocalContext.current)
                            .data(Uri.parse(member.imageUri)).crossfade(true).build(),
                        contentDescription = member.name,
                        contentScale     = ContentScale.Crop,
                        modifier         = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color      = if (member.isFixed) Color(0xFFE65100) else SBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                }
            }

            // Name + fixed badge
            Column(Modifier.weight(1f)) {
                Text(member.name, color = STextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (member.isFixed) {
                    Text("🔒 Fixed", color = Color(0xFFE65100), fontSize = 11.sp)
                } else {
                    Text("Auto-split", color = SGray, fontSize = 11.sp)
                }
            }

            // Editable amount
            if (editing) {
                OutlinedTextField(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    prefix        = { Text("₹", color = SBlue, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine    = true,
                    modifier      = Modifier.width(110.dp),
                    shape         = RoundedCornerShape(10.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SBlue),
                    trailingIcon  = {
                        IconButton(onClick = {
                            val amt = inputText.toDoubleOrNull() ?: member.amount
                            onAmountChange(amt)
                            editing = false
                        }) {
                            Icon(Icons.Default.Check, null, tint = SGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.clickable { editing = true }
                ) {
                    Text(
                        "₹${String.format(Locale.getDefault(), "%,.2f", member.amount)}",
                        color      = SBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                    Text("tap to edit", color = SGray, fontSize = 10.sp)
                }
            }
        }
    }
}