package com.vichitra.casho.data

import kotlinx.coroutines.flow.Flow

class CashoRepository(private val database: AppDatabase) {

    val allTasks: Flow<List<TaskEntity>>               = database.taskDao().getAllTasks()
    val allTransactions: Flow<List<TransactionEntity>> = database.transactionDao().getAllTransactions()
    val allSplitGroups: Flow<List<SplitGroupEntity>>   = database.splitGroupDao().getAllGroups()

    suspend fun insertTask(task: TaskEntity)           = database.taskDao().insertTask(task)
    suspend fun deleteTask(task: TaskEntity)           = database.taskDao().deleteTask(task)
    suspend fun insertTransaction(t: TransactionEntity) = database.transactionDao().insertTransaction(t)

    suspend fun setTotalAvailable(amount: Double) =
        database.metadataDao().insertMetadata(MetadataEntity("total_available", amount.toString()))

    suspend fun getTotalAvailable(): Double =
        database.metadataDao().getValue("total_available")?.toDoubleOrNull() ?: 0.0

    suspend fun insertSplitGroup(group: SplitGroupEntity)   = database.splitGroupDao().insertGroup(group)
    suspend fun deleteSplitGroup(group: SplitGroupEntity)   = database.splitGroupDao().deleteGroup(group)
    suspend fun insertSplitMember(m: SplitMemberEntity)     = database.splitGroupDao().insertMember(m)
    suspend fun deleteSplitMember(m: SplitMemberEntity)     = database.splitGroupDao().deleteMember(m)
    suspend fun getMembersOfGroup(id: String): List<SplitMemberEntity> =
        database.splitGroupDao().getMembersOfGroupOnce(id)
}