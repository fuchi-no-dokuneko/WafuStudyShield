package dev.studyshield.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.studyshield.companion.CompanionPackArchiveExporter
import dev.studyshield.companion.CompanionPackArchiveImporter
import dev.studyshield.AccessibilityStatus
import dev.studyshield.InstalledAppInfo
import dev.studyshield.InstalledAppsReader
import dev.studyshield.NotificationPermissionStatus
import dev.studyshield.PersistedUriPermissionStore
import dev.studyshield.StudyShieldApplication
import dev.studyshield.UsageAccessStatus
import dev.studyshield.companion.CompanionPackManifestCodec
import dev.studyshield.companion.CompanionPackValidationResult
import dev.studyshield.companion.CompanionPackValidator
import dev.studyshield.domain.BackgroundMode
import dev.studyshield.domain.CompanionSide
import dev.studyshield.domain.CompanionPack
import dev.studyshield.domain.FocusProfile
import dev.studyshield.domain.OverlayLayout
import dev.studyshield.domain.ReminderIntensity
import dev.studyshield.domain.ScheduleRule
import dev.studyshield.domain.TargetApp
import dev.studyshield.domain.TimeRange
import dev.studyshield.storage.FocusEventEntity
import dev.studyshield.storage.StudyShieldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId

data class TimeRangeEditorState(
    val ruleId: Long = 0,
    val start: String = "18:00",
    val end: String = "21:00"
)

data class ProfileEditorState(
    val profileId: Long = 0,
    val name: String = "Self-study",
    val enabled: Boolean = false,
    val intensity: ReminderIntensity = ReminderIntensity.Gentle,
    val companionPackId: Long? = null,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val timeRanges: List<TimeRangeEditorState> = listOf(TimeRangeEditorState()),
    val selectedPackages: Set<String> = emptySet(),
    val appQuery: String = "",
    val layout: OverlayLayout = OverlayLayout(),
    val error: String? = null
)

data class MainUiState(
    val profiles: List<FocusProfile> = emptyList(),
    val companionPacks: List<CompanionPack> = emptyList(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val editor: ProfileEditorState = ProfileEditorState(),
    val accessibilityEnabled: Boolean = false,
    val usageAccessEnabled: Boolean = false,
    val notificationPermissionEnabled: Boolean = false,
    val todayCount: Int = 0,
    val recentEvents: List<FocusEventEntity> = emptyList(),
    val saving: Boolean = false,
    val pendingCompanionExport: CompanionExport? = null
)

data class CompanionExport(
    val id: Long,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudyShieldApplication
    private val repository: StudyShieldRepository = app.repository
    private val installedAppsReader = InstalledAppsReader(application)
    private val accessibilityStatus = AccessibilityStatus(application)
    private val usageAccessStatus = UsageAccessStatus(application)
    private val notificationPermissionStatus = NotificationPermissionStatus(application)
    private val uriPermissionStore = PersistedUriPermissionStore(application)
    private val editor = MutableStateFlow(ProfileEditorState())
    private val installedApps = MutableStateFlow(emptyList<InstalledAppInfo>())
    private val accessibilityEnabled = MutableStateFlow(false)
    private val usageAccessEnabled = MutableStateFlow(false)
    private val notificationPermissionEnabled = MutableStateFlow(false)
    private val saving = MutableStateFlow(false)
    private val pendingCompanionExport = MutableStateFlow<CompanionExport?>(null)
    private var editorSeeded = false

    val uiState: StateFlow<MainUiState> = combine(
        repository.observeProfiles(),
        repository.observeCompanionPacks(),
        installedApps,
        editor,
        accessibilityEnabled,
        usageAccessEnabled,
        notificationPermissionEnabled,
        repository.observeTodayEventCount(),
        repository.observeRecentEvents(),
        saving,
        pendingCompanionExport
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val profiles = values[0] as List<FocusProfile>
        @Suppress("UNCHECKED_CAST")
        val packs = values[1] as List<CompanionPack>
        @Suppress("UNCHECKED_CAST")
        val apps = values[2] as List<InstalledAppInfo>
        val editorState = values[3] as ProfileEditorState
        val access = values[4] as Boolean
        val usageAccess = values[5] as Boolean
        val notificationAccess = values[6] as Boolean
        val count = values[7] as Int
        @Suppress("UNCHECKED_CAST")
        val events = values[8] as List<FocusEventEntity>
        val isSaving = values[9] as Boolean
        val export = values[10] as CompanionExport?
        MainUiState(
            profiles = profiles,
            companionPacks = packs,
            installedApps = apps,
            editor = editorState,
            accessibilityEnabled = access,
            usageAccessEnabled = usageAccess,
            notificationPermissionEnabled = notificationAccess,
            todayCount = count,
            recentEvents = events,
            saving = isSaving,
            pendingCompanionExport = export
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        refreshPlatformState()
        viewModelScope.launch {
            repository.observeProfiles()
                .map { profiles -> profiles.firstOrNull() }
                .distinctUntilChanged()
                .collect { firstProfile ->
                    if (!editorSeeded && firstProfile != null) {
                        editor.value = firstProfile.toEditorState()
                        editorSeeded = true
                    }
                }
        }
    }

    fun refreshPlatformState() {
        installedApps.value = installedAppsReader.launcherApps()
        accessibilityEnabled.value = accessibilityStatus.isStudyShieldEnabled()
        usageAccessEnabled.value = usageAccessStatus.isUsageAccessEnabled()
        notificationPermissionEnabled.value = notificationPermissionStatus.isNotificationPermissionEnabled()
    }

    fun updateName(value: String) {
        updateEditor { it.copy(name = value, error = null) }
    }

    fun updateEnabled(value: Boolean) {
        updateEditor { it.copy(enabled = value) }
    }

    fun updateIntensity(value: ReminderIntensity) {
        updateEditor { it.copy(intensity = value) }
    }

    fun toggleDay(day: DayOfWeek) {
        val days = editor.value.days
        updateEditor { state -> state.copy(
            days = if (day in days) days - day else days + day,
            error = null
        ) }
    }

    fun updateTimeRangeStart(index: Int, value: String) {
        updateEditor { state ->
            state.copy(
                timeRanges = state.timeRanges.mapIndexed { itemIndex, range ->
                    if (itemIndex == index) range.copy(start = value) else range
                },
                error = null
            )
        }
    }

    fun updateTimeRangeEnd(index: Int, value: String) {
        updateEditor { state ->
            state.copy(
                timeRanges = state.timeRanges.mapIndexed { itemIndex, range ->
                    if (itemIndex == index) range.copy(end = value) else range
                },
                error = null
            )
        }
    }

    fun addTimeRange() {
        updateEditor { state ->
            state.copy(
                timeRanges = state.timeRanges + TimeRangeEditorState(),
                error = null
            )
        }
    }

    fun removeTimeRange(index: Int) {
        updateEditor { state ->
            if (state.timeRanges.size == 1) {
                state.copy(error = "Keep at least one time range.")
            } else {
                state.copy(
                    timeRanges = state.timeRanges.filterIndexed { itemIndex, _ -> itemIndex != index },
                    error = null
                )
            }
        }
    }

    fun toggleTarget(packageName: String) {
        val selected = editor.value.selectedPackages
        updateEditor { state -> state.copy(
            selectedPackages = if (packageName in selected) selected - packageName else selected + packageName,
            error = null
        ) }
    }

    fun updateAppQuery(value: String) {
        updateEditor { it.copy(appQuery = value) }
    }

    fun updateSide(side: CompanionSide) {
        updateEditor { it.copy(layout = it.layout.copy(sidePreference = side)) }
    }

    fun updateReduceMotion(value: Boolean) {
        updateEditor { it.copy(layout = it.layout.copy(reduceMotion = value)) }
    }

    fun updateRandomizeActionOrder(value: Boolean) {
        updateEditor { it.copy(layout = it.layout.copy(randomizeActionOrder = value)) }
    }

    fun updateCompanionPack(packId: Long?) {
        if (packId == null) {
            updateEditor { it.copy(companionPackId = null, error = null) }
            return
        }
        viewModelScope.launch {
            val pack = repository.listCompanionPacks().firstOrNull { it.id == packId }
            editorSeeded = true
            editor.value = editor.value.copy(
                companionPackId = packId,
                layout = pack?.layout ?: editor.value.layout,
                error = null
            )
        }
    }

    fun updateCharacterScale(value: Float) {
        updateEditor { it.copy(layout = it.layout.copy(characterScale = value)) }
    }

    fun updateCharacterOpacity(value: Float) {
        updateEditor { it.copy(layout = it.layout.copy(characterOpacity = value)) }
    }

    fun updateCharacterAnchorX(value: Float) {
        updateEditor { it.copy(layout = it.layout.copy(characterAnchorX = value)) }
    }

    fun updateCharacterAnchorY(value: Float) {
        updateEditor { it.copy(layout = it.layout.copy(characterAnchorY = value)) }
    }

    fun updateBubbleAnchorX(value: Float) {
        updateEditor { it.copy(layout = it.layout.copy(bubbleAnchorX = value)) }
    }

    fun updateBubbleAnchorY(value: Float) {
        updateEditor { it.copy(layout = it.layout.copy(bubbleAnchorY = value)) }
    }

    fun updateOverlayDim(value: Float) {
        updateEditor { it.copy(layout = it.layout.copy(overlayDim = value)) }
    }

    fun updateWallpaperScale(value: Float) {
        updateEditor { it.copy(layout = it.layout.copy(wallpaperScale = value)) }
    }

    fun updateWallpaperOffsetX(value: Float) {
        updateEditor { it.copy(layout = it.layout.copy(wallpaperOffsetX = value)) }
    }

    fun updateWallpaperOffsetY(value: Float) {
        updateEditor { it.copy(layout = it.layout.copy(wallpaperOffsetY = value)) }
    }

    fun updateCompanionCharacterImage(uri: String?) {
        val packId = selectedPackIdOrSetError() ?: return
        viewModelScope.launch {
            repository.updateCompanionCharacterImage(packId, uri)
            editor.value = editor.value.copy(error = null)
        }
    }

    fun updateCompanionWallpaper(uri: String?) {
        val packId = selectedPackIdOrSetError() ?: return
        viewModelScope.launch {
            repository.updateCompanionWallpaper(packId, uri)
            editor.value = editor.value.copy(
                layout = editor.value.layout.copy(backgroundMode = if (uri == null) BackgroundMode.Solid else BackgroundMode.DimmedWallpaper),
                error = null
            )
        }
    }

    fun updateTriggeredAudio(uri: String?) {
        val packId = selectedPackIdOrSetError() ?: return
        viewModelScope.launch {
            repository.updateTriggeredAudio(packId, uri)
            editor.value = editor.value.copy(error = null)
        }
    }

    fun importCompanionPackage(uri: Uri) {
        val bytes = runCatching {
            app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null) {
            editor.value = editor.value.copy(error = "Companion package could not be read.")
            return
        }
        importCompanionResult(CompanionPackArchiveImporter(app).importBytes(bytes))
    }

    fun importCompanionManifest(json: String) {
        importCompanionResult(CompanionPackValidator().validate(json))
    }

    private fun importCompanionResult(result: CompanionPackValidationResult) {
        when (result) {
            is CompanionPackValidationResult.Invalid -> {
                editor.value = editor.value.copy(error = result.errors.joinToString("; "))
            }
            is CompanionPackValidationResult.Valid -> {
                viewModelScope.launch {
                    val packId = repository.saveCompanionPack(result.bundle)
                    editor.value = editor.value.copy(
                        companionPackId = packId,
                        layout = result.bundle.pack.layout,
                        error = null
                    )
                }
            }
        }
    }

    fun requestCompanionExport() {
        val packId = selectedPackIdOrSetError() ?: return
        viewModelScope.launch {
            val bundle = repository.companionPackBundle(packId)
            if (bundle == null) {
                editor.value = editor.value.copy(error = "Companion pack could not be loaded.")
                return@launch
            }
            pendingCompanionExport.value = CompanionExport(
                id = System.nanoTime(),
                fileName = "${bundle.pack.slug.sanitizedFileStem()}.studyshield-pack.zip",
                mimeType = "application/zip",
                bytes = CompanionPackArchiveExporter(app).exportZip(bundle)
            )
        }
    }

    fun consumeCompanionExport() {
        pendingCompanionExport.value = null
    }

    fun selectProfile(profileId: Long) {
        viewModelScope.launch {
            repository.listProfiles()
                .firstOrNull { it.id == profileId }
                ?.let { profile ->
                    editor.value = profile.toEditorState()
                    editorSeeded = true
                }
        }
    }

    fun newProfile() {
        editorSeeded = true
        editor.value = ProfileEditorState(
            name = "New focus profile",
            enabled = false,
            days = DayOfWeek.entries.toSet(),
            timeRanges = listOf(TimeRangeEditorState()),
            selectedPackages = emptySet()
        )
    }

    fun save() {
        val current = editor.value
        val parsedRanges = current.timeRanges.map { range ->
            ParsedTimeRange(
                ruleId = range.ruleId,
                start = parseTime(range.start),
                end = parseTime(range.end)
            )
        }
        if (current.name.isBlank()) {
            editor.value = current.copy(error = "Name is required.")
            return
        }
        if (current.days.isEmpty()) {
            editor.value = current.copy(error = "Choose at least one day.")
            return
        }
        if (parsedRanges.isEmpty() || parsedRanges.any { it.start == null || it.end == null || it.start == it.end }) {
            editor.value = current.copy(error = "Use 24-hour times like 18:00 and avoid zero-length ranges.")
            return
        }

        viewModelScope.launch {
            saving.value = true
            val appsByPackage = installedApps.value.associateBy { it.packageName }
            val savedId = repository.saveProfile(
                FocusProfile(
                    id = current.profileId,
                    name = current.name.trim(),
                    enabled = current.enabled,
                    intensity = current.intensity,
                    companionPackId = current.companionPackId,
                    schedules = parsedRanges.map { range ->
                        ScheduleRule(
                            id = range.ruleId,
                            profileId = current.profileId,
                            days = current.days,
                            range = TimeRange(requireNotNull(range.start), requireNotNull(range.end)),
                            zoneId = ZoneId.systemDefault()
                        )
                    },
                    targetApps = current.selectedPackages.sorted().map { packageName ->
                        val app = appsByPackage[packageName]
                        TargetApp(packageName, app?.label ?: packageName, enabled = true)
                    },
                    layout = current.layout
                )
            )
            editor.value = current.copy(profileId = savedId, error = null)
            saving.value = false
        }
    }

    fun clearUserData() {
        viewModelScope.launch {
            repository.clearUserData()
            app.ensureBundledCompanionPacks()
            uriPermissionStore.releaseAllReadPermissions()
            editorSeeded = false
            editor.value = ProfileEditorState()
        }
    }

    private fun updateEditor(transform: (ProfileEditorState) -> ProfileEditorState) {
        editorSeeded = true
        editor.value = transform(editor.value)
    }

    private fun selectedPackIdOrSetError(): Long? {
        val packId = editor.value.companionPackId
        if (packId == null) {
            editor.value = editor.value.copy(error = "Choose a companion pack first.")
        }
        return packId
    }

    private fun String.sanitizedFileStem(): String {
        return lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-', '.', '_')
            .ifBlank { "companion-pack" }
    }

    private fun FocusProfile.toEditorState(): ProfileEditorState {
        return ProfileEditorState(
            profileId = id,
            name = name,
            enabled = enabled,
            intensity = intensity,
            companionPackId = companionPackId,
            days = schedules.firstOrNull()?.days ?: DayOfWeek.entries.toSet(),
            timeRanges = schedules.takeIf { it.isNotEmpty() }?.map { schedule ->
                TimeRangeEditorState(
                    ruleId = schedule.id,
                    start = schedule.range.start.toUiString(),
                    end = schedule.range.end.toUiString()
                )
            } ?: listOf(TimeRangeEditorState()),
            selectedPackages = targetApps.filter { it.enabled }.map { it.packageName }.toSet(),
            layout = layout
        )
    }

    private fun parseTime(value: String): LocalTime? {
        val parts = value.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return runCatching { LocalTime.of(hour, minute) }.getOrNull()
    }

    private fun LocalTime.toUiString(): String = "%02d:%02d".format(hour, minute)

    private data class ParsedTimeRange(
        val ruleId: Long,
        val start: LocalTime?,
        val end: LocalTime?
    )
}
