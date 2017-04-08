package edu.csulb.com.gostrike.app;

import android.net.Uri;
import android.os.AsyncTask;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by rishi on 4/1/17.
 */

public class WebService extends AsyncTask<String, Integer, JSONObject>{

    String link;
    Uri.Builder builder;
    WebServiceResult<JSONObject> callback;

    public WebService(String link, Uri.Builder builder, WebServiceResult<JSONObject> callback){
        this.link = link;
        this.builder = builder;
        this.callback = callback;
        this.execute();
    }


    @Override
    protected JSONObject doInBackground(String... params) {
        String result=null;
        String imap = "";
        JSONObject jsonObject=null;
        try {
            URL url = new URL(link);
            HttpURLConnection huc = (HttpURLConnection)url.openConnection();
            huc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            huc.setRequestMethod("POST");

//            Uri.Builder builder = new Uri.Builder()
//                    .appendQueryParameter("email", email)
//                    .appendQueryParameter("server",imap)
//                    .appendQueryParameter("username",email)
//                    .appendQueryParameter("ssl","1")
//                    .appendQueryParameter("port","993")
//                    .appendQueryParameter("type","IMAP")
//                    .appendQueryParameter("password",secret);
            String query = builder.build().getEncodedQuery();

            OutputStream os = huc.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();

            huc.connect();

            int resCode = huc.getResponseCode();

            if(resCode == 200)
            {
                StringBuffer sb = new StringBuffer();
                BufferedReader br = new BufferedReader(new InputStreamReader(huc.getInputStream()));
                String line = "";
                while((line = br.readLine()) != null)
                {
                    sb.append(line);
                }
                result = sb.toString();
                jsonObject = new JSONObject(result);
            }
        }
        catch (Exception e)
        {
            System.out.print(e.toString());
        }
       return jsonObject;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        super.onPostExecute(jsonObject);
        callback.onTaskComplete(jsonObject);
    }

}
