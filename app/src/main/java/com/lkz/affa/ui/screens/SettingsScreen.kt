package com.lkz.affa.ui.screens

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.launch
import com.lkz.affa.R
import com.lkz.affa.data.local.BackupManager
import com.lkz.affa.ui.components.*
import com.lkz.affa.ui.theme.*

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "1.0"
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "1"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "1"
    }
    
    var showSupportDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val currentLocales = androidx.core.app.LocaleManagerCompat.getApplicationLocales(context)
    val currentLanguageTag = if (!currentLocales.isEmpty) currentLocales.get(0)?.language else java.util.Locale.getDefault().language
    val currentLanguageName = if (currentLanguageTag == "es") stringResource(R.string.language_spanish) else stringResource(R.string.language_english)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = BackupManager.exportData(context, uri)
                if (result.isSuccess) {
                    Toast.makeText(context, context.getString(R.string.settings_export_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.settings_export_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = BackupManager.importData(context, uri)
                if (result.isSuccess) {
                    Toast.makeText(context, context.getString(R.string.settings_import_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.settings_import_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppScaffoldBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    tint = AppTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                color = AppTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(end = 48.dp),
                textAlign = TextAlign.Center
            )
        }

        PaddingValues(20.dp).let { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SectionLabel(stringResource(R.string.settings_section_general))
                SettingsCardContainer {
                    SettingsItemRow(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.settings_app_language),
                        trailingText = currentLanguageName,
                        onClick = { showLanguageDialog = true }
                    )
                    val sharedPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    var notificationsEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("enable_notifications", true)) }
                    
                    SettingsSwitchItemRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.settings_notifications),
                        subtitle = stringResource(R.string.settings_notifications_subtitle),
                        isChecked = notificationsEnabled,
                        onCheckedChange = { 
                            notificationsEnabled = it
                            sharedPrefs.edit().putBoolean("enable_notifications", it).apply()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel(stringResource(R.string.settings_section_backup))
                SettingsCardContainer {
                    SettingsItemRow(
                        icon = Icons.Default.FileUpload,
                        title = stringResource(R.string.settings_export_data),
                        subtitle = stringResource(R.string.settings_export_data_subtitle),
                        onClick = { exportLauncher.launch("backup.json") }
                    )
                    SettingsItemRow(
                        icon = Icons.Default.FileDownload,
                        title = stringResource(R.string.settings_import_data),
                        subtitle = stringResource(R.string.settings_import_data_subtitle),
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel(stringResource(R.string.settings_section_support))
                SettingsCardContainer {
                    SettingsItemRow(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline, 
                        title = stringResource(R.string.settings_help_support),
                        onClick = { showSupportDialog = true }
                    )
                    SettingsItemRow(
                        icon = Icons.Outlined.PrivacyTip, 
                        title = stringResource(R.string.settings_privacy_policy),
                        onClick = { showPrivacyDialog = true }
                    )
                    SettingsItemRow(
                        icon = Icons.Outlined.Info, 
                        title = stringResource(R.string.settings_about),
                        onClick = { showAboutDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${stringResource(R.string.app_name)} v$versionName (Build $versionCode)",
                        color = AppTextSecondary.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_made_with_love),
                        color = AppTextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    LanguageDialog(
        showDialog = showLanguageDialog,
        currentLanguageTag = currentLanguageTag ?: "es",
        onDismiss = { showLanguageDialog = false }
    )
    
    AboutDialog(
        showDialog = showAboutDialog,
        versionName = versionName,
        onDismiss = { showAboutDialog = false }
    )
    
    PrivacyDialog(
        showDialog = showPrivacyDialog,
        onDismiss = { showPrivacyDialog = false }
    )
    
    SupportDialog(
        showDialog = showSupportDialog,
        context = context,
        onDismiss = { showSupportDialog = false }
    )
}



