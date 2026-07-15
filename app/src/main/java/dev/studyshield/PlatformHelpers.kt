package dev.studyshield

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

data class InstalledAppInfo(
    val packageName: String,
    val label: String
)

class InstalledAppsReader(private val context: Context) {
    fun launcherApps(): List<InstalledAppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(launcherIntent, 0)
        }
        return activities
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName
                val label = resolveInfo.loadLabel(context.packageManager).toString()
                InstalledAppInfo(packageName, label)
            }
            .filterNot { it.packageName == context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}

class AccessibilityStatus(private val context: Context) {
    fun isStudyShieldEnabled(): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                info.resolveInfo.serviceInfo.packageName == context.packageName &&
                    info.resolveInfo.serviceInfo.name == "dev.studyshield.accessibility.StudyShieldAccessibilityService"
            }
    }

    fun settingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

class UsageAccessStatus(private val context: Context) {
    @Suppress("DEPRECATION")
    fun isUsageAccessEnabled(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

class PersistedUriPermissionStore(private val context: Context) {
    fun persistReadPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun releaseAllReadPermissions() {
        context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .forEach { permission ->
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        permission.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
    }
}
