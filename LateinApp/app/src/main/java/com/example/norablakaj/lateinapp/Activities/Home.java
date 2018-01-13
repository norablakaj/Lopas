package com.example.norablakaj.lateinapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.example.norablakaj.lateinapp.Databases.AndroidDatabaseManager;
import com.example.norablakaj.lateinapp.R;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Button lektion1Button, lektion2Button, lektion3Button, lektion4Button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Defining the button to be able to edit it
        lektion1Button = (Button)findViewById(R.id.lektion1);
        lektion2Button = (Button)findViewById(R.id.lektion2);
        lektion3Button = (Button)findViewById(R.id.lektion3);
        lektion4Button = (Button)findViewById(R.id.lektion4);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {

        } else if (id == R.id.nav_wörterbuch) {

        } else if (id == R.id.nav_hilfe) {

        } else if (id == R.id.nav_impressum) {

        } else if (id == R.id.nav_dbhelper) {
            Intent dbManager = new Intent(this, AndroidDatabaseManager.class);
            startActivity(dbManager);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void lektion1Clicked(View view){

        Intent startLektion1 = new Intent(view.getContext(), Lektion1.class);
        startActivity(startLektion1);
    }

    public void lektion2Clicked(View view){

        Intent startLektion2 = new Intent(view.getContext(), Lektion2.class);
        startActivity(startLektion2);
    }

    public void lektion3Clicked(View view){

        Intent startLektion3 = new Intent(view.getContext(), Lektion3.class);
        startActivity(startLektion3);
    }

    public void lektion4Clicked(View view){

        Intent startLektion4 = new Intent(view.getContext(), Lektion4.class);
        startActivity(startLektion4);
    }
}
