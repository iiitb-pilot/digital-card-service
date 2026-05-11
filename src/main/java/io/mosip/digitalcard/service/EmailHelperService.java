package io.mosip.digitalcard.service;

import java.util.Map;

public interface EmailHelperService {

    /**
     * Email service
     * @param fileName
     * @param additionalAttributes
     * @param pdfBytes
     */
    void sendDigitalCardInEmail( String fileName, Map<String,Object> additionalAttributes, byte[] pdfBytes, String templateLang);
}
