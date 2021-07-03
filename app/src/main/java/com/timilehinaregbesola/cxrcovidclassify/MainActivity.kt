package com.timilehinaregbesola.cxrcovidclassify

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.timilehinaregbesola.cxrcovidclassify.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setClickListeners()
    }

    private fun setClickListeners() {
        binding.btnCameraMode.setOnClickListener {
            startActivity(Intent(this, ClassifierActivity::class.java))
        }
        binding.btnUpload.setOnClickListener {
            startActivity(Intent(this, UploadImageActivity::class.java))
        }
    }
}
