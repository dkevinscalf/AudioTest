package com.example.audiotest;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity {

    private MediaPlayer mPlayer;
	private VisualizerView mVisualizerView;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private void init()
    {
      mPlayer = MediaPlayer.create(this, R.raw.test);
      mPlayer.setLooping(true);
      //mPlayer.start();

      // We need to link the visualizer view to the media player so that
      // it displays something
      mVisualizerView = new VisualizerView(this);
      mVisualizerView.link(mPlayer);
      setContentView(mVisualizerView);
    }
}
