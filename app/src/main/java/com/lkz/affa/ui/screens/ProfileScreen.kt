package com.lkz.affa.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lkz.affa.navigation.Screen
import com.lkz.affa.R
import com.lkz.affa.ui.components.AssetImage
import com.lkz.affa.ui.components.ButtonVariant
import com.lkz.affa.ui.components.CommonImage
import com.lkz.affa.ui.components.PrimaryButton
import com.lkz.affa.ui.components.SectionLabel
import com.lkz.affa.ui.theme.*

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

    var userName by remember { mutableStateOf(prefs.getString("user_name", "Otaku") ?: "Otaku") }
    var avatarUri by remember { mutableStateOf(prefs.getString("user_avatar_url", null)) }

    DisposableEffect(prefs) {
        val listener =
                android.content.SharedPreferences.OnSharedPreferenceChangeListener {
                        sharedPreferences,
                        key ->
                    if (key == "user_name") {
                        userName = sharedPreferences.getString("user_name", "Otaku") ?: "Otaku"
                    }
                    if (key == "user_avatar_url") {
                        avatarUri = sharedPreferences.getString("user_avatar_url", null)
                    }
                }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        userName = prefs.getString("user_name", "Otaku") ?: "Otaku"
        avatarUri = prefs.getString("user_avatar_url", null)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf(userName) }

    var showAvatarDialog by remember { mutableStateOf(false) }
    val availableAvatars =
            listOf(
                    "images/Avatar1.png",
                    "images/Avatar2.png",
                    "images/Avatar3.png",
                    "images/Avatar4.png"
            )

    val favoritesRepo = remember { com.lkz.affa.data.repository.FavoritesRepository(context) }
    var vistosCount by remember { mutableStateOf("0") }
    var enListaCount by remember { mutableStateOf("0") }

    LaunchedEffect(Unit) {
        val completados = favoritesRepo.getFavorites("Completado").size
        val viendo = favoritesRepo.getFavorites("Viendo").size
        val planeado = favoritesRepo.getFavorites("Planeado").size

        vistosCount = completados.toString()
        enListaCount = (viendo + planeado).toString()
    }

    Scaffold(containerColor = AppScaffoldBackground, topBar = { TopBarAccount() }) { paddingValues
        ->
        LazyColumn(
                modifier =
                        Modifier.fillMaxSize()
                                .background(AppScaffoldBackground)
                                .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
        ) {
            item {
                UserProfileHeader(
                        userName = userName,
                        avatarUri = avatarUri,
                        vistosCount = vistosCount,
                        enListaCount = enListaCount,
                        onEditClick = {
                            editNameText = userName
                            showEditDialog = true
                        },
                        onAvatarClick = { showAvatarDialog = true }
                )
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }

            item { SectionLabel(stringResource(R.string.profile_content)) }
            item {
                SettingsGroup {
                    SettingsItem(
                            icon = Icons.Default.History,
                            title = stringResource(R.string.profile_history),
                            subtitle = stringResource(R.string.profile_continue_watching),
                            iconTint = AppRating // Orange
                    ) { navController.navigate(Screen.History.route) }
                    SettingsItem(
                            icon = Icons.Default.DownloadDone,
                            title = stringResource(R.string.profile_downloads),
                            subtitle = stringResource(R.string.profile_available_episodes),
                            iconTint = AppRating // Orange
                    ) { navController.navigate(Screen.Downloads.route) }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item { SectionLabel(stringResource(R.string.profile_settings_support)) }
            item {
                SettingsGroup {
                    SettingsItem(
                            icon = Icons.Default.Tune,
                            title = stringResource(R.string.profile_app_preferences),
                            iconTint = AppSuccess
                    ) { navController.navigate(Screen.Settings.route) }
                    SettingsItem(
                            icon = Icons.Default.LocalCafe,
                            title = stringResource(R.string.profile_support_kofi),
                            subtitle = stringResource(R.string.profile_support_kofi_subtitle),
                            iconTint = AppError // Ko-Fi red/pink color
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/affa"))
                        context.startActivity(intent)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (showEditDialog) {
        AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text(stringResource(R.string.profile_edit_profile), color = AppTextPrimary) },
                text = {
                    OutlinedTextField(
                            value = editNameText,
                            onValueChange = { editNameText = it },
                            label = { Text(stringResource(R.string.profile_username), color = AppTextSecondary) },
                            singleLine = true,
                            colors =
                                    OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = AppTextPrimary,
                                            unfocusedTextColor = AppTextPrimary,
                                            cursorColor = AppPrimary,
                                            focusedBorderColor = AppPrimary,
                                            unfocusedBorderColor = AppTextSecondary,
                                            focusedLabelColor = AppPrimary,
                                            unfocusedLabelColor = AppTextSecondary
                                    )
                    )
                },
                containerColor = AppSurface,
                confirmButton = {
                    TextButton(
                            onClick = {
                                userName = editNameText
                                prefs.edit().putString("user_name", editNameText).apply()
                                showEditDialog = false
                            }
                    ) { Text(stringResource(R.string.profile_save), color = AppPrimary, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text(stringResource(R.string.profile_cancel), color = AppTextSecondary)
                    }
                }
        )
    }

    if (showAvatarDialog) {
        AlertDialog(
                onDismissRequest = { showAvatarDialog = false },
                title = { Text(stringResource(R.string.profile_choose_avatar), color = AppTextPrimary) },
                text = {
                    LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(availableAvatars) { avatarPath ->
                            Box(
                                    modifier =
                                            Modifier.size(80.dp)
                                                    .clip(CircleShape)
                                                    .background(AppPrimary.copy(alpha = 0.1f))
                                                    .clickable {
                                                        avatarUri = avatarPath
                                                        prefs.edit()
                                                                .putString(
                                                                        "user_avatar_url",
                                                                        avatarPath
                                                                )
                                                                .apply()
                                                        showAvatarDialog = false
                                                    }
                            ) {
                                AssetImage(
                                        path = avatarPath,
                                        contentDescription = "Avatar Option",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                        }
                    }
                },
                containerColor = AppSurface,
                confirmButton = {
                    TextButton(onClick = { showAvatarDialog = false }) {
                        Text(stringResource(R.string.profile_cancel), color = AppTextSecondary)
                    }
                }
        )
    }
}

@Composable
fun TopBarAccount() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text = stringResource(R.string.profile_my_account), style = AppTextStyles.headline)
    }
}

@Composable
fun UserProfileHeader(
        userName: String,
        avatarUri: String?,
        vistosCount: String,
        enListaCount: String,
        onEditClick: () -> Unit,
        onAvatarClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(10.dp))
        Box {
            Box(modifier = Modifier.size(104.dp).border(2.dp, AppPrimary, CircleShape)) {
                val currentAvatar = avatarUri ?: "images/Avatar1.png"
                if (currentAvatar.startsWith("images/")) {
                    AssetImage(
                            path = currentAvatar,
                            contentDescription = "Avatar",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    CommonImage(
                            imageUrl = currentAvatar,
                            contentDescription = "Avatar",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
            }
            Box(
                    modifier =
                            Modifier.align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .background(AppPrimary, CircleShape)
                                    .clickable { onAvatarClick() }
                                    .padding(6.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Avatar",
                        tint = AppTextPrimary,
                        modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = userName, style = AppTextStyles.headline.copy(fontSize = 22.sp))
        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
                label = stringResource(R.string.profile_edit_profile),
                onClick = onEditClick,
                variant = ButtonVariant.Outlined
        )
        Spacer(modifier = Modifier.height(30.dp))

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(value = vistosCount, label = stringResource(R.string.profile_watched))
            HiveDivider()
            StatItem(value = enListaCount, label = stringResource(R.string.profile_in_list))
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
                text = label,
                color = AppTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.0.sp
        )
    }
}

@Composable
fun HiveDivider() {
    Box(modifier = Modifier.height(30.dp).width(1.dp).background(AppTextPrimary.copy(alpha = 0.25f)))
}

@Composable
fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
            modifier =
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppSurface)
    ) { content() }
}

@Composable
fun SettingsItem(
        icon: ImageVector,
        title: String,
        subtitle: String? = null,
        iconTint: Color = AppSuccess,
        onClick: () -> Unit = {}
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable { onClick() }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier =
                        Modifier.background(
                                        AppTextPrimary.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp)
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AppTextStyles.cardTitle)
            subtitle?.let { Text(text = it, style = AppTextStyles.subtitle) }
        }
        Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = AppTextSecondary,
                modifier = Modifier.size(24.dp)
        )
    }
}
