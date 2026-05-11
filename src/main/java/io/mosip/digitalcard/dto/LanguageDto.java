package io.mosip.digitalcard.dto;

import lombok.Data;

@Data
public class LanguageDto {

	private String code;

	private String name;

	private String family;

	private String nativeName;

	private Boolean isActive;

}
