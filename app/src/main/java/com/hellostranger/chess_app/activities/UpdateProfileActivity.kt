package com.hellostranger.chess_app.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
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
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.utils.TokenManager
import com.hellostranger.chess_app.databinding.ActivityUpdateProfileBinding
import com.hellostranger.chess_app.dto.requests.UpdateRequest
import com.hellostranger.chess_app.models.entities.User
import com.hellostranger.chess_app.network.retrofit.backend.BackendRetrofitClient
import com.hellostranger.chess_app.utils.Constants
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
/**
 * Activity for updating user profile information.
 */
@ExperimentalUnsignedTypes
class UpdateProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityUpdateProfileBinding // Binding for the UI

    private var mSelectedImageFileUri : Uri? = null // The image URI
    private var mProfileImageURL : String = "" // The URL for the image
    private var tokenManager : TokenManager = MyApp.tokenManager // Manages tokens and user email

    private lateinit var currentUser : User // The current user

    /**
     * Called when the activity is first created. Initializes the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpActionBar()

        currentUser = if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.USER, User::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Constants.USER)!!
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

    /**
     * Sets the current user data in the UI components.
     */
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

    /**
     * Updates the user data by sending requests to the backend.
     */
    private fun updateUserData(){
        // Update name
        if(binding.etName.text!!.isNotEmpty() && binding.etName.text!!.toString() != currentUser.name){
            CoroutineScope(Dispatchers.IO).launch {
                BackendRetrofitClient.instance.updateUserName(tokenManager.getUserEmail(), UpdateRequest(binding.etName.text!!.toString()))
            }

        }
        // Update picture
        if(mProfileImageURL != currentUser.image){
            CoroutineScope(Dispatchers.IO).launch {
                BackendRetrofitClient.instance.uploadProfileImage(tokenManager.getUserEmail(), UpdateRequest(mProfileImageURL))
            }

        }

        Toast.makeText(
            this@UpdateProfileActivity,
            "Saved updated Information",
            Toast.LENGTH_LONG
        ).show()

    }

    /**
     * Launcher for opening the gallery to pick an image.
     */
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

    /**
     * Request permissions for accessing storage.
     */
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val perMissionName = it.key
                val isGranted = it.value

                if (isGranted ) {
                    Toast.makeText(
                        this@UpdateProfileActivity,
                        "Permission granted, we can read storage files.",
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

    /**
     * Requests storage permission from the user.
     */
    private fun requestStoragePermission(){
        val neededPermission = if(VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, neededPermission)
        ){
            showRationaleDialog(
                "Chess App",
                "Chess App " + "needs to Access Your External Storage"
            ) { requestPermission.launch(arrayOf(neededPermission)) }
            requestPermission.launch(arrayOf(neededPermission))
        }
        else {
            // You can directly ask for the permission.
            requestPermission.launch(arrayOf(neededPermission))
        }
    }
    /**
     * Shows a rationale dialog to explain why the app needs a certain permission.
     * @param title: String - The title of the dialog.
     * @param message: String - The message to be displayed in the dialog.
     * @param onAccept: () -> Unit - The function to be called when the user accepts.
     */
    @Suppress("SameParameterValue")
    private fun showRationaleDialog(
        title: String,
        message: String,
        onAccept: () -> Unit,
    ) {

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
                onAccept.invoke()
            }
        builder.create().show()
    }

    /**
     * Sets up the action bar for the activity.
     */
    private fun setUpActionBar() {
        val toolbarUpdateProfileActivity = binding.toolbarMyProfileActivity
        setSupportActionBar(toolbarUpdateProfileActivity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_arrow_back)
            actionBar.title = resources.getString(R.string.my_profile)
        }

        toolbarUpdateProfileActivity.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    /**
     * Uploads the user image to Firebase storage and gets the downloadable URL.
     */
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

    /**
     * Gets the file extension of the given URI.
     * @param uri: Uri? - The URI of the file.
     * @return String? - The file extension of the URI.
     */
    private fun getFileExtension(uri : Uri?) : String?{
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

}