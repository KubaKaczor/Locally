package com.example.locally.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.locally.R
import com.example.locally.adapters.OrdersItemsAdapter
import com.example.locally.database.LocallyApp
import com.example.locally.database.OrderEntity
import com.example.locally.database.UserEntity
import com.example.locally.databinding.ActivityOrdersListBinding
import com.example.locally.utils.Constants
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class OrdersListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersListBinding

    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var updateListResultLauncher: ActivityResultLauncher<Intent>

    private var mActive: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBar()

        registerOnActivityUpdateListForResult()

        mSharedPreferences = this.getSharedPreferences(Constants.LOCALLY_PREFERENCES, Context.MODE_PRIVATE)

        if(intent.hasExtra(Constants.ORDERS_ACTIVE)){
            mActive = intent.getIntExtra(Constants.ORDERS_ACTIVE, 1)
            loadMyOrders(mActive)
        }

        if(intent.hasExtra(Constants.PROFILE_ID)){
            val userId = intent.getIntExtra(Constants.PROFILE_ID, -1)
            loadOrdersOfUser(userId, mActive)
        }

    }

    private fun setUpActionBar(){
        val toolbar = binding.toolbarOrdersList

        if(toolbar != null){
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(getDrawable(R.drawable.ic_baseline_arrow_back_ios_24))

            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun loadMyOrders(active: Int){

        val orderDao = (application as LocallyApp).db.orderDao()
        val userDao = (application as LocallyApp).db.userDao()

        val userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

        lifecycleScope.launch {
            var user: UserEntity? = null

            userDao.findUserByEmail(userLogin!!).collect {
                user = it

                orderDao.fetchOrdersOfUser(user!!.id, active).collect {

                    val ordersList = it

                    if(ordersList.isNotEmpty()) {

                        val adapter = OrdersItemsAdapter(ordersList, this@OrdersListActivity)
                        binding.rvUserOrders.layoutManager = LinearLayoutManager(this@OrdersListActivity)
                        binding.rvUserOrders.adapter = adapter

                        adapter.setOnClickListener(object : OrdersItemsAdapter.OnClickListener{
                            override fun onClick(position: Int, orderId: Int) {
                                val intent = Intent(this@OrdersListActivity, OrderDetailsActivity::class.java)
                                intent.putExtra(Constants.ORDER_DETAILS_ID_EXTRA, orderId)
                                intent.putExtra(Constants.ORDERS_ACTIVE, active)
                                updateListResultLauncher.launch(intent)
                            }
                        })

                        binding.tvNoUserOrders.visibility = View.GONE
                        binding.rvUserOrders.visibility = View.VISIBLE
                    }
                    else{
                        binding.tvNoUserOrders.visibility = View.VISIBLE
                        binding.rvUserOrders.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun loadOrdersOfUser(userId: Int, active: Int){
        val orderDao = (application as LocallyApp).db.orderDao()
        val userDao = (application as LocallyApp).db.userDao()

        val userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

        lifecycleScope.launch {
            var user: UserEntity? = null

            userDao.findUserById(userId!!).collect {
                user = it

                orderDao.fetchOrdersOfUser(user!!.id, active).collect {

                    val ordersList = it

                    if(ordersList.isNotEmpty()) {

                        val adapter = OrdersItemsAdapter(ordersList, this@OrdersListActivity)
                        binding.rvUserOrders.layoutManager = LinearLayoutManager(this@OrdersListActivity)
                        binding.rvUserOrders.adapter = adapter

                        adapter.setOnClickListener(object : OrdersItemsAdapter.OnClickListener{
                            override fun onClick(position: Int, orderId: Int) {
                                val intent = Intent(this@OrdersListActivity, OrderDetailsActivity::class.java)
                                intent.putExtra(Constants.ORDER_DETAILS_ID_EXTRA, orderId)
                                intent.putExtra(Constants.ORDERS_ACTIVE, active)
                                updateListResultLauncher.launch(intent)
                            }
                        })

                        binding.tvNoUserOrders.visibility = View.GONE
                        binding.rvUserOrders.visibility = View.VISIBLE
                    }
                    else{
                        binding.tvNoUserOrders.visibility = View.VISIBLE
                        binding.rvUserOrders.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun registerOnActivityUpdateListForResult(){
        updateListResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadMyOrders(mActive)
            }
        }
    }
}