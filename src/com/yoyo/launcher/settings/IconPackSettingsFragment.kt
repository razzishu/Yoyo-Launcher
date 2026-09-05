/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yoyo.launcher.settings

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.PathShape
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.PathParser
import androidx.core.graphics.withSave
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yoyo.launcher.LauncherAppState
import com.yoyo.launcher.LauncherPrefs
import com.yoyo.launcher.R
import com.yoyo.launcher.graphics.ThemeManager
import com.yoyo.launcher.icons.BitmapInfo
import com.yoyo.launcher.icons.FastBitmapDrawable
import com.yoyo.launcher.icons.FastBitmapDrawableDelegate
import com.yoyo.launcher.icons.IconPackManager
import com.yoyo.launcher.icons.IconShape
import com.yoyo.launcher.icons.LauncherIcons
import com.yoyo.launcher.icons.ThemedBitmap
import com.yoyo.launcher.icons.mono.MonoIconThemeController
import com.yoyo.launcher.shapes.IconShapeModel
import com.yoyo.launcher.shapes.ShapesProvider
import com.yoyo.launcher.util.Themes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class IconPackSettingsFragment : Fragment() {

    private lateinit var previewGrid: RecyclerView
    private lateinit var previewDockGrid: RecyclerView
    private lateinit var previewStatusTime: TextView
    private lateinit var previewAtAGlance: TextView
    private lateinit var previewWallpaper: ImageView
    private lateinit var iconPackList: RecyclerView
    private lateinit var shapeList: RecyclerView
    private lateinit var shapeContainer: View
    private lateinit var themedOptionsContainer: View
    private lateinit var homescreenOnlySwitch: SwitchCompat
    private lateinit var applyButton: Button
    private lateinit var loadingOverlay: View
    private lateinit var sizeSlider: SeekBar
    private lateinit var sizeLabel: TextView
    
    private lateinit var iconPackAdapter: IconPackAdapter
    private lateinit var shapeAdapter: ShapeAdapter
    private lateinit var previewAdapter: PreviewAdapter
    private lateinit var dockAdapter: PreviewAdapter
    
    private var selectedPackage: String = "default"
    private var selectedShapeKey: String = ""
    private var currentSizeFactor: Float = 1.0f
    private var homescreenOnlyThemed: Boolean = false
    private val random = Random()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.icon_pack_customization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.pref_icon_pack_title)
        
        previewGrid = view.findViewById(R.id.preview_grid)
        previewDockGrid = view.findViewById(R.id.preview_dock_grid)
        previewStatusTime = view.findViewById(R.id.preview_status_time)
        previewAtAGlance = view.findViewById(R.id.preview_at_a_glance)
        previewWallpaper = view.findViewById(R.id.preview_wallpaper)
        iconPackList = view.findViewById(R.id.icon_pack_list)
        shapeList = view.findViewById(R.id.shape_list)
        shapeContainer = view.findViewById(R.id.shape_container)
        themedOptionsContainer = view.findViewById(R.id.themed_options_container)
        homescreenOnlySwitch = view.findViewById(R.id.homescreen_only_switch)
        applyButton = view.findViewById(R.id.apply_button)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
        sizeSlider = view.findViewById(R.id.icon_size_slider)
        sizeLabel = view.findViewById(R.id.icon_size_label)

        val context = requireContext()
        val appState = LauncherAppState.getInstance(context)
        val prefs = LauncherPrefs.get(context)
        selectedPackage = prefs.get(LauncherPrefs.ICON_PACK) ?: "default"
        selectedShapeKey = prefs.get(ThemeManager.PREF_ICON_SHAPE) ?: ""
        currentSizeFactor = prefs.get(LauncherPrefs.ICON_SIZE_FACTOR)
        homescreenOnlyThemed = prefs.get(LauncherPrefs.THEMED_ICONS_HOMESCREEN_ONLY)

        homescreenOnlySwitch.isChecked = homescreenOnlyThemed
        homescreenOnlySwitch.setOnCheckedChangeListener { _, isChecked ->
            homescreenOnlyThemed = isChecked
        }

        updateStatusTime()
        updateAtAGlance()
        setupRandomBackground()
        setupIconPackList()
        setupShapeList()
        setupPreviewGrid()
        setupSizeSlider(view)
        
        updateShapeVisibility()
        
        applyButton.setOnClickListener {
            showLoading()
            
            val isThemed = selectedPackage == "themed"
            ThemeManager.INSTANCE.get(context).isMonoThemeEnabled = isThemed
            
            // Critical: Update all prefs synchronously
            prefs.putSync(
                LauncherPrefs.ICON_PACK.to(selectedPackage),
                ThemeManager.PREF_ICON_SHAPE.to(selectedShapeKey),
                LauncherPrefs.ALLAPPS_THEMED_ICONS.to(isThemed),
                LauncherPrefs.THEMED_ICONS_HOMESCREEN_ONLY.to(homescreenOnlyThemed),
                LauncherPrefs.ICON_SIZE_FACTOR.to(currentSizeFactor)
            )
            
            ThemeManager.INSTANCE.get(context).refresh()
            appState.invariantDeviceProfile.onConfigChanged()
            
            appState.iconProvider.updateSystemState()
            appState.iconCache.updateIconParams(
                appState.invariantDeviceProfile.fillResIconDpi,
                appState.invariantDeviceProfile.iconBitmapSize
            )
            
            appState.model.forceReload()
            
            Handler(Looper.getMainLooper()).postDelayed({
                activity?.onBackPressed()
            }, 800)
        }
    }

    private fun setupSizeSlider(rootView: View) {
        val initialProgress = ((currentSizeFactor - 0.8f) / 0.01f).toInt().coerceIn(0, 50)
        sizeSlider.progress = initialProgress
        updateSizeLabel(currentSizeFactor)
        updateChipHighlights(rootView, currentSizeFactor)

        sizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentSizeFactor = 0.8f + (progress * 0.01f)
                updateSizeLabel(currentSizeFactor)
                updateChipHighlights(rootView, currentSizeFactor)
                if (fromUser) {
                    notifyPreviewChanged()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        rootView.findViewById<View>(R.id.chip_size_80)?.setOnClickListener {
            sizeSlider.progress = 0
            currentSizeFactor = 0.80f
            updateSizeLabel(currentSizeFactor)
            updateChipHighlights(rootView, currentSizeFactor)
            notifyPreviewChanged()
        }
        rootView.findViewById<View>(R.id.chip_size_100)?.setOnClickListener {
            sizeSlider.progress = 20
            currentSizeFactor = 1.00f
            updateSizeLabel(currentSizeFactor)
            updateChipHighlights(rootView, currentSizeFactor)
            notifyPreviewChanged()
        }
        rootView.findViewById<View>(R.id.chip_size_115)?.setOnClickListener {
            sizeSlider.progress = 35
            currentSizeFactor = 1.15f
            updateSizeLabel(currentSizeFactor)
            updateChipHighlights(rootView, currentSizeFactor)
            notifyPreviewChanged()
        }
        rootView.findViewById<View>(R.id.chip_size_130)?.setOnClickListener {
            sizeSlider.progress = 50
            currentSizeFactor = 1.30f
            updateSizeLabel(currentSizeFactor)
            updateChipHighlights(rootView, currentSizeFactor)
            notifyPreviewChanged()
        }
    }

    private fun notifyPreviewChanged() {
        if (::previewAdapter.isInitialized) {
            previewAdapter.notifyDataSetChanged()
        }
        if (::dockAdapter.isInitialized) {
            dockAdapter.notifyDataSetChanged()
        }
    }

    private fun updateStatusTime() {
        try {
            val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
            previewStatusTime.text = timeFormat.format(Date())
        } catch (e: Exception) {
            previewStatusTime.text = "09:41"
        }
    }

    private fun updateAtAGlance() {
        try {
            val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
            previewAtAGlance.text = "${dateFormat.format(Date())} • 24°C ☀️"
        } catch (e: Exception) {
            previewAtAGlance.text = "Tuesday, Sep 5 • 24°C ☀️"
        }
    }

    private fun updateChipHighlights(rootView: View, factor: Float) {
        val chips = listOf(
            Pair(rootView.findViewById<TextView>(R.id.chip_size_80), 0.80f),
            Pair(rootView.findViewById<TextView>(R.id.chip_size_100), 1.00f),
            Pair(rootView.findViewById<TextView>(R.id.chip_size_115), 1.15f),
            Pair(rootView.findViewById<TextView>(R.id.chip_size_130), 1.30f)
        )
        val context = rootView.context
        val primaryColor = context.getColor(R.color.materialColorPrimary)
        val secondaryColor = Themes.getAttrColor(context, android.R.attr.textColorSecondary)

        for ((chip, targetFactor) in chips) {
            if (chip == null) continue
            val isMatch = kotlin.math.abs(factor - targetFactor) < 0.02f
            if (isMatch) {
                chip.setTextColor(primaryColor)
                chip.setBackgroundResource(R.drawable.bg_icon_pack_item_selected)
            } else {
                chip.setTextColor(secondaryColor)
                chip.setBackgroundResource(R.drawable.bg_settings_tag_pill)
            }
        }
    }

    private fun updateSizeLabel(factor: Float) {
        val percentage = (factor * 100).toInt()
        sizeLabel.text = "$percentage%"
    }

    private fun showLoading() {
        loadingOverlay.visibility = View.VISIBLE
        val anim = AlphaAnimation(0f, 1f)
        anim.duration = 400
        loadingOverlay.startAnimation(anim)
    }

    private fun updateShapeVisibility() {
        val isDefaultOrThemed = selectedPackage == "default" || selectedPackage == "themed"
        shapeContainer.visibility = if (isDefaultOrThemed) View.VISIBLE else View.GONE
        themedOptionsContainer.visibility = if (selectedPackage == "themed") View.VISIBLE else View.GONE
    }

    private fun setupRandomBackground() {
        try {
            val wm = WallpaperManager.getInstance(requireContext())
            val wallpaper = wm.drawable
            if (wallpaper != null) {
                previewWallpaper.setImageDrawable(wallpaper)
                return
            }
        } catch (e: Exception) {
            // Fallback to dynamic gradient
        }

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val context = requireContext()
        val primaryContainer = context.getColor(R.color.materialColorPrimaryContainer)
        val tertiaryContainer = context.getColor(R.color.materialColorTertiaryContainer)
        val secondaryContainer = context.getColor(R.color.materialColorSecondaryContainer)
        val surfaceColor = Themes.getAttrColor(context, android.R.attr.colorBackground)

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            if (isDark) {
                intArrayOf(surfaceColor, primaryContainer, tertiaryContainer, surfaceColor)
            } else {
                intArrayOf(primaryContainer, secondaryContainer, tertiaryContainer)
            }
        )
        previewWallpaper.setImageDrawable(gradient)
    }

    private fun setupIconPackList() {
        val iconPackManager = LauncherAppState.getInstance(requireContext()).iconProvider.mIconPackManager
        val packs = iconPackManager.getIconPackList()
        
        iconPackAdapter = IconPackAdapter(packs) { pack ->
            selectedPackage = pack.packageName
            updateShapeVisibility()
            notifyPreviewChanged()
            iconPackAdapter.notifyDataSetChanged()
        }
        
        iconPackList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        iconPackList.adapter = iconPackAdapter
        
        val index = packs.indexOfFirst { it.packageName == selectedPackage }
        if (index >= 0) {
            iconPackList.scrollToPosition(index)
        }
    }

    private fun setupShapeList() {
        val shapes = ShapesProvider.iconShapes.toList()
        shapeAdapter = ShapeAdapter(shapes) { shape ->
            selectedShapeKey = shape.key
            notifyPreviewChanged()
            shapeAdapter.notifyDataSetChanged()
        }
        shapeList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        shapeList.adapter = shapeAdapter
        
        val index = shapes.indexOfFirst { it.key == selectedShapeKey }
        if (index >= 0) {
            shapeList.scrollToPosition(index)
        }
    }

    private fun setupPreviewGrid() {
        val context = requireContext()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val allApps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }

        // Pick typical launcher dock apps (phone, messages, browser, camera)
        val dialerApp = allApps.find { it.activityInfo.packageName.contains("dialer") || it.activityInfo.packageName.contains("phone") }
        val msgApp = allApps.find { it.activityInfo.packageName.contains("messaging") || it.activityInfo.packageName.contains("mms") }
        val browserApp = allApps.find { it.activityInfo.packageName.contains("chrome") || it.activityInfo.packageName.contains("browser") }
        val cameraApp = allApps.find { it.activityInfo.packageName.contains("camera") }

        val preferredDock = listOfNotNull(dialerApp, msgApp, browserApp, cameraApp).distinct()
        val dockApps = if (preferredDock.size == 4) {
            preferredDock
        } else {
            val remaining = allApps.filterNot { preferredDock.contains(it) }
            (preferredDock + remaining).take(4)
        }

        val remainingForDesktop = allApps.filterNot { dockApps.contains(it) }
        val desktopApps = if (remainingForDesktop.size >= 4) {
            remainingForDesktop.take(4)
        } else {
            allApps.take(4)
        }

        previewAdapter = PreviewAdapter(desktopApps, isDock = false)
        previewGrid.layoutManager = GridLayoutManager(context, 4)
        previewGrid.adapter = previewAdapter

        dockAdapter = PreviewAdapter(dockApps, isDock = true)
        previewDockGrid.layoutManager = GridLayoutManager(context, 4)
        previewDockGrid.adapter = dockAdapter
    }

    class ClippedPreviewDrawable(
        private val icon: Drawable, 
        private val path: Path,
        private val isLegacy: Boolean
    ) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private val drawMatrix = Matrix()

        override fun draw(canvas: Canvas) {
            val b = bounds
            if (b.isEmpty) return

            canvas.withSave {
                val cx = b.centerX().toFloat()
                val cy = b.centerY().toFloat()

                drawMatrix.reset()
                val side = minOf(b.width(), b.height()).toFloat()
                drawMatrix.setScale(side / 100f, side / 100f)
                drawMatrix.postTranslate(b.left + (b.width() - side) / 2f, b.top + (b.height() - side) / 2f)

                val scaledPath = Path()
                path.transform(drawMatrix, scaledPath)

                if (isLegacy) {
                    canvas.drawPath(scaledPath, bgPaint)
                }

                canvas.clipPath(scaledPath)

                if (icon is AdaptiveIconDrawable) {
                    icon.background?.let { bg ->
                        bg.setBounds(b.left, b.top, b.right, b.bottom)
                        bg.draw(canvas)
                    }
                    icon.foreground?.let { fg ->
                        val expandX = (b.width() * 0.13f).toInt()
                        val expandY = (b.height() * 0.13f).toInt()
                        fg.setBounds(b.left - expandX, b.top - expandY, b.right + expandX, b.bottom + expandY)
                        fg.draw(canvas)
                    }
                } else {
                    val baseInnerScale = if (isLegacy) 0.70f else 1.0f
                    scale(baseInnerScale, baseInnerScale, cx, cy)

                    icon.setBounds(b.left, b.top, b.right, b.bottom)
                    icon.draw(canvas)
                }
            }
        }

        override fun setAlpha(alpha: Int) { 
            paint.alpha = alpha 
            bgPaint.alpha = alpha
            icon.alpha = alpha
        }
        override fun setColorFilter(cf: ColorFilter?) { 
            paint.colorFilter = cf 
            bgPaint.colorFilter = cf
            icon.colorFilter = cf
        }
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    inner class PreviewAdapter(
        private val apps: List<ResolveInfo>,
        private val isDock: Boolean
    ) : RecyclerView.Adapter<PreviewAdapter.ViewHolder>() {
        
        fun refresh() {
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.preview_icon_image)
            val label: TextView = view.findViewById(R.id.preview_icon_label)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_preview_icon, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            val context = holder.itemView.context
            val pm = context.packageManager
            val iconPackManager = LauncherAppState.getInstance(context).iconProvider.mIconPackManager
            
            val pkg = app.activityInfo.packageName
            val componentName = ComponentName(pkg, app.activityInfo.name)
            
            val shapeModel = ShapesProvider.iconShapes.find { it.key == selectedShapeKey } 
                ?: ShapesProvider.iconShapes[0]
            val path = PathParser.createPathFromPathData(shapeModel.pathString)
            val rawIcon = app.loadIcon(pm)

            val baseDrawable: Drawable = when (selectedPackage) {
                "default" -> rawIcon
                "themed" -> {
                    try {
                        val factory = LauncherIcons.obtain(context)
                        var bitmapInfo = factory.createBadgedIconBitmap(rawIcon)
                        if (bitmapInfo.themedBitmap == null) {
                            val monoController = MonoIconThemeController(shouldForceThemeIcon = true)
                            val themedBitmap = monoController.createThemedBitmap(rawIcon, bitmapInfo, factory, null)
                            if (themedBitmap !== ThemedBitmap.NOT_SUPPORTED) {
                                bitmapInfo = bitmapInfo.copy(themedBitmap = themedBitmap)
                            }
                        }
                        val themedIcon = bitmapInfo.newIcon(context, BitmapInfo.FLAG_THEMED, IconShape(100, path, Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)))
                        factory.recycle()
                        themedIcon
                    } catch (e: Exception) {
                        rawIcon
                    }
                }
                else -> {
                    iconPackManager.getIcon(componentName, selectedPackage) ?: rawIcon
                }
            }
            
            val isLegacy = (selectedPackage == "default") && (rawIcon !is AdaptiveIconDrawable)
            val iconDrawable = ClippedPreviewDrawable(baseDrawable, path, isLegacy)
            
            val baseIconSizeDp = if (isDock) 42f else 38f
            val scaledSizeDp = (baseIconSizeDp * currentSizeFactor).toInt()
            val lp = holder.image.layoutParams
            lp.width = dpToPx(scaledSizeDp)
            lp.height = dpToPx(scaledSizeDp)
            holder.image.layoutParams = lp
            holder.image.setImageDrawable(iconDrawable)
            
            if (isDock) {
                holder.label.visibility = View.GONE
            } else {
                holder.label.visibility = View.VISIBLE
                holder.label.text = app.loadLabel(pm)
            }
        }

        override fun getItemCount() = apps.size
    }

    inner class IconPackAdapter(
        private val packs: List<IconPackManager.IconPackInfo>,
        private val onSelected: (IconPackManager.IconPackInfo) -> Unit
    ) : RecyclerView.Adapter<IconPackAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.icon_pack_image)
            val name: TextView = view.findViewById(R.id.icon_pack_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.icon_pack_item, parent, false)
            view.layoutParams = ViewGroup.LayoutParams(dpToPx(84), ViewGroup.LayoutParams.WRAP_CONTENT)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pack = packs[position]
            holder.name.text = pack.label
            holder.name.visibility = View.VISIBLE
            
            val typedValue = TypedValue()
            holder.itemView.context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            holder.name.setTextColor(typedValue.data)
            
            if (pack.icon != null) {
                holder.image.setImageDrawable(pack.icon)
            } else {
                when (pack.packageName) {
                    "default" -> holder.image.setImageResource(R.drawable.ic_default_icon_pack)
                    "themed" -> holder.image.setImageResource(R.drawable.ic_palette)
                    else -> holder.image.setImageResource(R.drawable.ic_default_icon_pack)
                }
            }
            
            holder.itemView.setOnClickListener {
                onSelected(pack)
            }
            
            val isSelected = pack.packageName == selectedPackage
            holder.itemView.alpha = 1.0f
            
            if (isSelected) {
                holder.itemView.setBackgroundResource(R.drawable.bg_icon_pack_item_selected)
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_icon_pack_item)
            }
        }

        override fun getItemCount() = packs.size
    }

    inner class ShapeAdapter(
        private val shapes: List<IconShapeModel>,
        private val onSelected: (IconShapeModel) -> Unit
    ) : RecyclerView.Adapter<ShapeAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.icon_pack_image)
            val name: TextView = view.findViewById(R.id.icon_pack_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.icon_pack_item, parent, false)
            view.layoutParams = ViewGroup.LayoutParams(dpToPx(76), ViewGroup.LayoutParams.WRAP_CONTENT)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val shape = shapes[position]
            val context = holder.itemView.context
            holder.name.text = context.getString(shape.titleId)
            holder.name.visibility = View.VISIBLE
            
            val typedValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            holder.name.setTextColor(typedValue.data)
            
            val isSelected = shape.key == selectedShapeKey
            val path = PathParser.createPathFromPathData(shape.pathString)
            val shapeDrawable = ShapeDrawable(PathShape(path, 100f, 100f))
            shapeDrawable.intrinsicWidth = dpToPx(38)
            shapeDrawable.intrinsicHeight = dpToPx(38)
            val accentColor = fetchAccentColor(context)
            val unselectedColor = Themes.getAttrColor(context, android.R.attr.textColorSecondary)
            shapeDrawable.paint.color = if (isSelected) accentColor else unselectedColor
            shapeDrawable.paint.style = Paint.Style.FILL
            
            holder.image.setImageDrawable(shapeDrawable)
            
            holder.itemView.setOnClickListener {
                onSelected(shape)
            }
            
            holder.itemView.alpha = 1.0f

            if (isSelected) {
                holder.itemView.setBackgroundResource(R.drawable.bg_icon_pack_item_selected)
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_icon_pack_item)
            }
        }

        override fun getItemCount() = shapes.size
        
        private fun fetchAccentColor(context: Context): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
            return typedValue.data
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.pref_icon_pack_title)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }
}
