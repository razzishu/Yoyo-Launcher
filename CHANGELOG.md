# Changelog

## [1.0.3-beta] - 2025-02-23

### 🎨 Redesigned Settings Menu
- **Single Page Main Navigation**: Transformed settings into a clean, single-page dashboard featuring 5 primary Material You category menu cards (*Icons & Theme, General, Homescreen, App Drawer, Dock & Search Bar*).
- **Vibrant Material You Styling**: Added pastel color badge frames (*Rose Pink, Teal, Emerald Green, Lavender, Amber*) and dedicated vector icons for every setting option.
- **Custom Preference Cards**: Redesigned sub-option items into Material You cards with rounded corners (`20dp`), subtle elevation, and clean typography.
- **Header & Title Refresh**: Updated launcher settings title to **Yoyo Settings** in a bold `32sp` header with dynamic Material You background gradients.

### 🖼️ Icon Customization & Masking
- **Real-Time Icon Preview**: Fixed preview grid alignment and real-time shape masking across default icons, themed icons, and icon packs.
- **Improved Adaptive Foreground**: Expanded adaptive icon foreground scale to `126%` (`fgScale = 1.26f`) to eliminate empty background padding and make app logos fill custom shape silhouettes cleanly.
- **Icon Shape Support**: Fully restored custom icon shape clipping (*Squircle, Pebble, Arch, Teardrop, Vessel, Heart, Hexagon, Octagon, Cookie, Tapered Rect*) for default app icons on the home screen, dock, and all-apps drawer.
- **Default Icon Pack Logo**: Introduced a custom vector logo (`ic_default_icon_pack.xml`) for the default icon pack option.

### 📐 Grid & Layout Rules
- **5 Columns Default**: Made 5 columns the default layout throughout the launcher.
- **Adaptive Workspace Grids**:
  - **5 Columns Mode**: `5 * 8` grid when dock search bar is enabled, `5 * 9` grid when dock search bar is disabled.
  - **4 Columns Mode**: `4 * 7` grid when dock search bar is enabled, `4 * 8` grid when dock search bar is disabled.
- **5-Column All-Apps & Dock**: Automatically scales all-apps drawer grid and hotseat dock to 5 columns in 5-column mode.
