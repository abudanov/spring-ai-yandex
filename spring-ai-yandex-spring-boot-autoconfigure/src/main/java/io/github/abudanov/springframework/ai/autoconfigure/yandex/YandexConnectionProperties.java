package io.github.abudanov.springframework.ai.autoconfigure.yandex;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = YandexConnectionProperties.CONFIG_PREFIX)
public class YandexConnectionProperties extends YandexCommonProperties {

	public static final String CONFIG_PREFIX = "spring.ai.yandex";

	public static final String DEFAULT_BASE_URL = "https://llm.api.cloud.yandex.net/foundationModels";

	public YandexConnectionProperties() {
		super.setBaseUrl(DEFAULT_BASE_URL);
	}

}
