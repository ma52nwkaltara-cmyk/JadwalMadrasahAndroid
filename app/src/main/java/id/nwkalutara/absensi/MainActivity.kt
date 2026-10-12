package id.nwkalutara.absensi

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            val origin = pendingGeoOrigin
            val callback = pendingGeoCallback
            pendingGeoOrigin = null
            pendingGeoCallback = null
            if (origin != null && callback != null) {
                callback.invoke(origin, locationGranted && isTrustedOrigin(origin), false)
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                results[Manifest.permission.POST_NOTIFICATIONS] == false) {
                Toast.makeText(this, "Izin notifikasi belum diberikan.", Toast.LENGTH_LONG).show()
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scheduleButton = Button(this).apply {
            text = "Atur pengumuman TTS"
            setOnClickListener { showScheduleControls() }
        }
        root.addView(scheduleButton, LinearLayout.LayoutParams(-1, -2))

        webView = WebView(this)
        root.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setGeolocationEnabled(true)
            loadsImagesAutomatically = true
            allowFileAccess = false
            allowContentAccess = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                return uri.scheme != "https" || uri.host != "absensi.rdmku.cloud"
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?, callback: GeolocationPermissions.Callback?
            ) {
                if (origin == null || callback == null || !isTrustedOrigin(origin)) {
                    callback?.invoke(origin ?: "", false, false)
                    return
                }
                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS
                    ).filter { it != Manifest.permission.POST_NOTIFICATIONS || Build.VERSION.SDK_INT >= 33 }
                        .toTypedArray())
                }
            }
        }

        if (savedInstanceState == null) webView.loadUrl("https://absensi.rdmku.cloud/index.html")
        else webView.restoreState(savedInstanceState)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showScheduleControls() {
        val picker = TimePicker(this).apply { setIs24HourView(true) }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 12, 36, 12)
            addView(TextView(this@MainActivity).apply {
                text = "Pilih waktu pengumuman TTS harian"
                textSize = 18f
            })
            addView(picker)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Jadwal TTS")
            .setView(layout)
            .setPositiveButton("Simpan jadwal") { _, _ ->
                val hour = if (Build.VERSION.SDK_INT >= 23) picker.hour else picker.currentHour
                val minute = if (Build.VERSION.SDK_INT >= 23) picker.minute else picker.currentMinute
                scheduleDailyAnnouncement(hour, minute)
                Toast.makeText(this, "Jadwal TTS disimpan.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun scheduleDailyAnnouncement(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ScheduledAnnouncementReceiver::class.java).apply {
            putExtra("message", "Pengumuman Absensi RDMKU. Silakan periksa jadwal dan informasi terbaru.")
        }
        val pending = PendingIntent.getBroadcast(
            this, 701, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
        getSharedPreferences("schedule", MODE_PRIVATE).edit()
            .putInt("hour", hour).putInt("minute", minute).putBoolean("enabled", true).apply()
    }

    private fun isTrustedOrigin(origin: String): Boolean = try {
        val uri = Uri.parse(origin)
        uri.scheme == "https" && uri.host == "absensi.rdmku.cloud"
    } catch (_: Exception) { false }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
