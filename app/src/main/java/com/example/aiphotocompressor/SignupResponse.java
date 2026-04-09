package com.example.aiphotocompressor;

public class SignupResponse {

    private Long id;
    private String fullName;
    private String email;
    private String message;
    private String error;

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public boolean isSuccess() {
        return error == null;
    }

    public Long getUserId() {
        return id;
    }
}