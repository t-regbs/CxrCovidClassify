package com.timilehinaregbesola.cxrcovidclassify.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CovidTopAppBar(title: String, showBack: Boolean = false, onBackPressed: (() -> Unit)? = null) {
    val pad = if (showBack) 56.dp else 0.dp
    TopAppBar(
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    modifier = Modifier.padding(end = pad),
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start
                )
            }
        },
        navigationIcon = if (showBack) {
            {
                IconButton(onClick = { onBackPressed?.invoke() }) {
                    Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = null)
                }
            }
        } else null,
        backgroundColor = Color.White,
    )
}

@Preview
@Composable
fun AppBarPreview() {
    CovidTopAppBar("Covid Scanner")
}
