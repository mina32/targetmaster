package org.boofcv.objecttracking;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveActivity extends Activity {

    public static final String TAG = "SaveActivity";

    private EditText txtTime;
    private EditText txtDate;
    private EditText txtDistance;
    private EditText txtWindSpeed;
    private EditText txtWindSDirection;
    private EditText txtLocation;

    Button saveInfoButton;
    Button statsButton;

    private TargetMasterData targetMasterData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save);
        Log.d(TAG, "onCreate");

        targetMasterData = TargetMasterData.getInstance(getApplication());


        txtTime = (EditText) findViewById(R.id.input_time);
        txtDate = (EditText) findViewById(R.id.input_date);
        txtDistance = (EditText) findViewById(R.id.input_distance);
        txtWindSpeed = (EditText) findViewById(R.id.input_wind_speed);
        txtWindSDirection = (EditText) findViewById(R.id.input_wind_direction);
        txtLocation = (EditText) findViewById(R.id.input_location);


        String date = new SimpleDateFormat(
                "MM/dd/yyyy").format(new Date());

        String time = new SimpleDateFormat(
                "HH:mm").format(new Date());

        txtDate.setText(date);
        txtTime.setText(time);
        txtDistance.setText("50 yds");
        txtLocation.setText("My home");
        txtWindSDirection.setText("10 degrees");
        txtWindSpeed.setText("10 mph");

        statsButton = (Button) findViewById(R.id.btn_stats);
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Stats Button Clicked");
                Intent intent = new Intent(SaveActivity.this, StatisticsActivity.class);
                startActivity(intent);
            }
        });

        saveInfoButton = (Button) findViewById(R.id.btn_save_info);
        saveInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Save Info Pressed");
                String time = txtTime.getText().toString();
                String date = txtDate.getText().toString();
                String distance = txtDistance.getText().toString();
                String wind_speed = txtWindSpeed.getText().toString();
                String wind_direction = txtWindSDirection.getText().toString();
                String location = txtLocation.getText().toString();


                targetMasterData.SaveInfo(time, date, distance, wind_speed,
                        wind_direction, location, "/sdcard/DCIM/Camera/20160506_124505.mp4");
                Toast.makeText(SaveActivity.this, "Successful Save", Toast.LENGTH_SHORT).show();
                Log.d(TAG , "Info saved ");


            }
        });


    }


}
