package com.timilehinaregbesola.cxrcovidclassify.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CovButton(
    title: String,
    icon: ImageVector? = null,
    onClick: () -> Unit = {},
    color: Color,
    textColor: Color = Color.White,
    enabled: Boolean = true
) {
    Button(
        modifier = Modifier
            .height(70.dp)
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(30.dp),
                tint = Color.White,
                imageVector = icon,
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = textColor
        )
    }
}
