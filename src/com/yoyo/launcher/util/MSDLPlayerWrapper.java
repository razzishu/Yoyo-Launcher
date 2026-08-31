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

package com.yoyo.launcher.util;

import android.content.Context;
import android.os.Vibrator;

import androidx.annotation.Nullable;

import com.yoyo.launcher.dagger.ApplicationContext;
import com.yoyo.launcher.dagger.LauncherAppSingleton;
import com.yoyo.launcher.dagger.LauncherBaseAppComponent;
import com.yoyo.launcher.logging.DumpManager;

import com.google.android.msdl.data.model.MSDLToken;
import com.google.android.msdl.domain.InteractionProperties;
import com.google.android.msdl.domain.MSDLPlayer;
import com.google.android.msdl.logging.MSDLEvent;

import java.io.PrintWriter;
import java.util.List;

import javax.inject.Inject;

/**
 * Wrapper around {@link com.google.android.msdl.domain.MSDLPlayer} to perform MSDL feedback.
 */
@LauncherAppSingleton
public class MSDLPlayerWrapper {

    public static final DaggerSingletonObject<MSDLPlayerWrapper> INSTANCE =
            new DaggerSingletonObject<>(LauncherBaseAppComponent::getMSDLPlayerWrapper);

    /** Internal player */
    private final MSDLPlayer mMSDLPlayer;

    @Inject
    public MSDLPlayerWrapper(@ApplicationContext Context context,
            DumpManager dumpManager, DaggerSingletonTracker lifeCycle) {
        Vibrator vibrator = context.getSystemService(Vibrator.class);
        mMSDLPlayer = MSDLPlayer.Companion.createPlayer(vibrator,
                java.util.concurrent.Executors.newSingleThreadExecutor(),
                null /* useHapticFeedbackForToken */);
        lifeCycle.addCloseable(dumpManager.register(this::dump));
    }

    /** Perform MSDL feedback for a token with interaction properties */
    public void playToken(MSDLToken token, InteractionProperties properties) {
        if (mMSDLPlayer != null) {
            mMSDLPlayer.playToken(token, properties);
        }
    }

    /** Perform MSDL feedback for a token without properties */
    public void playToken(MSDLToken token) {
        playToken(token, null);
    }

    public List<MSDLEvent> getHistory() {
        return mMSDLPlayer != null ? mMSDLPlayer.getHistory() : new java.util.ArrayList<>();
    }

    /** Print the latest history of MSDL tokens played */
    private void dump(String prefix, PrintWriter writer, @Nullable String[] args) {
        if (mMSDLPlayer != null) {
            writer.println(prefix + mMSDLPlayer.toString());
            writer.println(prefix + "MSDLPlayerWrapper history of latest events:");
            List<MSDLEvent> events = getHistory();
            for (MSDLEvent event: events) {
                writer.println(prefix + "\t" + event);
            }
        } else {
            writer.println(prefix + "MSDLPlayer is null");
        }
    }
}
