package io.github.abudanov.springframework.ai.yandex;

import io.github.abudanov.springframework.ai.yandex.api.YandexApi;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.lang.NonNull;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class YandexEmbeddingModel implements EmbeddingModel {

	private static final Logger logger = LoggerFactory.getLogger(YandexEmbeddingModel.class);

	private static final EmbeddingModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultEmbeddingModelObservationConvention();

	private final YandexApi yandexApi;

	private final String folderId;

	private final YandexEmbeddingOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	private final ObservationRegistry observationRegistry;

	private EmbeddingModelObservationConvention observationConvention;

	public YandexEmbeddingModel(YandexApi yandexApi, String folderId, YandexEmbeddingOptions defaultOptions) {
		this(yandexApi, folderId, defaultOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public YandexEmbeddingModel(YandexApi yandexApi, String folderId, YandexEmbeddingOptions defaultOptions,
			RetryTemplate retryTemplate) {
		this(yandexApi, folderId, defaultOptions, retryTemplate, ObservationRegistry.NOOP);
	}

	public YandexEmbeddingModel(YandexApi yandexApi, String folderId, YandexEmbeddingOptions defaultOptions,
			RetryTemplate retryTemplate, ObservationRegistry observationRegistry) {
		this.yandexApi = yandexApi;
		this.folderId = folderId;
		this.defaultOptions = defaultOptions;
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;
	}

	@Override
	public float[] embed(String text) {
		return EmbeddingModel.super.embed(text);
	}

	@Override
	public EmbeddingResponse call(EmbeddingRequest embeddingRequest) {
		var textEmbeddingRequests = toRequest(embeddingRequest);
		return EmbeddingModelObservationDocumentation.EMBEDDING_MODEL_OPERATION
			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION,
					() -> EmbeddingModelObservationContext.builder()
						.embeddingRequest(embeddingRequest)
						.provider(YandexApiConstants.PROVIDER_NAME)
						.requestOptions(buildRequestOptions(embeddingRequest.getOptions()))
						.build(),
					this.observationRegistry)
			.observe(() -> {
				var embeddings = new ArrayList<Embedding>(textEmbeddingRequests.size());
				for (int i = 0; i < textEmbeddingRequests.size(); i++) {
					var textEmbeddingRequest = textEmbeddingRequests.get(i);
					var responseEntity = this.retryTemplate
						.execute(ctx -> this.yandexApi.textEmbedding(textEmbeddingRequest));
					var response = responseEntity.getBody();

					float[] embedding;
					if (response == null) {
						logger.warn("No embeddings returned for request: {}", embeddingRequest);
						embedding = EmbeddingUtils.EMPTY_FLOAT_ARRAY;
					}
					else {
						embedding = EmbeddingUtils.doubleToFloatPrimitive(response.embedding());
					}
					embeddings.add(new Embedding(embedding, i));
				}
				return new EmbeddingResponse(embeddings);
			});
	}

	private EmbeddingOptions buildRequestOptions(EmbeddingOptions options) {
		var yandexEmbeddingOptions = ModelOptionsUtils.copyToTarget(options, EmbeddingOptions.class,
				YandexEmbeddingOptions.class);
		return ModelOptionsUtils.merge(yandexEmbeddingOptions, defaultOptions, YandexEmbeddingOptions.class);
	}

	private @NonNull List<YandexApi.TextEmbeddingRequest> toRequest(EmbeddingRequest request) {
		var embeddingOptions = buildRequestOptions(request.getOptions());
		String model = Objects.requireNonNull(embeddingOptions.getModel());
		var modelUri = YandexApi.EmbeddingModel.ofValue(model).getModelUri(folderId);
		return request.getInstructions()
			.stream()
			.map(input -> new YandexApi.TextEmbeddingRequest(modelUri, input))
			.toList();
	}

	@Override
	public float[] embed(Document document) {
		return this.embed(document.getFormattedContent());
	}

	public void setObservationConvention(EmbeddingModelObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
	}

	@Override
	public int dimensions() {
		return 256;
	}

}
