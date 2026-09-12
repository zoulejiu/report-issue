package com.github;

import com.google.gson.annotations.SerializedName;

public class GitHubInstallationToken {

    private String token;

    @SerializedName(value = "expiresAt", alternate = {"expires_at"})
    private String expiresAt;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}
