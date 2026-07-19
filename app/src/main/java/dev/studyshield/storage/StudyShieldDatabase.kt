package dev.studyshield.storage

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FocusProfileEntity::class,
        ScheduleRuleEntity::class,
        TargetAppEntity::class,
        CompanionPackEntity::class,
        DialogueCueEntity::class,
        FocusEventEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class StudyShieldDatabase : RoomDatabase() {
    abstract fun profileDao(): FocusProfileDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun targetAppDao(): TargetAppDao
    abstract fun companionPackDao(): CompanionPackDao
    abstract fun dialogueCueDao(): DialogueCueDao
    abstract fun focusEventDao(): FocusEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_profiles ADD COLUMN wallpaperScale REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE focus_profiles ADD COLUMN wallpaperOffsetX REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE focus_profiles ADD COLUMN wallpaperOffsetY REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE companion_packs ADD COLUMN characterImageUri TEXT")
                db.execSQL("ALTER TABLE companion_packs ADD COLUMN wallpaperUri TEXT")
                db.execSQL("ALTER TABLE companion_packs ADD COLUMN wallpaperScale REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE companion_packs ADD COLUMN wallpaperOffsetX REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE companion_packs ADD COLUMN wallpaperOffsetY REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_profiles ADD COLUMN randomizeActionOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE companion_packs ADD COLUMN randomizeActionOrder INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
