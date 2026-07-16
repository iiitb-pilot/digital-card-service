package io.mosip.digitalcard.service;


import org.json.JSONObject;

import java.util.Map;

public interface CardGeneratorService {

	/**
	 * The PDFCardService
	 *
	 * @param decryptedCredentialJson
	 * @param credentialType
	 * @param password
	 * @param templateLang
	 * @param vc
	 * @return
	 */
	public byte[] generateCard(JSONObject decryptedCredentialJson, String credentialType, String password, Map<String,Object> additionalAttributes, String templateLang,String vc) throws Exception;

}