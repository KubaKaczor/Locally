package com.example.locally.activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.locally.R
import com.example.locally.adapters.OrdersItemsAdapter
import com.example.locally.database.LocallyApp
import com.example.locally.database.OrderDao
import com.example.locally.database.OrderEntity
import com.example.locally.databinding.ActivityOrderDetailsBinding
import com.example.locally.utils.Constants
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import kotlinx.coroutines.launch

class OrderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailsBinding

    private lateinit var mOrder: OrderEntity

    private var mActive: Boolean = false

    private lateinit var orderDao: OrderDao

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private var orderId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderDao = (application as LocallyApp).db.orderDao()

        setUpActionBar()

        registerOnActivityEditForResult()

        if(intent.hasExtra(Constants.ORDER_DETAILS_ID_EXTRA)){
            orderId = intent.getIntExtra(Constants.ORDER_DETAILS_ID_EXTRA, 0)
            loadOrderDetails(orderId)
        }

        if(intent.hasExtra(Constants.ORDERS_ACTIVE)){
            val active = intent.getIntExtra(Constants.ORDERS_ACTIVE, 0)
            if(active == 1){
                mActive = true
                invalidateOptionsMenu()
            }

        }

        binding.btnOrderDetailsLocation.setOnClickListener{
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra(Constants.ORDER_DETAILS_ID_EXTRA, orderId)
            startActivity(intent)
        }

        binding.btnContactProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra(Constants.PROFILE_ID, mOrder.userId)
            startActivity(intent)
        }
    }

    private fun loadOrderDetails(id: Int){

        //val orderDao = (application as LocallyApp).db.orderDao()
        val userDao = (application as LocallyApp).db.userDao()

        lifecycleScope.launch {

            orderDao.findOrderById(id).collect {
                mOrder = it

                val image = when(mOrder.imagePosition){
                    1 -> R.drawable.ic_baseline_directions_car_24
                    2 -> R.drawable.ic_baseline_home_24
                    3 -> R.drawable.ic_baseline_shopping_basket_24
                    4 -> R.drawable.ic_baseline_people_24
                    5 -> R.drawable.ic_baseline_all_inclusive_24
                    else -> R.drawable.ic_user_place_holder

                }

                Glide
                    .with(this@OrderDetailsActivity)
                    .load(image)
                    .centerCrop()
                    .placeholder(image)
                    .into(binding.ivOrderDetailsImage)

                binding.tvOrderDetailsTitle.text = mOrder.title
                binding.tvOrderDetailsDescription.text = mOrder.description
                binding.tvOrderDetailsCategory.text = mOrder.category
                binding.tvOrderDetailsPrice.text = "${mOrder.price} zł"
                binding.tvOrderDetailsCity.text = mOrder.location
                binding.tvContactName.text = mOrder.contactName
                binding.tvContactTelephone.text = mOrder.contactPhone
                if(it.Type == 1) {
                    binding.tvType.text = "zlecenie regularne"
                }

                userDao.findUserById(mOrder!!.userId).collect{
                    binding.orderContactName.text = "${it.name} ${it.lastname}"
                    binding.orderContactEmail.text = it.email

                    Glide
                        .with(this@OrderDetailsActivity)
                        .load(it.image)
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_place_holder)
                        .into(binding.orderContactPhoto)
                }
            }
        }

    }

    private fun setUpActionBar(){
        val toolbar = binding.toolbarOrderDetails

        if(toolbar != null){
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(getDrawable(R.drawable.ic_baseline_arrow_back_ios_24))

            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.order_details_menu, menu)
        if(mActive){
            menu.getItem(0).setVisible(true)
            menu.getItem(1).setVisible(true)
            menu.getItem(2).setVisible(true)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.order_menu_finish ->{
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Na pewno chcesz zakończyć zlecenie?")
                    .setPositiveButton("Tak",
                        DialogInterface.OnClickListener { _, _ ->
                            lifecycleScope.launch {
                                val updatedOrder = mOrder
                                updatedOrder.active = 0
                                orderDao.update(updatedOrder)
                                setResult(RESULT_OK)
                                finish()
                                Toast.makeText(this@OrderDetailsActivity, "Zakończono zlecenie", Toast.LENGTH_SHORT).show()
                            }
                        })
                    .setNegativeButton("Nie",
                        DialogInterface.OnClickListener { dialog, _ ->
                            dialog.dismiss()
                        })
                val alertDialog = builder.create()
                alertDialog.show()
                return true
            }
            R.id.order_menu_edit ->{
                val intent = Intent(this, CreateOrderActivity::class.java)
                intent.putExtra(Constants.ORDER_DETAILS_ID_EXTRA, mOrder.id)
                resultLauncher.launch(intent)
                return true
            }
            R.id.order_menu_delete ->{
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Na pewno chcesz usunąć zlecenie?")
                    .setPositiveButton("Tak",
                        DialogInterface.OnClickListener { _, _ ->
                            lifecycleScope.launch {
                                orderDao.delete(mOrder)
                                setResult(RESULT_OK)
                                finish()
                                Toast.makeText(this@OrderDetailsActivity, "Usunięto zlecenie", Toast.LENGTH_SHORT).show()
                            }
                        })
                    .setNegativeButton("Nie",
                        DialogInterface.OnClickListener { dialog, _ ->
                            dialog.dismiss()
                        })
                val alertDialog = builder.create()
                alertDialog.show()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun registerOnActivityEditForResult(){
        //returns: the launcher that can be used to start the activity or dispose of the prepared call.
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadOrderDetails(orderId)
            }
        }
    }
}