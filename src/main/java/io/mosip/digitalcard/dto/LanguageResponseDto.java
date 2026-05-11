package io.mosip.digitalcard.dto;

import lombok.Data;

import java.util.List;

@Data
public class LanguageResponseDto {

	/**
	 * List of Languages.
	 */
	private List<LanguageDto> languages;

}