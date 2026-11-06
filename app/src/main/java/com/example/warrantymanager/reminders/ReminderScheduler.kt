package com.example.warrantymanager.reminders

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Warranty reminder"
        val body = inputData.getString("body") ?: "A warranty is approaching its expiry date."
        NotificationHelper.show(applicationContext, title, body)
        return Result.success()
    }
}

object ReminderScheduler {
    fun schedule(context: Context, id: Long, itemName: String, expiryMillis: Long, daysBefore: Int) {
        val trigger = expiryMillis - TimeUnit.DAYS.toMillis(daysBefore.toLong())
        val delay = (trigger - System.currentTimeMillis()).coerceAtLeast(0)
        val data = workDataOf("title" to "Warranty expiring soon", "body" to "$itemName: expiry in $daysBefore days")
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data).build()
        WorkManager.getInstance(context).enqueueUniqueWork("warranty_${id}_$daysBefore", ExistingWorkPolicy.REPLACE, request)
    }
}

object NotificationHelper {
    private const val CHANNEL = "warranty_expiry"
    fun show(context: Context, title: String, body: String) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(android.app.NotificationChannel(CHANNEL, "Warranty reminders", android.app.NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(body).setAutoCancel(true).build()
        val manager = androidx.core.app.NotificationManagerCompat.from(context)
        try { manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification) } catch (_: SecurityException) {}
    }
}
