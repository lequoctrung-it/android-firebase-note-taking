package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

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

        binding.signupTextview.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });

        binding.loginBtn.setOnClickListener(v -> {
            validateForm();
        });

        binding.forgotPasswordTextview.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void validateForm() {
        String email_phoneNumber = binding.loginEmailPhoneEdittext.getText().toString().trim();
        String password = binding.loginPasswordEdittext.getText().toString().trim();

        if (email_phoneNumber.isEmpty()) {
            binding.loginEmailPhoneEdittext.setError("Please enter your email or phone number");
            binding.loginEmailPhoneEdittext.requestFocus();
            return;
        }

        if (!email_phoneNumber.matches(PHONE_REGEX)) {
            if (!Patterns.EMAIL_ADDRESS.matcher(email_phoneNumber).matches()) {
                binding.loginEmailPhoneEdittext.setError("Please provide valid email or phone number(start with +84)");
                binding.loginEmailPhoneEdittext.requestFocus();
                return;
            }
        }

        if (password.isEmpty()) {
            binding.loginPasswordEdittext.setError("Please enter your password");
            binding.loginPasswordEdittext.requestFocus();
            return;
        }

        if (password.length() < 8) {
            binding.loginPasswordEdittext.setError("Your password should be more than 8 characters");
            binding.loginPasswordEdittext.requestFocus();
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
                    Toast.makeText(LoginActivity.this, "Failed to login! Please check your credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void directToMainActivity(String phoneNumber) {
        startActivity(new Intent(this, MainActivity.class).putExtra("phone_number", phoneNumber));
    }
}