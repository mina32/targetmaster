package org.boofcv.objecttracking;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
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

/**
 * Activity which opens a camera and lets the user select objects to track and switch between
 * different trackers.  The user selects an object by clicking and dragging until the drawn
 * rectangle fills the object.  To select a new object click reset.
 */

// TODO Use Yahoo Weather API

public class ObjectTrackerActivity extends VideoDisplayActivity
        implements AdapterView.OnItemSelectedListener, View.OnTouchListener
{
    int mode = 0;

    // size of the minimum square which the user can select
    final static int MINIMUM_MOTION = 20;

    Point2D_I32 click0 = new Point2D_I32();
    Point2D_I32 click1 = new Point2D_I32();


    TextView up;
    TextView down;
    TextView left;
    TextView right;
    Button weatherButton;
    Button saveButton;

    Quadrilateral_F64 location = new Quadrilateral_F64();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getLayoutInflater();
       LinearLayout controls = (LinearLayout)inflater.inflate(R.layout.objecttrack_controls,null);

        LinearLayout parent = getViewContent();
        parent.addView(controls);

        FrameLayout iv = getViewPreview();
        iv.setOnTouchListener(this);
        up = (TextView) findViewById(R.id.up_indicator);
        down = (TextView) findViewById(R.id.down_indicator);
        left = (TextView) findViewById(R.id.left_indicator);
        right = (TextView) findViewById(R.id.right_indicator);

        weatherButton = (Button) findViewById(R.id.button_weather);
        weatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ObjectTrackerActivity.this, WeatherActivity.class);
                startActivity(intent);
            }
        });

        saveButton = (Button) findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ObjectTrackerActivity.this, SaveActivity.class);
                startActivity(intent);
            }
        });

        up.setBackgroundColor(Color.GREEN);
        down.setBackgroundColor(Color.GREEN);
        left.setBackgroundColor(Color.GREEN);
        right.setBackgroundColor(Color.GREEN);


       // spinnerView = (Spinner)controls.findViewById(R.id.spinner_algs);
      /*  ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.tracking_objects, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      //  spinnerView.setAdapter(adapter);*/

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

}

