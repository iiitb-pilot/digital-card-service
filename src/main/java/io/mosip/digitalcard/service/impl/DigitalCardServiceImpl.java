package io.mosip.digitalcard.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.digitalcard.constant.IdType;
import io.mosip.digitalcard.controller.DigitalCardController;
import io.mosip.digitalcard.dto.*;
import io.mosip.digitalcard.entity.DigitalCardTransactionEntity;
import io.mosip.digitalcard.exception.*;
import io.mosip.digitalcard.repositories.DigitalCardTransactionRepository;
import io.mosip.digitalcard.service.*;
import io.mosip.digitalcard.util.*;
import io.mosip.digitalcard.websub.CredentialStatusEvent;
import io.mosip.digitalcard.websub.StatusEvent;
import io.mosip.digitalcard.websub.WebSubSubscriptionHelper;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.pdfgenerator.exception.PDFGeneratorException;
import io.mosip.kernel.core.qrcodegenerator.exception.QrcodeGenerationException;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.vercred.CredentialsVerifier;
import org.json.JSONObject;
import org.json.simple.JSONArray;
//import org.json.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;


/**
 * The DigitalCardServiceImpl.
 *
 * @author Dhanendra
 */
@Service
public class DigitalCardServiceImpl implements DigitalCardService {

    @Autowired
    private CardGeneratorService pdfCardServiceImpl;

    @Autowired
    private CredentialUtil credentialUtil;

    @Autowired
    Utility utility;

    @Autowired
    RestClient restClient;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private CredentialsVerifier credentialsVerifier;

    @Autowired
    private DataShareUtil dataShareUtil;

    @Autowired
    private WebSubSubscriptionHelper webSubSubscriptionHelper;

    @Autowired
    DigitalCardTransactionRepository digitalCardTransactionRepository;

    @Autowired
    private EmailHelperService emailHelperService;

    @Autowired
    private LanguageUtility languageUtility;

    @Autowired
    private ObjectMapper objectMapper;

    /** The Constant VALUE. */
    private static final String VALUE = "value";

    @Value("${mosip.digitalcard.datashare.partner.id}")
    private String dataSharePartnerId;

    @Value("${mosip.digitalcard.datashare.policy.id}")
    private String dataSharePolicyId;

    @Value("${mosip.digitalcard.verify.credentials.flag:true}")
    private boolean verifyCredentialsFlag;

    @Value("${mosip.digitalcard.credentials.request.initiate.flag:true}")
    private boolean isInitiateFlag;

    @Value("${mosip.digitalcard.pdf.password.enable.flag:true}")
    private boolean isPasswordProtected;

    @Value("${mosip.digitalcard.email.attachment.enable.flag:false}")
    private Boolean isEmailEnabled;


    @Value("${mosip.digitalcard.credential.request.partner.id}")
    private String partnerId;

    @Value("${mosip.digitalcard.credential.type}")
    private String credentialType;

    @Value("${mosip.digitalcard.websub.publish.topic:CREDENTIAL_STATUS_UPDATE}")
    private String topic;

    @Value("${mosip.digitalcard.uincard.password}")
    private String digitalCardPassword;

    @Value("${mosip.template-language}")
    private String defaultTplLangCode;

    @Value("${mosip.supported-languages}")
    private String supportedLang;


    @Value("${mosip.default.user-preferred-language-attribute:#{null}}")
    private String userPreferredLanguageAttribute;

    private Logger logger = DigitalCardRepoLogger.getLogger(DigitalCardController.class);

    @Autowired
    private PrintInjiVcService printInjiVcService;

    @Autowired
    private PixelPassService pixelPassService;

    public final class CredentialConstants {

        private CredentialConstants() {
        }

        public static final String FULL_NAME = "fullName";
        public static final String DOB = "dob";
        public static final String EMAIL = "email";
        public static final String PHONE = "phone";
        public static final String UIN = "UIN";
        public static final String VID = "VID";

        public static final String ADDRESS_LINE1 = "addressLine1";
        public static final String ADDRESS_LINE2 = "addressLine2";
        public static final String ADDRESS_LINE3 = "addressLine3";
        public static final String REGION = "region";
        public static final String CITY = "city";
        public static final String POSTAL_CODE = "postalCode";

    }


    public void generateDigitalCard(String credential, String credentialType,String dataShareUrl,String eventId,String transactionId,Map<String,Object> additionalAttributes) {
        boolean isGenerated = false;
        Map<String, Object> attributes = new LinkedHashMap<>();
        String decryptedCredential=null;
        String password=null;
        String rid=null;
        String fullName=null;
        String dob=null;
        String email=null;
        String phone=null;
        String UIN=null;
        String VID=null;
        String addressLine1=null;
        String addressLine2=null;
        String addressLine3=null;
        String region=null;
        String city=null;
        String postalCode=null;
        String address=null;
        try {
            if (dataShareUrl != null) {
                credential = restClient.getForObject(dataShareUrl, String.class);
            }
            attributes.putAll(additionalAttributes);
            decryptedCredential = encryptionUtil.decryptData(credential);
            JSONObject jsonObject = new org.json.JSONObject(decryptedCredential);
            JSONObject decryptedCredentialJson = jsonObject.getJSONObject("credentialSubject");
//            logger.info("DECRYPTED JSON RESPONSE {}", decryptedCredentialJson);
            rid=getRid(decryptedCredentialJson.get("id"));

//          fullName
            org.json.JSONArray fullNameArray =
                    decryptedCredentialJson.getJSONArray(CredentialConstants.FULL_NAME);
            org.json.JSONObject fullNameObj = fullNameArray.getJSONObject(0);
            fullName = fullNameObj.getString(VALUE);
            dob = decryptedCredentialJson.getString(CredentialConstants.DOB);
            email = decryptedCredentialJson.getString(CredentialConstants.EMAIL);
            phone = decryptedCredentialJson.getString(CredentialConstants.PHONE);
            UIN = decryptedCredentialJson.getString(CredentialConstants.UIN);
            VID = decryptedCredentialJson.getString(CredentialConstants.VID);

//          addressLine1
            org.json.JSONArray addressLine1Array =
                    decryptedCredentialJson.getJSONArray(CredentialConstants.ADDRESS_LINE1);
            org.json.JSONObject addressLine1Obj = addressLine1Array.getJSONObject(0);
            addressLine1 = addressLine1Obj.getString(VALUE);

//          addressLine2 (Optional)
            if (decryptedCredentialJson.has(CredentialConstants.ADDRESS_LINE2)
                    && !decryptedCredentialJson.isNull(CredentialConstants.ADDRESS_LINE2)) {

                org.json.JSONArray addressLine2Array =
                        decryptedCredentialJson.getJSONArray(CredentialConstants.ADDRESS_LINE2);

                if (addressLine2Array.length() > 0) {
                    addressLine2 = addressLine2Array.getJSONObject(0)
                            .optString(VALUE, null);
                }
            }

//          addressLine3 (Optional)
            if (decryptedCredentialJson.has(CredentialConstants.ADDRESS_LINE3)
                    && !decryptedCredentialJson.isNull(CredentialConstants.ADDRESS_LINE3)) {

                org.json.JSONArray addressLine3Array =
                        decryptedCredentialJson.getJSONArray(CredentialConstants.ADDRESS_LINE3);

                if (addressLine3Array.length() > 0) {
                    addressLine3 = addressLine3Array.getJSONObject(0)
                            .optString(VALUE, null);
                }
            }

//          region (Optional)
            if (decryptedCredentialJson.has(CredentialConstants.REGION)
                    && !decryptedCredentialJson.isNull(CredentialConstants.REGION)) {

                org.json.JSONArray regionArray =
                        decryptedCredentialJson.getJSONArray(CredentialConstants.REGION);

                if (regionArray.length() > 0) {
                    region = regionArray.getJSONObject(0)
                            .optString(VALUE, null);
                }
            }

//          city (Optional)
            if (decryptedCredentialJson.has(CredentialConstants.CITY)
                    && !decryptedCredentialJson.isNull(CredentialConstants.CITY)) {

                org.json.JSONArray cityArray =
                        decryptedCredentialJson.getJSONArray(CredentialConstants.CITY);

                if (cityArray.length() > 0) {
                    city = cityArray.getJSONObject(0)
                            .optString(VALUE, null);
                }
            }

            postalCode = decryptedCredentialJson.getString(CredentialConstants.POSTAL_CODE);

            // build address
            StringBuilder addressBuilder = new StringBuilder();

            if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
                addressBuilder.append(addressLine1);
            }

            if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
                if (!addressBuilder.isEmpty()) {
                    addressBuilder.append(",");
                }
                addressBuilder.append(addressLine2);
            }

            if (addressLine3 != null && !addressLine3.trim().isEmpty()) {
                if (!addressBuilder.isEmpty()) {
                    addressBuilder.append(",");
                }
                addressBuilder.append(addressLine3);
            }

            if (region != null && !region.trim().isEmpty()) {
                if (!addressBuilder.isEmpty()) {
                    addressBuilder.append(",");
                }
                addressBuilder.append(region);
            }

            if (city != null && !city.trim().isEmpty()) {
                if (!addressBuilder.isEmpty()) {
                    addressBuilder.append(",");
                }
                addressBuilder.append(city);
            }

            if (postalCode != null && !postalCode.trim().isEmpty()) {
                if (!addressBuilder.isEmpty()) {
                    addressBuilder.append(",");
                }
                addressBuilder.append(postalCode);
            }

            address = addressBuilder.toString();


//            System.out.println("IN Digital Service IMPL");
//            System.out.println("Full Name : " + fullName);
//            System.out.println("Email      : " + email);
//            System.out.println("Phone      : " + phone);
//            System.out.println("dob : " + dob);
//            System.out.println("UIN      : " + UIN);
//            System.out.println("VID      : " + VID);
//            System.out.println("address : " + address);
//            System.out.println("==================================");

            // Sending data to printInjiVcService

            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put(CredentialConstants.FULL_NAME, fullName);
            claims.put(CredentialConstants.DOB, dob);
            claims.put(CredentialConstants.EMAIL, email);
            claims.put(CredentialConstants.PHONE, phone);
            claims.put(CredentialConstants.UIN, UIN);
            claims.put(CredentialConstants.VID, VID);
            claims.put("address", address);
            String vc = printInjiVcService.generatePreAuthorizedCode(claims);


            attributes.put(IdType.RID.toString(), rid);
            //sets additional attributes for all templates.
            setTemplateAttributes(decryptedCredentialJson, attributes);
            String prefLangAttr = (String) attributes.get(userPreferredLanguageAttribute);
//            logger.info("prefLangAttr {}", prefLangAttr);

            String templateLangCode = languageUtility.getLangCodeFromNativeName(prefLangAttr);
//            logger.info("templateLangCode: {}, defaultTplLangCode: {}", templateLangCode, defaultTplLangCode);
//            logger.info("Additional Attributes: {}", attributes);
            if (!StringUtils.hasText(templateLangCode)) {
                templateLangCode = defaultTplLangCode;
            }
            if (verifyCredentialsFlag) {
                logger.info("Configured received credentials to be verified. Flag {}", verifyCredentialsFlag);
                boolean verified =credentialsVerifier.verifyCredentials(decryptedCredential);
                if (!verified) {
                    loginErrorDetails(rid,DigitalCardServiceErrorCodes.VC_VERIFICATION_FAILED.getError());
                    logger.error("Received Credentials failed in verifiable credential verify method. So, digital card is not getting generated." +
                            " Id: {}, Transaction Id: {}",eventId, transactionId);
                    throw new DigitalCardServiceException(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorCode(),DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage());
                }
            }
            if (isPasswordProtected) {
                password = getPassword(decryptedCredentialJson, templateLangCode);
            }
            byte[] pdfBytes=pdfCardServiceImpl.generateCard(decryptedCredentialJson, credentialType,password,attributes, templateLangCode,vc);
            digitalCardStatusUpdate(transactionId,pdfBytes,credentialType,rid);
            // Send digital Card Pdf to Email
            if (isEmailEnabled) {
                emailHelperService.sendDigitalCardInEmail((String) attributes.get(IdType.RID.toString()), attributes, pdfBytes, templateLangCode);
            }
            logger.info("successfully generated the digitalcard for rid: {}",rid);
        }catch (QrcodeGenerationException e) {
            loginErrorDetails(rid,DigitalCardServiceErrorCodes.QRCODE_NOT_GENERATED.getError());
            logger.error(DigitalCardServiceErrorCodes.QRCODE_NOT_GENERATED.getErrorMessage()+": {}",e);
        } catch (PDFGeneratorException e) {
            loginErrorDetails(rid,DigitalCardServiceErrorCodes.PDF_NOT_GENERATED.getError());
            logger.error(DigitalCardServiceErrorCodes.PDF_NOT_GENERATED.getErrorMessage()+": {}" ,e);
        }catch (JsonParseException | JsonMappingException e) {
            loginErrorDetails(rid,DigitalCardServiceErrorCodes.ATTRIBUTE_NOT_SET.getError());
            logger.error(DigitalCardServiceErrorCodes.ATTRIBUTE_NOT_SET.getErrorMessage()+": {}" ,e);
        } catch (Exception e){
            loginErrorDetails(rid, DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getError());
            logger.error(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage()+": {}" , e);
            throw new DigitalCardServiceException(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorCode(),DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage());
        }
    }


    @Override
    public DigitalCardStatusResponseDto getDigitalCard(String rid) {
        String pdfByteString=null;
        try {
            DigitalCardTransactionEntity digitalCardTransactionEntity=digitalCardTransactionRepository.findByRID(rid);
            if(digitalCardTransactionEntity!=null && digitalCardTransactionEntity.getDataShareUrl()!=null){
                DigitalCardStatusResponseDto digitalCardStatusResponseDto=new DigitalCardStatusResponseDto();
                digitalCardStatusResponseDto.setId(digitalCardTransactionEntity.getrid());
                digitalCardStatusResponseDto.setStatusCode(digitalCardTransactionEntity.getStatusCode());
                digitalCardStatusResponseDto.setUrl(digitalCardTransactionEntity.getDataShareUrl());
                return digitalCardStatusResponseDto;
            } else if(isInitiateFlag && digitalCardTransactionEntity==null) {
                CredentialRequestDto credentialRequestDto=new CredentialRequestDto();
                credentialRequestDto.setCredentialType(credentialType);
                credentialRequestDto.setIssuer(partnerId);
                credentialRequestDto.setId(rid);
                CredentialResponse credentialResponse = credentialUtil.reqCredential(credentialRequestDto);
                saveTransactionDetails(credentialResponse, null);
            }
            throw new DigitalCardServiceException(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_CREATED.getErrorCode(),DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_CREATED.getErrorMessage());
        } catch (DataNotFoundException | DataAccessException | DataAccessLayerException e) {
            throw new DigitalCardServiceException(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorCode(),DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage());
        }
    }

    @Override
    public void initiateCredentialRequest(String rid,String ridHash) {
        String pdfByteString = null;
        CredentialRequestDto credentialRequestDto = new CredentialRequestDto();
        credentialRequestDto.setCredentialType(credentialType);
        credentialRequestDto.setIssuer(partnerId);
        credentialRequestDto.setId(rid);
        try {
            CredentialResponse credentialResponse = credentialUtil.reqCredential(credentialRequestDto);
            saveTransactionDetails(credentialResponse, ridHash);
        } catch (DigitalCardServiceException e) {
            logger.error(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage(),e);
            throw new DigitalCardServiceException(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_CREATED.getErrorCode(),DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_CREATED.getErrorMessage());
        }
    }


    private void saveTransactionDetails(CredentialResponse credentialResponse, String idHash){
        DigitalCardTransactionEntity digitalCardEntity=new DigitalCardTransactionEntity();
        digitalCardEntity.setrid(credentialResponse.getId());
        digitalCardEntity.setUinSaltedHash(idHash);
        digitalCardEntity.setCredentialId(credentialResponse.getRequestId());
        digitalCardEntity.setCreateDateTime(LocalDateTime.now());
        digitalCardEntity.setCreatedBy(Utility.getUser());
        digitalCardEntity.setStatusCode("NEW");
        digitalCardTransactionRepository.save(digitalCardEntity);

    }
    private void digitalCardStatusUpdate(String requestId, byte[] data, String credentialType, String rid)
            throws DataShareException, ApiNotAccessibleException, IOException, Exception {
        DataShareDto dataShareDto = null;
        dataShareDto = dataShareUtil.getDataShare(data, dataSharePolicyId, dataSharePartnerId);
        CredentialStatusEvent creEvent = new CredentialStatusEvent();
        LocalDateTime currentDtime = DateUtils2.getUTCCurrentDateTime();
        DigitalCardTransactionEntity digitalCardTransactionEntity=digitalCardTransactionRepository.findByRID(rid);
        if(digitalCardTransactionEntity==null){
            DigitalCardTransactionEntity digitalCardEntity=new DigitalCardTransactionEntity();
            digitalCardEntity.setrid(rid);
            digitalCardEntity.setCreateDateTime(LocalDateTime.now());
            digitalCardEntity.setCreatedBy(Utility.getUser());
            digitalCardEntity.setDataShareUrl(dataShareDto.getUrl());
            digitalCardEntity.setStatusCode("AVAILABLE");
            digitalCardTransactionRepository.save(digitalCardEntity);
        }else{
            digitalCardTransactionRepository.updateTransactionDetails(rid,"AVAILABLE", dataShareDto.getUrl(),LocalDateTime.now(),Utility.getUser());
        }
        StatusEvent sEvent = new StatusEvent();
        sEvent.setId(UUID.randomUUID().toString());
        sEvent.setRequestId(requestId);
        sEvent.setStatus("STORED");
        sEvent.setUrl(dataShareDto.getUrl());
        sEvent.setTimestamp(Timestamp.valueOf(currentDtime).toString());
        creEvent.setPublishedOn(LocalDateTime.now().toString());
        creEvent.setPublisher("DIGITAL_CARD_SERVICE");
        creEvent.setTopic(topic);
        creEvent.setEvent(sEvent);
        webSubSubscriptionHelper.digitalCardStatusUpdateEvent(topic, creEvent);
        logger.info("publish event for topic : {} and rid : {}",topic,rid);
    }
    private String getRid(Object id) {
        String rid= id.toString().split("/credentials/")[1];
        return rid;
    }
    /**
     * Gets the password.
     *
     * @param jsonObject
     * @return
     * @throws Exception
     */
    private String getPassword(JSONObject jsonObject, String tplLangCode) throws Exception {
        String[] attributes = digitalCardPassword.split("\\|");
        List<String> list = new ArrayList<>(Arrays.asList(attributes));

        Iterator<String> it = list.iterator();
        String uinCardPd = "";
        Object obj=null;
        while (it.hasNext()) {
            String key = it.next().trim();

            Object object = jsonObject.get(key);
            if (object != null) {
                try {
                    obj = new JSONParser().parse(object.toString());
                } catch (Exception e) {
                    obj = object;
                }
            }
            if (obj instanceof JSONArray) {
                // JSONArray node = JsonUtil.getJSONArray(demographicIdentity, value);
                SimpleType[] jsonValues = Utility.mapJsonNodeToJavaObject(SimpleType.class, (JSONArray) obj);
                uinCardPd = uinCardPd.concat(getFormattedPasswordAttribute(getParameter(jsonValues, tplLangCode)).substring(0,4));
            } else if (object instanceof org.json.simple.JSONObject) {
                org.json.simple.JSONObject json = (org.json.simple.JSONObject) object;
                uinCardPd = uinCardPd.concat((String) json.get(VALUE));
            } else {
                uinCardPd = uinCardPd.concat(getFormattedPasswordAttribute((String) Objects.requireNonNull(object).toString()).substring(0,4));
            }
        }
        return uinCardPd.toUpperCase();
    }

    private String getFormattedPasswordAttribute(String password){
        if(password.length()==3){
            return password=password.concat(password.substring(0,1));
        }else if(password.length()==2){
            return password=password.repeat(2);
        }else if(password.length()==1) {
            return password=password.repeat(4);
        }else {
            return password;
        }
    }

    /**
     * Gets the parameter.
     *
     * @param jsonValues
     *            the json values
     * @param langCode
     *            the lang code
     * @return the parameter
     */
    private String getParameter(SimpleType[] jsonValues, String langCode) {

        String parameter = null;
        if (jsonValues != null) {
            for (int count = 0; count < jsonValues.length; count++) {
                String lang = jsonValues[count].getLanguage();
                if (langCode.contains(lang)) {
                    parameter = jsonValues[count].getValue();
                    break;
                }
            }
        }
        return parameter;
    }
    public void loginErrorDetails(String rid, String errorMsg){
        digitalCardTransactionRepository.updateErrorTransactionDetails(rid,"ERROR",errorMsg,LocalDateTime.now(),Utility.getUser());
    }
    /**
     * Gets the artifacts.
     *
     * @param attribute    the attribute
     * @return the artifacts
     * @throws IOException    Signals that an I/O exception has occurred.
     * @throws ParseException
     */
    @SuppressWarnings("unchecked")
    private void setTemplateAttributes(org.json.JSONObject demographicIdentity, Map<String, Object> attribute)
            throws Exception {
        try {
            if (demographicIdentity == null)
                throw new IdentityNotFoundException(DigitalCardServiceErrorCodes.IDENTITY_NOT_FOUND.getErrorCode(),DigitalCardServiceErrorCodes.IDENTITY_NOT_FOUND.getErrorMessage());

            String mapperJsonString = utility.getIdentityMappingJson(utility.getConfigServerFileStorageURL(),
                    utility.getIdentityJson());
            org.json.simple.JSONObject mapperJson = objectMapper.readValue(mapperJsonString, org.json.simple.JSONObject.class);
            org.json.simple.JSONObject mapperIdentity = utility.getJSONObject(mapperJson,
                    utility.getDemographicIdentity());

            List<String> mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());
            for (String key : mapperJsonKeys) {
                LinkedHashMap<String, String> jsonObject = utility.getJSONValue(mapperIdentity, key);
                Object obj = null;
                String values = jsonObject.get(VALUE);
                for (String value : values.split(",")) {
                    // Object object = demographicIdentity.get(value);
                    Object object = demographicIdentity.has(value)?demographicIdentity.get(value):null;
                    if (object != null) {
                        try {
                            obj = new JSONParser().parse(object.toString());
                        } catch (Exception e) {
                            obj = object;
                        }

                        if (obj instanceof JSONArray && !key.equalsIgnoreCase("bestTwoFingers")) {
                            // JSONArray node = JsonUtil.getJSONArray(demographicIdentity, value);
                            SimpleType[] jsonValues = Utility.mapJsonNodeToJavaObject(SimpleType.class, (JSONArray) obj);
                            for (SimpleType jsonValue : jsonValues) {
                                if (supportedLang.contains(jsonValue.getLanguage()))
                                    attribute.put(value + "_" + jsonValue.getLanguage(), jsonValue.getValue());
                            }
                        } else if (object instanceof org.json.simple.JSONObject) {
                            org.json.simple.JSONObject json = (org.json.simple.JSONObject) object;
                            attribute.put(value, (String) json.get(VALUE));
                        } else {
                            attribute.put(value, String.valueOf(object));
                        }
                    }

                }
            }
        } catch (JsonParseException | JsonMappingException | DigitalCardServiceException e) {
            logger.error("Error while parsing Json file" ,e);
        }

    }

}
