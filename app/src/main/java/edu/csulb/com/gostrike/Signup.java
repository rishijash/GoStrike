package edu.csulb.com.gostrike;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import edu.csulb.com.gostrike.app.Extra;
import edu.csulb.com.gostrike.app.WebService;
import edu.csulb.com.gostrike.app.WebServiceResult;

public class Signup extends AppCompatActivity {

    EditText username_et, email_et, password_et, confirmpassword_et;
    CheckBox agree_cb;
    Extra extra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        //Initialize
        username_et = (EditText)findViewById(R.id.signup_username_editText);
        email_et = (EditText)findViewById(R.id.signup_email_editText);
        password_et = (EditText)findViewById(R.id.signup_password_editText);
        confirmpassword_et = (EditText)findViewById(R.id.signup_confirmpassword_editText);
        agree_cb = (CheckBox)findViewById(R.id.signup_agree_checkBox);
        extra = new Extra(this);
    }

    public void signup(View v) {
        String link = "http://192.168.43.150:8080/signup";
        String username = username_et.getText().toString();
        String password = password_et.getText().toString();
        String confirmpassword = confirmpassword_et.getText().toString();
        String email = email_et.getText().toString();

        if (agree_cb.isChecked()) {
            if(password.equals(confirmpassword))
            {
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("username", username)
                        .appendQueryParameter("password", password)
                        .appendQueryParameter("email", email);
                WebService webService = new WebService(link, builder, new WebServiceResult<JSONObject>() {
                    @Override
                    public void onTaskComplete(JSONObject result) {
                        new Extra(getApplication()).Toast("Login to start playing..");
                        onBackPressed();
                    }
                });
            }
            else
            {
                extra.Toast("Password and Confirm Password does not match.");
            }
        } else {
            extra.Toast("Please agree our Terms & Conditions in order to create an account.");
        }
    }

}
