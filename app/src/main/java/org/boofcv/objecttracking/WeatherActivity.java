package org.boofcv.objecttracking;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.boofcv.objecttracking.data.Channel;
import org.boofcv.objecttracking.data.Wind;
import org.boofcv.objecttracking.service.WeatherServiceCallback;
import org.boofcv.objecttracking.service.YahooWeatherService;

public class WeatherActivity extends Activity implements WeatherServiceCallback {

    public static final String TAG = "WeatherActivity";

    private YahooWeatherService service;
    private ProgressDialog dialog;

    TextView locationTextView;
    TextView windSpeedTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        locationTextView = (TextView) findViewById(R.id.textView);
        windSpeedTextView = (TextView) findViewById(R.id.textView2);

        service = new YahooWeatherService(this);
        dialog = new ProgressDialog(this);
        dialog. setMessage("Loading...");
        dialog.show();

        service.refreshWeather("San Antonio, TX");
    }

    @Override
    public void ServiceSuccess(Channel channel) {
        dialog.hide();

        Wind wind = channel.getWind();

        locationTextView.setText("Location: " + service.getLocation());
        windSpeedTextView.setText("Wind speed: " + wind.getSpeed() + " " + channel.getUnits().getSpeed());

        Log.d(TAG, "Location: " + service.getLocation());
        Log.d(TAG, "Wind speed: " + wind.getSpeed() + " " + channel.getUnits().getSpeed());
        Log.d(TAG, "Wind direction: " + wind.getDirection());


    }

    @Override
    public void ServiceFailure(Exception exception) {
        dialog.hide();
        Toast.makeText(this,exception.getMessage(),Toast.LENGTH_LONG).show();

    }
}
