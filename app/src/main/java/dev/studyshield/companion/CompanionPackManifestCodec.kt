package dev.studyshield.companion

import dev.studyshield.domain.DialogueCue
import dev.studyshield.domain.OverlayLayout
import org.json.JSONArray
import org.json.JSONObject

class CompanionPackManifestCodec {
    fun encode(bundle: CompanionPackBundle): String {
        val pack = bundle.pack
        return JSONObject()
            .put("slug", pack.slug)
            .put("name", pack.name)
            .put("author", pack.author)
            .put("license", pack.license)
            .put("language", pack.language)
            .put("contentRating", pack.contentRating)
            .put("compatibleVersion", pack.compatibleVersion)
            .putOptional("characterImageUri", pack.characterImageUri)
            .putOptional("wallpaperUri", pack.wallpaperUri)
            .put("layout", pack.layout.toJson())
            .put("dialogue", bundle.dialogue.toJson())
            .toString(2)
    }

    private fun OverlayLayout.toJson(): JSONObject {
        return JSONObject()
            .put("sidePreference", sidePreference.name)
            .put("packDefaultSide", packDefaultSide.name)
            .put("characterAnchorX", characterAnchorX.toDouble())
            .put("characterAnchorY", characterAnchorY.toDouble())
            .put("characterScale", characterScale.toDouble())
            .put("characterOpacity", characterOpacity.toDouble())
            .put("bubbleAnchorX", bubbleAnchorX.toDouble())
            .put("bubbleAnchorY", bubbleAnchorY.toDouble())
            .put("bubbleAlignment", bubbleAlignment.name)
            .put("animation", animation.name)
            .put("backgroundMode", backgroundMode.name)
            .put("overlayDim", overlayDim.toDouble())
            .put("wallpaperScale", wallpaperScale.toDouble())
            .put("wallpaperOffsetX", wallpaperOffsetX.toDouble())
            .put("wallpaperOffsetY", wallpaperOffsetY.toDouble())
            .put("reduceMotion", reduceMotion)
            .put("randomizeActionOrder", randomizeActionOrder)
    }

    private fun List<DialogueCue>.toJson(): JSONArray {
        val array = JSONArray()
        forEach { cue ->
            array.put(
                JSONObject()
                    .put("scene", cue.scene.name)
                    .put("text", cue.text)
                    .putOptional("audioUri", cue.audioUri)
                    .put("transcript", cue.transcript)
            )
        }
        return array
    }

    private fun JSONObject.putOptional(key: String, value: String?): JSONObject {
        return if (value.isNullOrBlank()) this else put(key, value)
    }
}
