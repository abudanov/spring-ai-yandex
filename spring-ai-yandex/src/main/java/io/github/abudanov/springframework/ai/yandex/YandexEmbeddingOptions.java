package io.github.abudanov.springframework.ai.yandex;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.abudanov.springframework.ai.yandex.api.YandexApi;
import org.springframework.ai.embedding.EmbeddingOptions;

public class YandexEmbeddingOptions implements EmbeddingOptions {

	private @JsonProperty("model") String model;

	private @JsonProperty("dimensions") Integer dimensions;

	public YandexEmbeddingOptions(YandexApi.EmbeddingModel embeddingModel) {
		this(embeddingModel.getName());
	}

	public YandexEmbeddingOptions(String model) {
		this.model = model;
		this.dimensions = 256;
	}

	@Override
	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public Integer getDimensions() {
		return this.dimensions;
	}

}
