package com.example.locally.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.locally.R
import com.example.locally.database.LocallyApp
import com.example.locally.database.Settings
import com.example.locally.database.UserDao
import com.example.locally.database.UserEntity
import com.example.locally.databinding.ActivitySettingsBinding
import com.example.locally.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private lateinit var mSharedPreferences: SharedPreferences

    private var selectedLocation: Int = 0

    private lateinit var mUser: UserEntity

    private lateinit var userDao: UserDao

    private var userLogin : String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBar()

        mSharedPreferences = this.getSharedPreferences(Constants.LOCALLY_PREFERENCES, Context.MODE_PRIVATE)
        userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

        userDao = (application as LocallyApp).db.userDao()
        loadUser()
        loadSpinner()


    }

    private fun setUpActionBar(){
        val toolbar = binding.toolbarSettings

        if(toolbar != null){
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(getDrawable(R.drawable.ic_baseline_arrow_back_ios_24))

            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun loadUser(){

        lifecycleScope.launch(Dispatchers.Main) {

            userDao.findUserByEmail(userLogin!!).collect {
                //mUser = it
                selectedLocation = it.settings.ordersByLocation
                loadData()
            }
        }
    }

    private fun loadSpinner(){
        val dropdown = binding.settingsDistanceSpinner

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Constants.distanceSettings)

        val spinnerDistanceItem = mSharedPreferences.getInt(Constants.SHOW_ORDERS_DISTANCE, 1)

        dropdown.adapter = adapter
        dropdown.setSelection(spinnerDistanceItem)
        dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                val editor: SharedPreferences.Editor = mSharedPreferences.edit()
                editor.putInt(Constants.SHOW_ORDERS_DISTANCE, position)
                editor.apply()
            }
        }
    }

    private fun loadData(){

        val dropdown = binding.settingsSpinner

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Constants.locationSettings)

        dropdown.adapter = adapter
        dropdown.setSelection(selectedLocation)
        dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                try {

                    if(position == 1){
                        if(!isLocationEnabled()){
                            Toast.makeText(this@SettingsActivity, "Lokalizacja jest wyłączona", Toast.LENGTH_SHORT).show()

                            val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            startActivity(intent)
                        }else{
                            if(!isLocationPermissionGranted()){
                                return
                            }
                        }
                    }

                    if (selectedLocation != position) {
                        lifecycleScope.launch() {

                            var user : UserEntity? = null

                            userDao.findUserByEmail(userLogin!!).collect {
                                user = it
                                val settings = Settings(position)
                                user!!.settings = settings
                                userDao.update(user!!)
                            }
                        }

                        selectedLocation = position
                        Toast.makeText(this@SettingsActivity, "test", Toast.LENGTH_SHORT).show()
                    }
                }
                catch (ex: Exception){
                    Log.e("error settings", ex.message!!)
                }

            }
        }
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    private fun isLocationPermissionGranted(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
            false
        } else {
            true
        }
    }
}