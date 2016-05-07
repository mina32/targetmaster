package org.boofcv.objecttracking.service;

import org.boofcv.objecttracking.data.Channel;

/**
 * Created by moraffy on 5/1/2016.
 */
public interface WeatherServiceCallback {
    void ServiceSuccess (Channel channel);
    void ServiceFailure (Exception exception);
}
