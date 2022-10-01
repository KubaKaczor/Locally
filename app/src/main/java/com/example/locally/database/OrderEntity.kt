package com.example.locally.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey

@Entity(tableName = "orders", foreignKeys = [
    ForeignKey(entity = UserEntity::class,
    parentColumns = ["id"],
    childColumns = ["userId"],
    onDelete = CASCADE)])
data class OrderEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val location: String,
    val city: String,
    val contactName: String,
    val contactPhone: String,
    val userId: Int,
    val category: String,
    val Type: Int = 0,
    val price: Long = 0L,
    var active: Int = 1,
    val imagePosition: Int
)