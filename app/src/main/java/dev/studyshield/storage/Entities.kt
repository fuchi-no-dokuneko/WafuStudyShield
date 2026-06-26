package dev.studyshield.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "focus_profiles")
data class FocusProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    val intensity: String,
    val companionPackId: Long?,
    val sidePreference: String,
    val packDefaultSide: String,
    val characterAnchorX: Float,
    val characterAnchorY: Float,
    val characterScale: Float,
    val characterOpacity: Float,
    val bubbleAnchorX: Float,
    val bubbleAnchorY: Float,
    val bubbleAlignment: String,
    val animation: String,
    val backgroundMode: String,
    val overlayDim: Float,
    val wallpaperScale: Float,
    val wallpaperOffsetX: Float,
    val wallpaperOffsetY: Float,
    val reduceMotion: Boolean
)

@Entity(
    tableName = "schedule_rules",
    foreignKeys = [
        ForeignKey(
            entity = FocusProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class ScheduleRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val daysCsv: String,
    val startMinute: Int,
    val endMinute: Int,
    val zoneId: String
)

@Entity(
    tableName = "target_apps",
    primaryKeys = ["profileId", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = FocusProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index("packageName")]
)
data class TargetAppEntity(
    val profileId: Long,
    val packageName: String,
    val label: String,
    val enabled: Boolean
)

@Entity(tableName = "companion_packs", indices = [Index(value = ["slug"], unique = true)])
data class CompanionPackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val slug: String,
    val name: String,
    val author: String,
    val license: String,
    val language: String,
    val contentRating: String,
    val compatibleVersion: Int,
    val characterImageUri: String?,
    val wallpaperUri: String?,
    val sidePreference: String,
    val packDefaultSide: String,
    val characterAnchorX: Float,
    val characterAnchorY: Float,
    val characterScale: Float,
    val characterOpacity: Float,
    val bubbleAnchorX: Float,
    val bubbleAnchorY: Float,
    val bubbleAlignment: String,
    val animation: String,
    val backgroundMode: String,
    val overlayDim: Float,
    val wallpaperScale: Float,
    val wallpaperOffsetX: Float,
    val wallpaperOffsetY: Float,
    val reduceMotion: Boolean
)

@Entity(
    tableName = "dialogue_cues",
    foreignKeys = [
        ForeignKey(
            entity = CompanionPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packId"), Index("scene")]
)
data class DialogueCueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packId: Long,
    val scene: String,
    val text: String,
    val audioUri: String?,
    val transcript: String
)

@Entity(tableName = "focus_events", indices = [Index("createdAtEpochMillis"), Index("targetPackage")])
data class FocusEventEntity(
    @PrimaryKey
    val id: String,
    val createdAtEpochMillis: Long,
    val profileId: Long,
    val profileName: String,
    val targetPackage: String,
    val targetLabel: String,
    val outcome: String
)
