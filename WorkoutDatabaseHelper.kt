    package com.example.healthtracker

    import android.content.ContentValues
    import android.content.Context
    import android.database.sqlite.SQLiteDatabase
    import android.database.sqlite.SQLiteOpenHelper

    class WorkoutDatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            val CREATE_TABLE = ("CREATE TABLE " + TABLE_NAME + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_DURATION + " INTEGER,"
                    + COLUMN_STEPS + " INTEGER,"
                    + COLUMN_HEART_RATE + " INTEGER,"
                    + COLUMN_CALORIES + " REAL)")
            db.execSQL(CREATE_TABLE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }

        fun insertWorkout(duration: Int, steps: Int, heartRate: Int, calories: Double): Boolean {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_DURATION, duration)
                put(COLUMN_STEPS, steps)
                put(COLUMN_HEART_RATE, heartRate)
                put(COLUMN_CALORIES, calories)
            }
            val result = db.insert(TABLE_NAME, null, values)
            db.close()
            return result != -1L
        }

        fun getAllWorkouts(): List<WorkoutData> {
            val workoutList = mutableListOf<WorkoutData>()
            val db = this.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)

            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(0)
                    val duration = cursor.getInt(1)
                    val steps = cursor.getInt(2)
                    val heartRate = cursor.getInt(3)
                    val calories = cursor.getDouble(4)

                    workoutList.add(WorkoutData(id, duration, steps, heartRate, calories))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return workoutList
        }

        companion object {
            private const val DATABASE_NAME = "WorkoutDB"
            private const val DATABASE_VERSION = 1
            private const val TABLE_NAME = "workouts"
            private const val COLUMN_ID = "id"
            private const val COLUMN_DURATION = "duration"
            private const val COLUMN_STEPS = "steps"
            private const val COLUMN_HEART_RATE = "heart_rate"
            private const val COLUMN_CALORIES = "calories"
        }
    }

    data class WorkoutData(val id: Int, val duration: Int, val steps: Int, val heartRate: Int, val calories: Double)
