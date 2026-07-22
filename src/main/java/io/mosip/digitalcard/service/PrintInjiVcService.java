package io.mosip.digitalcard.service;
import java.util.Map;
public interface PrintInjiVcService {
    String generatePreAuthorizedCode(Map<String, Object> claims);
    String getCredentialOffer(String offerId);

    String exchangeCodeForToken(String preAuthorizedCode);

    String issueCredential(String accessToken, String cNonce);

    String generateProofJwt(String cNonce);
}
