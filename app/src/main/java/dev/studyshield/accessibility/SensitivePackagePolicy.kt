package dev.studyshield.accessibility

object SensitivePackagePolicy {
    private val platformSensitivePackages = setOf(
        "android",
        "com.android.settings",
        "com.android.systemui",
        "com.android.intentresolver",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.vending",
        "com.google.android.gms",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.incallui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.emergency",
        "com.android.nfc",
        "com.google.android.apps.walletnfcrel"
    )

    fun excludedPackages(ownPackageName: String): Set<String> {
        return platformSensitivePackages + ownPackageName
    }
}
