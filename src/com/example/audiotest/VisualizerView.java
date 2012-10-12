package com.example.audiotest;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.text.Editable.Factory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * A class that draws visualizations of data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture } and
 * {@link Visualizer.OnDataCaptureListener#onFftDataCapture }
 */
public class VisualizerView extends View {
  private static final String TAG = "VisualizerView";

  private byte[] mBytes;
  private byte[] mFFTBytes;
  private Rect mRect = new Rect(0,0,240,200);
  private Visualizer mVisualizer;

  private Paint mFlashPaint = new Paint();
  private Paint mFadePaint = new Paint();

  public VisualizerView(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs);
    init();
  }

  public VisualizerView(Context context, AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  public VisualizerView(Context context)
  {
    this(context, null, 0);
  }

  private void init() {
    mBytes = null;
    mFFTBytes = null;

    mFlashPaint.setColor(Color.argb(122, 0, 0, 0));
    mFadePaint.setColor(Color.argb(238, 255, 255, 255)); // Adjust alpha to change how quickly the image fades
    mFadePaint.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY));


  }

  /**
   * Links the visualizer to a player
   * @param player - MediaPlayer instance to link to
   */
  public void link(MediaPlayer player)
  {
    if(player == null)
    {
      throw new NullPointerException("Cannot link to null MediaPlayer");
    }

    // Create the Visualizer object and attach it to our media player.
    mVisualizer = new Visualizer(player.getAudioSessionId());
    mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

    // Pass through Visualizer data to VisualizerView
    Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener()
    {
      @Override
      public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
          int samplingRate)
      {
        updateVisualizer(bytes);
      }

      @Override
      public void onFftDataCapture(Visualizer visualizer, byte[] bytes,
          int samplingRate)
      {
        updateVisualizerFFT(bytes);
      }
    };

    mVisualizer.setDataCaptureListener(captureListener,
        Visualizer.getMaxCaptureRate() / 2, true, true);

    // Enabled Visualizer and disable when we're done with the stream
    mVisualizer.setEnabled(true);
    player.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
    {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer)
      {
        mVisualizer.setEnabled(false);
      }
    });
  }

  /**
   * Call to release the resources used by VisualizerView. Like with the
   * MediaPlayer it is good practice to call this method
   */
  public void release()
  {
    mVisualizer.release();
  }

  /**
   * Pass data to the visualizer. Typically this will be obtained from the
   * Android Visualizer.OnDataCaptureListener call back. See
   * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
   * @param bytes
   */
  public void updateVisualizer(byte[] bytes) {
    mBytes = bytes;
    invalidate();
  }

  /**
   * Pass FFT data to the visualizer. Typically this will be obtained from the
   * Android Visualizer.OnDataCaptureListener call back. See
   * {@link Visualizer.OnDataCaptureListener#onFftDataCapture }
   * @param bytes
   */
  public void updateVisualizerFFT(byte[] bytes) {
    mFFTBytes = bytes;
    invalidate();
  }

  boolean mFlash = false;

  /**
   * Call this to make the visualizer flash. Useful for flashing at the start
   * of a song/loop etc...
   */
  public void flash() {
    mFlash = true;
    invalidate();
  }

  Bitmap mCanvasBitmap;
  Canvas mCanvas;

private int mDivisions = 16;

private float[] mFFTPoints;

private boolean mTop = true;


  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    // Create canvas once we're ready to draw
    mRect.set(0, 0, getWidth(), getHeight());

    if(mCanvasBitmap == null)
    {
      mCanvasBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Config.ARGB_8888);
    }
    if(mCanvas == null)
    {
      mCanvas = new Canvas(mCanvasBitmap);
    }
    
    float[] mags = null;
    float[] omags = null;
    
   

    if (mFFTBytes != null) {
    	if (mags == null || mags.length <mFFTBytes.length / mDivisions) {
    		mags = new float[mFFTBytes.length / mDivisions];
     	    }
    	
    	if (omags == null || omags.length <mFFTBytes.length / mDivisions) {
    		omags = new float[mFFTBytes.length / mDivisions];
     	    }
    	
    	 if (mFFTPoints == null || mFFTPoints.length < mFFTBytes.length * 4) {
   	      mFFTPoints = new float[mFFTBytes.length * 4];
   	    }
    	try
    	{    		
	    	for (int i = 0; i < mFFTBytes.length / mDivisions; i++) {
    	      mFFTPoints[i * 4] = i * 4 * mDivisions;
    	      mFFTPoints[i * 4 + 2] = i * 4 * mDivisions;
    	      byte rfk = mFFTBytes[mDivisions * i];
    	      byte ifk = mFFTBytes[mDivisions * i + 1];
    	      float magnitude = (rfk * rfk + ifk * ifk);
    	      omags[i] = mags[i];
    	      mags[i] = magnitude;
    	      int dbValue = (int) (10 * Math.log10(magnitude));
    	      

    	      if(mTop)
    	      {
    	        mFFTPoints[i * 4 + 1] = 0;
    	        mFFTPoints[i * 4 + 3] = (dbValue * 2 - 10);
    	      }
    	      else
    	      {
    	        mFFTPoints[i * 4 + 1] = mRect.height();
    	        mFFTPoints[i * 4 + 3] = mRect.height() - (dbValue * 2 - 10);
    	      }
    	    }
	    	
	
    	    canvas.drawLines(mFFTPoints, mFlashPaint);
    	    
    	    int threshold = 3;
    	    int yOff = 0;
    	    
    	    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
    	    for(int i=1;i<mags.length;i++)
    	    {
    	    	if(i%6==0)
    	    	{
    	    		yOff+=bmp.getHeight();
    	    	}
    	    	if(mags[i] > omags[i]+threshold)
    	    	{
    	    		canvas.drawBitmap(bmp, (i%6)*bmp.getWidth()+5, 0+ yOff, null);   	   
    	    	}
    	    }
	    	    
	    	    
    	} catch (Exception ex)
    	{
    		canvas.drawText(ex.getMessage(), 0, 100, mFlashPaint);
    		
    	}
    }
  }
}