package com.example.locally.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.locally.R
import com.example.locally.adapters.OrdersItemsAdapter
import com.example.locally.database.LocallyApp
import com.example.locally.database.OrderEntity
import com.example.locally.database.UserEntity
import com.example.locally.databinding.ActivityMainBinding
import com.example.locally.databinding.NavHeaderMainBinding
import com.example.locally.utils.Constants
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    //private var mUser : UserEntity? = null

    private var doubleBackToExitPressedOnce = false

    private lateinit var updateListResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var updateProfileResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var adapter: OrdersItemsAdapter

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    var startLongitude: Double = 0.0
    var startLatitude: Double = 0.0

    private var settingsDistanceItem = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBar()

        mSharedPreferences = this.getSharedPreferences(Constants.LOCALLY_PREFERENCES, Context.MODE_PRIVATE)

        if(!Places.isInitialized()){
            Places.initialize(this@MainActivity, resources.getString(R.string.google_maps_api_key))
        }

        settingsDistanceItem = mSharedPreferences.getInt(Constants.SHOW_ORDERS_DISTANCE, 1)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)

        loadUserData()
        loadOrders()

        registerOnActivityUpdateListForResult()
        registerOnActivityUpdateProfileForResult()
        registerOnActivityAutocompleteForResult()

        binding.navView.setNavigationItemSelectedListener(this)

        binding.appBar.fabCreateOrder.setOnClickListener {
            val intent = Intent(this, CreateOrderActivity::class.java)
            updateListResultLauncher.launch(intent)
        }

        binding.appBar.fabMap.setOnClickListener {
            val user = mSharedPreferences.getString(Constants.USER_LOGGED, "")
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra(Constants.USER_LOGGED, user)
            startActivity(intent)
        }

        binding.appBar.fabHome.setOnClickListener {
            binding.appBar.toolbarMainActivity.title = "Locally"
            loadOrders()
            it.visibility = View.GONE
        }

        //testy tabLayout
        binding.appBar.mainContent.tabLayout.addOnTabSelectedListener(object:
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab!!.position

                when(position){
                    0 ->{
                        loadOrders()
                    }
                    1 ->{
                        loadOrders(regularType = 1)
                    }
                }

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })
    }

    override fun onResume() {
        if(settingsDistanceItem != mSharedPreferences.getInt(Constants.SHOW_ORDERS_DISTANCE, 1))
            loadOrders()
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_search_item ->{
                findPlace()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun findPlace(){
        try{
            val fields = listOf(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
            )

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this@MainActivity)
            resultLauncher.launch(intent)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun registerOnActivityAutocompleteForResult(){
        //returns: the launcher that can be used to start the activity or dispose of the prepared call.
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val place: Place = Autocomplete.getPlaceFromIntent(result.data!!)
                binding.appBar.toolbarMainActivity.title = place.address
                startLatitude = place.latLng!!.latitude
                startLongitude = place.latLng!!.longitude
                binding.appBar.fabHome.visibility = View.VISIBLE
                loadOrders(true)
            }
        }
    }

    private fun getLastKnownLocation(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    Log.d("MainActivity", "fused location listener poczatek")
                    startLatitude = location!!.latitude
                    startLongitude = location!!.longitude
                    Log.d("MainActivity", "fused location listener koniec")
                    Log.d(
                        "MainActivity",
                        "latitude: ${startLatitude}, longitude: ${startLongitude}"
                    )
                }
            return
        }
    }

    private fun registerOnActivityUpdateListForResult(){
        updateListResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadOrders()
            }
        }
    }

    private fun registerOnActivityUpdateProfileForResult(){
        updateProfileResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadUserData()
            }
        }
    }

    private fun loadUserData(){

        val userDao = (application as LocallyApp).db.userDao()

        val userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

        lifecycleScope.launch {
            userDao.findUserByEmail(userLogin!!).collect {
                //mUser = it

                val headerView = binding.navView.getHeaderView(0)
                val headerBinding = NavHeaderMainBinding.bind(headerView)

                headerBinding.tvUsername.text = "${it.name} ${it.lastname}"

                Glide
                    .with(this@MainActivity)
                    .load(it.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(headerBinding.profileItemPhoto)

            }
        }
    }

    private fun loadOrders(search: Boolean = false, regularType: Int = 0){

        val orderDao = (application as LocallyApp).db.orderDao()
        val userDao = (application as LocallyApp).db.userDao()

        val userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

        lifecycleScope.launch {
            var user: UserEntity? = null

            userDao.findUserByEmail(userLogin!!).collect {
                user = it

                orderDao.fetchOrdersByType(regularType).collect {

                    val ordersList = it

                    val ordersInRange = ArrayList<OrderEntity>()

                    if(!search){
                        if (user!!.settings.ordersByLocation == 0) {
                            startLongitude = user!!.cityLongitude
                            startLatitude = user!!.cityLatitude
                        } else {
                            //requestNewLocationData()
                            getLastKnownLocation()
                            Log.d("MainActivity", "inicjalizacja fusedLocation")
                        }
                    }

                    if(ordersList.isNotEmpty()) {


                        for (order in ordersList) {
                            val currentLocation = Location("currentLocation")

                            Log.d("MainActivity", "tworzenie orders list")

                            currentLocation.setLatitude(startLatitude)
                            currentLocation.setLongitude(startLongitude)

                            val orderLocation = Location("orderLocation")

                            orderLocation.setLatitude(order.latitude)
                            orderLocation.setLongitude(order.longitude)

                            val distance: Float = currentLocation.distanceTo(orderLocation)

                            val settingsDistance = mSharedPreferences.getInt(Constants.SHOW_ORDERS_DISTANCE, 1)
                            val distanceInKilometers = Constants.distanceSettings[settingsDistance]
                            val distanceInMetres = (distanceInKilometers * 1000).toDouble()

                            if (distance < distanceInMetres) {
//                                Toast.makeText(this@MainActivity, "$distance", Toast.LENGTH_SHORT)
//                                    .show()
                                ordersInRange.add(order)
                            }
                        }
                    }
                    if(ordersInRange.isNotEmpty()){
                        adapter = OrdersItemsAdapter(ordersInRange, this@MainActivity)
                        binding.appBar.mainContent.rvOrders.layoutManager = LinearLayoutManager(this@MainActivity)
                        binding.appBar.mainContent.rvOrders.adapter = adapter

                        adapter.setOnClickListener(object : OrdersItemsAdapter.OnClickListener{
                            override fun onClick(position: Int, orderId: Int) {
                                val intent = Intent(this@MainActivity, OrderDetailsActivity::class.java)
                                intent.putExtra(Constants.ORDER_DETAILS_ID_EXTRA, orderId)
                                startActivity(intent)
                            }
                        })

                        binding.appBar.mainContent.tvNoOrders.visibility = View.GONE
                        binding.appBar.mainContent.rvOrders.visibility = View.VISIBLE
                    }
                    else{
                        binding.appBar.mainContent.tvNoOrders.visibility = View.VISIBLE
                        binding.appBar.mainContent.rvOrders.visibility = View.GONE
                    }
                }
            }
        }
    }


    private fun setUpActionBar(){
        val toolbar = binding.appBar.toolbarMainActivity

        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(getDrawable(R.drawable.ic_baseline_menu_24))

        toolbar.setNavigationOnClickListener {
            toggleNavigationDrawer()
        }
    }

    private fun toggleNavigationDrawer(){
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }else{
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }else{
            doubleBackToExit()
        }
    }

    fun doubleBackToExit(){
        if(doubleBackToExitPressedOnce){
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(
            this,
            resources.getString(R.string.please_click_back_again_to_exit),
            Toast.LENGTH_SHORT
        ).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_my_profile ->{
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)

            }
            R.id.nav_my_orders ->{
                val intent = Intent(this, OrdersListActivity::class.java)
                intent.putExtra(Constants.ORDERS_ACTIVE, 1)
                startActivity(intent)

            }
            R.id.nav_orders_completed ->{
                val intent = Intent(this, OrdersListActivity::class.java)
                intent.putExtra(Constants.ORDERS_ACTIVE, 0)
                startActivity(intent)
            }
            R.id.nav_settings ->{
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_sign_out ->{

                mSharedPreferences.edit().clear().apply()

                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    private val mLocationCallback = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            Log.d("MainActivity", "poczatek callback")
            val mLastLocation: Location = locationResult.lastLocation!!
            startLatitude = mLastLocation.latitude
            startLongitude = mLastLocation.longitude
            Log.d("MainActivity", "koniec callback")

        }
    }




}