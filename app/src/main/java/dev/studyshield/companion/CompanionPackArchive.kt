package dev.studyshield.companion

import android.content.Context
import androidx.core.net.toUri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class CompanionPackArchiveImporter(private val context: Context) {
    fun importBytes(bytes: ByteArray): CompanionPackValidationResult {
        return if (bytes.isZipArchive()) {
            importZip(ByteArrayInputStream(bytes))
        } else {
            CompanionPackValidator().validate(bytes.toString(Charsets.UTF_8))
        }
    }

    fun importZip(inputStream: InputStream): CompanionPackValidationResult {
        val entries = readEntries(inputStream)
        val manifestBytes = entries["manifest.json"]
            ?: return CompanionPackValidationResult.Invalid(listOf("Zip package must contain manifest.json."))
        val manifest = runCatching { JSONObject(manifestBytes.toString(Charsets.UTF_8)) }.getOrElse {
            return CompanionPackValidationResult.Invalid(listOf("manifest.json is not valid JSON."))
        }
        val slug = manifest.optString("slug").sanitizePathPart().ifBlank { "companion-pack" }
        val targetDir = File(context.filesDir, "companion_packs/$slug").apply { mkdirs() }
        val assetErrors = mutableListOf<String>()

        manifest.copyAssetField("characterImageUri", entries, targetDir, assetErrors, "characterImageUri")
        manifest.copyAssetField("wallpaperUri", entries, targetDir, assetErrors, "wallpaperUri")
        manifest.optJSONArray("dialogue")?.copyDialogueAudio(entries, targetDir, assetErrors)
        if (assetErrors.isNotEmpty()) return CompanionPackValidationResult.Invalid(assetErrors)

        return CompanionPackValidator().validate(manifest.toString())
    }

    private fun JSONObject.copyAssetField(
        key: String,
        entries: Map<String, ByteArray>,
        targetDir: File,
        errors: MutableList<String>,
        label: String
    ) {
        val path = optString(key).trim()
        if (path.isBlank() || path.hasUriScheme()) return
        val entryBytes = entries[path.normalizedZipPath()]
        if (entryBytes == null) {
            errors += "$label references a zip asset that is missing: $path"
            return
        }
        put(key, entryBytes.writeImportedAsset(targetDir, path).toUri().toString())
    }

    private fun JSONArray.copyDialogueAudio(
        entries: Map<String, ByteArray>,
        targetDir: File,
        errors: MutableList<String>
    ) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            item.copyAssetField("audioUri", entries, targetDir, errors, "dialogue[$index].audioUri")
        }
    }

    private fun readEntries(inputStream: InputStream): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(inputStream.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    val name = entry.name.normalizedZipPath()
                    if (name.isNotBlank() && !name.contains("..")) {
                        entries[name] = zip.readBytes()
                    }
                }
                zip.closeEntry()
            }
        }
        return entries
    }

    private fun ByteArray.writeImportedAsset(targetDir: File, zipPath: String): File {
        val fileName = zipPath.substringAfterLast('/').sanitizePathPart().ifBlank { "asset.bin" }
        val target = File(targetDir, fileName)
        target.outputStream().use { it.write(this) }
        return target
    }
}

class CompanionPackArchiveExporter(private val context: Context) {
    fun exportZip(bundle: CompanionPackBundle): ByteArray {
        val manifest = JSONObject(CompanionPackManifestCodec().encode(bundle))
        val assets = linkedMapOf<String, ByteArray>()

        manifest.exportAssetField("characterImageUri", "assets/character", "png", assets)
        manifest.exportAssetField("wallpaperUri", "assets/wallpaper", "jpg", assets)
        manifest.optJSONArray("dialogue")?.exportDialogueAudio(assets)

        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            assets.forEach { (path, bytes) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun JSONObject.exportAssetField(
        key: String,
        targetStem: String,
        defaultExtension: String,
        assets: MutableMap<String, ByteArray>
    ) {
        val uriString = optString(key).trim()
        if (uriString.isBlank()) return
        val bytes = readUriBytes(uriString) ?: return
        val targetPath = "$targetStem.${uriString.extensionOrDefault(defaultExtension)}"
        assets[targetPath] = bytes
        put(key, targetPath)
    }

    private fun JSONArray.exportDialogueAudio(assets: MutableMap<String, ByteArray>) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val scene = item.optString("scene", "cue").sanitizePathPart().ifBlank { "cue" }
            item.exportAssetField("audioUri", "assets/audio-$scene", "mp3", assets)
        }
    }

    private fun readUriBytes(uriString: String): ByteArray? {
        val uri = uriString.toUri()
        return runCatching {
            if (uri.scheme == "file") {
                File(requireNotNull(uri.path)).readBytes()
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        }.getOrNull()
    }
}

private fun ByteArray.isZipArchive(): Boolean {
    return size >= 4 && this[0] == 0x50.toByte() && this[1] == 0x4B.toByte()
}

private fun String.hasUriScheme(): Boolean {
    return Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(this)
}

private fun String.normalizedZipPath(): String {
    return trim().replace('\\', '/').trimStart('/')
}

private fun String.sanitizePathPart(): String {
    return lowercase(Locale.US)
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-', '.', '_')
}

private fun String.extensionOrDefault(default: String): String {
    val clean = substringBefore('?').substringBefore('#')
    val ext = clean.substringAfterLast('.', missingDelimiterValue = "")
        .sanitizePathPart()
        .takeIf { it.isNotBlank() && it.length <= 8 }
    return ext ?: default
}
