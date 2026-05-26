package com.vichitra.casho

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vichitra.casho.data.AppDatabase
import com.vichitra.casho.data.TransactionEntity

class TransactionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val smsBody = inputData.getString("sms_body") ?: return Result.failure()
        
        val amount = extractAmount(smsBody)
        if (amount <= 0) return Result.success()

        val type = when {
            smsBody.lowercase().contains("debited") -> "DEBIT"
            smsBody.lowercase().contains("credited") -> "CREDIT"
            else -> return Result.success()
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val transaction = TransactionEntity(
            amount = amount,
            type = type,
            timestamp = System.currentTimeMillis(),
            description = smsBody.take(100) // Store a snippet of the message
        )

        database.transactionDao().insertTransaction(transaction)
        
        showNotification(amount, type)

        return Result.success()
    }

    private fun extractAmount(message: String): Double {
        val regex = Regex("(?:rs|inr|₹)\\.?\\s*([\\d,]+\\.?\\d*)", RegexOption.IGNORE_CASE)
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
    }

    private fun showNotification(amount: Double, type: String) {
        val channelId = "transaction_alerts"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Transaction Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
            .setContentTitle("New Transaction Detected")
            .setContentText("Amount: ₹$amount | Type: $type")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
