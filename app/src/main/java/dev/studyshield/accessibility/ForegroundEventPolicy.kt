package dev.studyshield.accessibility

enum class ForegroundEventAction {
    Evaluate,
    HideCurrentReminder,
    IgnoreOwnOverlayEvent
}

object ForegroundEventPolicy {
    fun actionFor(
        packageName: String?,
        className: String?,
        ownPackageName: String,
        hasCurrentReminder: Boolean
    ): ForegroundEventAction {
        if (packageName != ownPackageName) return ForegroundEventAction.Evaluate
        if (!hasCurrentReminder) return ForegroundEventAction.Evaluate
        return if (className.isOwnMainActivity(ownPackageName)) {
            ForegroundEventAction.HideCurrentReminder
        } else {
            ForegroundEventAction.IgnoreOwnOverlayEvent
        }
    }

    private fun String?.isOwnMainActivity(ownPackageName: String): Boolean {
        val normalized = this?.trim()?.removePrefix(".") ?: return false
        return normalized == "MainActivity" ||
            normalized == "$ownPackageName.MainActivity" ||
            (normalized.startsWith("$ownPackageName.") && normalized.endsWith(".MainActivity"))
    }
}
