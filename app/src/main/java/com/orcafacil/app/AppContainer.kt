package com.orcafacil.app

import android.content.Context
import com.orcafacil.app.data.AppDatabase
import com.orcafacil.app.data.AppRepository

object AppContainer {
    @Volatile
    private var repository: AppRepository? = null

    fun repository(context: Context): AppRepository {
        return repository ?: synchronized(this) {
            AppRepository(AppDatabase.getInstance(context)).also { repository = it }
        }
    }
}
