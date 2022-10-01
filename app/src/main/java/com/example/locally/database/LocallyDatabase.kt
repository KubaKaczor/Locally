package com.example.locally.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class, OrderEntity::class, OpinionEntity::class], version = 1)
abstract class LocallyDatabase: RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun orderDao(): OrderDao
    abstract fun opinionDao(): OpinionDao

    companion object{

        @Volatile
        private var INSTANCE : LocallyDatabase? = null

        fun getInstance(context : Context): LocallyDatabase{

            synchronized(this){
                var instance = INSTANCE
                if(instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        LocallyDatabase::class.java,
                        "locally-database"
                    ).fallbackToDestructiveMigration()
                        .build()

                    INSTANCE = instance
                }
                return  instance
            }
        }
    }
}
