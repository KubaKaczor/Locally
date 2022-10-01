package com.example.locally.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.locally.R
import com.example.locally.databinding.ActivityIntroBinding
import com.example.locally.utils.Constants

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding

    private lateinit var mSharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mSharedPreferences = this.getSharedPreferences(Constants.LOCALLY_PREFERENCES, Context.MODE_PRIVATE)

        val userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

        if(!userLogin.isNullOrEmpty()){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.btnSign.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }
}