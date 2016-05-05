package org.boofcv.objecttracking;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class SaveActivity extends Activity {

    public static final String TAG = "SaveActivity";


    Button statsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save);

        Log.d(TAG, "onCreate");

        statsButton = (Button) findViewById(R.id.btn_stats);
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Stats Button Clicked");
                Intent intent = new Intent(SaveActivity.this, StatisticsActivity.class);
                startActivity(intent);
            }
        });
    }


}
