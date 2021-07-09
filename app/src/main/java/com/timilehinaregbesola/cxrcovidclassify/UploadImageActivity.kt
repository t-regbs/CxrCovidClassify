package com.timilehinaregbesola.cxrcovidclassify

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProviders
import com.timilehinaregbesola.cxrcovidclassify.databinding.ActivityUploadImageBinding
import com.timilehinaregbesola.cxrcovidclassify.ml.Covid
import java.io.File

class UploadImageActivity : AppCompatActivity() {
    private var bitmapp: Bitmap? = null
    companion object {
        const val PICK_IMAGE = 1
        const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityUploadImageBinding
    private lateinit var model: UploadViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        model = ViewModelProviders.of(this)[UploadViewModel::class.java]
        model.getResult().observe(
            this,
            { results ->
                // update UI
                println("Result: $results")
                println("-Re: $results")
                binding.btnRun.isEnabled = true
                binding.loading.visibility = View.GONE
                binding.txtLabel.visibility = View.VISIBLE
                binding.txtLabelValue.visibility = View.VISIBLE
                if (results < 0.5) {
                    // means prediction was for category corresponding to 0
                    binding.txtLabel.text = "Negative"
                    binding.txtLabelValue.text =
                        String.format("%.2f", 100 * results) + "%"
                } else {
                    // means prediction was for category corresponding to 1
                    binding.txtLabel.text = "Positive"
                    binding.txtLabelValue.text =
                        String.format("%.2f", 100 * results) + "%"
                }
            }
        )

        binding.btnRun.isEnabled = false
        binding.txtLabel.visibility = View.INVISIBLE
        binding.txtLabelValue.visibility = View.INVISIBLE
        binding.btnSelectImage.setOnClickListener {
            pickImage()
        }
        binding.btnRun.setOnClickListener {
//            classifier = Classifier.create(this, Classifier.Device.CPU, 1)
            processImage()
        }
    }

    private fun pickImage() {
        if (ActivityCompat.checkSelfPermission(
                this,
                READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.type = "image/*"

            val pickIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickIntent.type = "image/*"
            pickIntent.putExtra("crop", "true")
            pickIntent.putExtra("scale", true)
            pickIntent.putExtra("outputX", 224)
            pickIntent.putExtra("outputY", 224)
            pickIntent.putExtra("aspectX", 1)
            pickIntent.putExtra("aspectY", 1)
            pickIntent.putExtra("return-data", true)

            val chooserIntent = Intent.createChooser(getIntent, "Select Image")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))

            startActivityForResult(chooserIntent, PICK_IMAGE)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(READ_EXTERNAL_STORAGE),
                READ_EXTERNAL_STORAGE_REQUEST_CODE
            )
        }
    }

    fun processImage() {
//        rgbFrameBitmap!!.setPixels(
//            getRgbBytes(),
//            0,
//            previewWidth,
//            0,
//            0,
//            previewWidth,
//            previewHeight
//        )
//        val cropSize = Math.min(previewWidth, previewHeight)
        binding.btnRun.isEnabled = false
        binding.txtLabel.visibility = View.INVISIBLE
        binding.txtLabelValue.visibility = View.INVISIBLE
        binding.loading.visibility = View.VISIBLE
        val cxrModel = Covid.newInstance(this)
        model.process(cxrModel, bitmapp!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE) {
            if (resultCode != RESULT_OK) {
                println("Result not ok")
                return
            }
            val uri = data?.data
//            if (uri != null) {
//                val imageFile = uriToImageFile(uri)
//                // do something with file
//            }
            if (uri != null) {
                println(uri.toString())
                bitmapp = uriToBitmap(uri)
                binding.imgSelectedImage.setImageBitmap(bitmapp)
                binding.btnRun.isEnabled = true
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // pick image after request permission success
                    pickImage()
                }
            }
        }
    }

    private fun uriToImageFile(uri: Uri): File? {
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                val filePath = cursor.getString(columnIndex)
                cursor.close()
                return File(filePath)
            }
            cursor.close()
        }
        return null
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }
}
