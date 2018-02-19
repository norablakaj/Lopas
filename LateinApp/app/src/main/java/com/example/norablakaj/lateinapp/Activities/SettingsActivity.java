package com.example.norablakaj.lateinapp.Activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.example.norablakaj.lateinapp.Activities.Home;
import com.example.norablakaj.lateinapp.R;

public class SettingsActivity extends AppCompatActivity {

    Switch toggleDevModeSwitch;

    //TODO: Make this into a enum
    private int devSwitchState = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toggleDevModeSwitch = findViewById(R.id.toggleDevModeSwitch);

        toggleDevModeSwitch.setChecked(Home.getDeveloper());

        toggleDevModeSwitch.setVisibility(View.GONE);

        Button buttonTopLeft = findViewById(R.id.settingsInvisTopLeft);
        Button buttonTopRight = findViewById(R.id.settingsInvisTopRight);
        Button buttonBottomLeft = findViewById(R.id.settingsInvisBottomLeft);
        Button buttonBottomRight = findViewById(R.id.settingsInvisBottomRight);

        buttonTopLeft.setVisibility(View.INVISIBLE);
        buttonTopRight.setVisibility(View.INVISIBLE);
        buttonBottomLeft.setVisibility(View.INVISIBLE);
        buttonBottomRight.setVisibility(View.INVISIBLE);
        //TODO: Make this work for invisible buttons
        buttonTopLeft.setVisibility(View.VISIBLE);
        buttonTopRight.setVisibility(View.VISIBLE);
        buttonBottomLeft.setVisibility(View.VISIBLE);
        buttonBottomRight.setVisibility(View.VISIBLE);

    }

    public void switchPressed(View v){

        if (v.getId() == R.id.toggleDevModeSwitch){
            Home.setDeveloper(toggleDevModeSwitch.isChecked());
        }
    }

    public void settingsButtonClicked(View v){
        int id = v.getId();
        //Code: 2-2-3-1-4
        if (id == R.id.settingsInvisTopLeft){
            Log.d("Button1", "Button 1 was pressed");
            if (devSwitchState == 3){
                devSwitchState++;
            }else{
                devSwitchState = 0;
            }
        }else if (id == R.id.settingsInvisTopRight){
            Log.d("Button2", "Button 2 was pressed");
            if (devSwitchState == 0 || devSwitchState == 1){
                devSwitchState++;
            }else{
                devSwitchState = 0;
            }
        }else if (id == R.id.settingsInvisBottomLeft){
            Log.d("Button3", "Button 3 was pressed");
            if (devSwitchState == 2){
                devSwitchState++;
            }else{
                devSwitchState = 0;
            }
        }else if (id == R.id.settingsInvisBottomRight){
            Log.d("Button4", "Button 4 was pressed");
            if (devSwitchState == 4){
                toggleDevModeSwitch.setVisibility(View.VISIBLE);
            }else{
                devSwitchState = 0;
            }
        }
    }

}
