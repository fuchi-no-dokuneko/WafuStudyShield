# Companion Pack Specification

Companion packs are local metadata and assets used to render optional character-style reminders. The public product name is Companion mode; users may still see Waifu mode as an optional style label.

## Package Format

The shareable package format is a `.studyshield-pack.zip` file containing:

- `manifest.json`
- optional image assets, usually under `assets/`
- optional MP3 dialogue assets, usually under `assets/`

The app imports package assets into app-private storage and rewrites manifest asset paths to local `file://` URIs before saving the pack. This makes an imported package independent from the sender's original file picker URIs.

Legacy standalone JSON manifest import remains supported for local testing and older packs. JSON-only imports can reference Android `content://`, `file://`, or `android.resource://` media URIs, but JSON exports are no longer the sharing format.

The bundled demo package is:

```text
app/src/main/assets/companion_packs/codex_cat_girl/codex-cat-girl.studyshield-pack.zip
```

It contains `manifest.json`, `assets/character.png`, and `assets/voice.mp3`.

## Manifest

The current manifest format is JSON:

```json
{
  "slug": "study-guide",
  "name": "Study Guide",
  "author": "StudyShield project",
  "license": "CC0-1.0",
  "language": "en",
  "contentRating": "general",
  "compatibleVersion": 1,
  "characterImageUri": "assets/character.png",
  "wallpaperUri": "assets/wallpaper.jpg",
  "layout": {
    "sidePreference": "PackDefault",
    "packDefaultSide": "Right",
    "characterAnchorX": 0.78,
    "characterAnchorY": 0.62,
    "characterScale": 1.0,
    "characterOpacity": 1.0,
    "bubbleAnchorX": 0.22,
    "bubbleAnchorY": 0.30,
    "bubbleAlignment": "Start",
    "animation": "Fade",
    "backgroundMode": "Solid",
    "overlayDim": 0.72,
    "wallpaperScale": 1.0,
    "wallpaperOffsetX": 0.0,
    "wallpaperOffsetY": 0.0,
    "reduceMotion": false
  },
  "dialogue": [
    {
      "scene": "Triggered",
      "text": "This is your study window.",
      "audioUri": "assets/voice.mp3",
      "transcript": "This is your study window."
    }
  ]
}
```

## Required Fields

- `slug`
- `name`
- `author`
- `license`
- `language`
- `contentRating`
- `compatibleVersion`
- at least one `dialogue` cue with scene `Triggered`

## Dialogue Rules

Supported scenes are `Triggered`, `FirstSkip`, and `SessionEnded`.

Every audio cue must include an editable transcript. The transcript is used whenever audio focus is denied, media volume is zero, the device cannot decode the file, the file is missing, or voice is disabled.

## Asset URI Fields

`characterImageUri` and `wallpaperUri` are optional. Inside a zip package, relative paths such as `assets/character.png` refer to files in the same zip. Runtime-selected images should come from Android Photo Picker or another system picker that grants only the selected file. The app stores the URI string and attempts to persist read access; it does not scan the media library.

Audio belongs to each dialogue cue as `audioUri`. Inside a zip package, relative paths such as `assets/voice.mp3` refer to files in the same zip. Runtime-selected MP3 files should come from Android's document picker so the app can retain read access without requesting broad storage permission.

The app exports a selected pack as a `.studyshield-pack.zip` through Android's create-document picker. The export writes `manifest.json` plus readable character, wallpaper, and dialogue audio bytes into the zip. If a referenced asset can no longer be read, that field is omitted from the exported package and the transcript remains available as fallback.

## Layout Rules

Coordinates are normalized floats from `0.0` to `1.0`. The runtime resolves these against status bars, navigation bars, display cutouts, rotation, and split-screen sizes.

Wallpaper crop values:

- `wallpaperScale`: `1.0` to `3.0`
- `wallpaperOffsetX`: `-1.0` to `1.0`
- `wallpaperOffsetY`: `-1.0` to `1.0`

Side preferences:

- `Left`
- `Right`
- `RandomPerEvent`
- `PackDefault`

Random side selection is locked for a single reminder event and does not move repeatedly while the overlay is visible.

In preview mode, `RandomPerEvent` uses the pack default side so the preview remains stable while editing.

## Asset Policy

Official packs must use original or clearly redistributable assets and include license metadata. Do not submit unauthorized anime, game, celebrity, actor, voice actor, music, wallpaper, or character assets. Do not submit sexual content, romantic manipulation, threats, shame-based learning copy, or material targeted at minors in a coercive relationship frame.
