package com.example.myapplication.Account;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.Note.MainActivity;
import com.example.myapplication.databinding.ActivityOtpBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OtpActivity extends AppCompatActivity {

    private static final String TAG = OtpActivity.class.getName();

    private FirebaseAuth mAuth;

    private ActivityOtpBinding binding;

    private String mPhoneNumber;
    private String mVerificationId;
    private String mPassword;
    private PhoneAuthProvider.ForceResendingToken mForceResendingToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        mAuth = FirebaseAuth.getInstance();

        getDataIntent();

        binding.btnVerifyOtp.setOnClickListener(v -> {
            if (binding.etOtp.getText().toString().isEmpty()) {
                binding.etOtp.setError("OTP required!");
                return;
            }

            if (mVerificationId != null) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, binding.etOtp.getText().toString());
                signInWithPhoneAuthCredential(credential, mPassword);
            }
        });

        binding.tvResendOtp.setOnClickListener(v -> {
            PhoneAuthOptions options =
                    PhoneAuthOptions.newBuilder(mAuth)
                            .setPhoneNumber(mPhoneNumber)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(this)
                            .setForceResendingToken(mForceResendingToken)
                            .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                @Override
                                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                    signInWithPhoneAuthCredential(phoneAuthCredential, mPassword);
                                }

                                @Override
                                public void onVerificationFailed(@NonNull FirebaseException e) {
                                    Toast.makeText(OtpActivity.this, "Verification Failed", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                    super.onCodeSent(verificationId, forceResendingToken);
                                    mVerificationId = verificationId;
                                    mForceResendingToken = forceResendingToken;
                                }
                            })
                            .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
        });
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
                            // Update UI
                            String fakeEmail = user.getPhoneNumber() + "@fakemail.com";
                            signUpWithEmailAndPassword(fakeEmail, password);
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(OtpActivity.this, "The verification code entered was invalid", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void signUpWithEmailAndPassword(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("email", email);

                            FirebaseFirestore.getInstance().collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .set(data)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
//                                            Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                                            // Verify mail if it is not fake mail
                                            if (!email.contains("@fakemail.com")) {
                                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                                user.sendEmailVerification();
                                            }

                                            loginWithEmailAndPassword(email, password);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error adding document", e);
                                        }
                                    });
                        }else {
                            Toast.makeText(OtpActivity.this, "Failed to register outter! Try again!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loginWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    directToMainActivity(email);
                }else {
                    Toast.makeText(OtpActivity.this, "Failed to login! Please check your credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void directToMainActivity(String phoneNumber) {
        startActivity(new Intent(this, MainActivity.class).putExtra("phone_number", phoneNumber));
    }

    private void getDataIntent() {
        mPhoneNumber = getIntent().getStringExtra("phone_number");
        mVerificationId = getIntent().getStringExtra("verificationId");
        mPassword = getIntent().getStringExtra("password");
    }

    static String encodeUserEmail(String userEmail) {
        return userEmail.replace(".", ",");
    }

    static String decodeUserEmail(String userEmail) {
        return userEmail.replace(",", ".");
    }
}