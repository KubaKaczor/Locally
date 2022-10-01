package com.example.locally.activities

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.SyncStateContract
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.locally.R
import com.example.locally.adapters.OpinionsItemsAdapter
import com.example.locally.database.LocallyApp
import com.example.locally.database.OpinionEntity
import com.example.locally.database.OrderEntity
import com.example.locally.databinding.ActivityOpinionsBinding
import com.example.locally.databinding.OpinionDialogBinding
import com.example.locally.utils.Constants
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class OpinionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOpinionsBinding

    private var userId: Int = -1

    private lateinit var mSharedPreferences: SharedPreferences

    private var disableOpinion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpinionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBar()

        mSharedPreferences = this.getSharedPreferences(Constants.LOCALLY_PREFERENCES, Context.MODE_PRIVATE)

        if(intent.hasExtra(Constants.PROFILE_ID)){
            userId = intent.getIntExtra(Constants.PROFILE_ID, -1)
            loadOpinions()
            checkUser()
        }
        if(userId == -1){
            disableOpinion = true
            invalidateOptionsMenu()
        }
    }

    private fun setUpActionBar(){
        val toolbar = binding.toolbarOpinions

        if(toolbar != null){
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(getDrawable(R.drawable.ic_baseline_arrow_back_ios_24))

            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun checkUser(){
        val userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

        val userDao = (application as LocallyApp).db.userDao()

        lifecycleScope.launch {
            userDao.findUserByEmail(userLogin!!).collect{
                if(it.id == userId) {
                    disableOpinion = true
                    invalidateOptionsMenu()
                }
            }
        }
    }

    private fun loadOpinions(){

        val opinionDao = (application as LocallyApp).db.opinionDao()

        lifecycleScope.launch {

            opinionDao.getOpinionsOfUser(userId).collect{

                val opinionsList = it

                if(opinionsList.isNotEmpty()){
                    binding.rvUserOrders.layoutManager = LinearLayoutManager(this@OpinionsActivity)
                    val adapter = OpinionsItemsAdapter(opinionsList, this@OpinionsActivity)
                    binding.rvUserOrders.adapter = adapter
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.opinion_menu, menu)

        if(disableOpinion){
            menu.getItem(0).setVisible(false)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.opinion_menu_item ->{
                customDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun customDialog(){
        val dialog = Dialog(this)
        val binding = OpinionDialogBinding.inflate(layoutInflater)
        //dialog.setContentView(binding.root)
        dialog.setContentView(binding.root)

        //val tvAdd = dialog.findViewById<TextView>(R.id.btn_give_opinion)
        //val tvCancel = dialog.findViewById<TextView>(R.id.btn_opinion_dismiss)
        binding.btnGiveOpinion.setOnClickListener {
            addOpinion(binding, dialog)
        }

        binding.btnOpinionDismiss.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addOpinion(binding: OpinionDialogBinding, dialog: Dialog){

        val rate = binding.rbOpinion.rating.toDouble()
        val description = binding.etOpinionDescription.text.toString()

        if(validateForm(rate, description)){

            val opinionDao = (application as LocallyApp).db.opinionDao()
            val userDao = (application as LocallyApp).db.userDao()

            val userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

            lifecycleScope.launch {

                userDao.findUserByEmail(userLogin!!).collect{

                    val opinion = OpinionEntity(userRatedId = userId, userRatingName = it.email, rate = rate, description = description, imageOfUser = it.image)

                    opinionDao.insert(opinion)

                    Toast.makeText(
                        this@OpinionsActivity,
                        "Dodano opinię",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    //setResult(Activity.RESULT_OK)
                }

            }
        }
    }

    private fun validateForm(rate: Double, description: String): Boolean{
        return when {
            TextUtils.isEmpty(description) ->{
                showErrorSnackBar("Podaj uzasadnienie")
                false
            }
            rate == 0.0 ->{
                showErrorSnackBar("Podaj ocenę")
                false
            }
            else -> {
                true
            }
        }
    }
    private fun showErrorSnackBar(message: String){
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.snackbar_error_color))

        snackBar.show()
    }
}