package com.lateinapp.noraalex.lopade.Activities.Einheiten;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lateinapp.noraalex.lopade.Activities.Home;
import com.lateinapp.noraalex.lopade.Activities.LateinAppActivity;
import com.lateinapp.noraalex.lopade.Databases.DBHelper;
import com.lateinapp.noraalex.lopade.Databases.Tables.Personalendung_PräsensDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.Vokabel;
import com.lateinapp.noraalex.lopade.R;

import java.util.Random;

/**
 * Created by Alexander on 07.03.2018.
 */

public class UserInputPersonalendung extends LateinAppActivity{

    private SharedPreferences sharedPref;
    private DBHelper dbHelper;

    private TextView request,
            solution,
            titel;
    private EditText userInput;
    private ProgressBar progressBar;
    //TODO: Remove button elevation to make it align with 'userInput'-EditText
    private Button bestaetigung,
            weiter,
            reset,
            zurück;

    private Vokabel currentVokabel;
    private String currentPersonalendung;

    private int[] weights;
    private String[] faelle = {
            Personalendung_PräsensDB.FeedEntry.COLUMN_1_SG,
            Personalendung_PräsensDB.FeedEntry.COLUMN_2_SG,
            Personalendung_PräsensDB.FeedEntry.COLUMN_3_SG,
            Personalendung_PräsensDB.FeedEntry.COLUMN_1_PL,
            Personalendung_PräsensDB.FeedEntry.COLUMN_2_PL,
            Personalendung_PräsensDB.FeedEntry.COLUMN_3_PL};

    private int lektion;
    private int backgroundColor;
    private int maxProgress = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer_user_input);

        Intent intent = getIntent();
        lektion = intent.getIntExtra("lektion",0);

        sharedPref = getSharedPreferences("SharedPreferences", 0);
        dbHelper = new DBHelper(getApplicationContext());

        backgroundColor = ResourcesCompat.getColor(getResources(), R.color.GhostWhite, null);
        request = findViewById(R.id.textUserInputLatein);
        solution = findViewById(R.id.textUserInputDeutsch);
        userInput = findViewById(R.id.textUserInputUserInput);
        progressBar = findViewById(R.id.progressBarUserInput);
        bestaetigung = findViewById(R.id.buttonUserInputEingabeBestätigt);
        weiter = findViewById(R.id.buttonUserInputNächsteVokabel);
        reset = findViewById(R.id.buttonUserInputFortschrittLöschen);
        zurück = findViewById(R.id.buttonUserInputZurück);
        titel = findViewById(R.id.textUserInputÜberschrift);

        userInput.setHint("Konjugiertes Verb");
        titel.setText("Konjugationstrainer");

        solution.setVisibility(View.GONE);
        weiter.setVisibility(View.GONE);

        weightSubjects(lektion);

        progressBar.setMax(maxProgress);

        newVocabulary();
    }

    private void newVocabulary(){

        int progress = sharedPref.getInt("UserInputPersonalendung"+lektion, 0);

        if (progress < maxProgress) {

            progressBar.setProgress(progress);

            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }catch (NullPointerException npe){
                npe.printStackTrace();
            }

            //Resetting the userInput.
            userInput.setText("");
            userInput.setBackgroundColor(backgroundColor);
            userInput.setFocusableInTouchMode(true);

            //Getting a new vocabulary.
            currentVokabel = dbHelper.getRandomVocabulary(lektion);
            currentPersonalendung = faelle[getRandomVocabularyNumber()];

            String lateinText = dbHelper.getKonjugiertesVerb(currentVokabel.getId(), "Inf");
            lateinText += "\n" + currentPersonalendung;

            //#DEVELOPER
            if (Home.isDEVELOPER() && Home.isDEV_CHEAT_MODE()){
                lateinText += "\n" + dbHelper.getKonjugiertesVerb(currentVokabel.getId(), currentPersonalendung);
            }
            request.setText(lateinText);

            bestaetigung.setVisibility(View.VISIBLE);
            weiter.setVisibility(View.GONE);
            solution.setVisibility(View.GONE);

        } else {

            progressBar.setProgress(maxProgress);

            //Hiding the keyboard.
            try {
                InputMethodManager imm = (InputMethodManager)getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(userInput.getWindowToken(), 0);
            }catch (NullPointerException npe){
                npe.printStackTrace();
            }

            allLearned();
        }


    }

    /**
     * Sets weights for all entries of 'faelle' depending on the current value of lektion
     */
    private void weightSubjects(int lektion){

        int weightErsteSg,
                weightZweiteSg,
                weightDritteSg,
                weightErstePl,
                weightZweitePl,
                weightDrittePl;

        if (lektion == 1){
            weightErsteSg = 0;
            weightZweiteSg = 0;
            weightDritteSg = 1;
            weightErstePl = 0;
            weightZweitePl = 0;
            weightDrittePl = 1;
        }else if (lektion == 2){
            weightErsteSg = 2;
            weightZweiteSg = 2;
            weightDritteSg = 1;
            weightErstePl = 2;
            weightZweitePl = 2;
            weightDrittePl = 1;
        }else {
            weightErsteSg = 1;
            weightZweiteSg = 1;
            weightDritteSg = 1;
            weightErstePl = 1;
            weightZweitePl = 1;
            weightDrittePl = 1;
        }

        weights = new int[] {weightErsteSg,
                weightZweiteSg,
                weightDritteSg,
                weightErstePl,
                weightZweitePl,
                weightDrittePl};
    }

    /**
     * @return a int corresponding to to position of a case in faelle[] with respect to the
     *          previously set weights[]-arr
     */
    public int getRandomVocabularyNumber(){

        //Getting a upper bound for the random number being retrieved afterwards
        int max =  (weights[0] +
                weights[1] +
                weights[2] +
                weights[3] +
                weights[4] +
                weights[5]);

        Random randomNumber = new Random();
        int intRandom = randomNumber.nextInt(max) + 1;
        int sum = 1;
        int sumNew;

        /*
        Each case gets a width corresponding to the 'weights'-arr.
        Goes through every case and checks if the 'randomInt' is in the area of the current case
         */
        int randomVocabulary = -1;
        for(int i = 0; i < weights.length; i++){

            sumNew = sum + weights[i];

            //checks if 'intRandom' is between the 'sum' and 'sumNew' and thus in the area of the current case
            if (intRandom >= sum && intRandom < sumNew){

                randomVocabulary = i;
                break;
            }
            else {
                sum = sumNew;
            }
        }

        if(randomVocabulary == -1){
            //Something went wrong. Log error-message
            Log.e("randomVocabulary", "Getting a randomKonjugation failed! Returned -1 for " +
                    "\nrandomNumber: " + randomNumber +
                    "\nlektion: " + lektion);
        }


        return randomVocabulary;
    }

    /**
     * Handling the button-presses
     * @param view the view of the pressed button
     */
    public void vokabeltrainerButtonClicked(View view){

        switch (view.getId()){

            //Checking if all vocabularies have been learned already and getting a new one
            case (R.id.buttonUserInputNächsteVokabel):

                newVocabulary();
                break;

            //Checking if the user input was correct
            case (R.id.buttonUserInputEingabeBestätigt):

                userInput.setFocusable(false);

                //Hiding the keyboard
                //TODO: Why do we need to use the RootView instead of sth like: this.getCurrentFocus();
                try {
                    View v = getWindow().getDecorView().getRootView();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }catch (NullPointerException npe){
                    npe.printStackTrace();
                }


                //Checking the userInput against the translation
                int color;
                if(compareString(userInput.getText().toString(), dbHelper.getKonjugiertesVerb(currentVokabel.getId(), currentPersonalendung))){
                    color = ResourcesCompat.getColor(getResources(), R.color.InputRightGreen, null);

                    SharedPreferences.Editor editor = sharedPref.edit();

                    //Increasing the counter by 1
                    editor.putInt("UserInputPersonalendung" + lektion,
                            sharedPref.getInt("UserInputPersonalendung"+lektion, 0) + 1);
                    editor.apply();
                }else {
                    color = ResourcesCompat.getColor(getResources(), R.color.InputWrongRed, null);

                    SharedPreferences.Editor editor = sharedPref.edit();
                    //Decreasing the counter by 1
                    editor.putInt("UserInputPersonalendung" + lektion,
                            sharedPref.getInt("UserInputPersonalendung"+lektion, 0) - 1);
                    editor.apply();
                }
                userInput.setBackgroundColor(color);

                //Showing the correct translation
                solution.setText(dbHelper.getKonjugiertesVerb(currentVokabel.getId(), currentPersonalendung));

                bestaetigung.setVisibility(View.GONE);
                weiter.setVisibility(View.VISIBLE);
                solution.setVisibility(View.VISIBLE);
                break;

            //Setting the 'learned' state of all vocabularies of the current lektion to false
            case (R.id.buttonUserInputFortschrittLöschen):
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("UserInputPersonalendung"+lektion, 0);
                editor.apply();
                finish();
                break;

            //Returning to the previous activity
            case (R.id.buttonUserInputZurück):
                finish();
                break;
        }
    }

    /**
     * Compares the userInput with a wanted input and returns if the comparison was successful.
     * @param userInput String to be compared with the wanted input
     * @param wantedString the original string to be compared with
     * @return Was the comparison successful?
     */
    private boolean compareString(String userInput, String wantedString){

        // Returns false for empty input
        if (userInput.equals("")){
            return false;
        }

        //Deleting all whitespaces at the start of the input
        if (userInput.length() > 1) {
            while (userInput.charAt(0) == ' ') {
                userInput = userInput.substring(1, userInput.length());
                if (userInput.length() == 1) break;
            }
        }
        //Deleting all whitespaces at the end of the input
        if (userInput.length() > 1) {
            while (userInput.charAt(userInput.length() - 1) == ' ') {
                userInput = userInput.substring(0, userInput.length() - 1);
                if (userInput.length() == 1) break;
            }
        }

        if (userInput.equalsIgnoreCase(wantedString)) return true;
        else return false;

    }

    /**
     * Executed when all vocabularies are learned.
     */
    private void allLearned(){

        request.setVisibility(View.GONE);
        solution.setVisibility(View.GONE);
        userInput.setVisibility(View.GONE);
        bestaetigung.setVisibility(View.GONE);
        weiter.setVisibility(View.GONE);
        reset.setVisibility(View.VISIBLE);
        zurück.setVisibility(View.VISIBLE);
    }

}
