package com.hellostranger.chess_app.activities

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.hellostranger.chess_app.MyApp
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.TokenManager
import com.hellostranger.chess_app.databinding.ActivityUpdateProfileBinding
import com.hellostranger.chess_app.dto.UpdateRequest
import com.hellostranger.chess_app.models.User
import com.hellostranger.chess_app.retrofit.general.GeneralRetrofitClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

class UpdateProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityUpdateProfileBinding

    private var mSelectedImageFileUri : Uri? = null
    private var mProfileImageURL : String = ""
    private var tokenManager : TokenManager = MyApp.tokenManager

    private lateinit var currentUser : User


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBar()
        currentUser = intent.getParcelableExtra("USER")!!
        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
            throwable.printStackTrace()
        }
        GlobalScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val response =
                GeneralRetrofitClient.instance.getUserByEmail(tokenManager.getUserEmail())
            if (response.isSuccessful && response.body() != null) {
            }
        }
        setUserDataInUI()
        binding.ivUserImage.setOnClickListener {
            requestStoragePermission()
            if(mSelectedImageFileUri != null){
                uploadUserImage()
            }
        }
        binding.etEmail.isEnabled = false

        binding.btnUpdate.setOnClickListener {
            if(mSelectedImageFileUri != null){
                uploadUserImage()
            }
            updateUserData()
        }
    }

    private fun setUserDataInUI(){
        Glide
            .with(this@UpdateProfileActivity)
            .load(currentUser.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivUserImage)

        binding.etName.setText(currentUser.name)
        binding.etEmail.setText(currentUser.email)
    }

    private fun updateUserData(){


        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
            throwable.printStackTrace()
        }
        if(binding.etName.text!!.isNotEmpty() && binding.etName.text!!.toString() != currentUser.name){
            GlobalScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
                val response = GeneralRetrofitClient.instance.updateUserName(tokenManager.getUserEmail(), UpdateRequest(binding.etName.text!!.toString()))
                if(response.isSuccessful && response.body() != null){
                    Log.i("SaveData", "Saved name to the backend server. response is: ${response.body()}")
                }else{
                    Log.e("SaveData", "Failed. response is: $response , ${response.body()}")
                }
            }

        }
        Log.i("TAG", "mProfileImageURL: $mProfileImageURL and userimage: ${currentUser.image}")
        if(mProfileImageURL != currentUser.image){
            GlobalScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
                Log.i("TAG", mProfileImageURL)
                val response = GeneralRetrofitClient.instance.uploadProfileImage(tokenManager.getUserEmail(), UpdateRequest(mProfileImageURL))
                if(response.isSuccessful && response.body() != null){
                    Log.i("SaveData", "Saved name to the backend server. response is: ${response.body()}")
                }else{
                    Log.e("SaveData", "Failed. response is: $response , ${response.body()}")
                }
            }

        }

        Toast.makeText(
            this@UpdateProfileActivity,
            "Saved updated Information",
            Toast.LENGTH_LONG
        ).show()

    }
    private val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
            if(result.resultCode == RESULT_OK && result.data!= null){
                mSelectedImageFileUri = result.data!!.data
                try{
                    Glide
                        .with(this@UpdateProfileActivity)
                        .load(mSelectedImageFileUri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_place_holder)
                        .into(binding.ivUserImage)
                    uploadUserImage()
                } catch (e : IOException){
                    e.printStackTrace()
                }

            }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val perMissionName = it.key
                val isGranted = it.value

                if (isGranted ) {
                    Toast.makeText(
                        this@UpdateProfileActivity,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_LONG
                    ).show()

                    val pickIntent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)

                } else {
                    if (perMissionName == Manifest.permission.READ_MEDIA_IMAGES)
                        Toast.makeText(
                            this@UpdateProfileActivity,
                            "Oops you just denied the permission.",
                            Toast.LENGTH_LONG
                        ).show()
                }
            }
        }

    private fun requestStoragePermission(){
        val neededPermission = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, neededPermission)
        ){
            showRationaleDialog("Chess App","Chess App " + "needs to Access Your External Storage")
        }
        else {
            // You can directly ask for the permission.
            requestPermission.launch(arrayOf(neededPermission,))
        }
    }
    private fun setUpActionBar(){
        val toolbarUpdateProfileActivity = binding.toolbarMyProfileActivity
        setSupportActionBar(toolbarUpdateProfileActivity)
        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_arrow_back)
            actionBar.title = resources.getString(R.string.my_profile)
        }

        toolbarUpdateProfileActivity.setNavigationOnClickListener { onBackPressed() }
    }


    private fun uploadUserImage(){
        showProgressDialog("Please wait...")
        if(mSelectedImageFileUri != null){
            val sRef : StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE" + System.currentTimeMillis() + "." + getFileExtension(mSelectedImageFileUri)
            )
            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->
                Log.i(
                    "Firebase Image URL",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                    Log.i("Downloadable Image URL", uri.toString())
                    mProfileImageURL = uri.toString()

                    hideProgressDialog()
                }
            }.addOnFailureListener{
                exception ->
                Toast.makeText(
                    this@UpdateProfileActivity,
                    exception.message,
                    Toast.LENGTH_LONG
                ).show()
                hideProgressDialog()
            }
        }
    }

    private fun getFileExtension(uri : Uri?) : String?{
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

}