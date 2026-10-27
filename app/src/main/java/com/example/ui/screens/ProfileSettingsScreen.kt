package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.model.CategoryEntity
import com.example.data.repository.DayformRepository
import com.example.notifications.ReminderScheduler
import com.example.ui.DayformUiState
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.LavenderPurple
import com.example.ui.theme.NeonLime
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PrimaryDarkText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.StarterCategoryColors
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.pressScaleClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val DEVELOPER_CREDIT_NAME = "Rahmathullah | Designwithrahma"
const val DEVELOPER_LINKTREE_URL = "https://linktr.ee/designwithrahma"

fun openExternalHttpsUrl(
    context: Context,
    url: String,
    onError: (String) -> Unit
): Boolean {
    return try {
        val uri = Uri.parse(url.trim())
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
            onError("Invalid external HTTPS link")
            return false
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        onError("No browser available to open link")
        false
    } catch (e: Exception) {
        onError("Could not open link: ${e.localizedMessage ?: "Error"}")
        false
    }
}

@Composable
fun ProfileSettingsScreen(
    uiState: DayformUiState,
    repository: DayformRepository,
    onUpdateDisplayName: (String) -> Unit,
    onCreateCategory: (String, String) -> Unit,
    onUpdateCategory: (CategoryEntity, String, String) -> Unit,
    onDeleteCategory: (Long, Long?) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleWeekStartsMonday: (Boolean) -> Unit,
    onToggleUse24Hour: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var nameDraft by remember(uiState.settings.displayName) {
        mutableStateOf(uiState.settings.displayName)
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    var pendingImportPreview by remember {
        mutableStateOf<DayformRepository.BackupPreview?>(null)
    }
    val isImportInProgress by repository.isImportInProgress.collectAsStateWithLifecycle()

    // Export JSON launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = repository.exportBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    onShowMessage("Exported DAYFORM tasks backup successfully")
                } catch (e: Exception) {
                    onShowMessage("Export failed: ${e.localizedMessage ?: "Error"}")
                }
            }
        }
    }

    // Import JSON launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val rawJson = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            input.bufferedReader(Charsets.UTF_8).readText()
                        } ?: ""
                    }
                    val validation = repository.validateBackupJson(rawJson)
                    validation.onSuccess { preview ->
                        pendingImportPreview = preview
                    }.onFailure { err ->
                        onShowMessage(err.localizedMessage ?: "Invalid backup file")
                    }
                } catch (e: Exception) {
                    onShowMessage("Could not read file: ${e.localizedMessage ?: "Error"}")
                }
            }
        }
    }

    val initials = uiState.settings.displayName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "D" }

    val canExactAlarms = remember { ReminderScheduler.canScheduleExactAlarms(context) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .padding(horizontal = 20.dp)
            .testTag("profile_settings_screen"),
        contentPadding = PaddingValues(top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "Profile & Settings",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )
        }

        // 1. Profile Identity Card + Stats
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(LavenderPurple, NeonLime)
                                    )
                                )
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(CharcoalElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = uiState.settings.displayName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White,
                                modifier = Modifier.testTag("profile_display_name")
                            )
                            Text(
                                text = "Personal Offline Workspace",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            nameDraft = uiState.settings.displayName
                            showEditNameDialog = true
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CharcoalElevated)
                            .testTag("profile_edit_name_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit display name",
                            tint = NeonLime,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CharcoalElevated)
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = uiState.completedAllTimeCount.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = NeonLime
                            )
                            Text(
                                text = "Total Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CharcoalElevated)
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "${uiState.insights.currentDailyStreak} days",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = LavenderPurple
                            )
                            Text(
                                text = "Current Streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText
                            )
                        }
                    }
                }
            }
        }

        // 2. Manage Categories Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Manage Categories",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Create, rename, or recolor project categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }

                    Button(
                        onClick = { showAddCategoryDialog = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonLime,
                            contentColor = PrimaryDarkText
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("profile_add_category_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.animateContentSize(
                        animationSpec = DayformMotion.contentSizeSpec(reducedMotion)
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(CharcoalElevated)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(category.colorHex))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { editingCategory = category },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit category ${category.name}",
                                        tint = SecondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { deletingCategory = category },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete category ${category.name}",
                                        tint = OverdueRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Preferences & Feedback Toggles
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Preferences & Feedback",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )

                PreferenceToggleRow(
                    title = "Task Reminders & Notifications",
                    subtitle = if (canExactAlarms) {
                        "Exact reminders active for scheduled tasks"
                    } else {
                        "Standard OS window reminders enabled"
                    },
                    checked = uiState.settings.notificationsEnabled,
                    onCheckedChange = onToggleNotifications,
                    testTag = "switch_notifications"
                )

                PreferenceToggleRow(
                    title = "Haptic Feedback",
                    subtitle = "Tactile vibration when completing tasks and subtasks",
                    checked = uiState.settings.hapticFeedbackEnabled,
                    onCheckedChange = onToggleHaptics,
                    testTag = "switch_haptics"
                )

                PreferenceToggleRow(
                    title = "Sound Feedback",
                    subtitle = "Subtle audio chime on task completion",
                    checked = uiState.settings.soundFeedbackEnabled,
                    onCheckedChange = onToggleSound,
                    testTag = "switch_sound"
                )

                PreferenceToggleRow(
                    title = "Start Week on Monday",
                    subtitle = if (uiState.settings.weekStartsOnMonday) "Calendar starts on Monday" else "Calendar starts on Sunday",
                    checked = uiState.settings.weekStartsOnMonday,
                    onCheckedChange = onToggleWeekStartsMonday,
                    testTag = "switch_week_start"
                )

                PreferenceToggleRow(
                    title = "24-Hour Time Format",
                    subtitle = if (uiState.settings.use24HourFormat) "e.g., 14:30" else "e.g., 2:30pm",
                    checked = uiState.settings.use24HourFormat,
                    onCheckedChange = onToggleUse24Hour,
                    testTag = "switch_24h_time"
                )
            }
        }

        // 4. Data Management (JSON Export/Import)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
                Text(
                    text = "Your tasks never leave your device. Automatic cloud backup and device transfer are disabled for strict local privacy — export a JSON backup before switching devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("dayform_backup.json") },
                        enabled = !isImportInProgress,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonLime,
                            contentColor = PrimaryDarkText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_json_button")
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON", fontWeight = FontWeight.ExtraBold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (!isImportInProgress) {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }
                        },
                        enabled = !isImportInProgress,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("import_json_button")
                    ) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isImportInProgress) "Importing..." else "Import JSON",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. About DAYFORM + Permanent Developer Credit
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp)
                    .testTag("about_dayform_card"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "About DAYFORM",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "DAYFORM — Your Daily Task Manager",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = NeonLime
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CharcoalElevated)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("about_version_badge")
                    ) {
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = "Designed for focused personal productivity with local Room persistence, offline reliability, and zero tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                HorizontalDivider(color = CharcoalBorder, thickness = 1.dp)

                // Developer Credit Block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CharcoalElevated)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("developer_credit_block"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Developed by",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                    Text(
                        text = DEVELOPER_CREDIT_NAME,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White,
                        modifier = Modifier.testTag("developer_credit_name")
                    )
                }

                // Interactive Linktree Row (Minimum 48.dp touch target + accessibility description)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CharcoalElevated)
                        .border(1.dp, NeonLime.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .pressScaleClickable(
                            pressedScale = 0.98f,
                            onClick = {
                                openExternalHttpsUrl(
                                    context = context,
                                    url = DEVELOPER_LINKTREE_URL,
                                    onError = onShowMessage
                                )
                            }
                        )
                        .semantics {
                            contentDescription = "Open Designwithrahma portfolio and links"
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("developer_credit_linktree_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Portfolio & Links",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                        Text(
                            text = DEVELOPER_LINKTREE_URL,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonLime,
                            modifier = Modifier.testTag("developer_credit_linktree_url")
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open Designwithrahma portfolio and links",
                        tint = NeonLime,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Edit Display Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = CharcoalSurface,
            title = { Text("Edit Display Name", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonLime,
                        unfocusedBorderColor = CharcoalBorder
                    ),
                    modifier = Modifier.testTag("dialog_display_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDisplayName(nameDraft)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = PrimaryDarkText
                    ),
                    modifier = Modifier.testTag("dialog_save_name_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    // Add / Edit Category Dialog
    if (showAddCategoryDialog || editingCategory != null) {
        val target = editingCategory
        var catName by remember(target) { mutableStateOf(target?.name ?: "") }
        var catColor by remember(target) {
            mutableStateOf(target?.colorHex ?: StarterCategoryColors.first())
        }
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                editingCategory = null
            },
            containerColor = CharcoalSurface,
            title = {
                Text(
                    text = if (target == null) "Create Category" else "Edit Category",
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        placeholder = { Text("Category name", color = SecondaryText) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonLime,
                            unfocusedBorderColor = CharcoalBorder
                        ),
                        modifier = Modifier.testTag("category_dialog_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StarterCategoryColors.forEach { hex ->
                            val selected = catColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(
                                        width = if (selected) 2.5.dp else 0.dp,
                                        color = if (selected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { catColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            if (target == null) {
                                onCreateCategory(catName.trim(), catColor)
                            } else {
                                onUpdateCategory(target, catName.trim(), catColor)
                            }
                            showAddCategoryDialog = false
                            editingCategory = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = PrimaryDarkText
                    ),
                    modifier = Modifier.testTag("category_dialog_save_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCategoryDialog = false
                        editingCategory = null
                    }
                ) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    // Safe Category Deletion Dialog (preserves tasks as uncategorized or moves to another category)
    deletingCategory?.let { catToDelete ->
        val otherCategories = uiState.categories.filter { it.id != catToDelete.id }
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            containerColor = CharcoalSurface,
            title = { Text("Delete '${catToDelete.name}'?", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tasks in '${catToDelete.name}' will be preserved as Uncategorized unless you choose another category below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                    otherCategories.forEach { targetCat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CharcoalElevated)
                                .clickable {
                                    onDeleteCategory(catToDelete.id, targetCat.id)
                                    deletingCategory = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(targetCat.colorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Move tasks to ${targetCat.name}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(catToDelete.id, null)
                        deletingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OverdueRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete (Keep Uncategorized)")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    // Validated Backup Import Confirmation Dialog (prevents silent data overwrite!)
    pendingImportPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = {
                if (!isImportInProgress) {
                    pendingImportPreview = null
                }
            },
            containerColor = CharcoalSurface,
            title = { Text("Import DAYFORM Backup", color = Color.White) },
            text = {
                Text(
                    text = "Validated backup containing ${preview.taskCount} tasks, ${preview.subtaskCount} subtasks, and ${preview.categoryCount} categories.\n\nWould you like to merge these with your current tasks or replace existing data?",
                    color = SecondaryText
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (isImportInProgress) return@OutlinedButton
                            val json = preview.rawJson
                            pendingImportPreview = null
                            scope.launch {
                                val res = repository.importValidatedBackup(json, replaceExisting = true)
                                res.onSuccess { count ->
                                    onShowMessage("Replaced workspace with $count imported tasks")
                                }.onFailure { err ->
                                    onShowMessage("Import failed: ${err.localizedMessage}")
                                }
                            }
                        },
                        enabled = !isImportInProgress,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OverdueRed),
                        modifier = Modifier.testTag("dialog_import_replace_all_button")
                    ) {
                        Text("Replace All")
                    }

                    Button(
                        onClick = {
                            if (isImportInProgress) return@Button
                            val json = preview.rawJson
                            pendingImportPreview = null
                            scope.launch {
                                val res = repository.importValidatedBackup(json, replaceExisting = false)
                                res.onSuccess { count ->
                                    onShowMessage("Merged $count tasks into your workspace")
                                }.onFailure { err ->
                                    onShowMessage("Import failed: ${err.localizedMessage}")
                                }
                            }
                        },
                        enabled = !isImportInProgress,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonLime,
                            contentColor = PrimaryDarkText
                        ),
                        modifier = Modifier.testTag("dialog_import_merge_button")
                    ) {
                        Text("Merge")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isImportInProgress) {
                            pendingImportPreview = null
                        }
                    },
                    enabled = !isImportInProgress
                ) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryDarkText,
                checkedTrackColor = NeonLime,
                uncheckedThumbColor = SecondaryText,
                uncheckedTrackColor = CharcoalElevated
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
