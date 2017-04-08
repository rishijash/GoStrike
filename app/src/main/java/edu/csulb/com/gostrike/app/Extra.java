package edu.csulb.com.gostrike.app;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by rishi on 4/1/17.
 */

public class Extra {

    Context ctx;

    public Extra(Context ctx){
        this.ctx = ctx;
    }

    public void Toast(String message){
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
    }

    public JSONArray JSONObjectToArray(JSONObject jo)
    {
        Iterator x = jo.keys();
        JSONArray jsonArray = new JSONArray();
        while (x.hasNext()){
            String key = (String) x.next();
            try {
                JSONObject temp  = new JSONObject();
                temp.put("id",key);
                temp.put("location",jo.get(key));
                jsonArray.put(temp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonArray;
    }
}
