package com.vichitra.casho

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody
                
                // Offload processing to WorkManager to ensure it runs even if the app is closed
                context?.let { ctx ->
                    val data = Data.Builder()
                        .putString("sms_body", body)
                        .build()

                    val workRequest = OneTimeWorkRequestBuilder<TransactionWorker>()
                        .setInputData(data)
                        .build()

                    WorkManager.getInstance(ctx).enqueue(workRequest)
                }
            }
        }
    }
}
