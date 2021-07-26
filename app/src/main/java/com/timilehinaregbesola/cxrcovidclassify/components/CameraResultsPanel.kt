package com.timilehinaregbesola.cxrcovidclassify.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.cxrcovidclassify.Recognition

@Composable
fun CameraResultsPanel(
    modifier: Modifier = Modifier,
    recognition: Recognition
) {
    Row(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier
                .weight(2f)
                .padding(8.dp),
            text = recognition.label,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            text = recognition.probabilityString,
            textAlign = TextAlign.End,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
