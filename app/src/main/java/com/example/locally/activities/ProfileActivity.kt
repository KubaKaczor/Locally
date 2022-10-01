package com.example.locally.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.locally.R
import com.example.locally.database.LocallyApp
import com.example.locally.databinding.ActivityProfileBinding
import com.example.locally.databinding.NavHeaderMainBinding
import com.example.locally.utils.Constants
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private var menuState: Boolean = true

    private lateinit var mSharedPreferences: SharedPreferences

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mSharedPreferences = this.getSharedPreferences(Constants.LOCALLY_PREFERENCES, Context.MODE_PRIVATE)

        if(intent.hasExtra(Constants.PROFILE_ID)){
            menuState = false
            invalidateOptionsMenu()
            userId = intent.getIntExtra(Constants.PROFILE_ID, 0)
            loadUserProfile(userId)
        }else{
            loadOwnProfile()
        }

        binding.btnActiveOrders.setOnClickListener {
            val intent = Intent(this, OrdersListActivity::class.java)
            if(userId > -1)
                intent.putExtra(Constants.PROFILE_ID, userId)
            startActivity(intent)
        }

        binding.btnOpinions.setOnClickListener {
            val intent = Intent(this, OpinionsActivity::class.java)
            intent.putExtra(Constants.PROFILE_ID, userId)
            startActivity(intent)
        }


        setUpActionBar()
        binding.rating.setRating(4F)
    }

    private fun setUpActionBar(){
        val toolbar = binding.toolbarProfile

        if(toolbar != null){
            if(!menuState){
                toolbar.title = "Profil użytkownika"
            }
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(getDrawable(R.drawable.ic_baseline_arrow_back_ios_24))

            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        if(!menuState){
            val item = menu.getItem(0)
            item.setVisible(false)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menuEdit ->{
                val intent = Intent(this@ProfileActivity, EditProfileActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadOwnProfile(){
        val userDao = (application as LocallyApp).db.userDao()
        val orderDao = (application as LocallyApp).db.orderDao()
        val opinionDao = (application as LocallyApp).db.opinionDao()

        val userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

        lifecycleScope.launch {
            userDao.findUserByEmail(userLogin!!).collect {

                Glide
                    .with(this@ProfileActivity)
                    .load(it.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(binding.ivImage)

                userId = it.id
                binding.tvName.text = "${it.name} ${it.lastname}"
                binding.tvEmail.text = it.email
                binding.tvTelephone.text = it.telephone
                binding.tvCity.text = it.city

                orderDao.getValueOfActiveOrders(it.id).collect{ aktywne->
                    binding.tvActiveOrders.setText(aktywne.toString())

                    orderDao.getValueOfCompletedOrders(it.id).collect{ liczba->
                        binding.tvCompletedOrders.setText(liczba.toString())

                        opinionDao.getNumberOfOpinions(it.id).collect{ liczbaOpinii->
                            binding.tvOpinions.setText(liczbaOpinii.toString())

                            opinionDao.getRateOfUser(it.id).collect{ rate->
                                //Log.d("rate", "$rate")
                                binding.rating.rating = rate.toFloat()
                            }
                        }
                    }
                }


            }

        }
    }

    private fun loadUserProfile(id: Int){
        val userDao = (application as LocallyApp).db.userDao()
        val orderDao = (application as LocallyApp).db.orderDao()
        val opinionDao = (application as LocallyApp).db.opinionDao()

        lifecycleScope.launch {
            userDao.findUserById(id).collect {

                Glide
                    .with(this@ProfileActivity)
                    .load(it.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(binding.ivImage)

                binding.tvName.text = "${it.name} ${it.lastname}"
                binding.tvEmail.text = it.email
                binding.tvTelephone.text = it.telephone
                binding.tvCity.text = it.city

                orderDao.getValueOfCompletedOrders(id).collect{ aktywne->
                    binding.tvCompletedOrders.setText(aktywne.toString())

                    orderDao.getValueOfActiveOrders(id).collect{ liczba->
                        binding.tvActiveOrders.setText(liczba.toString())

                        opinionDao.getNumberOfOpinions(it.id).collect{ liczbaOpinii->
                            binding.tvOpinions.setText(liczbaOpinii.toString())

                            opinionDao.getRateOfUser(it.id).collect{ rate->
                                //Log.d("rate", "$rate")
                                binding.rating.rating = rate.toFloat()
                            }
                        }
                    }
                }


            }
        }
    }

    override fun onResume() {
        if(userId != -1)
            loadUserProfile(userId)
        super.onResume()
    }
}