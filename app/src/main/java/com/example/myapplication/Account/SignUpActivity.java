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
import com.example.myapplication.databinding.ActivitySignUpBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = SignUpActivity.class.getName();

    //Phone number validate
    //Start with +84
    public static final String PHONE_REGEX = "((\\+|)84)(3|5|7|8|9)+([0-9]{8})\\b";

    private FirebaseAuth mAuth;

    //View Binding
    private ActivitySignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        //Firebase authentication
        mAuth = FirebaseAuth.getInstance();

        binding.tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        binding.btnSignup.setOnClickListener(v -> {
            validateForm();
        });
    }

    private void validateForm() {
        String email_phoneNumber = binding.etSignupEmailPhone.getText().toString().trim();
        String password = binding.etSignupPassword.getText().toString().trim();
        String reenter_password = binding.etSignupReenterPassword.getText().toString().trim();

        if (email_phoneNumber.isEmpty()) {
            binding.etSignupEmailPhone.setError("Please enter your email or phone number");
            binding.etSignupEmailPhone.requestFocus();
            return;
        }

        if (!email_phoneNumber.matches(PHONE_REGEX)) {
            if (!Patterns.EMAIL_ADDRESS.matcher(email_phoneNumber).matches()) {
                binding.etSignupEmailPhone.setError("Please provide valid email or phone number(start with +84)");
                binding.etSignupEmailPhone.requestFocus();
                return;
            }
        }

        if (password.isEmpty()) {
            binding.etSignupPassword.setError("Please enter your password");
            binding.etSignupPassword.requestFocus();
            return;
        }

        if (password.length() < 8) {
            binding.etSignupPassword.setError("Your password should be more than 8 characters");
            binding.etSignupPassword.requestFocus();
            return;
        }

        if (reenter_password.isEmpty()) {
            binding.etSignupReenterPassword.setError("Please enter your password again");
            binding.etSignupReenterPassword.requestFocus();
            return;
        }

        if (!password.equals(reenter_password)) {
            binding.etSignupReenterPassword.setError("Please make sure your passwords match ");
            binding.etSignupReenterPassword.requestFocus();
            return;
        }

        if (email_phoneNumber.matches(PHONE_REGEX)) {
            verifyPhoneNumber(email_phoneNumber, password);
        } else if (Patterns.EMAIL_ADDRESS.matcher(email_phoneNumber).matches()) {
            signUpWithEmailAndPassword(email_phoneNumber, password);
        }
    }

    private void signUpWithEmailAndPassword(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseDatabase.getInstance().getReference("Users")
                                    .child(encodeUserEmail(email))
                                    .setValue(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        //Verify mail
                                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                        user.sendEmailVerification();

                                        directToMainActivity(email);
                                        Toast.makeText(SignUpActivity.this, "User has been registered successfully!", Toast.LENGTH_SHORT).show();
                                    }else {
                                        Toast.makeText(SignUpActivity.this, "Failed to register inner! Try again!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }else {
                            Toast.makeText(SignUpActivity.this, "Failed to register outter! Try again!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void verifyPhoneNumber(String phoneNumber, String password) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signInWithPhoneAuthCredential(phoneAuthCredential, password);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(SignUpActivity.this, "Verification Failed " + e, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verificationId, forceResendingToken);
                        directToOtpActivity(phoneNumber, verificationId, password);
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential, String password) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.e(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            // fake email to store into realtime db
                            String fakeEmail = user.getPhoneNumber() + "@fakemail.com";
                            signUpWithEmailAndPassword(fakeEmail, password);
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(SignUpActivity.this, "The verification code entered was invalid", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void directToMainActivity(String phoneNumber) {
        startActivity(new Intent(this, MainActivity.class).putExtra("phone_number", phoneNumber));
    }

    private void directToOtpActivity(String phoneNumber, String verificationId, String password) {
        startActivity(new Intent(this, OtpActivity.class).putExtra("phone_number", phoneNumber).putExtra("verificationId", verificationId).putExtra("password", password));
    }

    static String encodeUserEmail(String userEmail) {
        return userEmail.replace(".", ",");
    }

    static String decodeUserEmail(String userEmail) {
        return userEmail.replace(",", ".");
    }
}