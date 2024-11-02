package io.github.abudanov.springframework.ai.yandex.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.model.ChatModelDescription;
import org.springframework.ai.model.EmbeddingModelDescription;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.List;

public class YandexApi {

	private final RestClient restClient;

	private final String completionPath;

	private final String embeddingPath;

	public YandexApi(String baseUrl, String apiKey, MultiValueMap<String, String> headers,
			RestClient.Builder restClientBuilder, String completionPath, String embeddingPath,
			ResponseErrorHandler responseErrorHandler) {
		Assert.hasText(completionPath, "Completion path must not be empty");
		Assert.hasText(embeddingPath, "Embedding path must not be empty");
		Assert.notNull(headers, "Headers must not be null");
		this.completionPath = completionPath;
		this.embeddingPath = embeddingPath;
		this.restClient = restClientBuilder.baseUrl(baseUrl).defaultHeaders(h -> {
			h.set(HttpHeaders.AUTHORIZATION, "Api-Key " + apiKey);
			h.setContentType(MediaType.APPLICATION_JSON);
			h.addAll(headers);
		}).defaultStatusHandler(responseErrorHandler).build();
	}

	interface YandexModelDescription {

		String getName();

		String getModelScheme();

		default String getVersion() {
			return "latest";
		}

		default String getModelUri(String folderId) {
			return getModelScheme() + "://" + folderId + '/' + getName() + '/' + getVersion();
		}

	}

	public enum ChatModel implements ChatModelDescription, YandexModelDescription {

		YANDEXGPT_PRO("yandexgpt", ModelVersion.LATEST, "3"),
		YANDEXGPT_LITE("yandexgpt-lite", ModelVersion.LATEST, "3"),
		YANDEXGPT_PRO_RC("yandexgpt", ModelVersion.RELEASE_CANDIDATE, "4"),
		YANDEXGPT_LITE_RC("yandexgpt-lite", ModelVersion.RELEASE_CANDIDATE, "4"),
		YANDEXGPT_32K_RC("yandexgpt-32k", ModelVersion.RELEASE_CANDIDATE, "4");

		private final String value;

		private final ModelVersion version;

		private final String generation;

		ChatModel(String value, ModelVersion version, String generation) {
			this.value = value;
			this.version = version;
			this.generation = generation;
		}

		@Override
		public String getName() {
			return this.value;
		}

		@Override
		public String getModelScheme() {
			return "gpt";
		}

		@Override
		public String getVersion() {
			return this.version.getName();
		}

		public String getGeneration() {
			return generation;
		}

		public static ChatModel ofValue(String value) {
			return switch (value) {
				case "yandexgpt" -> YANDEXGPT_PRO;
				case "yandexgpt-lite" -> YANDEXGPT_LITE;
				case "yandexgpt/rc" -> YANDEXGPT_PRO_RC;
				case "yandexgpt-lite/rc" -> YANDEXGPT_LITE_RC;
				case "yandexgpt-32k", "yandexgpt-32k/rc" -> YANDEXGPT_32K_RC;
				default -> throw new IllegalArgumentException("Unknown chat model: " + value);
			};
		}

	}

	public enum ModelVersion {

		DEPRECATED("deprecated"), LATEST("latest"), RELEASE_CANDIDATE("rc");

		private final String value;

		ModelVersion(String value) {
			this.value = value;
		}

		public String getName() {
			return this.value;
		}

	}

	public enum CompletionStatus {

		/**
		 * Unspecified generation status.
		 */
		@JsonProperty("ALTERNATIVE_STATUS_UNSPECIFIED")
		ALTERNATIVE_STATUS_UNSPECIFIED,

		/**
		 * Partially generated alternative.
		 */
		@JsonProperty("ALTERNATIVE_STATUS_PARTIAL")
		ALTERNATIVE_STATUS_PARTIAL,

		/**
		 * Incomplete final alternative resulting from reaching the maximum allowed number
		 * of tokens.
		 */
		@JsonProperty("ALTERNATIVE_STATUS_TRUNCATED_FINAL")
		ALTERNATIVE_STATUS_TRUNCATED_FINAL,

		/**
		 * Final alternative generated without running into any limits.
		 */
		@JsonProperty("ALTERNATIVE_STATUS_FINAL")
		ALTERNATIVE_STATUS_FINAL,

		/**
		 * Generation was stopped due to the discovery of potentially sensitive content in
		 * the prompt or generated response. To fix, modify the prompt and restart
		 * generation.
		 */
		@JsonProperty("ALTERNATIVE_STATUS_CONTENT_FILTER")
		ALTERNATIVE_STATUS_CONTENT_FILTER

	}

	public enum Role {

		/**
		 * System message.
		 */
		@JsonProperty("system")
		SYSTEM,

		/**
		 * User message.
		 */
		@JsonProperty("user")
		USER,

		/**
		 * Assistant message.
		 */
		@JsonProperty("assistant")
		ASSISTANT

	}

	/**
	 * @param modelUri The
	 * <a href="https://yandex.cloud/docs/foundation-models/concepts/yandexgpt/models">ID
	 * of the model</a> to be used for completion generation.
	 * @param completionOptions Configuration options for completion generation.
	 * @param messages A list of messages representing the context for the completion
	 * model.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CompletionRequest(@JsonProperty("modelUri") String modelUri,
			@JsonProperty("completionOptions") CompletionOptions completionOptions,
			@JsonProperty("messages") List<CompletionMessage> messages) {

	}

	/**
	 * Defines the options for completion generation.
	 *
	 * @param stream
	 * @param temperature
	 * @param maxTokens
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CompletionOptions(@JsonProperty("stream") Boolean stream,
			@JsonProperty("temperature") Double temperature, @JsonProperty("maxTokens") Integer maxTokens) {

	}

	/**
	 * A record representing the result of a completion generation request.
	 *
	 * @param result The completion response containing generated alternatives, usage
	 * statistics, and model version.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CompletionResult(@JsonProperty("result") CompletionResponse result) {
	}

	/**
	 * @param alternatives A list of generated completion alternatives.
	 * @param usage A set of statistics describing the number of content tokens used by
	 * the completion model.
	 * @param modelVersion The model version changes with each new releases.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CompletionResponse(@JsonProperty("alternatives") List<Alternative> alternatives,
			@JsonProperty("usage") Usage usage, @JsonProperty("modelVersion") String modelVersion) {

		/**
		 * A list of generated completion alternatives.
		 *
		 * @param message A message containing the content of the alternative.
		 * @param status The generation status of the alternative
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record Alternative(@JsonProperty("message") CompletionMessage message,
				@JsonProperty("status") CompletionStatus status) {

		}
	}

	/**
	 * A message object representing a wrapper over the inputs and outputs of the
	 * completion model.
	 *
	 * @param role The ID of the message sender.
	 * @param text A message containing the content of the alternative.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CompletionMessage(@JsonProperty("role") Role role, @JsonProperty("text") String text) {

	}

	/**
	 * A set of statistics describing the number of content tokens used by the completion
	 * model.
	 * <p>
	 * An object representing the number of content <a href=
	 * "https://yandex.cloud/docs/foundation-models/concepts/yandexgpt/tokens">tokens</a>
	 * used by the completion model.
	 *
	 * @param inputTextTokens The number of tokens in the textual part of the model input.
	 * @param completionTokens The total number of tokens in the generated completions.
	 * @param totalTokens The total number of tokens, including all input tokens and all
	 * generated tokens.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Usage(@JsonProperty("inputTextTokens") @Nullable Long inputTextTokens,
			@JsonProperty("completionTokens") Long completionTokens, @JsonProperty("totalTokens") Long totalTokens) {

	}

	public ResponseEntity<CompletionResult> completionEntity(CompletionRequest request) {
		return completionEntity(request, new LinkedMultiValueMap<>());
	}

	public ResponseEntity<CompletionResult> completionEntity(CompletionRequest request,
			MultiValueMap<String, String> additionalHttpHeaders) {

		Assert.notNull(request, "The request body must not be null.");
		Assert.isTrue(!request.completionOptions().stream(), "Request must set the stream property to false.");
		Assert.notNull(additionalHttpHeaders, "The additional HTTP headers must not be null.");

		return this.restClient.post()
			.uri(this.completionPath)
			.headers(headers -> headers.addAll(additionalHttpHeaders))
			.body(request)
			.retrieve()
			.toEntity(CompletionResult.class);

	}

	/**
	 * Vectorization models.
	 *
	 * @see <a href=
	 * "https://yandex.cloud/en-ru/docs/foundation-models/concepts/embeddings#yandexgpt-embeddings">
	 * Yandex: Models for text vectorization</a>
	 */
	public enum EmbeddingModel implements EmbeddingModelDescription, YandexModelDescription {

		/**
		 * Vectorization of large source texts, e.g., documentation articles
		 */
		TEXT_SEARCH_DOC("text-search-doc", 256, "Vectorization of large source texts, e.g., documentation articles"),
		/**
		 * Vectorization of short texts, such as search queries, requests, etc.
		 */
		TEXT_SEARCH_QUERY("text-search-query", 256,
				"Vectorization of short texts, such as search queries, requests, etc.");

		private final String value;

		private final int vectorSize;

		private final String description;

		EmbeddingModel(String value, int vectorSize, String description) {
			this.value = value;
			this.vectorSize = vectorSize;
			this.description = description;
		}

		@Override
		public String getName() {
			return this.value;
		}

		@Override
		public String getModelScheme() {
			return "emb";
		}

		@Override
		public String getDescription() {
			return this.description;
		}

		@Override
		public String getVersion() {
			return "latest";
		}

		@Override
		public int getDimensions() {
			return this.vectorSize;
		}

		public static EmbeddingModel ofValue(String value) {
			return switch (value) {
				case "text-search-doc" -> TEXT_SEARCH_DOC;
				case "text-search-query" -> TEXT_SEARCH_QUERY;
				default -> throw new IllegalArgumentException("Unknown embedding model: " + value);
			};
		}

	}

	/**
	 * Request for the service to obtain text embeddings.
	 *
	 * @param modelUri The
	 * <a href="https://yandex.cloud/docs/foundation-models/concepts/yandexgpt/models">ID
	 * of the model</a> to be used for completion generation.
	 * @param text The input text for which the embedding is requested.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record TextEmbeddingRequest(@JsonProperty("modelUri") String modelUri, @JsonProperty("text") String text) {

	}

	/**
	 * Response containing generated text embedding.
	 *
	 * @param embedding A repeated list of double values representing the embedding.
	 * @param numTokens The number of tokens in the input text.
	 * @param modelVersion The model version changes with each new releases.
	 */
	public record TextEmbeddingResponse(@JsonProperty("embedding") double[] embedding,
			@JsonProperty("numTokens") long numTokens, @JsonProperty("modelVersion") String modelVersion) {

	}

	public ResponseEntity<TextEmbeddingResponse> textEmbedding(TextEmbeddingRequest request) {
		Assert.notNull(request, "TextEmbeddingRequest must not be null");
		return this.restClient.post()
			.uri(this.embeddingPath)
			.body(request)
			.retrieve()
			.toEntity(TextEmbeddingResponse.class);
	}

}
