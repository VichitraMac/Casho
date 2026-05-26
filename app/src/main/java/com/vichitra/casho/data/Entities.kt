package com.vichitra.casho.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// ── 1. Tasks (Achievable Goals) ───────────────────────────────────────────────
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amount: Double,
    val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "") val imageUri: String = ""   // added in v2
)

// ── 2. Transactions ───────────────────────────────────────────────────────────
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,           // "CREDIT" or "DEBIT"
    val timestamp: Long,
    val description: String = "",
    val taskId: String? = null
)

// ── 3. Metadata (key-value store for totalAvailable etc.) ─────────────────────
@Entity(tableName = "metadata")
data class MetadataEntity(
    @PrimaryKey val key: String,
    val value: String
)

// ── 4. Split Groups ───────────────────────────────────────────────────────────  (added in v3)
@Entity(tableName = "split_groups")
data class SplitGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

// ── 5. Split Members ──────────────────────────────────────────────────────────  (added in v3)
@Entity(
    tableName = "split_members",
    foreignKeys = [ForeignKey(
        entity        = SplitGroupEntity::class,
        parentColumns = ["id"],
        childColumns  = ["groupId"],
        onDelete      = ForeignKey.CASCADE   // deleting a group removes all its members
    )]
)
data class SplitMemberEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val name: String,
    @ColumnInfo(defaultValue = "") val imageUri: String = ""
)