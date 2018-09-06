package com.lateinapp.noraalex.lopade.Activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;

import com.lateinapp.noraalex.lopade.R;
import com.lateinapp.noraalex.lopade.General;

import static com.lateinapp.noraalex.lopade.Global.DEVELOPER;
public class SettingsActivity extends LateinAppActivity {

    private static final String TAG = "SettingsActivity";

    private Switch toggleDevModeSwitch;

    //TODO: Make this into a enum
    //This gets set to 1->2->3->4->unlocks devSwitch
    //#DEVELOPER
    private int devSwitchState = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //#DEVELOPER
        toggleDevModeSwitch = findViewById(R.id.switchToggleDevMode);
        toggleDevModeSwitch.setChecked(DEVELOPER);
        toggleDevModeSwitch.setVisibility(View.GONE);

        /* TODO
         * Currently making the buttons transperent and without text in the .xml file.
         * Can we do this with getVisible(View.INVISIBLE).
         */
    }

    public void settingsButtonClicked(View view){

        switch (view.getId()){

            //toggling the developer mode
            //#DEVELOPER
            case (R.id.switchToggleDevMode):
                DEVELOPER = toggleDevModeSwitch.isChecked();
                SharedPreferences sharedPref = General.getSharedPrefrences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("DEVELOPER", toggleDevModeSwitch.isChecked());
                editor.apply();

                General.showMessage("Developer Mode Activated!", getApplicationContext());
                break;

            //#DEVELOPER
            case (R.id.settingsInvisTopLeft):
                if (devSwitchState == 3) devSwitchState++;
                else devSwitchState = 0;

                break;

            //#DEVELOPER
            case (R.id.settingsInvisTopRight):
                if (devSwitchState == 0 || devSwitchState == 1) devSwitchState++;
                else devSwitchState = 0;

                break;

            //#DEVELOPER
            case (R.id.settingsInvisBottomLeft):
                if (devSwitchState == 2) devSwitchState++;
                else devSwitchState = 0;

                break;

            //#DEVELOPER
            case (R.id.settingsInvisBottomRight):
                if (devSwitchState == 4) toggleDevModeSwitch.setVisibility(View.VISIBLE);
                else devSwitchState = 0;

                break;

        }
    }

}
