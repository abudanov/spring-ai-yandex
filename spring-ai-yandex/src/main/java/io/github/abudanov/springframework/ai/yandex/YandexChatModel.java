package io.github.abudanov.springframework.ai.yandex;

import io.github.abudanov.springframework.ai.yandex.api.YandexApi;
import io.github.abudanov.springframework.ai.yandex.api.YandexApi.CompletionMessage;
import io.github.abudanov.springframework.ai.yandex.api.YandexApi.CompletionOptions;
import io.github.abudanov.springframework.ai.yandex.api.YandexApi.CompletionRequest;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

public class YandexChatModel implements ChatModel {

	private static final Logger logger = LoggerFactory.getLogger(YandexChatModel.class);

	private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();

	private final YandexApi yandexApi;

	private final String folderId;

	private final YandexChatOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	private final ObservationRegistry observationRegistry;

	private ChatModelObservationConvention observationConvention;

	public YandexChatModel(YandexApi yandexApi, String folderId, YandexChatOptions defaultOptions) {
		this(yandexApi, folderId, defaultOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public YandexChatModel(YandexApi yandexApi, String folderId, YandexChatOptions defaultOptions,
			RetryTemplate retryTemplate) {
		this(yandexApi, folderId, defaultOptions, retryTemplate, ObservationRegistry.NOOP);
	}

	public YandexChatModel(YandexApi yandexApi, String folderId, YandexChatOptions defaultOptions,
			RetryTemplate retryTemplate, ObservationRegistry observationRegistry) {
		Assert.notNull(yandexApi, "YandexFoundationModelsApi must not be null");
		Assert.notNull(defaultOptions, "Options must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");
		Assert.notNull(observationRegistry, "ObservationRegistry must not be null");
		Assert.hasText(folderId, "FolderId must not be empty");
		this.folderId = folderId;
		this.yandexApi = yandexApi;
		this.defaultOptions = defaultOptions;
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;
	}

	@Override
	public ChatResponse call(Prompt prompt) {

		var observationContext = ChatModelObservationContext.builder()
			.prompt(prompt)
			.provider(YandexApiConstants.PROVIDER_NAME)
			.requestOptions(buildRequestOptions(prompt.getOptions()))
			.build();

		var request = toRequest(prompt);
		return ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry)
			.observe(() -> {
				var completionEntity = this.retryTemplate.execute(ctx -> this.yandexApi.completionEntity(request));
				var response = completionEntity.getBody();
				if (response == null || response.result() == null) {
					logger.warn("No completion response returned for prompt: {}", prompt);
					return new ChatResponse(List.of());
				}
				var result = response.result();
				List<Generation> generations = result.alternatives().stream().map(alternative -> {
					var assistantMessage = new AssistantMessage(alternative.message().text());
					var generationMetadata = ChatGenerationMetadata.from(alternative.status().name(), null);
					return new Generation(assistantMessage, generationMetadata);
				}).toList();
				var metadata = ChatResponseMetadata.builder()
					.withUsage(result.usage() != null ? YandexChatUsage.from(result.usage()) : new EmptyUsage())
					.withModel(result.modelVersion())
					.build();
				return new ChatResponse(generations, metadata);
			});
	}

	private CompletionRequest toRequest(Prompt prompt) {
		var options = buildRequestOptions(prompt.getOptions());
		var completionOptions = new CompletionOptions(false, options.getTemperature(), options.getMaxTokens());
		var messages = prompt.getInstructions().stream().map(message -> {
			var role = switch (message.getMessageType()) {
				case USER -> YandexApi.Role.USER;
				case ASSISTANT -> YandexApi.Role.ASSISTANT;
				case SYSTEM -> YandexApi.Role.SYSTEM;
				case TOOL -> throw new IllegalArgumentException("Yandex does not support tool messages");
			};
			return new CompletionMessage(role, message.getContent());
		}).toList();
		String model = Objects.requireNonNull(options.getModel());
		String modelUri = YandexApi.ChatModel.ofValue(model).getModelUri(folderId);
		return new CompletionRequest(modelUri, completionOptions, messages);
	}

	private YandexChatOptions buildRequestOptions(ChatOptions options) {
		var defaultOptions = this.getDefaultOptions();
		if (options != null) {
			ModelOptionsUtils.merge(options, defaultOptions, YandexChatOptions.class);
		}
		return defaultOptions;
	}

	@Override
	public YandexChatOptions getDefaultOptions() {
		return this.defaultOptions.copy();
	}

	public void setObservationConvention(ChatModelObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
	}

}
