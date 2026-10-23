package com.example.attendly.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.attendly.data.dao.AttendlyDao
import com.example.attendly.data.entity.AttendanceRecordEntity
import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.data.entity.TimetableClassEntity

@Database(
    entities = [
        SubjectEntity::class,
        TimetableClassEntity::class,
        AttendanceRecordEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AttendlyDatabase : RoomDatabase() {

    abstract fun attendlyDao(): AttendlyDao

    companion object {
        @Volatile
        private var INSTANCE: AttendlyDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subjects ADD COLUMN manualAttendedRecordCount INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE subjects ADD COLUMN manualConductedRecordCount INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subjects ADD COLUMN manualGraceCount INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `attendance_records_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timetableClassId` INTEGER,
                        `subjectId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        FOREIGN KEY(`timetableClassId`) REFERENCES `timetable_classes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `attendance_records_new` (`id`, `timetableClassId`, `subjectId`, `date`, `status`)
                    SELECT `id`, `timetableClassId`, `subjectId`, `date`, `status` FROM `attendance_records`
                """.trimIndent())

                db.execSQL("DROP TABLE `attendance_records`")
                db.execSQL("ALTER TABLE `attendance_records_new` RENAME TO `attendance_records`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_records_timetableClassId_date` ON `attendance_records` (`timetableClassId`, `date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_records_subjectId` ON `attendance_records` (`subjectId`)")
            }
        }

        fun getDatabase(context: Context): AttendlyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AttendlyDatabase::class.java,
                    "attendly_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
