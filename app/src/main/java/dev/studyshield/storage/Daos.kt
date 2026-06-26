package dev.studyshield.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusProfileDao {
    @Query("SELECT * FROM focus_profiles ORDER BY enabled DESC, name COLLATE NOCASE")
    fun observeProfiles(): Flow<List<FocusProfileEntity>>

    @Query("SELECT * FROM focus_profiles ORDER BY enabled DESC, name COLLATE NOCASE")
    suspend fun listProfiles(): List<FocusProfileEntity>

    @Query("SELECT * FROM focus_profiles WHERE id = :id")
    suspend fun getProfile(id: Long): FocusProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: FocusProfileEntity): Long

    @Query("DELETE FROM focus_profiles")
    suspend fun deleteAll()
}

@Dao
interface ScheduleRuleDao {
    @Query("SELECT * FROM schedule_rules WHERE profileId = :profileId ORDER BY id")
    suspend fun listForProfile(profileId: Long): List<ScheduleRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<ScheduleRuleEntity>)

    @Query("DELETE FROM schedule_rules WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Long)
}

@Dao
interface TargetAppDao {
    @Query("SELECT * FROM target_apps WHERE profileId = :profileId ORDER BY label COLLATE NOCASE")
    suspend fun listForProfile(profileId: Long): List<TargetAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<TargetAppEntity>)

    @Query("DELETE FROM target_apps WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Long)
}

@Dao
interface CompanionPackDao {
    @Query("SELECT * FROM companion_packs ORDER BY name COLLATE NOCASE")
    fun observePacks(): Flow<List<CompanionPackEntity>>

    @Query("SELECT * FROM companion_packs ORDER BY name COLLATE NOCASE")
    suspend fun listPacks(): List<CompanionPackEntity>

    @Query("SELECT * FROM companion_packs WHERE id = :id")
    suspend fun getPack(id: Long): CompanionPackEntity?

    @Query("SELECT * FROM companion_packs WHERE slug = :slug")
    suspend fun getBySlug(slug: String): CompanionPackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pack: CompanionPackEntity): Long

    @Query("UPDATE companion_packs SET characterImageUri = :characterImageUri WHERE id = :packId")
    suspend fun updateCharacterImage(packId: Long, characterImageUri: String?)

    @Query("UPDATE companion_packs SET wallpaperUri = :wallpaperUri WHERE id = :packId")
    suspend fun updateWallpaper(packId: Long, wallpaperUri: String?)

    @Query("DELETE FROM companion_packs")
    suspend fun deleteAll()
}

@Dao
interface DialogueCueDao {
    @Query("SELECT * FROM dialogue_cues WHERE packId = :packId AND scene = :scene ORDER BY id LIMIT 1")
    suspend fun firstForScene(packId: Long, scene: String): DialogueCueEntity?

    @Query("SELECT * FROM dialogue_cues WHERE packId = :packId ORDER BY id")
    suspend fun listForPack(packId: Long): List<DialogueCueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cues: List<DialogueCueEntity>)

    @Query("UPDATE dialogue_cues SET audioUri = :audioUri WHERE packId = :packId AND scene = :scene")
    suspend fun updateAudioUri(packId: Long, scene: String, audioUri: String?)

    @Query("DELETE FROM dialogue_cues WHERE packId = :packId")
    suspend fun deleteForPack(packId: Long)
}

@Dao
interface FocusEventDao {
    @Query("SELECT COUNT(*) FROM focus_events WHERE createdAtEpochMillis >= :fromEpochMillis")
    fun observeCountSince(fromEpochMillis: Long): Flow<Int>

    @Query("SELECT * FROM focus_events ORDER BY createdAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FocusEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: FocusEventEntity)

    @Query("DELETE FROM focus_events")
    suspend fun deleteAll()
}
