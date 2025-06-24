package io.mosip.digitalcard.service;


import org.json.JSONObject;

import java.util.Map;

public interface CardGeneratorService {

	/**
	 * The PDFCardService
	 *
	 * @param additionalAttributes
	 * @param decryptedCredentialJson
	 * @param credentialType
	 * @param password
	 * @param templateLang
	 * @return
	 */
	public byte[] generateCard(JSONObject decryptedCredentialJson, String credentialType, String password, Map<String,Object> additionalAttributes, String templateLang) throws Exception;

}