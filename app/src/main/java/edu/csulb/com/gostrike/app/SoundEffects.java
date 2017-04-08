package edu.csulb.com.gostrike.app;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;

/**
 * Created by rishi on 4/3/17.
 */

public class SoundEffects extends AsyncTask {

    Context ctx;
    int sound;

    public SoundEffects(Context ctx, int sound)
    {
        this.ctx = ctx;
        this.sound = sound;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        MediaPlayer player = MediaPlayer.create(ctx, sound);
        player.setLooping(false); // Set looping
        player.setVolume(100,100);
        player.start();

        return null;
    }
}
