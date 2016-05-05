package org.boofcv.objecttracking;

import android.database.Cursor;
import android.os.Bundle;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.lang.annotation.Target;

public class StatisticsActivity extends Activity {

    public static final String TAG = "StatisticsActivity";

    private TargetMasterData targetMasterData;
    private SimpleCursorAdapter dataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        Log.d(TAG, "onCreate");


        targetMasterData = TargetMasterData.getInstance(getApplication());
        displayListView();

    }

    private void displayListView() {


        Cursor cursor = targetMasterData.getTestData();

        // The desired columns to be bound
        String[] columns = new String[] {
                TargetMasterData.TIME_TEXT,
                TargetMasterData.DATE_TEXT,
                TargetMasterData.DISTANCE_TEXT,
                TargetMasterData.WIND_SPEED_TEXT,
                TargetMasterData.WIND_DIRECTION_TEXT,
                TargetMasterData.LOCATION_TEXT
        };

        // the XML defined views which the data will be bound to
        int[] to = new int[] {
                R.id.time,
                R.id.date,
                R.id.distance,
                R.id.speed,
                R.id.direction,
                R.id.location,
        };

        // create the adapter using the cursor pointing to the desired data
        //as well as the layout information
        dataAdapter = new SimpleCursorAdapter(
                this, R.layout.stats_layout,
                cursor,
                columns,
                to,
                0);

        ListView listView = (ListView) findViewById(R.id.listView1);
        // Assign adapter to ListView
        listView.setAdapter(dataAdapter);


//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> listView, View view,
//                                    int position, long id) {
//                // Get the cursor, positioned to the corresponding row in the result set
//                Cursor cursor = (Cursor) listView.getItemAtPosition(position);
//
//                // Get the state's capital from this row in the database.
//                String countryCode =
//                        cursor.getString(cursor.getColumnIndexOrThrow(TargetMasterData.QN_TEXT));
//                Toast.makeText(getApplicationContext(),
//                        countryCode, Toast.LENGTH_SHORT).show();
//
//            }
//        });

        EditText myFilter = (EditText) findViewById(R.id.myFilter);
        myFilter.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                dataAdapter.getFilter().filter(s.toString());
            }
        });

        dataAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                return targetMasterData.fetchQuestions(constraint.toString());
            }
        });

    }

}
