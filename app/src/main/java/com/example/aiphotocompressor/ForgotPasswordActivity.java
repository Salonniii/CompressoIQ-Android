package com.example.aiphotocompressor;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etPhone, etNewPassword;
    private Button btnResetPassword;
    private TextView tvBackToLogin;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmailForgot);
        etPhone = findViewById(R.id.etPhoneForgot);
        etNewPassword = findViewById(R.id.etNewPassword);   // ✅ CORRECT ID
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        apiService = RetrofitClient.getApiService();

        btnResetPassword.setOnClickListener(v -> resetPassword());

        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        // Email Validation
        if (TextUtils.isEmpty(email)) {
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
        if (TextUtils.isEmpty(phone)) {
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
        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("New password is required");
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            etNewPassword.requestFocus();
            return;
        }

        ForgotPasswordRequest request = new ForgotPasswordRequest(email, phone, newPassword);

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Resetting...");

        apiService.forgotPassword(request).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("🔐 Reset Password");

                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().get("message");

                    Toast.makeText(ForgotPasswordActivity.this,
                            message != null ? message : "Password updated successfully",
                            Toast.LENGTH_LONG).show();

                    startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                    finish();

                } else {
                    try {
                        String error = response.errorBody().string();
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Reset failed: " + error,
                                Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Reset failed",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("🔐 Reset Password");

                Toast.makeText(ForgotPasswordActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}