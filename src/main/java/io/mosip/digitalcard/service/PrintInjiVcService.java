package io.mosip.digitalcard.service;

public interface PrintInjiVcService {
    String generatePreAuthorizedCode(
            String firstName,
            String lastName,
            String email,
            String phone);

    String getCredentialOffer(String offerId);

    String exchangeCodeForToken(String preAuthorizedCode);

    String issueCredential(String accessToken, String cNonce);

    String generateProofJwt(String cNonce);
}
