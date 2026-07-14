package io.mosip.digitalcard.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.*;
import java.util.Arrays;
import com.nimbusds.jose.jwk.RSAKey;
import io.mosip.digitalcard.util.DigitalCardRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import java.util.Date;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.util.MultiValueMap;


import io.mosip.digitalcard.service.PrintInjiVcService;

@Service
public class PrintInjiVcServiceImpl implements PrintInjiVcService {

    private static final Logger logger = DigitalCardRepoLogger.getLogger(PrintInjiVcServiceImpl.class);

    @Autowired
    @Qualifier("selfTokenRestTemplate")
    private RestTemplate restTemplate;

    @Value("${inji.aud.url}")
    private String audUrl;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${inji.certify.url}")
    private String certifyUrl;

    @Value("${inji.credential.configuration.id}")
    private String credentialConfigurationId;

    @Value("${inji.pre.auth.tx.code}")
    private String txCode;

    @Value("${inji.expires.in}")
    private Long expiresIn;

    @Override
    public String generatePreAuthorizedCode(String firstName,
                                            String lastName,
                                            String email,
                                            String phone) {

        try {

            Map<String, Object> claims = new HashMap<>();
            claims.put("firstName", firstName);
            claims.put("lastName", lastName);
            claims.put("email", email);
            claims.put("phone", phone);

            Map<String, Object> request = new HashMap<>();
            request.put("credential_configuration_id", credentialConfigurationId);
            request.put("claims", claims);
            request.put("expires_in", expiresIn);
            request.put("tx_code", txCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    certifyUrl + "/pre-authorized-data",
                    HttpMethod.POST,
                    entity,
                    String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            String credentialOfferUri = jsonNode.get("credential_offer_uri").asText();

            // Decode and extract Offer Id
            String decodedUri = URLDecoder.decode(credentialOfferUri, StandardCharsets.UTF_8);
            String offerId = decodedUri.substring(decodedUri.lastIndexOf("/") + 1);

            // Print Offer ID
            logger.info("Offer ID (API-1) : {}", offerId);
            // Call API-2 automatically
            return getCredentialOffer(offerId);


        } catch (Exception e) {
            throw new RuntimeException("Failed to generate pre-authorized code.", e);
        }
    }

    @Override
    public String getCredentialOffer(String offerId) {

        try {

            ResponseEntity<String> response = restTemplate.exchange(
                    certifyUrl + "/credential-offer-data/" + offerId,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            String preAuthorizedCode = jsonNode
                    .path("grants")
                    .path("urn:ietf:params:oauth:grant-type:pre-authorized_code")
                    .path("pre-authorized_code")
                    .asText();

            // Print Pre-Authorized Code
            logger.info("Pre-Authorized Code (API-2): {}", preAuthorizedCode);

            return exchangeCodeForToken(preAuthorizedCode);


        } catch (Exception e) {
            throw new RuntimeException("Failed to get Credential Offer", e);
        }
    }

    @Override
    public String exchangeCodeForToken(String preAuthorizedCode) {

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "urn:ietf:params:oauth:grant-type:pre-authorized_code");
            body.add("pre-authorized_code", preAuthorizedCode);
            body.add("tx_code", txCode);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    certifyUrl + "/oauth/token",
                    HttpMethod.POST,
                    entity,
                    String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            String accessToken = jsonNode.get("access_token").asText();
            String cNonce = jsonNode.get("c_nonce").asText();

            // Print API-3 Response
            logger.info("Access Token (API-3): {}", accessToken);
            logger.info("c_nonce (API-3): {}", cNonce);

            return issueCredential(accessToken, cNonce);

        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for token.", e);
        }
    }

    @Override
    public String generateProofJwt(String cNonce) {

        try {

            // Generate RSA Key Pair (same as Postman KEYUTIL.generateKeypair)
            RSAKey rsaKey = new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();

            // Public JWK
            RSAKey publicKey = rsaKey.toPublicJWK();

            // JWT Header
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(new JOSEObjectType("openid4vci-proof+jwt"))
                    .jwk(publicKey)
                    .build();

            // JWT Claims
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .audience(audUrl)
                    .claim("nonce", cNonce)
                    .issuer("")
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 600 * 1000))
                    .build();

            // Sign JWT
            SignedJWT signedJWT = new SignedJWT(header, claims);

            JWSSigner signer = new RSASSASigner(rsaKey);

            signedJWT.sign(signer);

            // return signedJWT.serialize();

            String proofJwt = signedJWT.serialize();

            logger.info("Proof JWT (API-4): {}", proofJwt);

            return proofJwt;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate proof JWT.", e);
        }
    }

    @Override
    public String issueCredential(String accessToken, String cNonce) {

        try {

            String proofJwt = generateProofJwt(cNonce);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            Map<String, Object> credentialDefinition = new HashMap<>();
            credentialDefinition.put(
                    "@context",
                    Arrays.asList(
                            "https://govarthananmosip.github.io/print-config/printcredential.json",
                            "https://www.w3.org/2018/credentials/v1"));

            credentialDefinition.put(
                    "type",
                    Arrays.asList(
                            "VerifiableCredential",
                            "printcredential"));

            Map<String, Object> proof = new HashMap<>();
            proof.put("proof_type", "jwt");
            proof.put("jwt", proofJwt);

            Map<String, Object> request = new HashMap<>();
            request.put("format", "ldp_vc");
            request.put("credential_definition", credentialDefinition);
            request.put("proof", proof);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    certifyUrl + "/issuance/credential",
                    HttpMethod.POST,
                    entity,
                    String.class);



            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode credentialNode = root.get("credential");

            // Convert only the credential object into JSON string
            String vc = objectMapper.writeValueAsString(credentialNode);

            logger.info("API-4 Credential Response : {}", vc);

            return vc;

        } catch (Exception e) {
            throw new RuntimeException("Failed to issue credential.", e);
        }
    }

}
