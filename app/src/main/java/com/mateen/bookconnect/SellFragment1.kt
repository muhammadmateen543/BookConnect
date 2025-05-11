package com.mateen.bookconnect

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.ArrayList

class SellFragment1 : Fragment() {

    private lateinit var btback: ImageView
    private lateinit var btnext: RelativeLayout
    private lateinit var ettitle: EditText
    private lateinit var etdescription: EditText
    private lateinit var addimages: LinearLayout
    private lateinit var images: Array<ImageView>
    private lateinit var imagescrossbtn: Array<ImageView>

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
            }
        }

    private val galleryImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val clipData: ClipData?=result.data?.clipData
                val imageUri: Uri? = result.data?.data
                if(clipData!==null)
                {
                    val totalImagesCollectedFromGallery=clipData.itemCount.coerceAtMost(8)
                    //Creates newClipData having images images of size less than 1Mb
                    var count = 0
                    var newClipData: ClipData? = null
                    for (i in 0 until totalImagesCollectedFromGallery) {
                        val uri = clipData.getItemAt(i).uri
                        val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)

                        if(isBase64UnderFirestoreLimit(bitmapToBase64(bitmap))) {
                            count++
                            val item = ClipData.Item(uri)
                            if (newClipData == null) {
                                newClipData = ClipData(clipData.description, item)
                            } else {
                                newClipData.addItem(item)
                            }
                        }
                    }
                    //

                    var index=0
                    for (i in 0..7)
                    {
                        if (images[i].drawable == null && index<count) {
                            val uri = newClipData?.getItemAt(index++)?.uri
                            var bitmap = MediaStore.Images.Media.getBitmap(
                                requireActivity().contentResolver,
                                uri
                            )
                            images[i].setImageBitmap(bitmap)
                            imagescrossbtn[i].visibility = View.VISIBLE
                        }
                    }
                }
                else if(imageUri!=null)
                {
                    val bitmap =
                        MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                    if(isBase64UnderFirestoreLimit(bitmapToBase64(bitmap))) {
                        for (i in 0..7) {
                            if (images[i].drawable == null) {
                                images[i].setImageBitmap(bitmap)
                                imagescrossbtn[i].visibility = View.VISIBLE
                                break
                            }
                        }
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
                        for (i in 0..7) {
                            if (images[i].drawable == null) {
                                images[i].setImageBitmap(bitmap)
                                imagescrossbtn[i].visibility = View.VISIBLE
                                break
                            }
                        }
                    }
                }
            }
        }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sell1, container, false)

        btback = view.findViewById(R.id.btback)
        btnext = view.findViewById(R.id.btnext)
        ettitle = view.findViewById(R.id.ettitle)
        etdescription = view.findViewById(R.id.etdescription)
        addimages = view.findViewById(R.id.addimages)

        images = arrayOf(
            view.findViewById(R.id.img1),
            view.findViewById(R.id.img2),
            view.findViewById(R.id.img3),
            view.findViewById(R.id.img4),
            view.findViewById(R.id.img5),
            view.findViewById(R.id.img6),
            view.findViewById(R.id.img7),
            view.findViewById(R.id.img8)
        )
        imagescrossbtn = arrayOf(
            view.findViewById(R.id.img1cross),
            view.findViewById(R.id.img2cross),
            view.findViewById(R.id.img3cross),
            view.findViewById(R.id.img4cross),
            view.findViewById(R.id.img5cross),
            view.findViewById(R.id.img6cross),
            view.findViewById(R.id.img7cross),
            view.findViewById(R.id.img8cross)
        )
        for (i in 0..7) {
            imagescrossbtn[i].visibility = View.GONE
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btback.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnext.setOnClickListener {
            if (ettitle.text.toString().isEmpty()) {
                ShowError(requireContext()).showError("TITLE IS MISSING.")
            } else if (etdescription.text.toString().isEmpty()) {
                ShowError(requireContext()).showError("DESCRIPTION IS MISSING.")
            } else {
                val bundle=Bundle()
                bundle.putString("Title", ettitle.text.toString())
                bundle.putString("Description", etdescription.text.toString())
                var Base64ListView=ArrayList<String>()
                for (i in 0..7)
                {
                    if(images[i].drawable!=null)
                    {
                        imageViewToBase64(images[i])?.let { it1 -> Base64ListView.add(it1) }
                    }
                }
                bundle.putStringArrayList("Base64Images", Base64ListView)
                var sellFragment2=SellFragment2()
                sellFragment2.arguments=bundle
                parentFragmentManager.beginTransaction().replace(R.id.fragmetSell1, sellFragment2)
                    .addToBackStack(null).commit()
            }
        }

        val items = arrayOf("Camera", "Gallery")
        addimages.setOnClickListener {
            var ad = AlertDialog.Builder(requireContext())
            ad.setTitle("Open")
            ad.setItems(items) { dialog, index ->
                if (items[index] == "Gallery") {
                    if (images.any{it.drawable==null})
                    {
                        val getImagesFromGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        getImagesFromGallery.type = "image/*"
                        getImagesFromGallery.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        galleryImage.launch(getImagesFromGallery)
                    }
                    else{
                        ShowError(requireContext()).showError("CAN'T ADD MORE IMAGES.")
                    }
                } else if (items[index] == "Camera") {
                    if (images.any{it.drawable==null})
                    {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            capturedImage.launch(captureImage)
                        } else {
                            requestPermissionLauncherForCamera.launch(Manifest.permission.CAMERA)
                        }
                    }
                    else{
                        ShowError(requireContext()).showError("CAN'T ADD MORE IMAGES.")
                    }
                }
            }
            var adcreate = ad.create()
            adcreate.show()
        }
        for (i in 0..7)
        {
            imagescrossbtn[i].setOnClickListener({
                images[i].setImageDrawable(null)
                imagescrossbtn[i].visibility=View.GONE
                for (j in i..6)
                {
                    if(images[j+1].drawable!=null)
                    {
                        images[j].setImageDrawable(images[j+1].drawable)
                        images[j+1].setImageDrawable(null)
                        imagescrossbtn[j+1].visibility=View.GONE
                        imagescrossbtn[j].visibility=View.VISIBLE
                    }
                }
            })
        }
    }
}