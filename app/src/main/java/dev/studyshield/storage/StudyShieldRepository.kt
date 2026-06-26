package dev.studyshield.storage

import androidx.room.withTransaction
import dev.studyshield.companion.CompanionPackBundle
import dev.studyshield.domain.ActiveReminder
import dev.studyshield.domain.CompanionPack
import dev.studyshield.domain.DialogueCue
import dev.studyshield.domain.FocusProfile
import dev.studyshield.domain.ReminderScene
import dev.studyshield.domain.UserOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

class StudyShieldRepository(
    private val database: StudyShieldDatabase,
    private val clock: Clock
) {
    fun observeProfiles(): Flow<List<FocusProfile>> {
        return database.profileDao().observeProfiles().map { profiles ->
            profiles.map { entity -> loadProfile(entity) }
        }
    }

    fun observeTodayEventCount(): Flow<Int> {
        val startOfDay = LocalDate.now(clock).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        return database.focusEventDao().observeCountSince(startOfDay)
    }

    fun observeRecentEvents(limit: Int = 20): Flow<List<FocusEventEntity>> {
        return database.focusEventDao().observeRecent(limit)
    }

    fun observeCompanionPacks(): Flow<List<dev.studyshield.domain.CompanionPack>> {
        return database.companionPackDao().observePacks().map { packs -> packs.map { it.toDomain() } }
    }

    suspend fun listCompanionPacks(): List<CompanionPack> {
        return database.companionPackDao().listPacks().map { it.toDomain() }
    }

    suspend fun companionPack(id: Long?): CompanionPack? {
        return id?.let { database.companionPackDao().getPack(it)?.toDomain() }
    }

    suspend fun companionPackBundle(packId: Long): CompanionPackBundle? {
        val pack = database.companionPackDao().getPack(packId)?.toDomain() ?: return null
        val dialogue = database.dialogueCueDao().listForPack(packId).map { it.toDomain() }
        return CompanionPackBundle(pack = pack, dialogue = dialogue)
    }

    suspend fun listProfiles(): List<FocusProfile> {
        return database.profileDao().listProfiles().map { loadProfile(it) }
    }

    suspend fun saveProfile(profile: FocusProfile): Long {
        return database.withTransaction {
            val profileId = database.profileDao().insert(profile.toEntity())
            database.scheduleRuleDao().deleteForProfile(profileId)
            database.targetAppDao().deleteForProfile(profileId)
            database.scheduleRuleDao().insertAll(profile.schedules.map { it.toEntity(profileId) })
            database.targetAppDao().insertAll(profile.targetApps.map { it.toEntity(profileId) })
            profileId
        }
    }

    suspend fun firstDialogueFor(reminder: ActiveReminder): DialogueCue? {
        val packId = reminder.profile.companionPackId ?: return null
        return database.dialogueCueDao()
            .firstForScene(packId, reminder.scene.name)
            ?.toDomain()
    }

    suspend fun saveCompanionPack(bundle: CompanionPackBundle): Long {
        return database.withTransaction {
            val existing = database.companionPackDao().getBySlug(bundle.pack.slug)
            val packId = database.companionPackDao().insert(bundle.pack.copy(id = existing?.id ?: bundle.pack.id).toEntity())
            database.dialogueCueDao().deleteForPack(packId)
            database.dialogueCueDao().insertAll(bundle.dialogue.map { it.toEntity(packId) })
            packId
        }
    }

    suspend fun updateCompanionCharacterImage(packId: Long, uri: String?) {
        database.companionPackDao().updateCharacterImage(packId, uri)
    }

    suspend fun updateCompanionWallpaper(packId: Long, uri: String?) {
        database.companionPackDao().updateWallpaper(packId, uri)
    }

    suspend fun updateTriggeredAudio(packId: Long, uri: String?) {
        database.dialogueCueDao().updateAudioUri(packId, ReminderScene.Triggered.name, uri)
    }

    suspend fun recordOutcome(reminder: ActiveReminder, outcome: UserOutcome) {
        database.focusEventDao().insert(
            FocusEventEntity(
                id = reminder.eventId,
                createdAtEpochMillis = clock.millis(),
                profileId = reminder.profile.id,
                profileName = reminder.profile.name,
                targetPackage = reminder.targetApp.packageName,
                targetLabel = reminder.targetApp.label,
                outcome = outcome.name
            )
        )
    }

    suspend fun clearUserData() {
        database.withTransaction {
            database.focusEventDao().deleteAll()
            database.profileDao().deleteAll()
            database.companionPackDao().deleteAll()
            seedDataIfEmpty()
        }
    }

    suspend fun ensureSeedData() {
        database.withTransaction {
            seedDataIfEmpty()
        }
    }

    private suspend fun seedDataIfEmpty() {
        if (database.profileDao().listProfiles().isNotEmpty()) return
        val packId = database.companionPackDao().insert(samplePack().toEntity())
        database.dialogueCueDao().insertAll(sampleDialogue(packId))
        val profile = sampleProfile().copy(companionPackId = packId)
        val profileId = database.profileDao().insert(profile.toEntity())
        database.scheduleRuleDao().insertAll(profile.schedules.map { it.toEntity(profileId) })
        database.targetAppDao().insertAll(profile.targetApps.map { it.toEntity(profileId) })
    }

    private suspend fun loadProfile(entity: FocusProfileEntity): FocusProfile {
        return entity.toDomain(
            schedules = database.scheduleRuleDao().listForProfile(entity.id),
            targets = database.targetAppDao().listForProfile(entity.id)
        )
    }
}
