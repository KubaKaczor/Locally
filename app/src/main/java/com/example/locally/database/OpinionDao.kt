package com.example.locally.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OpinionDao {

    @Insert
    suspend fun insert(opinionEntity: OpinionEntity)

    @Update
    suspend fun update(opinionEntity: OpinionEntity)

    @Delete
    suspend fun delete(opinionEntity: OpinionEntity)

    @Query("SELECT * FROM opinions where userRatedId=:userId")
    fun getOpinionsOfUser(userId: Int): Flow<List<OpinionEntity>>

    @Query("SELECT Count(*) FROM opinions where userRatedId=:userId")
    fun getNumberOfOpinions(userId: Int): Flow<Int>

    @Query("SELECT IFNULL(AVG(rate),0) FROM opinions where userRatedId=:userId")
    fun getRateOfUser(userId: Int): Flow<Double>

}