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
		logger.info("Language API bypassed temporarily, returning eng");
		return "eng";
	}
}