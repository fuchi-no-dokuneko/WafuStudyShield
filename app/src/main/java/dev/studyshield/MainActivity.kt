package dev.studyshield

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.studyshield.domain.CompanionPack
import dev.studyshield.domain.CompanionSide
import dev.studyshield.domain.ReminderIntensity
import dev.studyshield.media.PromptAudioResult
import dev.studyshield.overlay.FocusOverlayContent
import dev.studyshield.overlay.LayoutResolver
import dev.studyshield.storage.FocusEventEntity
import dev.studyshield.ui.CompanionExport
import dev.studyshield.ui.MainUiState
import dev.studyshield.ui.MainViewModel
import dev.studyshield.ui.ProfileEditorState
import dev.studyshield.ui.StudyShieldTheme
import dev.studyshield.ui.previewReminder
import java.time.DayOfWeek

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyShieldTheme {
                val context = LocalContext.current
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                var previewVisible by remember { mutableStateOf(false) }
                var activeExport by remember { mutableStateOf<CompanionExport?>(null) }
                val characterPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    uri?.let {
                        PersistedUriPermissionStore(context).persistReadPermission(it)
                        viewModel.updateCompanionCharacterImage(it.toString())
                    }
                }
                val wallpaperPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    uri?.let {
                        PersistedUriPermissionStore(context).persistReadPermission(it)
                        viewModel.updateCompanionWallpaper(it.toString())
                    }
                }
                val audioPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let {
                        PersistedUriPermissionStore(context).persistReadPermission(it)
                        viewModel.updateTriggeredAudio(it.toString())
                    }
                }
                val manifestPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let {
                        PersistedUriPermissionStore(context).persistReadPermission(it)
                        viewModel.importCompanionPackage(it)
                    }
                }
                val exportPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/zip")
                ) { uri ->
                    val export = activeExport
                    if (uri != null && export != null) {
                        writeBytes(context, uri, export.bytes)
                    }
                    activeExport = null
                    viewModel.consumeCompanionExport()
                }
                LaunchedEffect(state.pendingCompanionExport?.id) {
                    val export = state.pendingCompanionExport ?: return@LaunchedEffect
                    activeExport = export
                    exportPicker.launch(export.fileName)
                }
                StudyShieldApp(
                    state = state,
                    onOpenAccessibility = {
                        startActivity(AccessibilityStatus(this).settingsIntent())
                    },
                    onRefresh = viewModel::refreshPlatformState,
                    onName = viewModel::updateName,
                    onEnabled = viewModel::updateEnabled,
                    onIntensity = viewModel::updateIntensity,
                    onSelectProfile = viewModel::selectProfile,
                    onNewProfile = viewModel::newProfile,
                    onCompanionPack = viewModel::updateCompanionPack,
                    onPickCharacter = {
                        characterPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onClearCharacter = { viewModel.updateCompanionCharacterImage(null) },
                    onPickWallpaper = {
                        wallpaperPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onClearWallpaper = { viewModel.updateCompanionWallpaper(null) },
                    onPickAudio = { audioPicker.launch(arrayOf("audio/mpeg", "audio/mp3", "audio/*")) },
                    onClearAudio = { viewModel.updateTriggeredAudio(null) },
                    onImportManifest = { manifestPicker.launch(arrayOf("application/zip", "application/json", "text/*", "*/*")) },
                    onExportManifest = viewModel::requestCompanionExport,
                    onPreview = { previewVisible = true },
                    onToggleDay = viewModel::toggleDay,
                    onStart = viewModel::updateTimeRangeStart,
                    onEnd = viewModel::updateTimeRangeEnd,
                    onAddRange = viewModel::addTimeRange,
                    onRemoveRange = viewModel::removeTimeRange,
                    onToggleTarget = viewModel::toggleTarget,
                    onAppQuery = viewModel::updateAppQuery,
                    onSide = viewModel::updateSide,
                    onReduceMotion = viewModel::updateReduceMotion,
                    onCharacterScale = viewModel::updateCharacterScale,
                    onCharacterOpacity = viewModel::updateCharacterOpacity,
                    onCharacterAnchorX = viewModel::updateCharacterAnchorX,
                    onCharacterAnchorY = viewModel::updateCharacterAnchorY,
                    onBubbleAnchorX = viewModel::updateBubbleAnchorX,
                    onBubbleAnchorY = viewModel::updateBubbleAnchorY,
                    onOverlayDim = viewModel::updateOverlayDim,
                    onWallpaperScale = viewModel::updateWallpaperScale,
                    onWallpaperOffsetX = viewModel::updateWallpaperOffsetX,
                    onWallpaperOffsetY = viewModel::updateWallpaperOffsetY,
                    onSave = viewModel::save,
                    onClearData = viewModel::clearUserData
                )
                if (previewVisible) {
                    OverlayPreviewDialog(
                        state = state,
                        onDismiss = { previewVisible = false }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPlatformState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyShieldApp(
    state: MainUiState,
    onOpenAccessibility: () -> Unit,
    onRefresh: () -> Unit,
    onName: (String) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onIntensity: (ReminderIntensity) -> Unit,
    onSelectProfile: (Long) -> Unit,
    onNewProfile: () -> Unit,
    onCompanionPack: (Long?) -> Unit,
    onPickCharacter: () -> Unit,
    onClearCharacter: () -> Unit,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    onPickAudio: () -> Unit,
    onClearAudio: () -> Unit,
    onImportManifest: () -> Unit,
    onExportManifest: () -> Unit,
    onPreview: () -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onStart: (Int, String) -> Unit,
    onEnd: (Int, String) -> Unit,
    onAddRange: () -> Unit,
    onRemoveRange: (Int) -> Unit,
    onToggleTarget: (String) -> Unit,
    onAppQuery: (String) -> Unit,
    onSide: (CompanionSide) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    onCharacterScale: (Float) -> Unit,
    onCharacterOpacity: (Float) -> Unit,
    onCharacterAnchorX: (Float) -> Unit,
    onCharacterAnchorY: (Float) -> Unit,
    onBubbleAnchorX: (Float) -> Unit,
    onBubbleAnchorY: (Float) -> Unit,
    onOverlayDim: (Float) -> Unit,
    onWallpaperScale: (Float) -> Unit,
    onWallpaperOffsetX: (Float) -> Unit,
    onWallpaperOffsetY: (Float) -> Unit,
    onSave: () -> Unit,
    onClearData: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StudyShield") },
                actions = {
                    OutlinedButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StatusPanel(
                    accessibilityEnabled = state.accessibilityEnabled,
                    onOpenAccessibility = onOpenAccessibility
                )
            }
            item {
                ProfileSelectorPanel(
                    profiles = state.profiles,
                    selectedProfileId = state.editor.profileId,
                    onSelectProfile = onSelectProfile,
                    onNewProfile = onNewProfile
                )
            }
            item {
                ProfilePanel(
                    editor = state.editor,
                    saving = state.saving,
                    onName = onName,
                    onEnabled = onEnabled,
                    onIntensity = onIntensity,
                    onToggleDay = onToggleDay,
                    onStart = onStart,
                    onEnd = onEnd,
                    onAddRange = onAddRange,
                    onRemoveRange = onRemoveRange,
                    onSide = onSide,
                    onReduceMotion = onReduceMotion,
                    onSave = onSave
                )
            }
            item {
                CompanionPanel(
                    editor = state.editor,
                    packs = state.companionPacks,
                    onCompanionPack = onCompanionPack,
                    onPickCharacter = onPickCharacter,
                    onClearCharacter = onClearCharacter,
                    onPickWallpaper = onPickWallpaper,
                    onClearWallpaper = onClearWallpaper,
                    onPickAudio = onPickAudio,
                    onClearAudio = onClearAudio,
                    onImportManifest = onImportManifest,
                    onExportManifest = onExportManifest,
                    onPreview = onPreview,
                    onSide = onSide,
                    onReduceMotion = onReduceMotion,
                    onCharacterScale = onCharacterScale,
                    onCharacterOpacity = onCharacterOpacity,
                    onCharacterAnchorX = onCharacterAnchorX,
                    onCharacterAnchorY = onCharacterAnchorY,
                    onBubbleAnchorX = onBubbleAnchorX,
                    onBubbleAnchorY = onBubbleAnchorY,
                    onOverlayDim = onOverlayDim,
                    onWallpaperScale = onWallpaperScale,
                    onWallpaperOffsetX = onWallpaperOffsetX,
                    onWallpaperOffsetY = onWallpaperOffsetY
                )
            }
            item {
                TargetAppsPanel(
                    apps = state.installedApps,
                    selectedPackages = state.editor.selectedPackages,
                    appQuery = state.editor.appQuery,
                    onAppQuery = onAppQuery,
                    onToggleTarget = onToggleTarget
                )
            }
            item {
                RecordsPanel(
                    todayCount = state.todayCount,
                    recentEvents = state.recentEvents,
                    onClearData = onClearData
                )
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@Composable
private fun ProfileSelectorPanel(
    profiles: List<dev.studyshield.domain.FocusProfile>,
    selectedProfileId: Long,
    onSelectProfile: (Long) -> Unit,
    onNewProfile: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(icon = Icons.Outlined.Tune, title = "Profiles")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.forEach { profile ->
                    FilterChip(
                        selected = profile.id == selectedProfileId,
                        onClick = { onSelectProfile(profile.id) },
                        label = { Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
            OutlinedButton(onClick = onNewProfile, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("New profile")
            }
        }
    }
}

@Composable
private fun StatusPanel(
    accessibilityEnabled: Boolean,
    onOpenAccessibility: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Shield, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("Focus service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (accessibilityEnabled) "Enabled" else "Not enabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onOpenAccessibility) {
                    Icon(Icons.Outlined.AccessibilityNew, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Settings")
                }
            }
            DisclosureText()
        }
    }
}

@Composable
private fun DisclosureText() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = "The service compares only the foreground package name with apps and times you choose. It does not read messages, passwords, screen text, contacts, photos, location, browser history, or Accessibility node content.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePanel(
    editor: ProfileEditorState,
    saving: Boolean,
    onName: (String) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onIntensity: (ReminderIntensity) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onStart: (Int, String) -> Unit,
    onEnd: (Int, String) -> Unit,
    onAddRange: () -> Unit,
    onRemoveRange: (Int) -> Unit,
    onSide: (CompanionSide) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(icon = Icons.Outlined.Tune, title = "Profile")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = editor.name,
                    onValueChange = onName,
                    singleLine = true,
                    label = { Text("Name") }
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Active", style = MaterialTheme.typography.labelMedium)
                    Switch(checked = editor.enabled, onCheckedChange = onEnabled)
                }
            }

            Text("Intensity", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderIntensity.entries.forEach { intensity ->
                    FilterChip(
                        selected = editor.intensity == intensity,
                        onClick = { onIntensity(intensity) },
                        label = { Text(intensity.name) }
                    )
                }
            }

            Text("Days", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in editor.days,
                        onClick = { onToggleDay(day) },
                        label = { Text(day.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) }
                    )
                }
            }

            Text("Time ranges", style = MaterialTheme.typography.labelLarge)
            editor.timeRanges.forEachIndexed { index, range ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = range.start,
                        onValueChange = { onStart(index, it) },
                        singleLine = true,
                        label = { Text("Start ${index + 1}") }
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = range.end,
                        onValueChange = { onEnd(index, it) },
                        singleLine = true,
                        label = { Text("End ${index + 1}") }
                    )
                    IconButton(
                        onClick = { onRemoveRange(index) },
                        enabled = editor.timeRanges.size > 1
                    ) {
                        Icon(Icons.Outlined.RemoveCircle, contentDescription = "Remove time range")
                    }
                }
            }
            OutlinedButton(onClick = onAddRange, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Add time range")
            }

            editor.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (saving) "Saving" else "Save profile")
            }
        }
    }
}

@Composable
private fun CompanionPanel(
    editor: ProfileEditorState,
    packs: List<CompanionPack>,
    onCompanionPack: (Long?) -> Unit,
    onPickCharacter: () -> Unit,
    onClearCharacter: () -> Unit,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    onPickAudio: () -> Unit,
    onClearAudio: () -> Unit,
    onImportManifest: () -> Unit,
    onExportManifest: () -> Unit,
    onPreview: () -> Unit,
    onSide: (CompanionSide) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    onCharacterScale: (Float) -> Unit,
    onCharacterOpacity: (Float) -> Unit,
    onCharacterAnchorX: (Float) -> Unit,
    onCharacterAnchorY: (Float) -> Unit,
    onBubbleAnchorX: (Float) -> Unit,
    onBubbleAnchorY: (Float) -> Unit,
    onOverlayDim: (Float) -> Unit,
    onWallpaperScale: (Float) -> Unit,
    onWallpaperOffsetX: (Float) -> Unit,
    onWallpaperOffsetY: (Float) -> Unit
) {
    val selectedPack = packs.firstOrNull { it.id == editor.companionPackId }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(icon = Icons.Outlined.Image, title = "Companion")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = editor.companionPackId == null,
                    onClick = { onCompanionPack(null) },
                    label = { Text("Simple") }
                )
                packs.forEach { pack ->
                    FilterChip(
                        selected = editor.companionPackId == pack.id,
                        onClick = { onCompanionPack(pack.id) },
                        label = { Text(pack.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            selectedPack?.let { pack ->
                Text(
                    "${pack.author} · ${pack.license} · ${pack.contentRating}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPickCharacter, enabled = selectedPack != null) {
                    Icon(Icons.Outlined.Image, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Character")
                }
                OutlinedButton(onClick = onClearCharacter, enabled = selectedPack?.characterImageUri != null) {
                    Icon(Icons.Outlined.Image, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Clear")
                }
                Button(onClick = onPickWallpaper, enabled = selectedPack != null) {
                    Icon(Icons.Outlined.Wallpaper, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Wallpaper")
                }
                OutlinedButton(onClick = onClearWallpaper, enabled = selectedPack?.wallpaperUri != null) {
                    Icon(Icons.Outlined.Wallpaper, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Clear")
                }
                Button(onClick = onPickAudio, enabled = selectedPack != null) {
                    Icon(Icons.Outlined.Audiotrack, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("MP3")
                }
                OutlinedButton(onClick = onClearAudio, enabled = selectedPack != null) {
                    Icon(Icons.Outlined.Audiotrack, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Clear")
                }
                OutlinedButton(onClick = onImportManifest) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Import")
                }
                OutlinedButton(onClick = onExportManifest, enabled = selectedPack != null) {
                    Icon(Icons.Outlined.IosShare, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Export")
                }
                Button(onClick = onPreview) {
                    Icon(Icons.Outlined.SmartDisplay, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Preview")
                }
            }

            Text("Side", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    CompanionSide.PackDefault to "Pack",
                    CompanionSide.Left to "Left",
                    CompanionSide.Right to "Right",
                    CompanionSide.RandomPerEvent to "Random"
                ).forEach { (side, label) ->
                    FilterChip(
                        selected = editor.layout.sidePreference == side,
                        onClick = { onSide(side) },
                        label = { Text(label) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reduce motion", modifier = Modifier.weight(1f))
                Switch(checked = editor.layout.reduceMotion, onCheckedChange = onReduceMotion)
            }

            SliderSetting("Character X", editor.layout.characterAnchorX, 0f..1f, onCharacterAnchorX)
            SliderSetting("Character Y", editor.layout.characterAnchorY, 0f..1f, onCharacterAnchorY)
            SliderSetting("Character size", editor.layout.characterScale, 0.35f..2.5f, onCharacterScale)
            SliderSetting("Character opacity", editor.layout.characterOpacity, 0f..1f, onCharacterOpacity)
            SliderSetting("Bubble X", editor.layout.bubbleAnchorX, 0f..1f, onBubbleAnchorX)
            SliderSetting("Bubble Y", editor.layout.bubbleAnchorY, 0f..1f, onBubbleAnchorY)
            SliderSetting("Dim", editor.layout.overlayDim, 0f..1f, onOverlayDim)
            SliderSetting("Wallpaper scale", editor.layout.wallpaperScale, 1f..3f, onWallpaperScale)
            SliderSetting("Wallpaper X", editor.layout.wallpaperOffsetX, -1f..1f, onWallpaperOffsetX)
            SliderSetting("Wallpaper Y", editor.layout.wallpaperOffsetY, -1f..1f, onWallpaperOffsetY)
        }
    }
}

@Composable
private fun OverlayPreviewDialog(
    state: MainUiState,
    onDismiss: () -> Unit
) {
    val reminder = previewReminder(state)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        FocusOverlayContent(
            reminder = reminder,
            dialogueText = "This preview uses your current profile, companion, wallpaper, and layout settings.",
            audioResult = PromptAudioResult.TextFallback("Preview mode."),
            layout = LayoutResolver().resolve(reminder.profile.layout, reminder.resolvedSide),
            onReturnHome = onDismiss,
            onSkipOnce = onDismiss,
            onEndFocus = onDismiss
        )
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range
        )
    }
}

@Composable
private fun TargetAppsPanel(
    apps: List<InstalledAppInfo>,
    selectedPackages: Set<String>,
    appQuery: String,
    onAppQuery: (String) -> Unit,
    onToggleTarget: (String) -> Unit
) {
    val visibleApps = apps.filter { app ->
        val query = appQuery.trim()
        query.isEmpty() ||
            app.label.contains(query, ignoreCase = true) ||
            app.packageName.contains(query, ignoreCase = true)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(icon = Icons.Outlined.Apps, title = "Target apps")
            Text(
                "${selectedPackages.size} selected, ${visibleApps.size} shown",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = appQuery,
                onValueChange = onAppQuery,
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                label = { Text("Filter installed apps") }
            )
            visibleApps.forEach { app ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = app.packageName in selectedPackages,
                        onCheckedChange = { onToggleTarget(app.packageName) }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            app.packageName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (visibleApps.isEmpty()) {
                Text("No installed apps match this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) {
    runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(bytes)
        }
    }
}

@Composable
private fun RecordsPanel(
    todayCount: Int,
    recentEvents: List<FocusEventEntity>,
    onClearData: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(icon = Icons.Outlined.Delete, title = "Local records")
            Text("Today: $todayCount reminders", style = MaterialTheme.typography.titleMedium)
            if (recentEvents.isEmpty()) {
                Text("No local reminder records yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                recentEvents.take(5).forEach { event ->
                    Text(
                        "${event.targetLabel}: ${event.outcome}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            OutlinedButton(onClick = onClearData, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Delete my data")
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}
