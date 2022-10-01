package com.example.locally.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "opinions", foreignKeys = [
    ForeignKey(entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userRatedId"],
        onDelete = ForeignKey.CASCADE
    )])
data class OpinionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userRatedId: Int,
    val userRatingName: String,
    val rate: Double,
    val description: String,
    val imageOfUser: String
)