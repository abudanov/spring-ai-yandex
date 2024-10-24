package io.github.abudanov.springframework.ai.yandex;

import io.github.abudanov.springframework.ai.yandex.api.YandexApi;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.util.Assert;

public class YandexChatUsage implements Usage {

	private final YandexApi.Usage usage;

	protected YandexChatUsage(YandexApi.Usage usage) {
		Assert.notNull(usage, "Yandex Usage must not be null");
		this.usage = usage;
	}

	public static YandexChatUsage from(YandexApi.Usage usage) {
		return new YandexChatUsage(usage);
	}

	protected YandexApi.Usage getUsage() {
		return this.usage;
	}

	@Override
	public Long getPromptTokens() {
		Long t = getUsage().inputTextTokens();
		return (t == null) ? 0 : t;
	}

	@Override
	public Long getGenerationTokens() {
		Long t = getUsage().completionTokens();
		return (t == null) ? 0 : t;
	}

}
