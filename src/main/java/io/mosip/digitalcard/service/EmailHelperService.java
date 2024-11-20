package io.mosip.digitalcard.service;

import org.json.JSONObject;

import java.util.Map;

public interface EmailHelperService {

    /**
     * Email service
     * @param decryptedCredentialJson
     * @param rid
     * @param additionalAttributes
     * @param pdfBytes
     */
    void sendDigitalCardInEmail(JSONObject decryptedCredentialJson, String rid, Map<String,Object> additionalAttributes, byte[] pdfBytes);
}
