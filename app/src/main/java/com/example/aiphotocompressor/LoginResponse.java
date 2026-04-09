package com.example.aiphotocompressor;

public class LoginResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String message;

    public LoginResponse() {
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
    }
}