package com.example.aiphotocompressor;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "AIPhotoCompressorSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_LOGGED_IN = "is_logged_in";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveLoginSession(Long userId, String userName, String userEmail) {
        editor.putLong(KEY_USER_ID, userId != null ? userId : -1);
        editor.putString(KEY_USER_NAME, userName != null ? userName : "");
        editor.putString(KEY_USER_EMAIL, userEmail != null ? userEmail : "");
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.apply();
    }

    public Long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, -1);
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, "");
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false)
                && getUserId() != -1;
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    public void clearSession() {
        logout();
    }
}