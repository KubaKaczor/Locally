package com.example.locally.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.locally.R
import com.example.locally.database.LocallyApp
import com.example.locally.database.Settings
import com.example.locally.database.UserEntity
import com.example.locally.databinding.ActivityRegisterBinding
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private var mLatitude : Double = 0.0
    private var mLongitude : Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar()

        if(!Places.isInitialized()){
            Places.initialize(this@RegisterActivity, resources.getString(R.string.google_maps_api_key))
        }

        registerOnActivityAutocompleteForResult()

        binding.btnSignIn.setOnClickListener {
            registerUser()
        }

        binding.etCity.setOnClickListener{
            findPlace()
        }
    }

    private fun findPlace(){
        try{
            val fields = listOf(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
            )

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this@RegisterActivity)
            resultLauncher.launch(intent)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun setupActionBar(){
        setSupportActionBar(binding.registerToolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_24_black)
        binding.registerToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun registerUser(){
        val name = binding.etName.text.toString()
        val lastname = binding.etLastname.text.toString()
        val telephone = binding.etTelephone.text.toString()
        val city = binding.etCity.text.toString()
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()

        if(validateForm(name, lastname, telephone, city, email , password)){
            val dao = (application as LocallyApp).db.userDao()

            lifecycleScope.launch {
                dao.findUserByEmail(email).collect {
                    val user : UserEntity = it
                    if(user != null){
                        showErrorSnackBar("Podany adres e-mail jest już zarejestrowany")
                    } else {
                        val defaultSettings : Settings = Settings(0)
                        dao.insert(UserEntity(0, name, lastname, telephone, city, mLatitude, mLongitude, email, password, settings = defaultSettings))
                        Toast.makeText(
                            this@RegisterActivity,
                            "Pomyślnie zarejestrowano",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }

    }

    private fun validateForm(name: String, lastname:String, telephone:String, city: String, email: String, password: String): Boolean{
        return when {
            TextUtils.isEmpty(name) ->{
                showErrorSnackBar("Podaj imię")
                false
            }
            TextUtils.isEmpty(lastname) ->{
                showErrorSnackBar("Podaj nazwisko")
                false
            }
            TextUtils.isEmpty(telephone) ->{
                showErrorSnackBar("Podaj telefon")
                false
            }
            TextUtils.isEmpty(city) ->{
                showErrorSnackBar("Podaj miejsce zamieszkania")
                false
            }
            TextUtils.isEmpty(email) ->{
                showErrorSnackBar("Podaj adres e-mail")
                false
            }
            TextUtils.isEmpty(password) ->{
                showErrorSnackBar("Podaj hasło")
                false
            }
            else -> {
                true
            }
        }
    }

    fun showErrorSnackBar(message: String){
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.snackbar_error_color))

        snackBar.show()
    }

    private fun registerOnActivityAutocompleteForResult(){
        //returns: the launcher that can be used to start the activity or dispose of the prepared call.
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val place: Place = Autocomplete.getPlaceFromIntent(result.data!!)
                binding.etCity.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }
        }
    }

}