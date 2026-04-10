package com.commerce.bff.security.oauth2;

public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();
    String getName();
}
