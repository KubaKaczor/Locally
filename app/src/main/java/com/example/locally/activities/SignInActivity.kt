package com.example.locally.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.locally.R
import com.example.locally.database.LocallyApp
import com.example.locally.database.UserEntity
import com.example.locally.databinding.ActivitySignInBinding
import com.example.locally.utils.Constants
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.log

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBar()

        mSharedPreferences = this.getSharedPreferences(Constants.LOCALLY_PREFERENCES, Context.MODE_PRIVATE)

        binding.btnLogin.setOnClickListener {
            loginUser()
        }
    }

    private fun setUpActionBar(){
        val toolbar = binding.toolbarSign

        setSupportActionBar(toolbar)
        supportActionBar!!.setHomeAsUpIndicator(getDrawable(R.drawable.ic_baseline_arrow_back_ios_24_black))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loginUser(){
        val login = binding.etSignEmail.text.toString()
        val password = binding.etSignPassword.text.toString()

        if(validateForm(login, password)){

            val dao = (application as LocallyApp).db.userDao()

            lifecycleScope.launch {

                dao.findUserByEmail(login).collect {
                    val user: UserEntity = it

                    if(user != null){
                        if(user.password == password){

                            val editor: SharedPreferences.Editor = mSharedPreferences.edit()
                            editor.putString(Constants.USER_LOGGED, login)
                            editor.putInt(Constants.SHOW_ORDERS_DISTANCE, 1)
                            editor.apply()

                            val intent = Intent(this@SignInActivity, MainActivity::class.java)
                            startActivity(intent)
                        }else{
                            showErrorSnackBar("Podane hasło jest nieprawidłowe")
                        }
                    }else{
                        showErrorSnackBar("Nieznaleziono podanego użytkownika")
                    }
                }
            }
        }
    }

    private fun validateForm(login: String, password: String): Boolean{
        return when{
            TextUtils.isEmpty(login) ->{
                showErrorSnackBar("Podaj login")
                false
            }
            TextUtils.isEmpty(password) ->{
                showErrorSnackBar("Podaj hasło")
                false
            }
            else -> true
        }
    }

    fun showErrorSnackBar(message: String){
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.snackbar_error_color))

        snackBar.show()
    }
}