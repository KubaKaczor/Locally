package com.example.locally.database

import android.app.Application

class LocallyApp: Application() {
    val db by lazy {
        LocallyDatabase.getInstance(this)
    }
}