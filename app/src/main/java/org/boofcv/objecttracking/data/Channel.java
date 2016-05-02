package org.boofcv.objecttracking.data;

import org.json.JSONObject;

/**
 * Created by moraffy on 5/1/2016.
 */
public class Channel implements JSONPopulator{

    private Units units;
    private Wind wind;

    public Units getUnits() {
        return units;
    }

    public Wind getWind() {
        return wind;
    }


    @Override
    public void populate(JSONObject data) {

        units = new Units();
        units.populate(data.optJSONObject("units"));

        wind = new Wind();
        wind.populate(data.optJSONObject("wind"));

    }
}
