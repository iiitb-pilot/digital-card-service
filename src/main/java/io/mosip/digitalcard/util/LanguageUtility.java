package io.mosip.digitalcard.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.dto.LanguageDto;
import io.mosip.digitalcard.dto.LanguageResponseDto;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LanguageUtility {
	@Autowired
	private RestClient restClient;
	
	/** The logger. */
	private static Logger logger = DigitalCardRepoLogger.getLogger(LanguageUtility.class);

	@Autowired
	ObjectMapper mapper;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String getLangCodeFromNativeName(String nativeName) {
		String langCode=null;
		try {
			ResponseWrapper<LanguageResponseDto> response = (ResponseWrapper) restClient.getApi(ApiName.LANGUAGE,null, "", "", ResponseWrapper.class);

			if (response.getErrors() != null && response.getErrors().size() > 0) {
				response.getErrors().stream().forEach(r -> {
					logger.error("LanguageUtility::getLangCodeFromNativeName():: error with error message " + r.getMessage());
				});
			}

			LanguageResponseDto languageResponseDto = mapper.readValue(mapper.writeValueAsString(response.getResponse()), LanguageResponseDto.class);

			logger.debug("LanguageUtility::getLangCodeFromNativeName()::exit");
			for(LanguageDto dto:languageResponseDto.getLanguages()) {
				if(dto.getNativeName().equalsIgnoreCase(nativeName) || dto.getCode().equalsIgnoreCase(nativeName)
						|| dto.getName().equalsIgnoreCase(nativeName)) {
					langCode= dto.getCode();
					break;
				}
			}
		} catch (Exception e) {
			logger.error("LanguageUtility::getLangCodeFromNativeName():: error with error message " + e.getMessage());
			throw new DigitalCardServiceException("Failed to retrieve language details.", e);
		}
		return langCode;
	}
}
