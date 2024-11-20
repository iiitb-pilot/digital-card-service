package io.mosip.digitalcard.service.impl;

import io.mosip.digitalcard.dto.NotificationResponseDTO;
import io.mosip.digitalcard.service.EmailHelperService;
import io.mosip.digitalcard.util.DigitalCardRepoLogger;
import io.mosip.digitalcard.util.NotificationUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class EmailHelperServiceImpl implements EmailHelperService {

    private static final String UIN_CARD_EMAIL_SUB = "RPR_UIN_CARD_EMAIL_SUB";
    private static final String UIN_CARD_EMAIL = "RPR_UIN_CARD_EMAIL";

    private Logger logger = DigitalCardRepoLogger.getLogger(EmailHelperService.class);

    @Value("${mosip.send.uin.default-emailIds}")
    private String defaultEmailIds;
    @Value("${mosip.idschema.attribute.email:email}")
    private String emailAttribute;

    @Autowired
    private NotificationUtil notificationUtil;

    @Override
    public void sendDigitalCardInEmail(JSONObject decryptedCredentialJson, String rid, Map<String, Object> attributes, byte[] pdfBytes) {

        if (pdfBytes != null) {
            String residentEmailId = "";
            if (decryptedCredentialJson.has(emailAttribute)) {
                try {
                    residentEmailId = decryptedCredentialJson.getString(emailAttribute);
                } catch (JSONException e) {
                    logger.error("Resident email fetch failed", residentEmailId, e);
                }
            }
            try {
                List<String> emailIds = Arrays.asList(residentEmailId, defaultEmailIds);
                List<NotificationResponseDTO> responseDTOs = notificationUtil.emailNotification(emailIds, rid,
                        UIN_CARD_EMAIL, UIN_CARD_EMAIL_SUB, attributes, pdfBytes);
                responseDTOs.forEach(responseDTO ->
                        logger.info("UIN sent successfully via Email, server response..{}", responseDTO)
                );
            } catch (Exception e) {
                logger.error("Failed to send pdf UIN via email.{}", residentEmailId, e);
            }
        }
    }
}
