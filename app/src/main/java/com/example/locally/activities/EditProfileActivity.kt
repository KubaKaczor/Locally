package com.example.locally.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.PermissionRequest
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.locally.R
import com.example.locally.database.LocallyApp
import com.example.locally.database.UserEntity
import com.example.locally.databinding.ActivityEditProfileBinding
import com.example.locally.utils.Constants
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    private var mUser: UserEntity? = null

    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var galleryImageResultLauncher: ActivityResultLauncher<Intent>

    private var saveImageToInternalStorage : Uri? = null

    var selectedImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBar()

        mSharedPreferences = this.getSharedPreferences(Constants.LOCALLY_PREFERENCES, Context.MODE_PRIVATE)

        registerOnActivityGalleryForResult()
        loadUserData()

        binding.ivProfilePhoto.setOnClickListener {
            choosePhotoFromGallery()
        }

    }

    private fun setUpActionBar(){
        val toolbar = binding.editProfileToolbar

        if(toolbar != null){
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(getDrawable(R.drawable.ic_baseline_arrow_back_ios_24))

            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun loadUserData(){

        val userDao = (application as LocallyApp).db.userDao()

        val userLogin = mSharedPreferences.getString(Constants.USER_LOGGED, "")

        lifecycleScope.launch {
            userDao.findUserByEmail(userLogin!!).collect {
                mUser = it

                Glide
                    .with(this@EditProfileActivity)
                    .load(mUser!!.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(binding.ivProfilePhoto)

                binding.etProfileName.setText(mUser!!.name)
                binding.etProfileLastname.setText(mUser!!.lastname)
                binding.etProfileTelephone.setText(mUser!!.telephone)
                binding.etProfileCity.setText(mUser!!.city)
                //binding.etProfileEmail.setText(mUser!!.email)
                //binding.ivProfilePhoto.setImageURI(Uri.parse(mUser?.image))
                binding.btnUpdateProfile.setOnClickListener {
                    updateProfile()
                }

            }
        }
    }

    private fun updateProfile(){

        if(selectedImageBitmap != null)
            saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap!!)

        val name = binding.etProfileName.text.toString()
        val lastName = binding.etProfileLastname.text.toString()
        val telephone = binding.etProfileTelephone.text.toString()
        val city = binding.etProfileCity.text.toString()
        val imageUri = saveImageToInternalStorage.toString()
        //val email = binding.etProfileEmail.text.toString()

        val updatedUser = UserEntity(mUser!!.id,name,lastName,telephone,city,mUser!!.cityLatitude, mUser!!.cityLongitude, mUser!!.email, mUser!!.password, imageUri,settings = mUser!!.settings)

        val dao = (application as LocallyApp).db.userDao()

        lifecycleScope.launch(Dispatchers.IO) {

            dao.update(updatedUser)
            setResult(Activity.RESULT_OK)
            finish()
        }
        Toast.makeText(
            this@EditProfileActivity,
            "Zaktualizowano profil",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun choosePhotoFromGallery(){
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object:
                MultiplePermissionsListener {
                override fun onPermissionsChecked(report : MultiplePermissionsReport?)
                {
                    if(report!!.areAllPermissionsGranted()){
                        val galleryIntent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        galleryImageResultLauncher.launch(galleryIntent)
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                    token: PermissionToken?
                )
                {
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
    }

    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setTitle("It looks like you've turned off permissions required for this feature." +
                " It can be enabled under the Application Settings")
            .setPositiveButton("GO TO SETTINGS"){
                    _, _ ->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("CANCEL"){
                    dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun registerOnActivityGalleryForResult(){
        //returns: the launcher that can be used to start the activity or dispose of the prepared call.
        galleryImageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                if(data!=null){
                    val contentUri=data.data
                    try{
                        selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,contentUri)
//                        binding.selectedImageImageView.setImageBitmap(selectedImageBitmap)
                        //OR

                        //saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap!!)
                        //Log.e("Saved image:", "Path: ${saveImageToInternalStorage}")

                        //binding.ivProfilePhoto.setImageURI(contentUri)

                        Glide
                            .with(this@EditProfileActivity)
                            .load(contentUri)
                            .centerCrop()
                            .placeholder(R.drawable.ic_user_place_holder)
                            .into(binding.ivProfilePhoto)
                    }
                    catch (e: IOException){
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir("LocallyImages", Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try{
            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        }catch (e: IOException){
            e.printStackTrace()
        }

        return Uri.parse(file.absolutePath)

    }
}