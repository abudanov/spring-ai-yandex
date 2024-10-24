package io.github.abudanov.springframework.ai.yandex;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.abudanov.springframework.ai.yandex.api.YandexApi;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.util.Assert;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YandexChatOptions implements ChatOptions {

	private @JsonProperty("model") String model;

	private @JsonProperty("maxTokens") Integer maxTokens;

	private @JsonProperty("temperature") Double temperature;

	private @JsonIgnore String folderId;

	public static YandexChatOptions.Builder builder() {
		return new Builder();
	}

	@Override
	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public Integer getMaxTokens() {
		return this.maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	@Override
	public Double getTemperature() {
		return this.temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public String getFolderId() {
		return folderId;
	}

	public void setFolderId(String folderId) {
		this.folderId = folderId;
	}

	// not supported options

	@Override
	public Double getFrequencyPenalty() {
		return null;
	}

	@Override
	public Double getPresencePenalty() {
		return null;
	}

	@Override
	public List<String> getStopSequences() {
		return null;
	}

	@Override
	public Integer getTopK() {
		return null;
	}

	@Override
	public Double getTopP() {
		return null;
	}

	@Override
	public YandexChatOptions copy() {
		YandexChatOptions copy = new YandexChatOptions();
		copy.setModel(this.getModel());
		copy.setMaxTokens(this.getMaxTokens());
		copy.setTemperature(this.getTemperature());
		return copy;
	}

	public static class Builder {

		private String model;

		private Integer maxTokens;

		private Double temperature;

		private Builder() {
		}

		public Builder withModel(String model) {
			this.model = model;
			return this;
		}

		public Builder withModel(YandexApi.ChatModel chatModel) {
			Assert.notNull(chatModel, "ChatModel must not be null");
			return this.withModel(chatModel.getName());
		}

		public Builder withMaxTokens(Integer maxTokens) {
			this.maxTokens = maxTokens;
			return this;
		}

		public Builder withTemperature(Double temperature) {
			this.temperature = temperature;
			return this;
		}

		public YandexChatOptions build() {
			var options = new YandexChatOptions();
			options.setModel(this.model);
			options.setMaxTokens(this.maxTokens);
			options.setTemperature(this.temperature);
			return options;
		}

	}

}
