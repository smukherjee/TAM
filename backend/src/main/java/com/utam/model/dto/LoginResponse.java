package com.utam.model.dto;

public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private String icaoCode;
    private String company;

    public LoginResponse(String token, String username, String role, String icaoCode, String company) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.icaoCode = icaoCode;
        this.company = company;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getIcaoCode() {
        return icaoCode;
    }

    public String getCompany() {
        return company;
    }
}
