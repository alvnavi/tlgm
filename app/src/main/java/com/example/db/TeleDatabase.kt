package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.*

@Database(
    entities = [
        Contact::class,
        ChatRoom::class,
        Message::class,
        CryptoKey::class,
        Plugin::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TeleDatabase : RoomDatabase() {
    abstract fun teleDao(): TeleDao

    companion object {
        @Volatile
        private var INSTANCE: TeleDatabase? = null

        fun getDatabase(context: Context): TeleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeleDatabase::class.java,
                    "teleguard_secure_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
