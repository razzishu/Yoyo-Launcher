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

package com.yoyo.launcher.icons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import com.yoyo.launcher.LauncherPrefs
import com.yoyo.launcher.dagger.ApplicationContext
import com.yoyo.launcher.dagger.LauncherAppSingleton
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@LauncherAppSingleton
class IconPackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: LauncherPrefs
) {
    private val pm = context.packageManager
    private val iconPackCache = ConcurrentHashMap<String, Map<String, String>>()

    fun getIconPackList(): List<IconPackInfo> {
        val list = mutableListOf<IconPackInfo>()
        
        list.add(IconPackInfo("default", "Default", null))
        list.add(IconPackInfo("themed", "Themed", null))

        val intents = arrayOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.lucidlauncher.THEME",
            "com.smartlauncher.THEME"
        )
        
        for (action in intents) {
            val resolveInfos = pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
            for (ri in resolveInfos) {
                val packageName = ri.activityInfo.packageName
                if (list.none { it.packageName == packageName }) {
                    val label = ri.loadLabel(pm).toString()
                    val icon = ri.loadIcon(pm)
                    list.add(IconPackInfo(packageName, label, icon))
                }
            }
        }
        
        val allPackages = pm.getInstalledPackages(0)
        for (pkg in allPackages) {
            val pkgName = pkg.packageName
            if (list.none { it.packageName == pkgName }) {
                val lowerPkg = pkgName.lowercase()
                if (lowerPkg.contains("iconpack") || lowerPkg.contains("icon_pack")) {
                    try {
                        val appInfo = pkg.applicationInfo
                        if (appInfo != null) {
                            list.add(IconPackInfo(pkgName, pm.getApplicationLabel(appInfo).toString(), pm.getApplicationIcon(appInfo)))
                        }
                    } catch (e: Exception) {}
                }
            }
        }

        return list
    }

    fun getIcon(componentName: ComponentName, packageName: String): Drawable? {
        if (packageName == "default" || packageName == "themed") return null
        
        val appFilter = getAppFilter(packageName)
        
        val pkgName = componentName.packageName
        val className = componentName.className
        
        // Comprehensive matching
        val strategies = arrayOf(
            "ComponentInfo{$pkgName/$className}",
            "ComponentInfo{$pkgName/${componentName.shortClassName}}",
            "ComponentInfo{$pkgName.$className}",
            componentName.flattenToString(),
            componentName.toString(),
            "$pkgName/$className",
            pkgName
        )
        
        var drawableName: String? = null
        for (strategy in strategies) {
            drawableName = appFilter[strategy]
            if (drawableName != null) break
        }
        
        if (drawableName == null) {
            // Fuzzy match by component name parts
            for ((key, value) in appFilter) {
                if (key.contains(pkgName) && (key.contains(className) || key.contains(componentName.shortClassName))) {
                    drawableName = value
                    break
                }
            }
        }
        
        if (drawableName == null) drawableName = appFilter[pkgName]

        if (drawableName != null) {
            try {
                val iconPackRes = pm.getResourcesForApplication(packageName)
                var resourceName = drawableName.substringAfterLast("/")
                
                val id = iconPackRes.getIdentifier(resourceName, "drawable", packageName)
                if (id != 0) return iconPackRes.getDrawable(id, null)
                
                val altId = iconPackRes.getIdentifier(resourceName.lowercase().replace(".", "_"), "drawable", packageName)
                if (altId != 0) return iconPackRes.getDrawable(altId, null)
            } catch (e: Exception) {
                Log.e("IconPackManager", "Error loading icon $drawableName from pack $packageName", e)
            }
        }
        return null
    }

    private fun getAppFilter(packageName: String): Map<String, String> {
        return iconPackCache.getOrPut(packageName) {
            val map = mutableMapOf<String, String>()
            try {
                val iconPackRes = pm.getResourcesForApplication(packageName)
                val filterNames = arrayOf("appfilter", "appfilter_ips", "appfilter_ IPS", "app_filter", "icon_pack")
                var xpp: XmlPullParser? = null
                
                for (name in filterNames) {
                    val id = iconPackRes.getIdentifier(name, "xml", packageName)
                    if (id != 0) {
                        xpp = iconPackRes.getXml(id)
                        break
                    }
                }
                
                if (xpp == null) {
                    try {
                        val assetManager = context.createPackageContext(packageName, 0).assets
                        val filterFile = assetManager.list("")?.find { it.lowercase().startsWith("appfilter") }
                        if (filterFile != null) {
                            val inputStream = assetManager.open(filterFile)
                            xpp = XmlPullParserFactory.newInstance().newPullParser()
                            xpp.setInput(InputStreamReader(inputStream))
                        }
                    } catch (e: Exception) {}
                }
                
                if (xpp != null) {
                    var eventType = xpp.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if (xpp.name == "item") {
                                val component = xpp.getAttributeValue(null, "component")
                                val drawable = xpp.getAttributeValue(null, "drawable")
                                if (component != null && drawable != null) {
                                    map[component] = drawable
                                }
                            }
                        }
                        eventType = xpp.next()
                    }
                }
            } catch (e: Exception) {
                Log.e("IconPackManager", "Error parsing appfilter for $packageName", e)
            }
            map
        }
    }

    data class IconPackInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable?
    )
}
