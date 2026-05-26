package com.vichitra.casho.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
}

@Dao
interface MetadataDao {
    @Query("SELECT value FROM metadata WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: MetadataEntity)
}

@Dao
interface SplitGroupDao {

    @Query("SELECT * FROM split_groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<SplitGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: SplitGroupEntity)

    @Delete
    suspend fun deleteGroup(group: SplitGroupEntity)

    @Query("SELECT * FROM split_members WHERE groupId = :groupId")
    fun getMembersOfGroup(groupId: String): Flow<List<SplitMemberEntity>>

    @Query("SELECT * FROM split_members WHERE groupId = :groupId")
    suspend fun getMembersOfGroupOnce(groupId: String): List<SplitMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: SplitMemberEntity)

    @Delete
    suspend fun deleteMember(member: SplitMemberEntity)

    @Update
    suspend fun updateMember(member: SplitMemberEntity)
}