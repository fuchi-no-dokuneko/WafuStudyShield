package dev.studyshield.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitivePackagePolicyTest {
    @Test
    fun includesOwnAppAndCoreAndroidSystemPackages() {
        val excluded = SensitivePackagePolicy.excludedPackages("dev.studyshield")

        assertTrue("dev.studyshield" in excluded)
        assertTrue("android" in excluded)
        assertTrue("com.android.settings" in excluded)
        assertTrue("com.android.systemui" in excluded)
    }

    @Test
    fun includesPermissionInstallPhoneEmergencyAndPaymentSurfaces() {
        val excluded = SensitivePackagePolicy.excludedPackages("dev.studyshield")

        assertTrue("com.android.permissioncontroller" in excluded)
        assertTrue("com.google.android.permissioncontroller" in excluded)
        assertTrue("com.android.packageinstaller" in excluded)
        assertTrue("com.google.android.packageinstaller" in excluded)
        assertTrue("com.android.dialer" in excluded)
        assertTrue("com.google.android.dialer" in excluded)
        assertTrue("com.android.incallui" in excluded)
        assertTrue("com.android.phone" in excluded)
        assertTrue("com.android.server.telecom" in excluded)
        assertTrue("com.android.emergency" in excluded)
        assertTrue("com.android.vending" in excluded)
        assertTrue("com.google.android.gms" in excluded)
        assertTrue("com.google.android.apps.walletnfcrel" in excluded)
        assertTrue("com.android.nfc" in excluded)
    }

    @Test
    fun doesNotExcludeNormalUserSelectedApps() {
        val excluded = SensitivePackagePolicy.excludedPackages("dev.studyshield")

        assertFalse("com.instagram.android" in excluded)
        assertFalse("com.zhiliaoapp.musically" in excluded)
        assertFalse("com.discord" in excluded)
        assertFalse("com.google.android.youtube" in excluded)
    }
}
