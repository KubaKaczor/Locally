package com.example.locally.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert
    suspend fun insert(userEntity: UserEntity)

    @Update
    suspend fun update(userEntity: UserEntity)
//
//    @Delete
//    suspend fun delete(historyEntity: HistoryEntity)
//
    @Query("SELECT * FROM `users`")
    fun fetchAllUsers() : Flow<List<UserEntity>>

//
    @Query("SELECT * FROM `users` where email=:email")
    fun findUserByEmail(email: String) : Flow<UserEntity>

    @Query("SELECT * FROM `users` where id=:id")
    fun findUserById(id: Int) : Flow<UserEntity>


}