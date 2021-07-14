package com.timilehinaregbesola.cxrcovidclassify.navigation

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.timilehinaregbesola.cxrcovidclassify.UploadViewModel
import com.timilehinaregbesola.cxrcovidclassify.screens.HomeScreen
import com.timilehinaregbesola.cxrcovidclassify.screens.UploadScreen
import com.timilehinaregbesola.cxrcovidclassify.utils.Navigation

@Composable
fun NavGraph(
    viewModel: UploadViewModel,
    startCamera: () -> Unit
) {
    val navController = rememberNavController()
    Surface(color = MaterialTheme.colors.background) {
        NavHost(navController = navController, startDestination = Navigation.NAV_HOME) {
            composable(Navigation.NAV_HOME) {
                HomeScreen(navController, startCamera)
            }
            composable(Navigation.NAV_UPLOAD_PHOTO) {
                UploadScreen(navController, viewModel)
            }
        }
    }
}
