package com.lkz.affa.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.lkz.affa.R
import com.lkz.affa.ui.theme.*

@Composable
fun SettingsCardContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurfaceLight, RoundedCornerShape(12.dp))
    ) {
        content()
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector, 
    title: String, 
    subtitle: String? = null,
    trailingText: String? = null,
    iconColor: Color? = null,
    titleColor: Color? = null,
    showArrow: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor ?: AppTextSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                color = titleColor ?: AppTextPrimary, 
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        if (trailingText != null) {
            Text(text = trailingText, color = AppTextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (showArrow) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppTextSecondary)
        }
    }
}

@Composable
fun SettingsSwitchItemRow(
    icon: ImageVector, 
    title: String, 
    subtitle: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                color = AppTextPrimary, 
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppScaffoldBackground,
                checkedTrackColor = AppPrimary,
                uncheckedThumbColor = AppTextSecondary,
                uncheckedTrackColor = AppScaffoldBackground
            )
        )
    }
}

@Composable
fun LanguageDialog(
    showDialog: Boolean,
    currentLanguageTag: String,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_language_dialog_title), color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                                onDismiss()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentLanguageTag != "es", onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(stringResource(R.string.language_english), color = AppTextPrimary, fontSize = 16.sp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
                                onDismiss()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentLanguageTag == "es", onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(stringResource(R.string.language_spanish), color = AppTextPrimary, fontSize = 16.sp)
                    }
                }
            },
            containerColor = AppSurface,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_close), color = AppPrimary)
                }
            }
        )
    }
}

@Composable
fun AboutDialog(
    showDialog: Boolean,
    versionName: String,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_about_dialog_title), color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.app_name), color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_version, versionName), color = AppTextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.settings_about_dialog_description),
                        color = AppTextSecondary, fontSize = 14.sp, lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("2026 Anime FFA Team", color = AppTextSecondary, fontSize = 12.sp)
                }
            },
            containerColor = AppSurface,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_close), color = AppPrimary)
                }
            }
        )
    }
}

@Composable
fun PrivacyDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_privacy_policy), color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_privacy_summary_title), color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.settings_privacy_summary_desc), color = AppTextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
                }
            },
            containerColor = AppSurface,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_close), color = AppPrimary)
                }
            }
        )
    }
}

@Composable
fun SupportDialog(
    showDialog: Boolean,
    context: Context,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_help_support), color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_support_dialog_desc), color = AppTextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+ItOCIKKJnSM3Njgx"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(40.dp).background(AppError.copy(alpha = 0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = AppError)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.settings_support_report_bugs), color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("Telegram", color = AppTextSecondary, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Anim3FFA"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(40.dp).background(AppSecondary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = AppSecondary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.settings_support_join_community), color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("Telegram", color = AppTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            },
            containerColor = AppSurface,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_close), color = AppPrimary)
                }
            }
        )
    }
}
