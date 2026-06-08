package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.ui.theme.AppTextStyles
import com.lkz.affa.ui.theme.*

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {
    var obscureText by remember { mutableStateOf(true) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppSurfaceLight, RoundedCornerShape(12.dp))
            .border(1.dp, AppTextPrimary.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = AppTextStyles.body.copy(color = AppTextPrimary),
            cursorBrush = SolidColor(AppTextPrimary),
            visualTransformation = if (isPassword && obscureText) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = label,
                            style = AppTextStyles.body.copy(color = AppTextSecondary, fontSize = 14.sp)
                        )
                    }
                    innerTextField()
                }
            }
        )

        if (isPassword) {
            IconButton(
                onClick = { obscureText = !obscureText },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (obscureText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle password visibility",
                    tint = AppTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTextSecondary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp)
            )
        }
    }
}
