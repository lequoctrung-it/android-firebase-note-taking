package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.databinding.ActivityForgotPasswordBinding;
import com.example.myapplication.databinding.ActivityLoginBinding;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String TAG = ForgotPasswordActivity.class.getName();

    //Phone number validate
    //Start with +84
    public static final String PHONE_REGEX = "((\\+|)84)(3|5|7|8|9)+([0-9]{8})\\b";

    private ActivityForgotPasswordBinding binding;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        mAuth = FirebaseAuth.getInstance();

        binding.resetpasswordBtn.setOnClickListener(v -> {
            resetPassword();
        });
    }

    private void resetPassword() {
        String email_phoneNumber = binding.resetpasswordEdittext.getText().toString().trim();

        if (email_phoneNumber.isEmpty()) {
            binding.resetpasswordEdittext.setError("Please enter your email or phone number");
            binding.resetpasswordEdittext.requestFocus();
            return;
        }

        if (!email_phoneNumber.matches(PHONE_REGEX)) {
            if (!Patterns.EMAIL_ADDRESS.matcher(email_phoneNumber).matches()) {
                binding.resetpasswordEdittext.setError("Please provide valid email or phone number(start with +84)");
                binding.resetpasswordEdittext.requestFocus();
                return;
            }
        }

        if (email_phoneNumber.matches(PHONE_REGEX)) {
            forgotPasswordUsingPhoneNumber(email_phoneNumber);
        }else if (Patterns.EMAIL_ADDRESS.matcher(email_phoneNumber).matches()) {
            forgotPasswordUsingEmail(email_phoneNumber);
        }
    }

    private void forgotPasswordUsingPhoneNumber(String phoneNumber) {
        phoneNumber = phoneNumber + "@fakemail.com";
        DatabaseReference reference =  FirebaseDatabase.getInstance().getReference("Users");
        reference.child(encodeUserEmail(phoneNumber)).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        //User exist
                        verifyPhoneNumber(binding.resetpasswordEdittext.getText().toString().trim());
                    }else {
                        binding.resetpasswordEdittext.setError("No such user exist!");
                        binding.resetpasswordEdittext.requestFocus();
                    }
                }else {
                    Toast.makeText(ForgotPasswordActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void forgotPasswordUsingEmail(String email) {
        DatabaseReference reference =  FirebaseDatabase.getInstance().getReference("Users");
        reference.child(encodeUserEmail(email)).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        //User exist
                        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ForgotPasswordActivity.this, "Check your email to reset password!", Toast.LENGTH_SHORT).show();
                                }else {
                                    Toast.makeText(ForgotPasswordActivity.this, "Try again! Something went wrong!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }else {
                        binding.resetpasswordEdittext.setError("No such user exist!");
                        binding.resetpasswordEdittext.requestFocus();
                    }
                }else {
                    Toast.makeText(ForgotPasswordActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void verifyPhoneNumber(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                //problem here
                                signInWithPhoneAuthCredential(phoneAuthCredential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Toast.makeText(ForgotPasswordActivity.this, "Verification Failed " + e, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                super.onCodeSent(verificationId, forceResendingToken);
                                directToOtpActivity(phoneNumber, verificationId);
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    //this method does not need to be here
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.e(TAG, "signInWithCredential:success");
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(ForgotPasswordActivity.this, "The verification code entered was invalid", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void directToOtpActivity(String phoneNumber, String verificationId) {
        startActivity(new Intent(this, ResetPasswordOtpActivity.class).putExtra("phone_number", phoneNumber).putExtra("verificationId", verificationId));
    }

    static String encodeUserEmail(String userEmail) {
        return userEmail.replace(".", ",");
    }

    static String decodeUserEmail(String userEmail) {
        return userEmail.replace(",", ".");
    }
}