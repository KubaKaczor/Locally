package com.example.locally.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.locally.R
import com.example.locally.database.LocallyApp
import com.example.locally.database.OrderEntity
import com.example.locally.database.UserEntity
import com.example.locally.databinding.ActivityCreateOrderBinding
import com.example.locally.utils.Constants
import com.example.locally.utils.GetAddressFromLatLng
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CreateOrderActivity : AppCompatActivity() {

    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var binding : ActivityCreateOrderBinding

    private lateinit var mUser : UserEntity
    private var mUserId : Int = 0

    private val categories = Constants.categories

    private var category: String = ""

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private var mLatitude : Double = 0.0
    private var mLongitude : Double = 0.0

    private var imagePosition: Int = 0

    private var orderId = -1

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var orderCity : String =""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBar()


        mSharedPreferences = this.getSharedPreferences(Constants.LOCALLY_PREFERENCES, Context.MODE_PRIVATE)

        if(!Places.isInitialized()){
            Places.initialize(this@CreateOrderActivity, resources.getString(R.string.google_maps_api_key))
        }

        if(intent.hasExtra(Constants.ORDER_DETAILS_ID_EXTRA)){
            orderId = intent.getIntExtra(Constants.ORDER_DETAILS_ID_EXTRA, -1)
            loadOrder()
            binding.btnEditOrder.visibility = View.VISIBLE
            binding.btnSaveOrder.visibility = View.GONE
        }
        else{
            loadUserData()
        }

        registerOnActivityAutocompleteForResult()

        binding.btnSaveOrder.setOnClickListener {
            addOrder()
        }

        binding.btnEditOrder.setOnClickListener {
            editOrder()
        }

        binding.etOrderLocalization.setOnClickListener {
            findPlace()
        }

        binding.btnCurrentLocation.setOnClickListener {
            if(!isLocationEnabled()){
                Toast.makeText(this@CreateOrderActivity, "Lokalizacja jest wyłączona", Toast.LENGTH_SHORT).show()

                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }else{
                if(isLocationPermissionGranted()){
                    requestNewLocationData()
                }
            }
        }

    }

    private fun editOrder(){
        val title = binding.etOrderTitle.text.toString()
        val description = binding.etOrderDescription.text.toString()
        val location = binding.etOrderLocalization.text.toString()
        val contactName = binding.etOrderContactName.text.toString()
        val telephone = binding.etOrderTelephone.text.toString()
        val price = binding.etOrderPrice.text.toString().toLong()
        val regularType = if(binding.switchType.isChecked) 1 else 0

        if(validateForm(title, description, location, contactName, telephone, category, price, mLatitude, mLongitude)){
            val dao = (application as LocallyApp).db.orderDao()

            lifecycleScope.launch {

                dao.update(OrderEntity(orderId, title, description, mLatitude , mLongitude, location,orderCity, contactName, telephone, mUserId , category, regularType, price, imagePosition = imagePosition))
                Toast.makeText(
                    this@CreateOrderActivity,
                    "Edytowano ogłoszenie",
                    Toast.LENGTH_SHORT
                ).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun loadOrder(){
        val orderDao = (application as LocallyApp).db.orderDao()

        loadSpinner()

        lifecycleScope.launch {

            orderDao.findOrderById(orderId).collect {
                binding.etOrderTitle.setText(it.title)
                binding.spinnerCategory.setSelection(getIndex(binding.spinnerCategory, it.category))
                binding.etOrderDescription.setText(it.description)
                binding.etOrderLocalization.setText(it.location)
                binding.etOrderPrice.setText(it.price.toString())
                binding.etOrderContactName.setText(it.contactName)
                binding.etOrderTelephone.setText(it.contactPhone)
                mLatitude = it.latitude
                mLongitude = it.longitude
                mUserId = it.userId
            }
        }
    }

    private fun getIndex(spinner: Spinner, category: String): Int {
        for(i in 0..spinner.count){
            if(spinner.getItemAtPosition(i).toString() == category)
                return i
        }
        return 0
    }

    private fun findPlace(){
        try{
            val fields = listOf(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
            )

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this@CreateOrderActivity)
            resultLauncher.launch(intent)
        }catch (e: Exception){
            e.printStackTrace()
        }
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

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.numUpdates = 1


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Looper.myLooper()?.let {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                it
            )
        }
    }

    private val mLocationCallback = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation!!
            mLatitude = mLastLocation.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.e("Current Longitude", "$mLongitude")


            val addressTask= GetAddressFromLatLng(this@CreateOrderActivity,lat = mLatitude,lng = mLongitude)
            addressTask.setCustomAddressListener(object : GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String) {
                    binding.etOrderLocalization.setText(address)
                    orderCity = address.split(',')[0]
                }

                override fun onError() {
                    Log.e("Get address:: ", "onError: Something went wrong")
                }

            })

            lifecycleScope.launch(Dispatchers.IO){
                //CoroutineScope tied to this LifecycleOwner's Lifecycle.
                //This scope will be cancelled when the Lifecycle is destroyed
                addressTask.launchBackgroundProcessForRequest()  //starts the task to get the address in text from the lat and lng values
            }

        }
    }



    private fun isLocationEnabled(): Boolean{
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    private fun loadUserData(){

        val dao = (application as LocallyApp).db.userDao()

        val userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

        lifecycleScope.launch {

            dao.findUserByEmail(userLogin!!).collect {
                mUser = it
                binding.etOrderContactName.setText("${mUser.name} ${mUser.lastname}")
                binding.etOrderTelephone.setText("${mUser.telephone}")
            }
        }

        //spinner
        loadSpinner()
    }

    private fun loadSpinner(){
        val dropdown = binding.spinnerCategory

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        dropdown.adapter = adapter
        dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                if(position > 0){
                    category = categories[position]
                    imagePosition = position
                }
            }
        }
    }



    private fun setUpActionBar(){
        val toolbar = binding.toolbarCreateOrder

        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(getDrawable(R.drawable.ic_baseline_arrow_back_ios_24))

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }


    private fun addOrder(){
        val title = binding.etOrderTitle.text.toString()
        val description = binding.etOrderDescription.text.toString()
        val location = binding.etOrderLocalization.text.toString()
        val contactName = binding.etOrderContactName.text.toString()
        val telephone = binding.etOrderTelephone.text.toString()
        val price = binding.etOrderPrice.text.toString().toLong()
        val regularType = if(binding.switchType.isChecked) 1 else 0

        if(validateForm(title, description, location, contactName, telephone, category, price, mLatitude, mLongitude)){
            val dao = (application as LocallyApp).db.orderDao()

            lifecycleScope.launch {

                dao.insert(OrderEntity(0, title, description, mLatitude , mLongitude, location, orderCity, contactName, telephone, mUser.id , category, regularType ,price ,imagePosition = imagePosition))
                Toast.makeText(
                    this@CreateOrderActivity,
                    "Dodano ogłoszenie",
                    Toast.LENGTH_SHORT
                ).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun validateForm(title: String, description:String, city: String, contactName: String, telephone: String, category: String, price: Long, longitude: Double, latitude: Double): Boolean{
        return when {
            TextUtils.isEmpty(title) ->{
                showErrorSnackBar("Podaj tytuł")
                false
            }
            TextUtils.isEmpty(description) ->{
                showErrorSnackBar("Podaj opis")
                false
            }
            TextUtils.isEmpty(city) ->{
                showErrorSnackBar("Podaj lokalizacją")
                false
            }
            TextUtils.isEmpty(contactName) ->{
                showErrorSnackBar("Podaj osobę kontatkową")
                false
            }
            TextUtils.isEmpty(telephone) ->{
                showErrorSnackBar("Podaj telefon")
                false
            }
            TextUtils.isEmpty(category) ->{
                showErrorSnackBar("Podaj kategorię")
                false
            }
            price <= 0 ->{
                showErrorSnackBar("Podaj cenę wyższą niż 0")
                false
            }
            latitude == 0.0 ->{
                showErrorSnackBar("Lokalizacja jest pusta")
                false
            }
            longitude == 0.0 ->{
                showErrorSnackBar("Lokalizacja jest pusta")
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
                binding.etOrderLocalization.setText(place.address)
                val address = place.address.split(',')
                orderCity = address[0]
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }
        }
    }
}