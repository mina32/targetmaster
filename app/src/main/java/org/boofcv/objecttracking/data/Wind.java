package org.boofcv.objecttracking.data;

import org.json.JSONObject;

/**
 * Created by moraffy on 5/1/2016.
 */
public class Wind implements JSONPopulator {

    private int chill;
    private int direction;
    private int speed;

    public int getChill() {
        return chill;
    }

    public int getDirection() {
        return direction;
    }

    public int getSpeed() {
        return speed;
    }




    @Override
    public void populate(JSONObject data) {
        chill = data.optInt("chill");
        direction = data.optInt("direction");
        speed = data.optInt("speed");

    }
}
