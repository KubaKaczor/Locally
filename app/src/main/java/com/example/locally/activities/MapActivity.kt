package com.example.locally.activities

import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.locally.R
import com.example.locally.database.LocallyApp
import com.example.locally.database.OrderEntity
import com.example.locally.databinding.ActivityMapBinding
import com.example.locally.utils.Constants
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private var mOrderDetails: OrderEntity? = null

    private var startLatitude: Double = 0.0
    private var startLongitude: Double = 0.0

    private var ordersListNearby: ArrayList<OrderEntity> = ArrayList()

    private var singleOrder: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(intent.hasExtra(Constants.ORDER_DETAILS_ID_EXTRA)){
            val id = intent.getIntExtra(Constants.ORDER_DETAILS_ID_EXTRA, 0)
            loadOrderDetails(id)
        }
        else if(intent.hasExtra(Constants.USER_LOGGED)){
            val user = intent.getStringExtra(Constants.USER_LOGGED)
            val userDao = (application as LocallyApp).db.userDao()

            singleOrder = false

            lifecycleScope.launch {

                userDao.findUserByEmail(user!!).collect {
                    startLatitude = it.cityLatitude
                    startLongitude = it.cityLongitude
                    showOrdersNearby(it.cityLatitude, it.cityLongitude)
                }
            }
        }
    }

    private fun setUpActionBarAndFragment(){

            setSupportActionBar(binding.toolbarMap)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            if(singleOrder)
                supportActionBar!!.title = mOrderDetails!!.location

            binding.toolbarMap.setNavigationOnClickListener{
                onBackPressed()
            }

            val supportMapFragment : SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

            supportMapFragment.getMapAsync(this)

    }

    private fun loadOrderDetails(id: Int){
        val orderDao = (application as LocallyApp).db.orderDao()

        lifecycleScope.launch {

            orderDao.findOrderById(id).collect {
                mOrderDetails = it
                setUpActionBarAndFragment()
            }
        }
    }

    private fun showOrdersNearby(startLatitude: Double, startLongitude: Double){

        val orderDao = (application as LocallyApp).db.orderDao()
        lifecycleScope.launch {
            orderDao.fetchAllOrders().collect {

                val ordersList = it

                if (ordersList.isNotEmpty()) {

                    for (order in ordersList) {
                        val currentLocation = Location("currentLocation")

                        Log.d("MainActivity", "tworzenie orders list")

                        currentLocation.setLatitude(startLatitude)
                        currentLocation.setLongitude(startLongitude)

                        val orderLocation = Location("orderLocation")

                        orderLocation.setLatitude(order.latitude)
                        orderLocation.setLongitude(order.longitude)

                        val distance: Float = currentLocation.distanceTo(orderLocation)

                        if (distance < 50000.0) {
                            ordersListNearby.add(order)
                        }
                    }
                }
                Log.d("map", "${ordersListNearby.size}")
                Log.d("map", "$startLatitude  $startLongitude")
                setUpActionBarAndFragment()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if(singleOrder) {
            val position = LatLng(mOrderDetails!!.latitude, mOrderDetails!!.longitude)
            googleMap.addMarker(MarkerOptions().position(position).title(mOrderDetails!!.location))

            val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 10f)
            googleMap.animateCamera(newLatLngZoom)
        }
        else{
            if(ordersListNearby.isNotEmpty()){
                for(order in ordersListNearby){
                    val position = LatLng(order.latitude, order.longitude)
                    googleMap.addMarker(MarkerOptions().position(position).title(order.location))
                }

                val currentPosition = LatLng(startLatitude, startLongitude)
                val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(currentPosition, 10f)
                googleMap.animateCamera(newLatLngZoom)
            }
        }
    }
}