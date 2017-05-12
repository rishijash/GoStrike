package edu.csulb.com.gostrike;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.csulb.com.gostrike.app.Extra;
import edu.csulb.com.gostrike.app.WebService;
import edu.csulb.com.gostrike.app.WebServiceResult;

public class MainActivity extends AppCompatActivity {

    EditText username_et, password_et;
    private static final int PER_REQUEST_CODE1 = 100;
    private static final int PER_REQUEST_CODE2 = 200;
    Extra extra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        extra = new Extra(getApplicationContext());

        extra.setIp("192.168.0.114");

        //Initialize
        username_et = (EditText)findViewById(R.id.login_username_editText);
        password_et = (EditText)findViewById(R.id.login_password_editText);

        SharedPreferences sfe = getSharedPreferences("GoStrike",MODE_PRIVATE);
//        username_et.setText(sfe.getString("username",null));
        String un = sfe.getString("username",null);
        if(un!=null)
        {
            login();
        }

        //Check Permissions
        try{
            if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PER_REQUEST_CODE1);
            }
            else if(ActivityCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PER_REQUEST_CODE2);
            }
        }
        catch (Exception e)
        {
            System.out.print(e.toString());
        }
    }

    public void login(View v)
    {
        String link = "http://"+extra.getIP() + ":8080/login";
        String username = username_et.getText().toString();
        String password = password_et.getText().toString();
        Uri.Builder builder = new Uri.Builder()
                .appendQueryParameter("username", username)
                .appendQueryParameter("password",password);
        WebService webService = new WebService(link, builder, new WebServiceResult<JSONObject>() {
            @Override
            public void onTaskComplete(JSONObject result) {
                try {
                    if(result.getString("result").equals("ok"))
                    {
                        SharedPreferences.Editor sfe = getSharedPreferences("GoStrike",MODE_PRIVATE).edit();
                        sfe.putString("username",username_et.getText().toString());
                        sfe.commit();
                        login();
                    }
                    else
                    {
                        new Extra(getApplicationContext()).Toast("Invalid Username/Password");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void login(){

        Intent i = new Intent(this, ScoreBoard.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    public void signup(View v)
    {
        //Go to Signup Activity
        Intent i = new Intent(this, Signup.class);
        startActivity(i);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PER_REQUEST_CODE1){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //Your permission
                if(ActivityCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PER_REQUEST_CODE2);
                }
            }else{
                Toast.makeText(this, "Please provide appropriate permissions to avoid any unexpected behavior.", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == PER_REQUEST_CODE2){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //Your permission
            }else{
                Toast.makeText(this, "Please provide appropriate permissions to avoid any unexpected behavior.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
