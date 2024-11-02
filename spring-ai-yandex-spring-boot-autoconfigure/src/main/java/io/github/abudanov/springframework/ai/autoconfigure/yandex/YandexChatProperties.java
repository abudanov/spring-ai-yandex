package io.github.abudanov.springframework.ai.autoconfigure.yandex;

import io.github.abudanov.springframework.ai.yandex.YandexChatOptions;
import io.github.abudanov.springframework.ai.yandex.api.YandexApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = YandexChatProperties.CONFIG_PREFIX)
public class YandexChatProperties extends YandexCommonProperties {

	public static final String CONFIG_PREFIX = "spring.ai.yandex.chat";

	public static final YandexApi.ChatModel DEFAULT_CHAT_MODEL = YandexApi.ChatModel.YANDEXGPT_PRO;

	public static final String DEFAULT_COMPLETION_PATH = "/v1/completion";

	private static final double DEFAULT_TEMPERATURE = 0.3d;

	private boolean enabled = true;

	@NestedConfigurationProperty
	private YandexChatOptions options = YandexChatOptions.builder()
		.withModel(DEFAULT_CHAT_MODEL)
		.withTemperature(DEFAULT_TEMPERATURE)
		.build();

	private String completionPath = DEFAULT_COMPLETION_PATH;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public YandexChatOptions getOptions() {
		return options;
	}

	public void setOptions(YandexChatOptions options) {
		this.options = options;
	}

	public String getCompletionPath() {
		return completionPath;
	}

	public void setCompletionPath(String completionPath) {
		this.completionPath = completionPath;
	}

}
