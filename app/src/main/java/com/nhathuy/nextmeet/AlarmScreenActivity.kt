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
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.util.*

class AlarmScreenActivity : AppCompatActivity(), GestureDetector.OnGestureListener {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var timeTextView: TextView
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator

    private lateinit var alarmIcon : ImageView
    private lateinit var snoozeIcon: ImageView
    private lateinit var dismissIcon: ImageView

    private var screenWidth: Int = 0
    private var iconInitialX: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure the screen turns on and stays on
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

        setContentView(R.layout.activity_alarm_screen)

        gestureDetector = GestureDetector(this, this)
        timeTextView = findViewById(R.id.timeTextView)

        alarmIcon = findViewById(R.id.alarmIcon)
        snoozeIcon = findViewById(R.id.snoozeIcon)
        dismissIcon = findViewById(R.id.dismissIcon)

        screenWidth = resources.displayMetrics.widthPixels
        iconInitialX = alarmIcon.x


        updateTime()
        startAlarmSound()
        startVibration()
    }

    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = sdf.format(Date())
        timeTextView.text = currentTime
    }

    private fun startAlarmSound() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(this, alarmUri)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
    }

    private fun startVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 1000, 500), 0)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y

        if (abs(diffX) > abs(diffY)) {
            if (diffX > 0 && diffX > screenWidth/3) {
                // Swipe right - dismiss
                dismissAlarm()
            } else if(diffX<0 && abs(diffX) > screenWidth/3) {
                // Swipe left - snooze
                snoozeAlarm()
            }
            else{
                resetIconAlarm()
            }
        }
        return true
    }

    private fun resetIconAlarm() {
        alarmIcon.animate().translationX(0f).setDuration(100).start()
        alarmIcon.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_alarm))
        snoozeIcon.alpha=0.3f
        dismissIcon.alpha=0.3f
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        val scrollX = e2.x - e1.x
        val maxScroll = screenWidth / 2f

        // Calculate the percentage of the swipe
        val scrollPercentage = (scrollX / maxScroll).coerceIn(-1f, 1f)

        // Move the alarm icon
//        alarmIcon.translationX = scrollPercentage * maxScroll

        alarmIcon.translationX= scrollX

        when{
            scrollPercentage <= -0.5 -> {
                alarmIcon.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.zzz))
                snoozeIcon.alpha = 1f
                dismissIcon.alpha =0.3f
                alarmIcon.visibility = View.INVISIBLE
            }
            scrollPercentage >= 0.5 ->{
                alarmIcon.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.alarm_off))
                snoozeIcon.alpha = 0.3f
                dismissIcon.alpha = 1f
                alarmIcon.visibility = View.INVISIBLE
            }
            else -> {
                alarmIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_alarm))
                snoozeIcon.alpha = 0.3f + (abs(scrollPercentage) * 0.7f)
                dismissIcon.alpha = 0.3f + (abs(scrollPercentage) * 0.7f)
            }
        }


//        // Update visibility of snooze and dismiss icons
//        snoozeIcon.alpha = abs(scrollPercentage.coerceAtMost(0f))
//        dismissIcon.alpha = scrollPercentage.coerceAtLeast(0f)

        return true
    }
    private fun dismissAlarm() {
        stopAlarmAndVibration()
        // Cancel the notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val customerId = intent.getIntExtra("customer_id", -1)
        notificationManager.cancel(customerId)


        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(this,
                            customerId,
                            Intent(this, AlarmReceiver::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "Alarm dismissed", Toast.LENGTH_SHORT).show()

        finish()
    }

    private fun snoozeAlarm() {
        stopAlarmAndVibration()
        // Implement snooze logic here
        // For example, reschedule the alarm for 10 minutes later
        // You might want to use AlarmManager to schedule a new alarm
        // For now, we'll just finish the activity
        val customerId= intent.getIntExtra("customer_id",-1)
        val date = intent.getStringExtra("date")
        val time = intent.getStringExtra("time")
        val address = intent.getStringExtra("address")
        val notes = intent.getStringExtra("notes")

        val snoozeIntent= Intent(this,SnoozeAlarmReceiver::class.java).apply {
            putExtra("customer_id",customerId)
            putExtra("date",date)
            putExtra("time",time)
            putExtra("address",address)
            putExtra("notes",notes)
        }
        sendBroadcast(snoozeIntent)

        // Show a toast to inform the user
        Toast.makeText(this, "Alarm snoozed for 5 minutes", Toast.LENGTH_SHORT).show()

        finish()
    }

    private fun stopAlarmAndVibration() {
        mediaPlayer.stop()
        mediaPlayer.release()
        vibrator.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        if (::vibrator.isInitialized) {
            vibrator.cancel()
        }
    }

    // Implement other required methods from GestureDetector.OnGestureListener
    override fun onDown(e: MotionEvent): Boolean = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = false

    override fun onLongPress(e: MotionEvent) {}
}