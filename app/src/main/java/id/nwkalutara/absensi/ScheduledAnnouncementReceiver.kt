package id.nwkalutara.absensi

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class ScheduledAnnouncementReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("message")
            ?: "Pengumuman Absensi RDMKU. Silakan periksa informasi terbaru."

        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context, 801, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(
            context, context.getString(R.string.default_notification_channel_id)
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pengumuman Absensi RDMKU")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(801, notification)

        // TTS saat alarm diterima. Inisialisasi asynchronous; perangkat dapat membatasi playback background.
        val tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = TextToSpeech(context.applicationContext, null)
                engine.language = Locale("id", "ID")
                engine.speak(message, TextToSpeech.QUEUE_FLUSH, null, "rdmku_scheduled")
            }
        }

        // Jadwalkan pengulangan harian berikutnya.
        val prefs = context.getSharedPreferences("schedule", Context.MODE_PRIVATE)
        if (prefs.getBoolean("enabled", false)) {
            val hour = prefs.getInt("hour", 7)
            val minute = prefs.getInt("minute", 0)
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            val nextIntent = Intent(context, ScheduledAnnouncementReceiver::class.java).putExtra("message", message)
            val pending = PendingIntent.getBroadcast(
                context, 701, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
        }
    }
}
