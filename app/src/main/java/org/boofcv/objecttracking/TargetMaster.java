package org.boofcv.objecttracking;

import android.app.Application;
import android.util.Log;

/**
 * Created by moraffy on 5/4/2016.
 */
public class TargetMaster extends Application{
    public final static String TAG="TargetMaster Class";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application created");

    }
}
