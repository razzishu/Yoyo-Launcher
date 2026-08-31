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
import java.util.Random

class IconPackSettingsFragment : Fragment() {

    private lateinit var previewGrid: RecyclerView
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
        
        previewGrid = view.findViewById(R.id.preview_grid)
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

        setupRandomBackground()
        setupIconPackList()
        setupShapeList()
        setupPreviewGrid()
        setupSizeSlider()
        
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

    private fun setupSizeSlider() {
        val initialProgress = ((currentSizeFactor - 0.8f) / 0.01f).toInt()
        sizeSlider.progress = initialProgress
        updateSizeLabel(currentSizeFactor)

        sizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentSizeFactor = 0.8f + (progress * 0.01f)
                updateSizeLabel(currentSizeFactor)
                if (fromUser) {
                    previewAdapter.notifyDataSetChanged()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun updateSizeLabel(factor: Float) {
        val percentage = (factor * 100).toInt()
        sizeLabel.text = "Icon Size ($percentage%)"
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
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        val baseColor = if (isDark) {
            val darkColors = intArrayOf(
                Color.parseColor("#1A1C1E"), Color.parseColor("#1C1B1F"),
                Color.parseColor("#212121"), Color.parseColor("#263238")
            )
            darkColors[random.nextInt(darkColors.size)]
        } else {
            val lightColors = intArrayOf(
                Color.parseColor("#F5F5F5"), Color.parseColor("#F0F4F8"),
                Color.parseColor("#E8EAF6"), Color.parseColor("#F1F8E9")
            )
            lightColors[random.nextInt(lightColors.size)]
        }

        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        hsv[2] *= if (isDark) 1.1f else 0.95f
        val secondaryColor = Color.HSVToColor(hsv)
        
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(baseColor, secondaryColor)
        )
        previewWallpaper.setImageDrawable(gradient)
    }

    private fun setupIconPackList() {
        val iconPackManager = LauncherAppState.getInstance(requireContext()).iconProvider.mIconPackManager
        val packs = iconPackManager.getIconPackList()
        
        iconPackAdapter = IconPackAdapter(packs) { pack ->
            selectedPackage = pack.packageName
            updateShapeVisibility()
            previewAdapter.refresh()
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
            previewAdapter.notifyDataSetChanged()
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
        // Take exactly 6 apps for a clean 3x2 grid
        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .take(6)
        
        previewAdapter = PreviewAdapter(apps)
        previewGrid.layoutManager = GridLayoutManager(context, 3)
        previewGrid.adapter = previewAdapter
    }

    class ClippedPreviewDrawable(
        private val icon: Drawable, 
        private val path: Path,
        private val isLegacy: Boolean,
        private val sizeFactor: Float
    ) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private val drawMatrix = Matrix()

        override fun draw(canvas: Canvas) {
            canvas.withSave {
                val b = bounds
                
                // Scale entire container to reflect size changes properly
                translate(b.centerX().toFloat(), b.centerY().toFloat())
                scale(sizeFactor, sizeFactor)
                translate(-b.centerX().toFloat(), -b.centerY().toFloat())

                drawMatrix.reset()
                drawMatrix.setScale(b.width() / 100f, b.height() / 100f)
                drawMatrix.postTranslate(b.left.toFloat(), b.top.toFloat())
                
                val scaledPath = Path()
                path.transform(drawMatrix, scaledPath)
                
                if (isLegacy) {
                    canvas.drawPath(scaledPath, bgPaint)
                }
                
                canvas.clipPath(scaledPath)

                // Scaling inner content to fit within the shape naturally
                val baseInnerScale = if (isLegacy) 0.65f else 1.15f
                val centerX = b.centerX().toFloat()
                val centerY = b.centerY().toFloat()
                
                translate(centerX, centerY)
                scale(baseInnerScale, baseInnerScale)
                translate(-centerX, -centerY)
                
                icon.setBounds(b.left, b.top, b.right, b.bottom)
                icon.draw(canvas)
            }
        }

        override fun setAlpha(alpha: Int) { 
            paint.alpha = alpha 
            bgPaint.alpha = alpha
            icon.alpha = alpha
        }
        override fun setColorFilter(cf: ColorFilter?) { 
            paint.colorFilter = cf 
            icon.colorFilter = cf
        }
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    inner class PreviewAdapter(private val apps: List<ResolveInfo>) : RecyclerView.Adapter<PreviewAdapter.ViewHolder>() {
        
        fun refresh() {
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.icon_pack_image)
            val name: TextView = view.findViewById(R.id.icon_pack_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.icon_pack_item, parent, false)
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

            val icon: Drawable? = when (selectedPackage) {
                "default" -> {
                    val isLegacy = !(rawIcon is AdaptiveIconDrawable)
                    ClippedPreviewDrawable(rawIcon, path, isLegacy, currentSizeFactor)
                }
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
                        
                        object : Drawable() {
                            override fun draw(canvas: Canvas) {
                                canvas.withSave {
                                    val b = bounds
                                    scale(currentSizeFactor, currentSizeFactor, b.centerX().toFloat(), b.centerY().toFloat())
                                    themedIcon.bounds = b
                                    themedIcon.draw(canvas)
                                }
                            }
                            override fun setAlpha(a: Int) { themedIcon.alpha = a }
                            override fun setColorFilter(cf: ColorFilter?) { themedIcon.colorFilter = cf }
                            @Deprecated("Deprecated in Java")
                            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
                        }
                    } catch (e: Exception) {
                        rawIcon
                    }
                }
                else -> {
                    val packIcon = iconPackManager.getIcon(componentName, selectedPackage) ?: rawIcon
                    object : Drawable() {
                        override fun draw(canvas: Canvas) {
                            canvas.withSave {
                                val b = bounds
                                scale(currentSizeFactor, currentSizeFactor, b.centerX().toFloat(), b.centerY().toFloat())
                                packIcon.bounds = b
                                packIcon.draw(canvas)
                            }
                        }
                        override fun setAlpha(a: Int) { packIcon.alpha = a }
                        override fun setColorFilter(cf: ColorFilter?) { packIcon.colorFilter = cf }
                        @Deprecated("Deprecated in Java")
                        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
                    }
                }
            }
            
            holder.image.setImageDrawable(icon)
            holder.name.visibility = View.GONE
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
            view.layoutParams = ViewGroup.LayoutParams(dpToPx(100), ViewGroup.LayoutParams.WRAP_CONTENT)
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
                    "default" -> holder.image.setImageResource(R.mipmap.ic_launcher)
                    "themed" -> holder.image.setImageResource(R.drawable.ic_palette)
                }
            }
            
            holder.itemView.setOnClickListener {
                onSelected(pack)
            }
            
            val isSelected = pack.packageName == selectedPackage
            holder.itemView.alpha = if (isSelected) 1.0f else 0.6f
            holder.itemView.scaleX = if (isSelected) 1.05f else 1.0f
            holder.itemView.scaleY = if (isSelected) 1.05f else 1.0f
            
            if (isSelected) {
                holder.itemView.setBackgroundResource(R.drawable.rounded_action_button)
            } else {
                holder.itemView.background = null
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
            view.layoutParams = ViewGroup.LayoutParams(dpToPx(80), ViewGroup.LayoutParams.WRAP_CONTENT)
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
            
            val path = PathParser.createPathFromPathData(shape.pathString)
            val shapeDrawable = ShapeDrawable(PathShape(path, 100f, 100f))
            shapeDrawable.intrinsicWidth = dpToPx(40)
            shapeDrawable.intrinsicHeight = dpToPx(40)
            shapeDrawable.paint.color = if (shape.key == selectedShapeKey) 
                fetchAccentColor(holder.itemView.context) else Color.LTGRAY
            shapeDrawable.paint.style = Paint.Style.FILL
            
            holder.image.setImageDrawable(shapeDrawable)
            
            holder.itemView.setOnClickListener {
                onSelected(shape)
            }
            
            val isSelected = shape.key == selectedShapeKey
            holder.itemView.alpha = if (isSelected) 1.0f else 0.6f
            holder.itemView.scaleX = if (isSelected) 1.1f else 1.0f
            holder.itemView.scaleY = if (isSelected) 1.1f else 1.0f
        }

        override fun getItemCount() = shapes.size
        
        private fun fetchAccentColor(context: Context): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
            return typedValue.data
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }
}
