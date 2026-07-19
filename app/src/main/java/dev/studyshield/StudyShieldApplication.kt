package dev.studyshield

import android.app.Application
import androidx.room.Room
import dev.studyshield.companion.CompanionPackArchiveImporter
import dev.studyshield.companion.CompanionPackValidationResult
import dev.studyshield.storage.StudyShieldDatabase
import dev.studyshield.storage.StudyShieldRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock

class StudyShieldApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var repository: StudyShieldRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            StudyShieldDatabase::class.java,
            "studyshield.db"
        ).addMigrations(
            StudyShieldDatabase.MIGRATION_1_2,
            StudyShieldDatabase.MIGRATION_2_3
        ).build()
        repository = StudyShieldRepository(database, Clock.systemDefaultZone())
        appScope.launch {
            repository.ensureSeedData()
            ensureBundledCompanionPacks()
        }
    }

    suspend fun ensureBundledCompanionPacks() {
        if (repository.listCompanionPacks().any { it.slug == CODEX_CAT_GIRL_SLUG }) return
        val result = runCatching {
            assets.open(CODEX_CAT_GIRL_ASSET).use { input ->
                CompanionPackArchiveImporter(this).importZip(input)
            }
        }.getOrNull()
        if (result is CompanionPackValidationResult.Valid) {
            repository.saveCompanionPack(result.bundle)
        }
    }

    private companion object {
        const val CODEX_CAT_GIRL_SLUG = "codex-cat-girl"
        const val CODEX_CAT_GIRL_ASSET = "companion_packs/codex_cat_girl/codex-cat-girl.studyshield-pack.zip"
    }
}
