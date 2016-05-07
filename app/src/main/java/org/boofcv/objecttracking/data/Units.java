package org.boofcv.objecttracking.data;

import org.json.JSONObject;

/**
 * Created by moraffy on 5/1/2016.
 */
public class Units implements JSONPopulator {
    private String speed;

    public String getSpeed() {
        return speed;
    }

    @Override
    public void populate(JSONObject data) {
        speed = data.optString("speed");

    }
}
