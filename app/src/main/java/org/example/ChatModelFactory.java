package org.example;

import dev.langchain4j.model.bedrock.BedrockStreamingChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import software.amazon.awssdk.regions.Region;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * BedrockやAzure OpenAIなどのチャットモデルを生成するファクトリークラス。
 */
public class ChatModelFactory {
    /**
     * 利用可能なプロバイダーの列挙型。
     */
    public enum Provider {
        BEDROCK,
        AZURE_OPENAI
    }

    /**
     * 指定したプロバイダー・モデルID・リージョン/エンドポイント・プロファイル/APIキーからチャットモデルを生成します。
     * 
     * @param provider         利用するプロバイダー（BEDROCKまたはAZURE_OPENAI）
     * @param modelId          モデルID
     * @param regionOrEndpoint Bedrockの場合はリージョン、Azure OpenAIの場合はエンドポイント
     * @param profileOrApiKey  Bedrockの場合はプロファイル名（未使用）、Azure OpenAIの場合はAPIキー
     * @return StreamingChatModelのインスタンス
     */
    public static StreamingChatModel create(Provider provider, String modelId, String regionOrEndpoint,
            String profileOrApiKey) {
        switch (provider) {
            case BEDROCK:
                // profileOrApiKeyは未使用。profile指定したい場合はBedrockStreamingChatModel.builder()に追加で設定してください。
                return BedrockStreamingChatModel.builder()
                        .region(Region.of(regionOrEndpoint))
                        .modelId(modelId)
                        .build();
            case AZURE_OPENAI:
                // regionOrEndpoint: Azure OpenAIのエンドポイント, profileOrApiKey: APIキー
                return AzureOpenAiStreamingChatModel.builder()
                        .endpoint(regionOrEndpoint)
                        .apiKey(profileOrApiKey)
                        .build();
            default:
                throw new IllegalArgumentException("Unknown provider: " + provider);
        }
    }

    /**
     * LLMパラメータ（maxTokens, temperature, topP）を指定してチャットモデルを生成します。
     * 
     * @param provider         利用するプロバイダー（BEDROCKまたはAZURE_OPENAI）
     * @param modelId          モデルID
     * @param regionOrEndpoint Bedrockの場合はリージョン、Azure OpenAIの場合はエンドポイント
     * @param profileOrApiKey  Bedrockの場合はプロファイル名（未使用）、Azure OpenAIの場合はAPIキー
     * @param maxTokens        最大トークン数（nullの場合はデフォルト値）
     * @param temperature      温度パラメータ（nullの場合はデフォルト値）
     * @param topP             top-pサンプリング値（nullの場合はデフォルト値）
     * @return StreamingChatModelのインスタンス
     */
    public static StreamingChatModel create(Provider provider, String modelId, String regionOrEndpoint,
            String profileOrApiKey,
            Integer maxTokens, Double temperature, Double topP) {
        switch (provider) {
            case BEDROCK:
                BedrockStreamingChatModel.Builder bedrockBuilder = BedrockStreamingChatModel.builder()
                        .region(Region.of(regionOrEndpoint))
                        .modelId(modelId);
                // BedrockのBuilderにはmaxTokens/temperature/topPは未対応。警告ログを出す。
                if (maxTokens != null || temperature != null || topP != null) {
                    org.apache.logging.log4j.LogManager.getLogger(ChatModelFactory.class)
                            .warn("BedrockStreamingChatModel.BuilderはmaxTokens/temperature/topP未対応です。パラメータは無視されます。");
                }
                return bedrockBuilder.build();
            case AZURE_OPENAI:
                AzureOpenAiStreamingChatModel.Builder azureBuilder = AzureOpenAiStreamingChatModel.builder()
                        .endpoint(regionOrEndpoint)
                        .apiKey(profileOrApiKey);
                if (maxTokens != null)
                    azureBuilder.maxTokens(maxTokens);
                if (temperature != null)
                    azureBuilder.temperature(temperature);
                if (topP != null)
                    azureBuilder.topP(topP);
                return azureBuilder.build();
            default:
                throw new IllegalArgumentException("Unknown provider: " + provider);
        }
    }

    /**
     * プロパティファイルからmaxTokens, temperature, topPを読み込んでモデルを生成します。
     * Bedrockはパラメータ未対応ですが、Azure OpenAIには反映されます。
     * 
     * @param provider         利用するプロバイダー
     * @param modelId          モデルID
     * @param regionOrEndpoint Bedrockの場合はリージョン、Azure OpenAIの場合はエンドポイント
     * @param profileOrApiKey  Bedrockの場合はプロファイル名（未使用）、Azure OpenAIの場合はAPIキー
     * @param propertiesPath   プロパティファイルのパス
     * @return StreamingChatModelのインスタンス
     */
    public static StreamingChatModel createFromProperties(Provider provider, String modelId, String regionOrEndpoint,
            String profileOrApiKey, String propertiesPath) {
        Integer maxTokens = null;
        Double temperature = null;
        Double topP = null;
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propertiesPath)) {
            props.load(fis);
            if (props.getProperty("maxTokens") != null) {
                maxTokens = Integer.valueOf(props.getProperty("maxTokens"));
            }
            if (props.getProperty("temperature") != null) {
                temperature = Double.valueOf(props.getProperty("temperature"));
            }
            if (props.getProperty("topP") != null) {
                topP = Double.valueOf(props.getProperty("topP"));
            }
        } catch (IOException e) {
            org.apache.logging.log4j.LogManager.getLogger(ChatModelFactory.class)
                    .warn("プロパティファイルの読み込みに失敗しました: " + propertiesPath, e);
        }
        return create(provider, modelId, regionOrEndpoint, profileOrApiKey, maxTokens, temperature, topP);
    }

    /**
     * 指定したプロバイダー・モデルID・リージョン/エンドポイント・プロファイル/APIキー・入力テキストから
     * ストリーミングチャットを実行し、部分応答・完了応答・エラーをコールバックで受け取るユーティリティメソッド。
     * 
     * @param provider         利用するプロバイダー
     * @param modelId          モデルID
     * @param regionOrEndpoint Bedrockの場合はリージョン、Azure OpenAIの場合はエンドポイント
     * @param profileOrApiKey  Bedrockの場合はプロファイル名（未使用）、Azure OpenAIの場合はAPIキー
     * @param input            入力テキスト
     * @param onPartial        部分応答を受け取るコールバック（null可）
     * @param onComplete       完了応答を受け取るコールバック（null可）
     * @param onError          エラー発生時のコールバック（null可）
     */
    public static void streamChat(
            Provider provider,
            String modelId,
            String regionOrEndpoint,
            String profileOrApiKey,
            String input,
            java.util.function.Consumer<String> onPartial,
            java.util.function.Consumer<String> onComplete,
            java.util.function.Consumer<Throwable> onError) {
        StreamingChatModel model = create(provider, modelId, regionOrEndpoint, profileOrApiKey);
        model.chat(input, new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
            private final StringBuilder sb = new StringBuilder();

            @Override
            public void onPartialResponse(String partialResponse) {
                sb.append(partialResponse);
                if (onPartial != null)
                    onPartial.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse completeResponse) {
                if (onComplete != null)
                    onComplete.accept(sb.toString());
            }

            @Override
            public void onError(Throwable error) {
                if (onError != null)
                    onError.accept(error);
            }
        });
    }
}
