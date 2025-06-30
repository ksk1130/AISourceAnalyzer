package org.example;

import dev.langchain4j.model.bedrock.BedrockStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import software.amazon.awssdk.regions.Region;
import java.util.concurrent.CompletableFuture;

/**
 * Bedrockや他のプロバイダーのStreamingChatModelをラップし、
 * ストリーミングでチャット応答を受け取るためのクラスです。
 */
public class StreamChatWithLangChain4j {
    /**
     * 利用するStreamingChatModelのインスタンス。
     */
    private final StreamingChatModel model;

    /**
     * モデルIDとリージョンを指定してBedrockのStreamingChatModelを生成します。
     * @param modelId モデルID
     * @param region リージョン
     */
    public StreamChatWithLangChain4j(String modelId, Region region) {
        this.model = BedrockStreamingChatModel.builder()
                .region(region)
                .modelId(modelId)
                .build();
    }

    /**
     * 入力テキストをチャットモデルに投げ、ストリーミングで応答を受け取ります。
     * @param inputText ユーザーからの入力テキスト
     */
    public void chat(String inputText) {
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
        model.chat(inputText, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureChatResponse.complete(completeResponse);
            }
            @Override
            public void onError(Throwable error) {
                futureChatResponse.completeExceptionally(error);
            }
        });
        futureChatResponse.join();
    }
}
