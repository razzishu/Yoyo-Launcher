/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.yoyo.launcher.model;

import static com.yoyo.launcher.LauncherSettings.Favorites.CONTAINER_ALL_APPS_PREDICTION;
import static com.yoyo.launcher.LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION;
import static com.yoyo.launcher.util.Executors.MODEL_EXECUTOR;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.yoyo.launcher.InvariantDeviceProfile;
import com.yoyo.launcher.LauncherAppState;
import com.yoyo.launcher.LauncherModel;
import com.yoyo.launcher.LauncherPrefChangeListener;
import com.yoyo.launcher.LauncherPrefs;
import com.yoyo.launcher.LauncherSettings;
import com.yoyo.launcher.dagger.ApplicationContext;
import com.yoyo.launcher.icons.cache.CacheLookupFlag;
import com.yoyo.launcher.model.BgDataModel.FixedContainerItems;
import com.yoyo.launcher.model.data.AppInfo;
import com.yoyo.launcher.model.data.WorkspaceItemInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * ModelDelegate that uses the AppPredictionService via reflection to provide predicted apps.
 * Automatically falls back to UsageStats-based predictions if System API is restricted.
 */
public class PredictionModelDelegate extends ModelDelegate {

    private static final String TAG = "PredictionModelDelegate";
    private static final String BUNDLE_KEY_HOTSEAT_COUNT = "hotseat_count";

    private Object mAllAppsPredictor;
    private Object mHotseatPredictor;
    private boolean mUseFallback = false;

    private List<String> mDockPool = null;

    private static final long USAGE_CACHE_TTL_MS = 5 * 60 * 1000L; // 5 minutes cache
    private long mLastUsageQueryTime = 0L;
    private List<String> mCachedRecentPackages = null;
    private List<String> mCachedTopPackages = null;

    private final List<Object> mPendingTargetsAllApps = new ArrayList<>();
    private final List<Object> mPendingTargetsHotseat = new ArrayList<>();

    @Inject
    public PredictionModelDelegate(@ApplicationContext Context context) {
        super(context);
    }

    @Override
    public void init(LauncherModel model, AllAppsList appsList, BgDataModel dataModel) {
        super.init(model, appsList, dataModel);
        initPredictors();
        LauncherPrefs.get(mContext).addListener(mPrefListener, 
                LauncherPrefs.SUGGESTIONS_ALL_APPS, LauncherPrefs.SUGGESTIONS_HOTSEAT);
    }

    private final LauncherPrefChangeListener mPrefListener = key -> {
        if (LauncherPrefs.SUGGESTIONS_ALL_APPS.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SUGGESTIONS_HOTSEAT.getSharedPrefKey().equals(key)) {
            Log.d(TAG, "Suggestions preference changed, refreshing data...");
            mDockPool = null; 
            mLastUsageQueryTime = 0L; // Invalidate cache on setting toggle
            
            // Immediately clear UI if disabled
            LauncherPrefs lp = LauncherPrefs.get(mContext);
            if (!lp.get(LauncherPrefs.SUGGESTIONS_ALL_APPS)) {
                dispatchToUi(Collections.emptyList(), CONTAINER_ALL_APPS_PREDICTION);
            }
            if (!lp.get(LauncherPrefs.SUGGESTIONS_HOTSEAT)) {
                dispatchToUi(Collections.emptyList(), CONTAINER_HOTSEAT_PREDICTION);
            }
            
            requestPredictionUpdate();
        }
    };

    /**
     * Called by HybridHotseatOrganizer when something changes in the dock.
     * This triggers a re-shuffle of the dock pool to provide variety.
     */
    public void notifyDockChanged() {
        Log.d(TAG, "notifyDockChanged: resetting dock pool");
        mDockPool = null; 
        requestPredictionUpdate();
    }

    private void initPredictors() {
        if (mAllAppsPredictor != null || mHotseatPredictor != null) return;

        Log.d(TAG, "Initializing predictors...");
        try {
            @SuppressWarnings("WrongConstant")
            Object apm = mContext.getSystemService("app_prediction");
            if (apm == null) {
                mUseFallback = true;
                return;
            }

            Class<?> apmClass = Class.forName("android.app.prediction.AppPredictionManager");
            Class<?> contextClass = Class.forName("android.app.prediction.AppPredictionContext");
            Method createSession = apmClass.getMethod("createAppPredictionSession", contextClass);

            InvariantDeviceProfile idp = LauncherAppState.getIDP(mContext);

            mAllAppsPredictor = createSession.invoke(apm, createPredictorContext("all_apps", 
                    idp.numAllAppsColumns, null));
            registerListener(mAllAppsPredictor, CONTAINER_ALL_APPS_PREDICTION);

            int hotseatCount = idp.numShownHotseatIcons;
            Bundle hotseatBundle = new Bundle();
            hotseatBundle.putInt(BUNDLE_KEY_HOTSEAT_COUNT, hotseatCount);
            mHotseatPredictor = createSession.invoke(apm, createPredictorContext("hotseat", 
                    hotseatCount, hotseatBundle));
            registerListener(mHotseatPredictor, CONTAINER_HOTSEAT_PREDICTION);

            mUseFallback = false;
        } catch (Exception e) {
            Throwable cause = e;
            if (e instanceof InvocationTargetException) {
                cause = ((InvocationTargetException) e).getTargetException();
            }
            if (cause instanceof SecurityException) {
                Log.w(TAG, "System AppPredictor access denied. Using fallback.");
            } else {
                Log.e(TAG, "Failed to init predictors", e);
            }
            mUseFallback = true;
        }
    }

    private Object createPredictorContext(String uiSurface, int count, Bundle extras) throws Exception {
        Class<?> builderClass = Class.forName("android.app.prediction.AppPredictionContext$Builder");
        Object builder = builderClass.getConstructor(Context.class).newInstance(mContext);
        builderClass.getMethod("setUiSurface", String.class).invoke(builder, uiSurface);
        builderClass.getMethod("setPredictedTargetCount", int.class).invoke(builder, count);
        if (extras != null) {
            builderClass.getMethod("setExtras", Bundle.class).invoke(builder, extras);
        }
        return builderClass.getMethod("build").invoke(builder);
    }

    private void registerListener(Object predictor, int containerId) throws Exception {
        if (predictor == null) return;
        Class<?> predictorClass = Class.forName("android.app.prediction.AppPredictor");
        Class<?> callbackClass = Class.forName("android.app.prediction.AppPredictor$Callback");
        
        Object proxy = Proxy.newProxyInstance(
                callbackClass.getClassLoader(),
                new Class[]{callbackClass},
                (proxy1, method, args) -> {
                    if (method.getName().equals("onTargetsAvailable") && args != null && args.length > 0) {
                        handleTargets((List) args[0], containerId);
                    }
                    return null;
                });

        predictorClass.getMethod("registerPredictionUpdates", Executor.class, callbackClass)
                .invoke(predictor, MODEL_EXECUTOR, proxy);
    }

    @Override
    public void workspaceLoadComplete() {
        super.workspaceLoadComplete();
        requestPredictionUpdate();
    }

    public void requestPredictionUpdate() {
        LauncherPrefs prefs = LauncherPrefs.get(mContext);
        boolean allAppsEnabled = prefs.get(LauncherPrefs.SUGGESTIONS_ALL_APPS);
        boolean hotseatEnabled = prefs.get(LauncherPrefs.SUGGESTIONS_HOTSEAT);
        if (!allAppsEnabled && !hotseatEnabled) {
            // Both suggestions disabled, skip all background prediction work
            return;
        }

        if (mUseFallback) {
            MODEL_EXECUTOR.execute(() -> {
                if (allAppsEnabled) {
                    generateFallbackPredictions(CONTAINER_ALL_APPS_PREDICTION);
                }
                if (hotseatEnabled) {
                    generateFallbackPredictions(CONTAINER_HOTSEAT_PREDICTION);
                }
            });
            return;
        }

        try {
            if (mAllAppsPredictor != null && allAppsEnabled) {
                mAllAppsPredictor.getClass().getMethod("requestPredictionUpdate").invoke(mAllAppsPredictor);
            }
            if (mHotseatPredictor != null && hotseatEnabled) {
                mHotseatPredictor.getClass().getMethod("requestPredictionUpdate").invoke(mHotseatPredictor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to request prediction update", e);
        }
    }

    private synchronized void updateUsageCacheIfNeeded(UsageStatsManager usm) {
        long now = System.currentTimeMillis();
        if (mCachedRecentPackages != null && (now - mLastUsageQueryTime) < USAGE_CACHE_TTL_MS) {
            return;
        }
        long startTime = now - (1000L * 60 * 60 * 24 * 7); // 7 days window
        Map<String, UsageStats> statsMap = usm.queryAndAggregateUsageStats(startTime, now);
        if (statsMap == null || statsMap.isEmpty()) {
            mCachedRecentPackages = Collections.emptyList();
            mCachedTopPackages = Collections.emptyList();
            mLastUsageQueryTime = now;
            return;
        }

        String myPkg = mContext.getPackageName();
        List<UsageStats> validStats = statsMap.values().stream()
                .filter(s -> s != null && s.getTotalTimeInForeground() > 0 && !myPkg.equals(s.getPackageName()))
                .collect(Collectors.toList());

        mCachedRecentPackages = validStats.stream()
                .sorted((a, b) -> Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()))
                .map(UsageStats::getPackageName)
                .limit(15)
                .collect(Collectors.toList());

        mCachedTopPackages = validStats.stream()
                .sorted((a, b) -> Long.compare(b.getTotalTimeInForeground(), a.getTotalTimeInForeground()))
                .map(UsageStats::getPackageName)
                .limit(30)
                .collect(Collectors.toList());

        mLastUsageQueryTime = now;
    }

    @WorkerThread
    private void generateFallbackPredictions(int containerId) {
        LauncherPrefs prefs = LauncherPrefs.get(mContext);
        boolean enabled = (containerId == CONTAINER_ALL_APPS_PREDICTION)
                ? prefs.get(LauncherPrefs.SUGGESTIONS_ALL_APPS)
                : prefs.get(LauncherPrefs.SUGGESTIONS_HOTSEAT);

        if (!enabled) {
            dispatchToUi(Collections.emptyList(), containerId);
            return;
        }

        UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return;

        updateUsageCacheIfNeeded(usm);

        if (containerId == CONTAINER_ALL_APPS_PREDICTION) {
            List<String> combined = new ArrayList<>(mCachedRecentPackages != null ? mCachedRecentPackages : Collections.emptyList());
            if (mCachedTopPackages != null) {
                for (String pkg : mCachedTopPackages) {
                    if (!combined.contains(pkg)) combined.add(pkg);
                    if (combined.size() >= 20) break;
                }
            }
            dispatchToUi(resolvePackages(combined, containerId), containerId);
        } else {
            if (mDockPool == null) {
                List<String> top = new ArrayList<>(mCachedTopPackages != null ? mCachedTopPackages : Collections.emptyList());
                Collections.shuffle(top);
                mDockPool = top;
            }
            dispatchToUi(resolvePackages(mDockPool, containerId), containerId);
        }
    }

    private List<WorkspaceItemInfo> resolvePackages(List<String> packages, int containerId) {
        List<WorkspaceItemInfo> items = new ArrayList<>();
        InvariantDeviceProfile idp = LauncherAppState.getIDP(mContext);
        int limit = (containerId == CONTAINER_ALL_APPS_PREDICTION) ? idp.numAllAppsColumns : idp.numShownHotseatIcons;

        // Instant in-memory lookup from mAppsList if available
        Map<String, AppInfo> appMap = null;
        if (mAppsList != null && mAppsList.data != null) {
            appMap = new HashMap<>();
            for (AppInfo app : mAppsList.data) {
                if (app != null && app.componentName != null) {
                    appMap.putIfAbsent(app.componentName.getPackageName(), app);
                }
            }
        }

        LauncherApps launcherApps = null;
        UserHandle user = Process.myUserHandle();

        for (String pkg : packages) {
            if (appMap != null && appMap.containsKey(pkg)) {
                AppInfo app = appMap.get(pkg);
                WorkspaceItemInfo info = app.makeWorkspaceItem(mContext);
                info.container = containerId;
                items.add(info);
            } else {
                if (launcherApps == null) {
                    launcherApps = mContext.getSystemService(LauncherApps.class);
                }
                if (launcherApps == null) continue;
                List<LauncherActivityInfo> activities = launcherApps.getActivityList(pkg, user);
                if (activities == null || activities.isEmpty()) continue;
                
                LauncherActivityInfo lai = activities.get(0);
                WorkspaceItemInfo info = new WorkspaceItemInfo();
                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
                info.container = containerId;
                info.intent = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(lai.getComponentName())
                        .setPackage(pkg);
                info.user = user;
                
                LauncherAppState.getInstance(mContext).getIconCache()
                        .getTitleAndIcon(info, lai, CacheLookupFlag.DEFAULT_LOOKUP_FLAG);
                items.add(info);
            }
            if (items.size() >= limit) break;
        }
        return items;
    }

    @WorkerThread
    private void handleTargets(List targets, int containerId) {
        LauncherPrefs prefs = LauncherPrefs.get(mContext);
        boolean enabled = (containerId == CONTAINER_ALL_APPS_PREDICTION)
                ? prefs.get(LauncherPrefs.SUGGESTIONS_ALL_APPS)
                : prefs.get(LauncherPrefs.SUGGESTIONS_HOTSEAT);
        
        if (!enabled) {
            targets = Collections.emptyList();
        }

        if (mAppsList.data.isEmpty() && !targets.isEmpty()) {
            synchronized (mPendingTargetsAllApps) {
                if (containerId == CONTAINER_ALL_APPS_PREDICTION) {
                    mPendingTargetsAllApps.clear();
                    mPendingTargetsAllApps.addAll(targets);
                } else {
                    mPendingTargetsHotseat.clear();
                    mPendingTargetsHotseat.addAll(targets);
                }
            }
            return;
        }
        processTargets(targets, containerId);
    }

    @WorkerThread
    private void processTargets(List targets, int containerId) {
        try {
            Class<?> targetClass = Class.forName("android.app.prediction.AppTarget");
            List<WorkspaceItemInfo> items = new ArrayList<>();
            LauncherApps launcherApps = mContext.getSystemService(LauncherApps.class);

            for (Object target : targets) {
                ComponentName cn = (ComponentName) targetClass.getMethod("getComponentName").invoke(target);
                UserHandle user = (UserHandle) targetClass.getMethod("getUser").invoke(target);
                if (cn == null || user == null) continue;

                WorkspaceItemInfo info = new WorkspaceItemInfo();
                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
                info.container = containerId;
                info.intent = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(cn)
                        .setPackage(cn.getPackageName());
                info.user = user;

                AppInfo appInfo = mAppsList.data.stream()
                        .filter(a -> Objects.equals(a.componentName, cn) && Objects.equals(a.user, user))
                        .findFirst().orElse(null);

                if (appInfo != null) {
                    info.title = appInfo.title;
                    info.bitmap = appInfo.bitmap;
                    info.contentDescription = appInfo.contentDescription;
                } else {
                    LauncherActivityInfo lai = launcherApps.resolveActivity(info.intent, user);
                    if (lai != null) {
                        LauncherAppState.getInstance(mContext).getIconCache()
                                .getTitleAndIcon(info, lai, CacheLookupFlag.DEFAULT_LOOKUP_FLAG);
                    } else continue;
                }
                items.add(info);
            }
            dispatchToUi(items, containerId);
        } catch (Exception e) {
            Log.e(TAG, "Error processing prediction targets", e);
        }
    }

    private void dispatchToUi(List<WorkspaceItemInfo> items, int containerId) {
        MODEL_EXECUTOR.execute(() -> {
            FixedContainerItems fixedContainerItems = new FixedContainerItems(containerId, items);
            mDataModel.extraItems.put(containerId, fixedContainerItems);
            mModel.enqueueModelUpdateTask((taskController, dataModel, apps) ->
                    taskController.bindExtraContainerItems(fixedContainerItems));
        });
    }

    @Override
    public void modelLoadComplete() {
        super.modelLoadComplete();
        synchronized (mPendingTargetsAllApps) {
            if (!mPendingTargetsAllApps.isEmpty()) {
                processTargets(new ArrayList<>(mPendingTargetsAllApps), CONTAINER_ALL_APPS_PREDICTION);
                mPendingTargetsAllApps.clear();
            }
            if (!mPendingTargetsHotseat.isEmpty()) {
                processTargets(new ArrayList<>(mPendingTargetsHotseat), CONTAINER_HOTSEAT_PREDICTION);
                mPendingTargetsHotseat.clear();
            }
        }
    }

    @Override
    public void validateData() {
        super.validateData();
        initPredictors();
        requestPredictionUpdate();
    }

    @Override
    public void destroy() {
        super.destroy();
        LauncherPrefs.get(mContext).removeListener(mPrefListener, 
                LauncherPrefs.SUGGESTIONS_ALL_APPS, LauncherPrefs.SUGGESTIONS_HOTSEAT);
        try {
            if (mAllAppsPredictor != null) {
                mAllAppsPredictor.getClass().getMethod("destroy").invoke(mAllAppsPredictor);
                mAllAppsPredictor = null;
            }
            if (mHotseatPredictor != null) {
                mHotseatPredictor.getClass().getMethod("destroy").invoke(mHotseatPredictor);
                mHotseatPredictor = null;
            }
        } catch (Exception e) { /* Ignore */ }
    }
}
