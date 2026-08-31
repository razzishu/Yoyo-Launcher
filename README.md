# yoyo Launcher 🪀

![License GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Platform](https://img.shields.io/badge/Platform-Android%2012%2B-green.svg)
![Build](https://img.shields.io/badge/Build-Android%20Studio-brightgreen.svg)

**yoyo Launcher** is a fast, clean, and highly customizable third-party Android home screen replacement. Born from the stable foundation of LineageOS Trebuchet (16.2), it has been completely decoupled from system-level constraints to work seamlessly as a standalone app on *any* Android device without needing root or a custom ROM.

---

## ✨ Features & Highlights

* **🎨 Custom Icon Packs & Shapes:** Native support for third-party icon packs, custom icon mask shapes (Circle, Squircle, Teardrop, Cylinder, etc.), and customizable icon scaling.
* **🎭 Material You Themed Icons:** Built-in monochrome and Material You themed icon support for a cohesive, modern look across all app icons.
* **📐 Dynamic Launcher Layouts:** Easily switch between 4-column and 5-column grid layouts with seamless homescreen item migration and auto-scaling.
* **🚀 Lightweight & Smooth:** Stripped of system bloat for maximum performance, fast app drawer scrolling, and minimal memory usage.
* **🔒 Privacy & Security:** Built-in app lock / hidden apps capabilities and privacy features.

---

## 🛠 Under the Hood

Building a custom launcher often requires dealing with complex AOSP build systems. For **yoyo Launcher**, we did the heavy lifting to make development accessible to everyone:

* **Soong to Gradle:** Completely converted the original LineageOS Soong build configuration into a standard Android Studio Gradle project.
* **System-Free:** Cleaned up and removed all Quickstep and deep system-level dependencies that normally tie Trebuchet to custom ROMs.
* **Universal Compatibility:** Designed to run flawlessly as a standard third-party launcher application on any Android device.

---

## 🚀 Future Roadmap

yoyo Launcher is rapidly evolving into a platform for infinite customization. Upcoming updates will focus on:

* [ ] Advanced gesture controls and swipe actions.
* [ ] Expanded widget picker and custom widget options.
* [ ] Deep performance optimizations and smooth animation refinements.
* [ ] Desktop mode & multi-display improvements.
* [ ] Additional grid customization parameters.

---

## 💻 Getting Started

Since the project has been fully migrated to Gradle, building **yoyo Launcher** is as simple as building any standard Android app:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/razzishu/Yoyo-Launcher.git
   ```
2. **Open in Android Studio:**
   - Launch **Android Studio**.
   - Select **Open an existing project** and choose the `Yoyo-Launcher` folder.
3. **Build & Run:**
   - Sync Gradle and hit **Run** (`Shift + F10`) to deploy to your connected device or emulator!

---

## 🤝 Contributing

Contributions, pull requests, and bug reports are warmly welcomed! Since the codebase is now free from AOSP build complexities, anyone with Android Studio can easily contribute to the future of **yoyo Launcher**.

1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for details.
