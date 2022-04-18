package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

public class ResetPasswordOtpActivity extends AppCompatActivity {

    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password_otp);

        phoneNumber = getIntent().getStringExtra("phoneNumber");
        Toast.makeText(this, phoneNumber, Toast.LENGTH_SHORT).show();
    }

}