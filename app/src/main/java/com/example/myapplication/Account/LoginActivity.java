package com.example.myapplication.Account;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.Note.MainActivity;
import com.example.myapplication.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    //Phone number validate
    //Start with +84
    public static final String PHONE_REGEX = "((\\+|)84)(3|5|7|8|9)+([0-9]{8})\\b";

    private FirebaseAuth mAuth;
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        mAuth = FirebaseAuth.getInstance();

        binding.tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });

        binding.btnLogin.setOnClickListener(v -> {
            validateForm();
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void validateForm() {
        String email_phoneNumber = binding.etLoginEmailPhone.getText().toString().trim();
        String password = binding.etLoginPassword.getText().toString().trim();

        if (email_phoneNumber.isEmpty()) {
            binding.etLoginEmailPhone.setError("Please enter your email or phone number");
            binding.etLoginEmailPhone.requestFocus();
            return;
        }

        if (!email_phoneNumber.matches(PHONE_REGEX)) {
            if (!Patterns.EMAIL_ADDRESS.matcher(email_phoneNumber).matches()) {
                binding.etLoginEmailPhone.setError("Please provide valid email or phone number(start with +84)");
                binding.etLoginEmailPhone.requestFocus();
                return;
            }
        }

        if (password.isEmpty()) {
            binding.etLoginEmailPhone.setError("Please enter your password");
            binding.etLoginEmailPhone.requestFocus();
            return;
        }

        if (password.length() < 8) {
            binding.etLoginEmailPhone.setError("Your password should be more than 8 characters");
            binding.etLoginEmailPhone.requestFocus();
            return;
        }

        if (email_phoneNumber.matches(PHONE_REGEX)) {
            //Convert phone to fake email
            String fakeEmail = email_phoneNumber + "@fakemail.com";
            loginWithEmailAndPassword(fakeEmail, password);
        }else if (Patterns.EMAIL_ADDRESS.matcher(email_phoneNumber).matches()) {
            loginWithEmailAndPassword(email_phoneNumber, password);
        }
    }

    private void loginWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    directToMainActivity(email);
                }else {
//                    Log.e("ERROR", String.valueOf(task.getException()));
                    Toast.makeText(LoginActivity.this, "Failed to login! Please check your credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void directToMainActivity(String phoneNumber) {
        startActivity(new Intent(this, MainActivity.class).putExtra("phone_number", phoneNumber));
    }
}