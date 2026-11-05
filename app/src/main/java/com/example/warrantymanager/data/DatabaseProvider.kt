package com.example.warrantymanager.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var INSTANCE: AppDatabase? = null
    fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "warranty-manager.db")
            .build().also { INSTANCE = it }
    }
}
