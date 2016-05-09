package org.boofcv.objecttracking;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.boofcv.objecttracking.data.Channel;
import org.boofcv.objecttracking.data.Wind;
import org.boofcv.objecttracking.service.WeatherServiceCallback;
import org.boofcv.objecttracking.service.YahooWeatherService;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import boofcv.abst.tracker.ConfigComaniciu2003;
import boofcv.abst.tracker.ConfigTld;
import boofcv.abst.tracker.MeanShiftLikelihoodType;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoDisplayActivity;
import boofcv.android.gui.VideoImageProcessing;
import boofcv.core.image.ConvertImage;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;

import android.support.design.widget.Snackbar;

/**
 * Activity which opens a camera and lets the user select objects to track and switch between
 * different trackers.  The user selects an object by clicking and dragging until the drawn
 * rectangle fills the object.  To select a new object click reset.
 */

// TODO Use Yahoo Weather API

public class ObjectTrackerActivity extends VideoDisplayActivity
        implements AdapterView.OnItemSelectedListener, View.OnTouchListener, WeatherServiceCallback
{
    public static final String TAG = "ObjectTrackerActivity";

    private  String videopath = "none";
    private String windSpeed = "none";
//    MediaPlayer mediaPlayer;

    int mode = 0;

    // size of the minimum square which the user can select
    final static int MINIMUM_MOTION = 20;

    Point2D_I32 click0 = new Point2D_I32();
    Point2D_I32 click1 = new Point2D_I32();

    Button saveButton;

    private YahooWeatherService service;
    private ProgressDialog dialog;

    TextView windSpeedTextView;


    Button record;
    Button watch;

    Quadrilateral_F64 location = new Quadrilateral_F64();

    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private ToggleButton mToggleButton;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSIONS = 10;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getLayoutInflater();
       LinearLayout controls = (LinearLayout)inflater.inflate(R.layout.objecttrack_controls,null);

        LinearLayout parent = getViewContent();
        parent.addView(controls);

        FrameLayout iv = getViewPreview();
        iv.setOnTouchListener(this);

        record = (Button) findViewById(R.id.record);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ObjectTrackerActivity.this,VideoActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        watch = (Button) findViewById(R.id.watch);
        watch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.sound_file_1);

                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                File file = new File(videopath); // TODO Check if videopath is correct first
                viewIntent.setDataAndType(Uri.fromFile(file), "video/*");
                startActivity(Intent.createChooser(viewIntent, null));

/*                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(videopath);
                    mediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();

                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                }*/
            }
        });

        windSpeedTextView = (TextView) findViewById(R.id.txtView_WindSpeed);

        service = new YahooWeatherService(this);
        dialog = new ProgressDialog(this);
        dialog. setMessage("Loading...");
        dialog.show();

        service.refreshWeather("San Antonio, TX");


        saveButton = (Button) findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ObjectTrackerActivity.this, SaveActivity.class);
                intent.putExtra("videoPath", videopath);
                intent.putExtra("windSpeed",windSpeed);
                startActivity(intent);
            }
        });



        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(ObjectTrackerActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat
                        .checkSelfPermission(ObjectTrackerActivity.this,
                                Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (true ||
                            ActivityCompat.shouldShowRequestPermissionRationale
                                    (ObjectTrackerActivity.this, Manifest.permission.RECORD_AUDIO)) {
                        mToggleButton.setChecked(false);
                        Snackbar.make(findViewById(android.R.id.content), R.string.label_permissions,
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ActivityCompat.requestPermissions(ObjectTrackerActivity.this,
                                                new String[]{Manifest.permission
                                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                                REQUEST_PERMISSIONS);
                                    }
                                }).show();
                    } else {
                        ActivityCompat.requestPermissions(ObjectTrackerActivity.this,
                                new String[]{Manifest.permission
                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                REQUEST_PERMISSIONS);
                    }
                } else {
                    onToggleScreenShare(v);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        startObjectTracking();

        // uncomment the line below to visually show the FPS.
        // The FPS is affected by the camera speed and processing power.  The camera speed
        // is often determined by ambient lighting conditions
//        setShowFPS(true);
    }

    @Override
    protected Camera openConfigureCamera( Camera.CameraInfo cameraInfo )
    {
        Camera mCamera = UtilVarious.selectAndOpenCamera(cameraInfo, this);
        Camera.Parameters param = mCamera.getParameters();

        // Select the preview size closest to 320x240
        // Smaller images are recommended because some computer vision operations are very expensive
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        Camera.Size s = sizes.get(UtilVarious.closest(sizes, 320, 240));
        param.setPreviewSize(s.width,s.height);
        mCamera.setParameters(param);

        return mCamera;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id ) {
        startObjectTracking();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            mToggleButton.setChecked(false);
            return;
        }
        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    public void onToggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {
            initRecorder();
            shareScreen();
        } else {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.v(TAG, "Stopping Recording");
            stopScreenSharing();
        }
    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private void initRecorder() {
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            String videoName = new SimpleDateFormat(
                    "'TargetMaster_'yyyyMMddHHmmss'.mp4'").format(new Date());

//        videopath = "/sdcard/DCIM/Camera/" + videoName;
            videopath = Environment.getExternalStorageDirectory().getPath() + "/"+ videoName;
            mMediaRecorder.setOutputFile(videopath);
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(30);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        //mMediaRecorder.release(); //If used: mMediaRecorder object cannot
        // be reused again
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "MediaProjection Stopped");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if ((grantResults.length > 0) && (grantResults[0] +
                        grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    onToggleScreenShare(mToggleButton);
                } else {
                    mToggleButton.setChecked(false);
                    Snackbar.make(findViewById(android.R.id.content), R.string.label_permissions,
                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(intent);
                                }
                            }).show();
                }
                return;
            }
        }
    }

    private void startObjectTracking() {
        TrackerObjectQuad tracker = null;
        ImageType imageType = null;

                imageType = ImageType.single(ImageUInt8.class);
                tracker = FactoryTrackerObjectQuad.tld(new ConfigTld(false),ImageUInt8.class);

        setProcessing(new TrackingProcessing(tracker,imageType) );
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if( mode == 0 ) {
            if(MotionEvent.ACTION_DOWN == motionEvent.getActionMasked()) {
                click0.set((int) motionEvent.getX(), (int) motionEvent.getY());
                click1.set((int) motionEvent.getX(), (int) motionEvent.getY());
                mode = 1;
            }
        } else if( mode == 1 ) {
            if(MotionEvent.ACTION_MOVE == motionEvent.getActionMasked()) {
                click1.set((int)motionEvent.getX(),(int)motionEvent.getY());
            } else if(MotionEvent.ACTION_UP == motionEvent.getActionMasked()) {
                click1.set((int)motionEvent.getX(),(int)motionEvent.getY());
                mode = 2;
            }
        }
        return true;
    }

    public void resetPressed( View view ) {
        mode = 0;
    }

    @Override
    public void ServiceSuccess(Channel channel) {

        dialog.hide();

        Wind wind = channel.getWind();

        windSpeed = wind.getSpeed() + " " + channel.getUnits().getSpeed();

//        locationTextView.setText("Location: " + service.getLocation());
        windSpeedTextView.setText("Wind speed: " + windSpeed );

        Log.d(TAG, "Location: " + service.getLocation());
        Log.d(TAG, "Wind speed: " + wind.getSpeed() + " " + channel.getUnits().getSpeed());
        Log.d(TAG, "Wind direction: " + wind.getDirection());

    }

    @Override
    public void ServiceFailure(Exception exception) {

        dialog.hide();
        Toast.makeText(this,exception.getMessage(),Toast.LENGTH_LONG).show();

    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mToggleButton.isChecked()) {
                mToggleButton.setChecked(false);
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
            }
            mMediaProjection = null;
            stopScreenSharing();
        }
    }

    protected class TrackingProcessing<T extends ImageBase> extends VideoImageProcessing<MultiSpectral<ImageUInt8>>
    {

        T input;
        ImageType<T> inputType;

        TrackerObjectQuad tracker;
        boolean visible;

       // Quadrilateral_F64 location = new Quadrilateral_F64();

        Paint paintSelected = new Paint();
        Paint paintLine0 = new Paint();
        Paint paintLine1 = new Paint();
        Paint paintLine2 = new Paint();
        Paint paintLine3 = new Paint();
        Paint paintCircle1 = new Paint();
        Paint paintCircle2 = new Paint();
        private Paint textPaint = new Paint();

        protected TrackingProcessing(TrackerObjectQuad tracker , ImageType<T> inputType) {
            super(ImageType.ms(3,ImageUInt8.class));
            this.inputType = inputType;

            if( inputType.getFamily() == ImageType.Family.SINGLE_BAND ) {
                input = inputType.createImage(1,1);
            }

            mode = 0;
            this.tracker = tracker;

            paintSelected.setColor(Color.argb(0xFF / 2, 0xFF, 0, 0));

            paintLine0.setColor(Color.BLUE);
            paintLine0.setStrokeWidth(3f);
            paintLine1.setColor(Color.BLUE);
            paintLine1.setStrokeWidth(3f);
            paintLine2.setColor(Color.BLUE);
            paintLine2.setStrokeWidth(3f);
            paintLine3.setColor(Color.BLUE);
            paintLine3.setStrokeWidth(3f);
            paintCircle1.setColor(Color.RED);
            paintCircle2.setColor(Color.GREEN);

            // Create out paint to use for drawing
            textPaint.setARGB(255, 200, 0, 0);
            textPaint.setTextSize(60);

        }

        @Override
        protected void process(MultiSpectral<ImageUInt8> input, Bitmap output, byte[] storage)
        {
            updateTracker(input);
            visualize(input, output, storage);
        }

        private void updateTracker(MultiSpectral<ImageUInt8> color) {
            if( inputType.getFamily() == ImageType.Family.SINGLE_BAND ) {
                input.reshape(color.width,color.height);
                ConvertImage.average(color, (ImageUInt8) input);
            } else {
                input = (T)color;
            }

            if( mode == 2 ) {
                imageToOutput(click0.x, click0.y, location.a);
                imageToOutput(click1.x, click1.y, location.c);

                // make sure the user selected a valid region
                makeInBounds(location.a);
                makeInBounds(location.c);

                if( movedSignificantly(location.a,location.c) ) {
                    // use the selected region and start the tracker
                    location.b.set(location.c.x, location.a.y);
                    location.d.set( location.a.x, location.c.y );

                    tracker.initialize(input, location);
                    visible = true;
                    mode = 3;
                } else {
                    // the user screw up. Let them know what they did wrong
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ObjectTrackerActivity.this, "Drag a larger region", Toast.LENGTH_SHORT).show();
                        }
                    });
                    mode = 0;
                }
            } else if( mode == 3 ) {
                visible = tracker.process(input,location);
            }
        }

        private void visualize(MultiSpectral<ImageUInt8> color, Bitmap output, byte[] storage) {
            ConvertBitmap.multiToBitmap(color, output, storage);
            Canvas canvas = new Canvas(output);
            float midx = canvas.getWidth()/2;
            float midy = canvas.getHeight()/2;
            canvas.drawCircle(midx, midy, 3, paintCircle1);
            if( mode == 1 ) {
                Point2D_F64 a = new Point2D_F64();
                Point2D_F64 b = new Point2D_F64();

                imageToOutput(click0.x, click0.y, a);
                imageToOutput(click1.x, click1.y, b);

                canvas.drawRect((int) a.x, (int) a.y, (int) b.x, (int) b.y, paintSelected);


            } else if( mode >= 2 ) {
                if( visible ) {
                    Quadrilateral_F64 q = location;

                    drawLine(canvas,q.a,q.b,paintLine0);
                    drawLine(canvas,q.b,q.c,paintLine1);
                    drawLine(canvas,q.c,q.d,paintLine2);
                    drawLine(canvas,q.d, q.a, paintLine3);

                    float cx = (float) (q.a.getX()+q.c.getX())/2;
                    float cy = (float)(q.a.getY()+q.c.getY())/2;

                    canvas.drawCircle(cx, cy, 3, paintCircle2);

                    canvas.drawText(("Coordinates: ( " + Float.toString(cx) + " , " + Float.toString(cy) + " )"), 10, 10, paintCircle1);
                    //canvas.drawText((Float.toString(canvas.getWidth()) + "," + Float.toString(canvas.getHeight())), 50, 50, paintCircle1);

                    if(cx < midx -10 )
                        canvas.drawPath(leftArrow(20, 20),paintCircle2);
                   if(cx > midx +10)
                       canvas.drawPath(rightArrow(30, 30),paintCircle2);
                    if(cy > midy+10)
                        canvas.drawPath(downArrow(30, 30),paintCircle2);
                    if(cy< midy-10)
                        canvas.drawPath(upArrow(20,20),paintCircle2);

                } else {
                    canvas.drawText("?",color.width/2,color.height/2,textPaint);
                }
            }
        }




        private void drawLine( Canvas canvas , Point2D_F64 a , Point2D_F64 b , Paint color ) {
            canvas.drawLine((int)a.x,(int)a.y,(int)b.x,(int)b.y,color);
        }

        private void makeInBounds( Point2D_F64 p ) {
            if( p.x < 0 ) p.x = 0;
            else if( p.x >= input.width )
                p.x = input.width - 1;

            if( p.y < 0 ) p.y = 0;
            else if( p.y >= input.height )
                p.y = input.height - 1;

        }

        private boolean movedSignificantly( Point2D_F64 a , Point2D_F64 b ) {
            if( Math.abs(a.x-b.x) < MINIMUM_MOTION )
                return false;
            if( Math.abs(a.y-b.y) < MINIMUM_MOTION )
                return false;

            return true;
        }


    }

    private static Path rightArrow(float length, float height) {
        Path p = new Path();
        p.moveTo(-2.0f,0.0f);
        p.lineTo(length,height/2.0f);
        p.lineTo(-2.0f,height);
        p.lineTo(-2.0f,0.0f);
        p.offset(280,110);
        p.close();
        return p;

    }

    private static Path downArrow(float length, float height){
        Path p = new Path();
        p.moveTo(-2.0f,0.0f);
        p.lineTo(length, 0.0f);
        p.lineTo(length/2.0f,height);
        p.lineTo(-2.0f,0.0f);
        p.offset(150,200);
        p.close();
        return p;
    }

    private static Path leftArrow(float length, float height){
        Path p = new Path();
        p.moveTo(-0.0f,0.0f);
        p.lineTo(length, height/-1.3f);
        p.lineTo(length, height);
        p.lineTo(-0.0f, 0.0f);
        p.offset(10,110);
        p.close();
        return p;
    }

    private static Path upArrow(float length, float height){
        Path p = new Path();
        p.moveTo(-0.0f, 0.0f);
        p.lineTo(length/-1.3f,height);
        p.lineTo(length, height);
        p.lineTo(-0.0f, 0.0f);
        p.offset(165,10);
        p.close();
        return p;
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                videopath =data.getStringExtra("result");
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }//onActivityResult*/

}

