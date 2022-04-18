package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getDataIntent();
    }

    private void getDataIntent() {
        String phoneNumber = getIntent().getStringExtra("phone_number");
        TextView textView = findViewById(R.id.main_textview);
        textView.setText(phoneNumber);
    }
}