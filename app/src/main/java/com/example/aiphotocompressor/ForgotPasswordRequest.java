package com.example.aiphotocompressor;

public class ForgotPasswordRequest {

    private String email;
    private String phoneNumber;
    private String newPassword;

    public ForgotPasswordRequest(String email, String phoneNumber, String newPassword) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.newPassword = newPassword;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getNewPassword() {
        return newPassword;
    }
}