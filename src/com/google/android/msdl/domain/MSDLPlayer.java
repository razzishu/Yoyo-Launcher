package com.google.android.msdl.domain;

import android.content.Context;
import android.os.Vibrator;
import com.google.android.msdl.data.model.MSDLToken;
import com.google.android.msdl.logging.MSDLEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public interface MSDLPlayer {
    void playToken(MSDLToken token, InteractionProperties properties);
    List<MSDLEvent> getHistory();
    
    class Companion {
        public static MSDLPlayer createPlayer(Vibrator vibrator, Context context) {
            return new MSDLPlayerStub();
        }
        public static MSDLPlayer createPlayer(Vibrator vibrator, ExecutorService executor, Context context) {
            return new MSDLPlayerStub();
        }
    }
}

class MSDLPlayerStub implements MSDLPlayer {
    @Override
    public void playToken(MSDLToken token, InteractionProperties properties) {}
    
    @Override
    public List<MSDLEvent> getHistory() {
        return new ArrayList<>();
    }
    
    @Override
    public String toString() {
        return "MSDLPlayerStub";
    }
}
