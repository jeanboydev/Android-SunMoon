package com.jeanboy.app.sunmoon;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.jeanboy.component.sunmoon.SunMoonRiseSetView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SunMoonRiseSetView sunMoonRiseSetView = findViewById(R.id.sunMoonRiseSetView);
        sunMoonRiseSetView.setData(1591999200000L, 1592046000000L);
        sunMoonRiseSetView.startAnim();
    }
}