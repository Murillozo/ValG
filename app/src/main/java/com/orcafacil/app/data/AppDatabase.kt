package com.orcafacil.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, MaterialEntity::class, BudgetEntity::class, BudgetItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun materialDao(): MaterialDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetItemDao(): BudgetItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orcafacil.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
