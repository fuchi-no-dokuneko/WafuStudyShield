package dev.studyshield

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

enum class SetupPermission(
    val title: String,
    val detail: String
) {
    Accessibility(
        title = "Accessibility service",
        detail = "Enable StudyShield focus reminders in Android Accessibility settings."
    ),
    UsageAccess(
        title = "Usage access",
        detail = "Allow foreground app checks so already-open target apps can still trigger reminders."
    ),
    Notifications(
        title = "Notifications",
        detail = "Allow five-minute setup reminder notifications while setup is incomplete."
    )
}

class NotificationPermissionStatus(private val context: Context) {
    fun isNotificationPermissionEnabled(): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun canRequestRuntimePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
    }

    fun settingsIntent(): Intent {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

class SetupPermissionStatus(private val context: Context) {
    fun missingPermissions(): List<SetupPermission> {
        return buildList {
            if (!AccessibilityStatus(context).isStudyShieldEnabled()) {
                add(SetupPermission.Accessibility)
            }
            if (!UsageAccessStatus(context).isUsageAccessEnabled()) {
                add(SetupPermission.UsageAccess)
            }
            if (!NotificationPermissionStatus(context).isNotificationPermissionEnabled()) {
                add(SetupPermission.Notifications)
            }
        }
    }
}

object SetupReminderScheduler {
    const val REMINDER_INTERVAL_MILLIS = 5L * 60L * 1000L

    fun sync(context: Context) {
        val appContext = context.applicationContext
        val missingPermissions = SetupPermissionStatus(appContext).missingPermissions()
        if (missingPermissions.isEmpty()) {
            cancel(appContext)
            SetupReminderNotifier.dismiss(appContext)
            return
        }
        SetupReminderNotifier.showIfDue(appContext, missingPermissions)
        schedule(appContext)
    }

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + REMINDER_INTERVAL_MILLIS,
            REMINDER_INTERVAL_MILLIS,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            SETUP_REMINDER_REQUEST_CODE,
            Intent(context, SetupReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val SETUP_REMINDER_REQUEST_CODE = 5205
}

object SetupReminderNotifier {
    private const val CHANNEL_ID = "studyshield_setup_reminders"
    private const val CHANNEL_NAME = "Setup reminders"
    private const val NOTIFICATION_ID = 5205
    private const val PREFS_NAME = "studyshield_setup_reminders"
    private const val LAST_SHOWN_AT_KEY = "last_shown_at"

    fun showIfDue(context: Context, missingPermissions: List<SetupPermission>): Boolean {
        if (SetupPermission.Notifications in missingPermissions) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastShownAt = prefs.getLong(LAST_SHOWN_AT_KEY, 0L)
        if (lastShownAt > 0L && now - lastShownAt < SetupReminderScheduler.REMINDER_INTERVAL_MILLIS) {
            return false
        }
        val shown = show(context, missingPermissions)
        if (shown) {
            prefs.edit().putLong(LAST_SHOWN_AT_KEY, now).apply()
        }
        return shown
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, missingPermissions: List<SetupPermission>): Boolean {
        if (!NotificationPermissionStatus(context).isNotificationPermissionEnabled()) return false
        ensureChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val missingText = missingPermissions.joinToString(", ") { it.title.lowercase() }
        val body = "Enable $missingText so StudyShield can keep focus reminders active."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Finish StudyShield setup")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you every five minutes while StudyShield setup is incomplete."
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

class SetupReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        SetupReminderScheduler.sync(context)
    }
}
