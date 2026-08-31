# Yoyo Launcher 🪀

![License GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Platform](https://img.shields.io/badge/Platform-Android%2012%2B-green.svg)
![Language](https://img.shields.io/badge/Language-Kotlin%20%7C%20Java-orange.svg)
![Build](https://img.shields.io/badge/Build-Android%20Studio-brightgreen.svg)
![Architecture](https://img.shields.io/badge/Architecture-Launcher3%20%2F%20Trebuchet-purple.svg)

**Yoyo Launcher** is a fast, lightweight, and highly customizable modern Android home screen replacement launcher. Built upon the stable foundation of LineageOS Trebuchet (16.2 / Launcher3), it has been completely converted to **Gradle** and decoupled from custom ROM system dependencies to work as a standalone launcher app on any Android 12+ device.

---

## 🔍 Key Highlights & Features

| Feature | Description |
| :--- | :--- |
| 🎨 **Custom Icon Packs & Shapes** | Native support for third-party icon packs from Play Store, custom shape masking (Circle, Squircle, Teardrop, Cylinder, Oval), and custom icon scaling. |
| 🎭 **Material You Themed Icons** | Full monochrome and dynamic Material You color palette icon support for modern Android UI consistency. |
| 📐 **Flexible Grid Layouts** | Instant toggle between 4-column and 5-column home screen layouts with automatic workspace icon migration. |
| ⚡ **Fast & Lightweight** | Zero bloatware, fast app drawer searching, smooth gesture animations, and low memory usage. |
| 🛠️ **Developer Friendly** | Migrated from Soong to standard Android Studio Gradle build system — easy to inspect, fork, and build. |

---

## 🛠 Architecture & Engineering

Developing custom Android home screen launchers usually requires compiling the full AOSP source tree. **Yoyo Launcher** eliminates that complexity:

* **Soong to Gradle Migration:** Converted LineageOS Trebuchet's `Android.bp` / Soong configuration into standard Android Studio Gradle scripts.
* **System-Free Operation:** Stripped system-level Quickstep bindings to allow Yoyo Launcher to run as a standard user application without root or custom ROMs.
* **Modern Tooling:** Uses Android Studio, Kotlin, Dagger dependency injection, Protocol Buffers, and Jetpack components.

---

## 🏷️ GitHub Search Keywords & Topics

> `android` `launcher` `android-launcher` `trebuchet` `lineageos` `launcher3` `material-you` `customization` `icon-pack` `themed-icons` `home-screen` `kotlin` `android-app` `home-screen-replacement`

---

## 🚀 Future Roadmap

* [ ] Advanced gesture shortcuts (Double tap to sleep, pinch to edit, swipe down for notifications).
* [ ] Expanded widget picker & custom clock widgets.
* [ ] Folder customization styles and drawer category tabs.
* [ ] Desktop & multi-display optimization.

---

## 💻 Getting Started

Building **Yoyo Launcher** in Android Studio:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/razzishu/Yoyo-Launcher.git
   ```
2. **Open in Android Studio:**
   - Select **Open an Existing Project** and navigate to `Yoyo-Launcher`.
3. **Build & Run:**
   - Sync Gradle project and click **Run** (`Shift + F10`).

---

## 🤝 Contributing

Contributions, bug reports, and feature requests are welcome! Feel free to fork the repository, make changes, and open a Pull Request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE) for details.
