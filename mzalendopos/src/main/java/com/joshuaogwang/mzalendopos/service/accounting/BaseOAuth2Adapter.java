package com.joshuaogwang.mzalendopos.service.accounting;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.joshuaogwang.mzalendopos.entity.AccountingCredentials;

/**
 * Shared OAuth2 token-exchange and refresh logic reused by all adapters.
 */
public abstract class BaseOAuth2Adapter implements AccountingProviderAdapter {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final RestTemplate restTemplate;

    protected BaseOAuth2Adapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // -------------------------------------------------------------------------
    // Token exchange helpers
    // -------------------------------------------------------------------------

    /**
     * POST to the provider's token URL with authorization_code grant.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> exchangeCode(String tokenUrl,
            String clientId, String clientSecret,
            String code, String redirectUri) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        return restTemplate.postForObject(
                tokenUrl, new HttpEntity<>(body, headers), Map.class);
    }

    /**
     * POST to the provider's token URL with refresh_token grant.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> refreshToken(String tokenUrl,
            String clientId, String clientSecret, String refreshToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        return restTemplate.postForObject(
                tokenUrl, new HttpEntity<>(body, headers), Map.class);
    }

    /**
     * Apply token response map fields to credentials and save expiry.
     */
    protected void applyTokenResponse(Map<String, Object> tokenResponse,
            AccountingCredentials credentials) {
        if (tokenResponse == null) {
            throw new IllegalStateException("Empty token response from provider");
        }
        credentials.setAccessToken((String) tokenResponse.get("access_token"));
        if (tokenResponse.containsKey("refresh_token")) {
            credentials.setRefreshToken((String) tokenResponse.get("refresh_token"));
        }
        Object expiresIn = tokenResponse.get("expires_in");
        if (expiresIn != null) {
            long seconds = Long.parseLong(expiresIn.toString());
            credentials.setTokenExpiresAt(LocalDateTime.now().plusSeconds(seconds));
        }
        credentials.setLastRefreshedAt(LocalDateTime.now());
    }

    /**
     * Build a Bearer-auth JSON header for authenticated API calls.
     */
    protected HttpHeaders bearerHeaders(AccountingCredentials credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(credentials.getAccessToken());
        return headers;
    }
}
