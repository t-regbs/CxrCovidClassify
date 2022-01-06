package com.timilehinaregbesola.cxrcovidclassify.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.timilehinaregbesola.cxrcovidclassify.R
import com.timilehinaregbesola.cxrcovidclassify.utils.Navigation
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun SplashScreen(
    navController: NavController
) {
    SplashScreenContent()
    GlobalScope.launch(Dispatchers.Main) {
        delay(5000)
        navController.popBackStack()
        navController.navigate(Navigation.NAV_HOME)
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = "Splash Logo",
            modifier = Modifier
                .width(125.dp)
                .height(168.dp)
                .align(Alignment.Center)
        )
    }
}
