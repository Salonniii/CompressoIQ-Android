package com.example.aiphotocompressor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPhone, etFPassword;
    private Button btnSignupSubmit;
    private TextView tvGoToLogin;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnSignupSubmit = findViewById(R.id.btnSignupSubmit);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        apiService = RetrofitClient.getApiService();

        btnSignupSubmit.setOnClickListener(view -> signupUser());

        tvGoToLogin.setOnClickListener(view -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void signupUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Full Name Validation
        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (!fullName.matches("^[a-zA-Z ]+$")) {
            etFullName.setError("Name should contain only letters");
            etFullName.requestFocus();
            return;
        }

        // Email Validation
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Phone Validation
        if (phone.isEmpty()) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return;
        }

        if (!phone.matches("^[0-9]{10}$")) {
            etPhone.setError("Enter a valid 10-digit phone number");
            etPhone.requestFocus();
            return;
        }

        // Password Validation
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        SignupRequest request = new SignupRequest(fullName, email, phone, password);

        apiService.signup(request).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().get("message");

                    Toast.makeText(SignupActivity.this,
                            message != null ? message : "Signup successful 🎉",
                            Toast.LENGTH_LONG).show();

                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish();

                } else {
                    try {
                        String error = response.errorBody().string();
                        Toast.makeText(SignupActivity.this,
                                "Signup failed ❌\n" + error,
                                Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(SignupActivity.this,
                                "Signup failed ❌",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(SignupActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}