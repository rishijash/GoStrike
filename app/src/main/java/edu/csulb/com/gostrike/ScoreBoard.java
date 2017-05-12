package edu.csulb.com.gostrike;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import edu.csulb.com.gostrike.app.Extra;

public class ScoreBoard extends AppCompatActivity {

    Button battle_button;
    TextView wonscore, lostscore;
    Extra extra;
    TextView startlog,usernameshow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_board);

        //Initialize
        battle_button = (Button)findViewById(R.id.gotobattle);
        wonscore = (TextView)findViewById(R.id.wonscore);
        lostscore = (TextView)findViewById(R.id.lostscore);
        extra = new Extra(getApplicationContext());
        startlog = (TextView)findViewById(R.id.textView6);
        usernameshow = (TextView)findViewById(R.id.usernameshow);

        //Setdata and listeners
        usernameshow.setText("Welcome, " + extra.getUsername());
        wonscore.setText(extra.getWon()+"");
        lostscore.setText(extra.getLost()+"");

        battle_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });
    }

    private void play()
    {
        Intent i = new Intent(ScoreBoard.this, Dashboard.class);
        startActivity(i);
    }

    private void logout()
    {
        extra.clearSharedPref();
        Intent i = new Intent(ScoreBoard.this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.lout)
        {
            //Logout
            logout();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

}
