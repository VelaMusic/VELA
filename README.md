<div align="center">

  <img src="assets/ic_launcher-playstore.png" width="120" alt="Vela Logo"/>

  # Vela

  ### The Material 3 Expressive Music Player

  **Stream. Save. Loop. Repeat.**

  A modern Android music player with YouTube Music integration, local file playback, synced lyrics, offline downloads, and a clean Material 3 Expressive interface.

  Package: `com.vela.app`

  [![License: GPL v3](https://img.shields.io/badge/License-GPL_v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
  [![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
  [![GitHub stars](https://img.shields.io/github/stars/VelaMusic/VELA?style=social)](https://github.com/VelaMusic/VELA/stargazers)

  [**📥 Download**](https://github.com/VelaMusic/VELA/releases/latest) • [**🐛 Report Bug**](https://github.com/VelaMusic/VELA/issues)

  </div>

  ---

  ## ✨ Features

  ### 🎧 Playback
  - Ad-free streaming with background listening
  - Multiple account support with quick switching
  - Local file & playlist support
  - Fast startup, lightweight performance
  - EBU R128 loudness normalization
  - Tempo, pitch, and playback speed controls
  - Crossfade between tracks
  - System equalizer & spatial audio

  ### 🎤 Lyrics & Discovery
  - Live synced lyrics
  - AI translation & romanization
  - Music recognition (Shazam-style)
  - Real-time listening statistics
  - Import playlists from Spotify
  - YouTube Music account sync
  - Last.fm scrobbling
  - ListenBrainz history sync
  - Discord rich presence

  ### 🎨 Design
  - Material 3 Expressive design language
  - Album-art powered dynamic colors
  - 9 different player styles
  - 8 different player background styles
  - Responsive layouts for any screen
  - Clean browsing, player, artist, album, and lyrics views

  ### ⚙️ Customization
  - Deep playback & interface settings
  - Dynamic color theming
  - Gesture customization
  - Animation & layout tuning
  - Flexible controls

  ---

  ## 📥 Installation

  ### 🔽 Direct APK
  1. Go to [Releases](https://github.com/VelaMusic/VELA/releases/latest)
  2. Download the APK for your device (`arm64` for most modern phones, or `universal` if you’re unsure).
  3. Open the APK and, if Android prompts you, allow **Install unknown apps** for the browser or file manager opening it.

  ### 📦 Coming Soon
  - F-Droid
  - IzzyOnDroid
  - Obtainium

  ---

  ## 🛠️ Building from Source

  ### Requirements
  - [Android Studio with support for Android Gradle Plugin 9.2](https://developer.android.com/studio/releases#android_gradle_plugin_and_android_studio_compatibility)
  - JDK 21
  - Android SDK 37

  ### Steps
  ```bash
  git clone https://github.com/VelaMusic/VELA.git
  cd VELA
  ./gradlew assembleGmsMobileUniversalRelease
  ```

  ---

  ## 📄 License

  This project is licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE) for details.

  ## ❤️ Credits & Inspiration

  Vela is built on [NomaTune](https://github.com/Shahdullah/NomaTune) by Shahdullah, which itself is based on [ArchiveTune](https://github.com/rukamori/ArchiveTune) by @rukamori. Thank you to both authors — parts of the UI/UX and feature concepts were adapted with respect and appreciation.

  - Base: NomaTune (https://github.com/Shahdullah/NomaTune)
  - Base framework: ArchiveTune (https://github.com/rukamori/ArchiveTune)
  - Inspired by: @rukamori/ArchiveTune

  If you are an original author and would like additional attribution or changes, please open an issue or submit a pull request and we'll update this section promptly.

  © 2026 Vela — built on NomaTune and ArchiveTune
