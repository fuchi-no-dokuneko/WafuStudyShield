package dev.studyshield.companion

import dev.studyshield.domain.BackgroundMode
import dev.studyshield.domain.BubbleAlignment
import dev.studyshield.domain.BubbleAnimation
import dev.studyshield.domain.CompanionPack
import dev.studyshield.domain.CompanionSide
import dev.studyshield.domain.DialogueCue
import dev.studyshield.domain.OverlayLayout
import dev.studyshield.domain.ReminderScene
import dev.studyshield.domain.ResolvedSide
import dev.studyshield.media.PromptAudioPolicy
import org.json.JSONArray
import org.json.JSONObject

data class CompanionPackBundle(
    val pack: CompanionPack,
    val dialogue: List<DialogueCue>
)

sealed interface CompanionPackValidationResult {
    data class Valid(val bundle: CompanionPackBundle) : CompanionPackValidationResult
    data class Invalid(val errors: List<String>) : CompanionPackValidationResult
}

class CompanionPackValidator {
    fun validate(manifestJson: String): CompanionPackValidationResult {
        val errors = mutableListOf<String>()
        val root = runCatching { JSONObject(manifestJson) }.getOrElse {
            return CompanionPackValidationResult.Invalid(listOf("Manifest is not valid JSON."))
        }

        val slug = requiredString(root, "slug", errors)
        val name = requiredString(root, "name", errors)
        val author = requiredString(root, "author", errors)
        val license = requiredString(root, "license", errors)
        val language = requiredString(root, "language", errors)
        val contentRating = requiredString(root, "contentRating", errors)
        val characterImageUri = root.optString("characterImageUri").trim().takeIf { it.isNotBlank() }
        val wallpaperUri = root.optString("wallpaperUri").trim().takeIf { it.isNotBlank() }
        val compatibleVersion = root.optInt("compatibleVersion", -1)
        if (compatibleVersion < 1) errors += "compatibleVersion must be at least 1."
        if (contentRating.lowercase() !in setOf("general", "everyone", "teen")) {
            errors += "contentRating must be general, everyone, or teen."
        }
        if (license.equals("unknown", ignoreCase = true) || license.isBlank()) {
            errors += "license must identify redistributable rights."
        }

        val dialogueJson = root.optJSONArray("dialogue") ?: JSONArray()
        if (dialogueJson.length() == 0) errors += "dialogue must contain at least one cue."
        val dialogue = parseDialogue(dialogueJson, errors)
        if (dialogue.none { it.scene == ReminderScene.Triggered }) {
            errors += "dialogue must include a Triggered cue."
        }

        val layout = parseLayout(root.optJSONObject("layout"), errors)
        if (errors.isNotEmpty()) return CompanionPackValidationResult.Invalid(errors)

        return CompanionPackValidationResult.Valid(
            CompanionPackBundle(
                pack = CompanionPack(
                    slug = slug,
                    name = name,
                    author = author,
                    license = license,
                    language = language,
                    contentRating = contentRating,
                    compatibleVersion = compatibleVersion,
                    characterImageUri = characterImageUri,
                    wallpaperUri = wallpaperUri,
                    layout = requireNotNull(layout)
                ),
                dialogue = dialogue
            )
        )
    }

    private fun parseDialogue(json: JSONArray, errors: MutableList<String>): List<DialogueCue> {
        val cues = mutableListOf<DialogueCue>()
        for (index in 0 until json.length()) {
            val item = json.optJSONObject(index)
            if (item == null) {
                errors += "dialogue[$index] must be an object."
                continue
            }
            val sceneName = item.optString("scene")
            val scene = runCatching { enumValueOf<ReminderScene>(sceneName) }.getOrNull()
            if (scene == null) {
                errors += "dialogue[$index].scene is invalid."
                continue
            }
            val text = item.optString("text").trim()
            val audioUri = item.optString("audioUri").trim().takeIf { it.isNotBlank() }
            val transcript = item.optString("transcript", text).trim()
            if (text.isBlank()) errors += "dialogue[$index].text is required."
            if (audioUri != null && transcript.isBlank()) {
                errors += "dialogue[$index].transcript is required when audioUri is present."
            }
            if (audioUri != null && !PromptAudioPolicy.isSupportedLocalAudioUri(audioUri)) {
                errors += "dialogue[$index].audioUri must use a local content, file, or android.resource URI."
            }
            if (text.contains("worthless", ignoreCase = true) || text.contains("shame", ignoreCase = true)) {
                errors += "dialogue[$index].text uses coercive wording that is not allowed."
            }
            if (text.isNotBlank() && (audioUri == null || transcript.isNotBlank())) {
                cues += DialogueCue(scene = scene, text = text, audioUri = audioUri, transcript = transcript)
            }
        }
        return cues
    }

    private fun parseLayout(json: JSONObject?, errors: MutableList<String>): OverlayLayout? {
        if (json == null) return OverlayLayout()
        return runCatching {
            OverlayLayout(
                sidePreference = enumValueOrDefault(json.optString("sidePreference"), CompanionSide.PackDefault),
                packDefaultSide = enumValueOrDefault(json.optString("packDefaultSide"), ResolvedSide.Right),
                characterAnchorX = json.optDouble("characterAnchorX", 0.78).toFloat(),
                characterAnchorY = json.optDouble("characterAnchorY", 0.62).toFloat(),
                characterScale = json.optDouble("characterScale", 1.0).toFloat(),
                characterOpacity = json.optDouble("characterOpacity", 1.0).toFloat(),
                bubbleAnchorX = json.optDouble("bubbleAnchorX", 0.22).toFloat(),
                bubbleAnchorY = json.optDouble("bubbleAnchorY", 0.30).toFloat(),
                bubbleAlignment = enumValueOrDefault(json.optString("bubbleAlignment"), BubbleAlignment.Start),
                animation = enumValueOrDefault(json.optString("animation"), BubbleAnimation.Fade),
                backgroundMode = enumValueOrDefault(json.optString("backgroundMode"), BackgroundMode.Solid),
                overlayDim = json.optDouble("overlayDim", 0.72).toFloat(),
                wallpaperScale = json.optDouble("wallpaperScale", 1.0).toFloat(),
                wallpaperOffsetX = json.optDouble("wallpaperOffsetX", 0.0).toFloat(),
                wallpaperOffsetY = json.optDouble("wallpaperOffsetY", 0.0).toFloat(),
                reduceMotion = json.optBoolean("reduceMotion", false),
                randomizeActionOrder = json.optBoolean("randomizeActionOrder", false)
            )
        }.getOrElse { exception ->
            errors += "layout is invalid: ${exception.message ?: "values are outside supported ranges"}"
            null
        }
    }

    private fun requiredString(root: JSONObject, key: String, errors: MutableList<String>): String {
        val value = root.optString(key).trim()
        if (value.isBlank()) errors += "$key is required."
        return value
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T {
        return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
    }
}
