    package com.example.healthtracker

    import android.content.ContentValues
    import android.content.Context
    import android.database.sqlite.SQLiteDatabase
    import android.database.sqlite.SQLiteOpenHelper

    class SleepDatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            val CREATE_TABLE = ("CREATE TABLE " + TABLE_NAME + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_DEEP + " INTEGER,"
                    + COLUMN_LIGHT + " INTEGER,"
                    + COLUMN_REM + " INTEGER,"
                    + COLUMN_AWAKE + " INTEGER,"
                    + COLUMN_SCORE + " INTEGER)")
            db.execSQL(CREATE_TABLE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }

        fun insertSleepData(deep: Int, light: Int, rem: Int, awake: Int, score: Int): Boolean {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_DEEP, deep)
                put(COLUMN_LIGHT, light)
                put(COLUMN_REM, rem)
                put(COLUMN_AWAKE, awake)
                put(COLUMN_SCORE, score)
            }
            val result = db.insert(TABLE_NAME, null, values)
            db.close()
            return result != -1L
        }

        fun getAllSleepData(): List<SleepData> {
            val sleepList = mutableListOf<SleepData>()
            val db = this.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)

            if (cursor.moveToFirst()) {
                do {
                    val deep = cursor.getInt(1)
                    val light = cursor.getInt(2)
                    val rem = cursor.getInt(3)
                    val awake = cursor.getInt(4)
                    val score = cursor.getInt(5)

                    sleepList.add(SleepData(deep, light, rem, awake, score))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return sleepList
        }

        companion object {
            private const val DATABASE_NAME = "SleepDB"
            private const val DATABASE_VERSION = 1
            private const val TABLE_NAME = "sleep"
            private const val COLUMN_ID = "id"
            private const val COLUMN_DEEP = "deep"
            private const val COLUMN_LIGHT = "light"
            private const val COLUMN_REM = "rem"
            private const val COLUMN_AWAKE = "awake"
            private const val COLUMN_SCORE = "score"
        }
    }

    data class SleepData(val deepSleep: Int, val lightSleep: Int, val remSleep: Int, val awakeTime: Int, val sleepScore: Int)
