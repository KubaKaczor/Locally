package com.example.locally.database

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "users")
data class UserEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val lastname: String,
    val telephone: String,
    val city: String,
    var cityLatitude: Double,
    var cityLongitude: Double,
    val email: String,
    val password: String,
    val image: String ="",
    @Embedded
    var settings: Settings
)

data class Settings(
    val ordersByLocation: Int,
    val rangeDistance: Double = 10000.0
)
