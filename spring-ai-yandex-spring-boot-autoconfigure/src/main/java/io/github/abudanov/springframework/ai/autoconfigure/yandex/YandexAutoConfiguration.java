package io.github.abudanov.springframework.ai.autoconfigure.yandex;

import io.github.abudanov.springframework.ai.yandex.YandexChatModel;
import io.github.abudanov.springframework.ai.yandex.YandexEmbeddingModel;
import io.github.abudanov.springframework.ai.yandex.api.YandexApi;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.autoconfigure.retry.SpringAiRetryAutoConfiguration;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;

@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(YandexApi.class)
@EnableConfigurationProperties({ YandexConnectionProperties.class, YandexChatProperties.class,
		YandexEmbeddingProperties.class })
@ImportAutoConfiguration(classes = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
public class YandexAutoConfiguration {

	private static final String FOLDER_ID_HEADER = "x-folder-id";

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = YandexChatProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
			matchIfMissing = true)
	public YandexChatModel yandexChatModel(YandexConnectionProperties connectionProperties,
			YandexChatProperties completionProperties, YandexEmbeddingProperties embeddingProperties,
			RestClient.Builder restClientBuilder, RetryTemplate retryTemplate,
			ResponseErrorHandler responseErrorHandler, ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<ChatModelObservationConvention> observationConvention) {
		var properties = ResolvedConnectionProperties.of(connectionProperties, completionProperties);
		var yandexApi = new YandexApi(properties.baseUrl(), properties.apiKey(), properties.headers(),
				restClientBuilder, completionProperties.getCompletionPath(), embeddingProperties.getEmbeddingPath(),
				responseErrorHandler);
		var registry = observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP);
		var yandexChatModel = new YandexChatModel(yandexApi, properties.folderId(), completionProperties.getOptions(),
				retryTemplate, registry);
		observationConvention.ifAvailable(yandexChatModel::setObservationConvention);
		return yandexChatModel;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = YandexEmbeddingProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
			matchIfMissing = true)
	public YandexEmbeddingModel yandexEmbeddingModel(YandexConnectionProperties connectionProperties,
			YandexChatProperties completionProperties, YandexEmbeddingProperties embeddingProperties,
			RestClient.Builder restClientBuilder, RetryTemplate retryTemplate,
			ResponseErrorHandler responseErrorHandler, ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<EmbeddingModelObservationConvention> observationConvention) {
		var properties = ResolvedConnectionProperties.of(connectionProperties, embeddingProperties);
		var yandexApi = new YandexApi(properties.baseUrl(), properties.apiKey(), properties.headers(),
				restClientBuilder, completionProperties.getCompletionPath(), embeddingProperties.getEmbeddingPath(),
				responseErrorHandler);
		ObservationRegistry registry = observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP);
		var yandexEmbeddingModel = new YandexEmbeddingModel(yandexApi, properties.folderId(),
				embeddingProperties.getOptions(), retryTemplate, registry);
		observationConvention.ifAvailable(yandexEmbeddingModel::setObservationConvention);
		return yandexEmbeddingModel;
	}

	private record ResolvedConnectionProperties(String baseUrl, String folderId, String apiKey,
			MultiValueMap<String, String> headers) {

		static ResolvedConnectionProperties of(YandexConnectionProperties connectionProperties,
				YandexCommonProperties modelProperties) {

			var baseUrl = StringUtils.hasText(modelProperties.getBaseUrl()) ? modelProperties.getBaseUrl()
					: connectionProperties.getBaseUrl();
			var folderId = StringUtils.hasText(modelProperties.getFolderId()) ? modelProperties.getFolderId()
					: connectionProperties.getFolderId();
			var apiKey = StringUtils.hasText(modelProperties.getApiKey()) ? modelProperties.getApiKey()
					: connectionProperties.getApiKey();
			var headers = new HashMap<String, List<String>>();
			if (StringUtils.hasText(folderId)) {
				headers.put(FOLDER_ID_HEADER, List.of(folderId));
			}
			return new ResolvedConnectionProperties(baseUrl, folderId, apiKey,
					CollectionUtils.toMultiValueMap(headers));
		}
	}

}
