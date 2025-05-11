package com.mateen.bookconnect

import android.Manifest
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class SignUp : AppCompatActivity() {
    private lateinit var btsignup: RelativeLayout
    private lateinit var profile_pic: ImageView
    private lateinit var btgotologin: TextView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etLocation: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore

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
                if(isBase64UnderFirestoreLimit(bitmapToBase64(bitmap))) {
                    profile_pic.setImageBitmap(bitmap)
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
                    if(isBase64UnderFirestoreLimit(bitmapToBase64(bitmap))) {
                        profile_pic.setImageBitmap(bitmap)
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
        auth = FirebaseAuth.getInstance()
        authdb = FirebaseFirestore.getInstance()
        setContentView(R.layout.activity_sign_up)
        btsignup=findViewById(R.id.btsignup)
        profile_pic=findViewById(R.id.iv_profile_pic)
        btgotologin=findViewById(R.id.bt_goto_login)
        etName = findViewById(R.id.name)
        etEmail = findViewById(R.id.email)
        etPassword = findViewById(R.id.password)
        etLocation = findViewById(R.id.location)
        val items = arrayOf("Camera", "Gallery")
        profile_pic.setOnClickListener {
            var ad = AlertDialog.Builder(this)
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
        }
        btsignup.setOnClickListener {btsignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val location = etLocation.text.toString().trim()

            when {
                name.isEmpty() -> ShowError(this).showError("Name is required.")
                email.isEmpty() -> ShowError(this).showError("Email is required.")
                password.isEmpty() -> ShowError(this).showError("Password is required.")
                location.isEmpty() -> ShowError(this).showError("Location is required.")
                profile_pic.drawable == null -> ShowError(this).showError("Profile picture is required.")
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ShowError(
                    this
                ).showError("Invalid email address.")

                password.length < 8 -> ShowError(this).showError("Password must be at least 8 characters.")
                else -> {
                    btsignup.isEnabled = false // disable to prevent multiple clicks
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                if (userId != null) {
                                    val usr = hashMapOf(
                                        "Full Name" to name,
                                        "Location" to location,
                                        "Email" to email
                                    )
                                    authdb.collection("users").document(userId).set(usr)
                                        .addOnSuccessListener {
                                            val imagesCollectionRef = authdb.collection("Base64Images").document(userId).collection("ProfilePicture")
                                            val profile_picture = imageViewToBase64(profile_pic)
                                            if (profile_picture != null) {
                                                //Toast.makeText(this, prev_image, Toast.LENGTH_SHORT).show
                                                val profile_picture_data =
                                                    hashMapOf("base64" to profile_picture)
                                                imagesCollectionRef.add(profile_picture_data)
                                                Toast.makeText(this, "Calling Main activity", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this, MainActivity::class.java)
                                                startActivity(intent)
                                                this.finish()
                                            }
                                        }
                                }
                            } else {
                                ShowError(this).showError("Signup failed.")
                                btsignup.isEnabled = true
                            }
                        }
                }
            }
        }
        }
        btgotologin.setOnClickListener {
            var intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }
    }
}