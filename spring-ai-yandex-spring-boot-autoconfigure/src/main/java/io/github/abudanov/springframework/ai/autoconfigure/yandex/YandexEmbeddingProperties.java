package io.github.abudanov.springframework.ai.autoconfigure.yandex;

import io.github.abudanov.springframework.ai.yandex.YandexEmbeddingOptions;
import io.github.abudanov.springframework.ai.yandex.api.YandexApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = YandexEmbeddingProperties.CONFIG_PREFIX)
public class YandexEmbeddingProperties extends YandexCommonProperties {

	public static final String CONFIG_PREFIX = "spring.ai.yandex.embedding";

	public static final YandexApi.EmbeddingModel DEFAULT_EMBEDDINGS_MODEL = YandexApi.EmbeddingModel.TEXT_SEARCH_DOC;

	public static final String DEFAULT_EMBEDDING_PATH = "/v1/textEmbedding";

	private String embeddingPath = DEFAULT_EMBEDDING_PATH;

	@NestedConfigurationProperty
	private YandexEmbeddingOptions options = new YandexEmbeddingOptions(DEFAULT_EMBEDDINGS_MODEL);

	public String getEmbeddingPath() {
		return embeddingPath;
	}

	public void setEmbeddingPath(String embeddingPath) {
		this.embeddingPath = embeddingPath;
	}

	public YandexEmbeddingOptions getOptions() {
		return options;
	}

	public void setOptions(YandexEmbeddingOptions options) {
		this.options = options;
	}

}
