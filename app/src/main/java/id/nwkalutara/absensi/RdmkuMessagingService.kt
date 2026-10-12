package id.nwkalutara.absensi

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Locale

class RdmkuMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // TODO: kirim token ke backend aman. Untuk sekarang token hanya dicatat di Logcat.
        Log.i("RDMKU_FCM_TOKEN", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"] ?: "Notifikasi Absensi RDMKU"
        val body = message.notification?.body
            ?: message.data["body"] ?: "Ada informasi baru."
        val shouldSpeak = message.data["tts"]?.equals("true", ignoreCase = true) ?: true

        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 802, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(
            this, getString(R.string.default_notification_channel_id)
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(System.currentTimeMillis().toInt(), notification)

        if (shouldSpeak) {
            val tts = TextToSpeech(applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsEngine = null
                    val engine = TextToSpeech(applicationContext) { initStatus ->
                        if (initStatus == TextToSpeech.SUCCESS) {
                            engineSafeSpeak(body)
                        }
                    }
                    ttsEngine = engine
                }
            }
        }
    }

    private var ttsEngine: TextToSpeech? = null

    private fun engineSafeSpeak(text: String) {
        ttsEngine?.let { engine ->
            engine.language = Locale("id", "ID")
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rdmku_fcm_${System.currentTimeMillis()}")
        }
    }
}
