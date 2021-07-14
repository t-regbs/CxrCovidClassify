package com.timilehinaregbesola.cxrcovidclassify

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProviders
import com.timilehinaregbesola.cxrcovidclassify.navigation.NavGraph
import com.timilehinaregbesola.cxrcovidclassify.ui.CxrCovidClassifyTheme

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: UploadViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this)[UploadViewModel::class.java]
        window.makeTransparentStatusBar()
        setContent {
            CxrCovidClassifyTheme {
                window.statusBarColor = MaterialTheme.colors.background.toArgb()
                NavGraph(viewModel) { startCameraActivity() }
            }
        }
    }

    private fun startCameraActivity() {
        startActivity(Intent(this, ClassifierActivity::class.java))
    }

    private fun Window.makeTransparentStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
        }
    }
}
