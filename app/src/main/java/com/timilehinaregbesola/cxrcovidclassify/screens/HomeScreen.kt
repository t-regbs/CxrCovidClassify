package com.timilehinaregbesola.cxrcovidclassify.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.timilehinaregbesola.cxrcovidclassify.components.CovButton
import com.timilehinaregbesola.cxrcovidclassify.components.CovidTopAppBar
import com.timilehinaregbesola.cxrcovidclassify.ui.greenColor
import com.timilehinaregbesola.cxrcovidclassify.ui.homeTextColor
import com.timilehinaregbesola.cxrcovidclassify.ui.purpleColor
import com.timilehinaregbesola.cxrcovidclassify.utils.Navigation

@Composable
fun HomeScreen(
    navController: NavHostController,
    startCamera: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Scaffold(
            topBar = { CovidTopAppBar(title = "Covid Scanner") }
        ) {
            Column(
                modifier = Modifier.padding(top = 170.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 64.dp),
                    text = "Upload a photo or scan a picture",
                    lineHeight = 35.sp,
                    textAlign = TextAlign.Center,
                    fontSize = 25.sp,
                    color = homeTextColor
                )
                Spacer(modifier = Modifier.height(51.dp))
                CovButton(
                    title = "Upload Photo",
                    icon = Icons.Outlined.FileUpload,
                    onClick = { navController.navigate(Navigation.NAV_UPLOAD_PHOTO) },
                    color = greenColor
                )
                Spacer(modifier = Modifier.height(30.dp))
                CovButton(
                    title = "Camera mode",
                    icon = Icons.Outlined.PhotoCamera,
                    onClick = { startCamera.invoke() },
                    color = purpleColor
                )
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(rememberNavController(), {})
}
