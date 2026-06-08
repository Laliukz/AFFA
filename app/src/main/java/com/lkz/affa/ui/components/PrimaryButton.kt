package com.lkz.affa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lkz.affa.ui.theme.*

enum class ButtonVariant {
    Filled,
    Outlined,
    Text,
    Danger
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    isExpanded: Boolean = false
) {
    val buttonModifier = if (isExpanded) modifier.fillMaxWidth() else modifier

    val content: @Composable RowScope.() -> Unit = {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = if (variant == ButtonVariant.Filled) AppTextPrimary else AppPrimary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(label, style = AppTextStyles.button)
        }
    }

    when (variant) {
        ButtonVariant.Filled -> {
            Button(
                onClick = { if (!isLoading) onClick?.invoke() },
                modifier = buttonModifier,
                enabled = onClick != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPrimary,
                    contentColor = AppTextPrimary,
                    disabledContainerColor = AppPrimary.copy(alpha = 0.5f),
                    disabledContentColor = AppTextPrimary.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                content()
            }
        }
        ButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = { if (!isLoading) onClick?.invoke() },
                modifier = buttonModifier,
                enabled = onClick != null,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppTextPrimary,
                    disabledContentColor = AppTextPrimary.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, AppPrimary),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                content()
            }
        }
        ButtonVariant.Text -> {
            TextButton(
                onClick = { if (!isLoading) onClick?.invoke() },
                modifier = buttonModifier,
                enabled = onClick != null,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppSecondary,
                    disabledContentColor = AppSecondary.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                content()
            }
        }
        ButtonVariant.Danger -> {
            OutlinedButton(
                onClick = { if (!isLoading) onClick?.invoke() },
                modifier = buttonModifier,
                enabled = onClick != null,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = AppError.copy(alpha = 0.08f),
                    contentColor = AppError,
                    disabledContentColor = AppError.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, AppError.copy(alpha = 0.39f)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                content()
            }
        }
    }
}
