package edu.csulb.com.gostrike.app;

/**
 * Created by rishi on 4/1/17.
 */

public interface WebServiceResult<T> {
    public void onTaskComplete(T result);
}