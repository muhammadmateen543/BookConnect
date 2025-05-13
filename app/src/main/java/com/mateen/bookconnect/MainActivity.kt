package com.mateen.bookconnect

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore

    lateinit var drawerLayout: DrawerLayout
    lateinit var sideNavigation: NavigationView
    lateinit var bottomNavigation: BottomNavigationView
    lateinit var frameLayout: FrameLayout
    lateinit var btprofile: ImageView

    lateinit var headerView: View
    lateinit var ivProfilePic: ImageView
    lateinit var tvName:TextView
    lateinit var tvLocation:TextView
    lateinit var tvEmail:TextView

    private var currentFragmentTag: String = "HomeFragment"

    lateinit var currentUid:String

    fun isBase64UnderFirestoreLimit(base64String: String): Boolean {
        return base64String.toByteArray(Charsets.UTF_8).size < 1_048_576
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun imageViewToBase64(imageView: ImageView): String? {
        val drawable = imageView.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                return Base64.encodeToString(byteArray, Base64.DEFAULT)
            }
        }
        return null
    }

    private val requestPermissionLauncherForCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                capturedImage.launch(captureImage)
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            bitmap?.let {
                val base64Image = bitmapToBase64(bitmap)
                if(isBase64UnderFirestoreLimit(bitmapToBase64(bitmap))) {
                    authdb.collection("Base64Images").document(currentUid!!)
                        .collection("ProfilePicture").document().update("base64", base64Image)
                    Log.d("Base64", "Base64 is updated")
                    btprofile.setImageBitmap(bitmap)
                }
                else{
                    Toast.makeText(this, "Failed. Selected Image > 1 MB", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val capturedImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                bitmap?.let {
                    val base64Image = bitmapToBase64(bitmap)
                    if(isBase64UnderFirestoreLimit(base64Image)) {
                        authdb.collection("Base64Images").document(currentUid!!)
                            .collection("ProfilePicture").document().update("base64", base64Image)
                        Log.d("Base64", "Base64 is updated")
                        btprofile.setImageBitmap(bitmap)
                    }
                    else{
                        Toast.makeText(this, "Failed. Captured Image > 1 MB", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        authdb = FirebaseFirestore.getInstance()
        currentUid = auth.currentUser?.uid!!

        drawerLayout = findViewById(R.id.main_drawer)
        sideNavigation = findViewById(R.id.side_navigation)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        frameLayout = findViewById(R.id.fm_main_activity)
        btprofile = findViewById(R.id.profile)

        headerView = sideNavigation.getHeaderView(0)
        ivProfilePic= headerView.findViewById(R.id.iv_profile_pic)
        tvName= headerView.findViewById(R.id.tv_name)
        tvLocation= headerView.findViewById(R.id.tv_location)
        tvEmail= headerView.findViewById(R.id.tv_email)

        setProfileIcon()
        setSideNavigation()

        if (savedInstanceState == null) {
            sideNavigation.isEnabled=false
            bottomNavigation.isEnabled=false
            supportFragmentManager.beginTransaction()
                .replace(R.id.fm_main_activity, HomeFragment(), "HomeFragment")
                .commit()
            currentFragmentTag= "HomeFragment"
            sideNavigation.isEnabled=true
            bottomNavigation.isEnabled=true
        }

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home_navigation -> {
                    bottomNavigation.isEnabled=false
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fm_main_activity, HomeFragment(), "HomeFragment")
                        .commit()
                    currentFragmentTag = "HomeFragment"
                    bottomNavigation.isEnabled=true
                }
                R.id.chat_navigation -> {
                    bottomNavigation.isEnabled=false
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fm_main_activity, ChatFragment(), "ChatFragment")
                        .commit()
                    currentFragmentTag = "ChatFragment"
                    bottomNavigation.isEnabled=true
                }
                R.id.sell_navigation -> {
                    bottomNavigation.isEnabled=false
                    if (currentFragmentTag != "SellFragment") {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fm_main_activity, SellFragment(), "SellFragment")
                            .commit()
                        currentFragmentTag = "SellFragment"
                    }
                    bottomNavigation.isEnabled=true
                }
                R.id.mybooks_navigation -> {
                    bottomNavigation.isEnabled=false
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fm_main_activity, MyBooksFragment(), "MyBooksFragment")
                            .commit()
                        currentFragmentTag = "MyBooksFragment"
                    bottomNavigation.isEnabled=true
                }
                R.id.myoffers_navigation -> {
                    bottomNavigation.isEnabled=false
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fm_main_activity, MyOffersFragment(), "MyOffersFragment")
                            .commit()
                        currentFragmentTag = "MyOffersFragment"
                    bottomNavigation.isEnabled=true
                }
            }
            true
        }
    }

    private fun setProfileIcon() {
        authdb.collection("Base64Images").document(currentUid!!)
            .collection("ProfilePicture").get()
            .addOnSuccessListener { result ->
                for (pic in result){
                    val data = pic.data
                    val base64Image = data["base64"].toString()
                    val profileImage = base64ToBitmap(base64Image)
                    // In Main Activity
                    btprofile.setImageBitmap(profileImage)
                    // In side Navigation
                    ivProfilePic.setImageBitmap(profileImage)
                }
            }
        authdb.collection("users").document(currentUid).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    tvName.text = document.getString("Full Name") ?: "Name"
                    tvLocation.text = document.getString("Location") ?: "Location"
                    tvEmail.text = document.getString("Email") ?: "Email"
                }
            }
        btprofile.setOnClickListener {
            drawerLayout.openDrawer(sideNavigation)
        }
    }

    private fun setSideNavigation() {
        sideNavigation.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.profile_navigation -> {
                    sideNavigation.isEnabled=false
                    val options = arrayOf("Edit Profile Picture", "Edit Name", "Edit Location")
                    val ad = android.app.AlertDialog.Builder(this)
                    ad.setTitle("Select one option")
                    ad.setItems(options) { dialog, index ->
                        if (options[index] == "Edit Profile Picture") {
                            val items = arrayOf("Camera", "Gallery")
                            var ad = android.app.AlertDialog.Builder(this)
                            ad.setTitle("Open")
                            ad.setItems(items) { dialog, index ->
                                if (items[index] == "Gallery") {
                                    val getImageFromGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                    getImageFromGallery.type = "image/*"
                                    galleryImage.launch(getImageFromGallery)
                                } else if (items[index] == "Camera") {
                                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                        capturedImage.launch(captureImage)
                                    } else {
                                        requestPermissionLauncherForCamera.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            }
                            var adcreate = ad.create()
                            adcreate.show()
                        } else if (options[index] == "Edit Name") {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Enter New Name")
                            val input = EditText(this)
                            input.inputType = InputType.TYPE_CLASS_TEXT
                            builder.setView(input)
                            builder.setPositiveButton("Rename") { _, _ ->
                                val newName = input.text.toString().trim()
                                if (newName.isNotEmpty()) {
                                    authdb.collection("users").document(currentUid!!).update("Full Name", newName)
                                    tvName.text = newName
                                    Toast.makeText(this, "Name successfully changed", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_LONG).show()
                                }
                            }
                            builder.setNegativeButton("Cancel", null)
                            builder.show()
                        }
                        else if(options[index] == "Edit Location"){
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Enter New Location")
                            val input = EditText(this)
                            input.inputType = InputType.TYPE_CLASS_TEXT
                            builder.setView(input)
                            builder.setPositiveButton("Rename") { _, _ ->
                                val newLocation = input.text.toString().trim()
                                if (newLocation.isNotEmpty()) {
                                    authdb.collection("users").document(currentUid!!).update("Location", newLocation)
                                    tvLocation.text = newLocation
                                    Toast.makeText(this, "Location successfully changed", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this, "Location cannot be empty", Toast.LENGTH_LONG).show()
                                }
                            }
                            builder.setNegativeButton("Cancel", null)
                            builder.show()
                        }
                    }
                    var adcreate = ad.create()
                    adcreate.show()
                    sideNavigation.isEnabled=true
                    true
                }
                R.id.notification_navigation -> {
                    sideNavigation.isEnabled=false
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fm_main_activity, NotificationFragment(), "NotificationFragment")
                        .commit()
                    sideNavigation.isEnabled=true
                    true
                }
                R.id.logout_navigation -> {
                    sideNavigation.isEnabled=false
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LogIn::class.java))
                    finish()
                    sideNavigation.isEnabled=true
                    true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(sideNavigation)
            }
        }
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            Log.e("ChatFragment", "Failed to decode base64 image: ${e.message}")
            null
        }
    }

    override fun onBackPressed() {
        if (currentFragmentTag == "HomeFragment") {
            finish()
        } else {
            bottomNavigation.selectedItemId = R.id.home_navigation
        }
    }
}
