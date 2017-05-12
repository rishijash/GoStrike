package edu.csulb.com.gostrike.app;

import android.content.Context;
import android.content.SharedPreferences;
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

    public void setIp(String ip)
    {
        SharedPreferences.Editor sfe = ctx.getSharedPreferences("GoStrike",Context.MODE_PRIVATE).edit();
        sfe.putString("IP",ip);
        sfe.commit();
    }

    public String getIP()
    {
        SharedPreferences sf = ctx.getSharedPreferences("GoStrike",Context.MODE_PRIVATE);
        return sf.getString("IP",null);

    }

    public void setWon(int point)
    {
        SharedPreferences.Editor sfe = ctx.getSharedPreferences("GoStrike",Context.MODE_PRIVATE).edit();
        sfe.putInt("won",point);
        sfe.commit();
    }

    public void setLost(int point)
    {
        SharedPreferences.Editor sfe = ctx.getSharedPreferences("GoStrike",Context.MODE_PRIVATE).edit();
        sfe.putInt("lost",point);
        sfe.commit();
    }

    public int getWon()
    {
        SharedPreferences sf = ctx.getSharedPreferences("GoStrike",Context.MODE_PRIVATE);
        return sf.getInt("won",0);
    }

    public int getLost()
    {
        SharedPreferences sf = ctx.getSharedPreferences("GoStrike",Context.MODE_PRIVATE);
        return sf.getInt("lost",0);
    }

    public String getUsername()
    {
        SharedPreferences sf = ctx.getSharedPreferences("GoStrike",Context.MODE_PRIVATE);
        return sf.getString("username",null);
    }

    public void clearSharedPref()
    {
        SharedPreferences.Editor sfe = ctx.getSharedPreferences("GoStrike",ctx.MODE_PRIVATE).edit();
        sfe.putString("username",null);
        sfe.putInt("won",0);
        sfe.putInt("lost",0);
        sfe.commit();
    }
}
