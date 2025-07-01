package com.nhathuy.nextmeet

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nhathuy.nextmeet.model.NotificationType
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.util.*

class AlarmScreenActivity : AppCompatActivity(), GestureDetector.OnGestureListener {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var timeTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var messageTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var typeTextView: TextView
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator

    private lateinit var alarmIcon: ImageView
    private lateinit var snoozeIcon: ImageView
    private lateinit var dismissIcon: ImageView

    private var screenWidth: Int = 0
    private var iconInitialX: Float = 0f

    // Notification data
    private var notificationId: Int = -1
    private var notificationTitle: String = ""
    private var notificationMessage: String = ""
    private var notificationLocation: String? = null
    private var relatedId: Int = -1
    private var notificationType: NotificationType? = null

    // Legacy compatibility
    private var customerId: Int = -1

    companion object {
        private const val TAG = "AlarmScreenActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure the screen turns on and stays on
        setupScreenFlags()

        setContentView(R.layout.activity_alarm_screen)

        initializeViews()
        extractIntentData()
        setupUI()

        updateTime()
        startAlarmSound()
        startVibration()

        Log.d(TAG, "AlarmScreenActivity created for notification: $notificationId")
    }

    private fun setupScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun initializeViews() {
        gestureDetector = GestureDetector(this, this)
        timeTextView = findViewById(R.id.timeTextView)

        // Add these TextViews to your layout if they don't exist
        titleTextView = findViewById(R.id.titleTextView)
        messageTextView = findViewById(R.id.messageTextView)
        locationTextView = findViewById(R.id.locationTextView)
        typeTextView = findViewById(R.id.typeTextView)

        alarmIcon = findViewById(R.id.alarmIcon)
        snoozeIcon = findViewById(R.id.snoozeIcon)
        dismissIcon = findViewById(R.id.dismissIcon)

        screenWidth = resources.displayMetrics.widthPixels
        iconInitialX = alarmIcon.x
    }

    private fun extractIntentData() {
        // Extract new notification system data
        notificationId = intent.getIntExtra("notification_id", -1)
        notificationTitle = intent.getStringExtra("title") ?: ""
        notificationMessage = intent.getStringExtra("message") ?: ""
        notificationLocation = intent.getStringExtra("location")
        relatedId = intent.getIntExtra("related_id", -1)

        val notificationTypeString = intent.getStringExtra("notification_type") ?: ""
        notificationType = try {
            if (notificationTypeString.isNotEmpty()) {
                NotificationType.valueOf(notificationTypeString)
            } else null
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid notification type: $notificationTypeString")
            null
        }

        // Legacy compatibility
        customerId = intent.getIntExtra("customer_id", -1)

        // Use notification data if available, otherwise fall back to legacy
        if (notificationId == -1 && customerId != -1) {
            notificationId = customerId
        }

        // If no new data, extract from legacy fields
        if (notificationTitle.isEmpty()) {
            notificationTitle = "Nhắc nhở"
        }

        if (notificationMessage.isEmpty()) {
            val date = intent.getStringExtra("date") ?: ""
            val time = intent.getStringExtra("time") ?: ""
            val notes = intent.getStringExtra("notes") ?: ""
            notificationMessage = buildLegacyMessage(date, time, notes)
        }

        if (notificationLocation.isNullOrEmpty()) {
            notificationLocation = intent.getStringExtra("address")
        }
    }

    private fun setupUI() {
        // Set notification title
        titleTextView.text = notificationTitle

        // Set notification message
        messageTextView.text = notificationMessage

        // Set location if available
        if (!notificationLocation.isNullOrBlank()) {
            locationTextView.text = "📍 $notificationLocation"
            locationTextView.visibility = View.VISIBLE
        } else {
            locationTextView.visibility = View.GONE
        }

        // Set type indicator
        val typeText = when (notificationType) {
            NotificationType.APPOINTMENT_REMINDER -> "📅 Cuộc hẹn"
            NotificationType.NOTE_REMINDER -> "📝 Ghi chú"
            NotificationType.LOCATION_REMINDER -> "📍 Vị trí"
            NotificationType.TRAVEL_TIME -> "🚗 Thời gian di chuyển"
            else -> "🔔 Nhắc nhở"
        }
        typeTextView.text = typeText

        // Set appropriate icon based on type
        val iconResource = when (notificationType) {
            NotificationType.APPOINTMENT_REMINDER -> R.drawable.ic_appointment
            NotificationType.NOTE_REMINDER -> R.drawable.ic_note
            else -> R.drawable.ic_alarm
        }
        alarmIcon.setImageDrawable(ContextCompat.getDrawable(this, iconResource))
    }

    private fun buildLegacyMessage(date: String, time: String, notes: String): String {
        return buildString {
            if (time.isNotEmpty()) {
                append("⏰ Lúc $time")
            }
            if (date.isNotEmpty()) {
                append(" - $date")
            }
            if (notes.isNotEmpty()) {
                append("\n$notes")
            }
        }
    }

    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = sdf.format(Date())
        timeTextView.text = currentTime
    }

    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(this, alarmUri)
            mediaPlayer.isLooping = true
            mediaPlayer.start()
            Log.d(TAG, "Alarm sound started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound", e)
        }
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 500), 0)
            }
            Log.d(TAG, "Vibration started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration", e)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y

        if (abs(diffX) > abs(diffY)) {
            if (diffX > 0 && diffX > screenWidth / 3) {
                // Swipe right - dismiss
                dismissAlarm()
            } else if (diffX < 0 && abs(diffX) > screenWidth / 3) {
                // Swipe left - snooze
                snoozeAlarm()
            } else {
                resetIconAlarm()
            }
        }
        return true
    }

    private fun resetIconAlarm() {
        alarmIcon.animate().translationX(0f).setDuration(100).start()
        val originalIcon = when (notificationType) {
            NotificationType.APPOINTMENT_REMINDER -> R.drawable.ic_appointment
            NotificationType.NOTE_REMINDER -> R.drawable.ic_note
            else -> R.drawable.ic_alarm
        }
        alarmIcon.setImageDrawable(ContextCompat.getDrawable(this, originalIcon))
        alarmIcon.visibility = View.VISIBLE
        snoozeIcon.alpha = 0.3f
        dismissIcon.alpha = 0.3f
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        val scrollX = e2.x - e1.x
        val maxScroll = screenWidth / 2f

        // Calculate the percentage of the swipe
        val scrollPercentage = (scrollX / maxScroll).coerceIn(-1f, 1f)

        // Move the alarm icon
        alarmIcon.translationX = scrollX

        when {
            scrollPercentage <= -0.5 -> {
                alarmIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.zzz))
                snoozeIcon.alpha = 1f
                dismissIcon.alpha = 0.3f
                alarmIcon.visibility = View.INVISIBLE
            }
            scrollPercentage >= 0.5 -> {
                alarmIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.alarm_off))
                snoozeIcon.alpha = 0.3f
                dismissIcon.alpha = 1f
                alarmIcon.visibility = View.INVISIBLE
            }
            else -> {
                val originalIcon = when (notificationType) {
                    NotificationType.APPOINTMENT_REMINDER -> R.drawable.ic_appointment
                    NotificationType.NOTE_REMINDER -> R.drawable.ic_note
                    else -> R.drawable.ic_alarm
                }
                alarmIcon.setImageDrawable(ContextCompat.getDrawable(this, originalIcon))
                alarmIcon.visibility = View.VISIBLE
                snoozeIcon.alpha = 0.3f + (abs(scrollPercentage) * 0.7f)
                dismissIcon.alpha = 0.3f + (abs(scrollPercentage) * 0.7f)
            }
        }

        return true
    }

    private fun dismissAlarm() {
        Log.d(TAG, "Dismissing alarm for notification: $notificationId")

        stopAlarmAndVibration()

        // Cancel the notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        // Send dismiss broadcast for new notification system
        val dismissIntent = Intent(this, DismissAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationType?.name)
        }
        sendBroadcast(dismissIntent)

        // Legacy alarm cancellation (if needed)
        if (customerId != -1) {
            try {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    customerId,
                    Intent(this, AlarmReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel legacy alarm", e)
            }
        }

        Toast.makeText(this, "Đã tắt báo thức", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun snoozeAlarm() {
        Log.d(TAG, "Snoozing alarm for notification: $notificationId")

        stopAlarmAndVibration()

        // Send snooze broadcast for new notification system
        val snoozeIntent = Intent(this, SnoozeAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationType?.name)
            putExtra("title", notificationTitle)
            putExtra("message", notificationMessage)
            putExtra("location", notificationLocation)

            // Legacy compatibility
            putExtra("customer_id", if (customerId != -1) customerId else notificationId)
            putExtra("date", intent.getStringExtra("date") ?: getCurrentDate())
            putExtra("time", intent.getStringExtra("time") ?: getCurrentTime())
            putExtra("address", notificationLocation ?: "")
            putExtra("notes", notificationMessage)
        }
        sendBroadcast(snoozeIntent)

        Toast.makeText(this, "Sẽ nhắc lại sau 5 phút", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun stopAlarmAndVibration() {
        try {
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }

        try {
            if (::vibrator.isInitialized) {
                vibrator.cancel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibrator", e)
        }

        Log.d(TAG, "Alarm sound and vibration stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmAndVibration()
        Log.d(TAG, "AlarmScreenActivity destroyed")
    }

    override fun onBackPressed() {
        // Prevent back button from dismissing alarm screen
        // User must swipe to dismiss or snooze
        super.onBackPressed()
    }

    // Implement other required methods from GestureDetector.OnGestureListener
    override fun onDown(e: MotionEvent): Boolean = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        // Single tap to show alarm info or toggle UI elements
        return false
    }
    override fun onLongPress(e: MotionEvent) {
        // Long press could show additional options
    }
}