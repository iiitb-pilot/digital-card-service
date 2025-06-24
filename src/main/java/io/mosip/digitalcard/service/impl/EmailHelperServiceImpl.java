package io.mosip.digitalcard.service.impl;

import io.mosip.digitalcard.constant.DigitalCardConstants;
import io.mosip.digitalcard.dto.NotificationResponseDTO;
import io.mosip.digitalcard.service.EmailHelperService;
import io.mosip.digitalcard.util.DigitalCardRepoLogger;
import io.mosip.digitalcard.util.NotificationUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnExpression("${mosip.digitalcard.email.attachment.enable.flag:false}")
public class EmailHelperServiceImpl implements EmailHelperService {

    private static final String UIN_CARD_EMAIL_SUB = "RPR_UIN_CARD_EMAIL_SUB";
    private static final String UIN_CARD_EMAIL = "RPR_UIN_CARD_EMAIL";

    private static final String VID_CARD_EMAIL_SUB = "RPR_VID_CARD_EMAIL_SUB";
    private static final String VID_CARD_EMAIL = "RPR_VID_CARD_EMAIL";

    private Logger logger = DigitalCardRepoLogger.getLogger(EmailHelperService.class);

    @Value("${mosip.send.uin.default-emailIds}")
    private String defaultEmailIds;
    @Value("${mosip.idschema.attribute.email:email}")
    private String emailAttribute;

    @Autowired
    private NotificationUtil notificationUtil;

    @Override
    public void sendDigitalCardInEmail(String fileName, Map<String, Object> attributes, byte[] pdfBytes, String templateLang) {

        if (pdfBytes != null) {
            String residentEmailId = "";
            if (attributes.containsKey(emailAttribute)) {
                residentEmailId = (String) attributes.get(emailAttribute);
            }
            try {
                List<String> emailIds = Arrays.asList(residentEmailId, defaultEmailIds);

                List<NotificationResponseDTO> responseDTOs = notificationUtil.emailNotification(emailIds, fileName,
                        (attributes.containsKey(DigitalCardConstants.VID_CARD) ? VID_CARD_EMAIL : UIN_CARD_EMAIL),
                        (attributes.containsKey(DigitalCardConstants.VID_CARD) ? VID_CARD_EMAIL_SUB : UIN_CARD_EMAIL_SUB), attributes, pdfBytes, templateLang);
                responseDTOs.forEach(responseDTO ->
                        logger.info("UIN sent successfully via Email, server response..{}", responseDTO)
                );
            } catch (Exception e) {
                logger.error("Failed to send pdf UIN via email.{}", residentEmailId, e);
            }
        }
    }
}
