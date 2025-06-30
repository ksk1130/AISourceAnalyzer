package org.example;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

/**
 * Bedrock Runtime APIを使って、Javaコードの内容説明をAIに依頼するサンプルクラス。
 * コマンドライン引数で指定したファイルの内容をプロンプトに含めて送信します。
 */
public class ConverseStream {

    /**
     * メインメソッド。ファイルからプロンプトを作成し、Bedrockに送信して応答を表示します。
     * @param args 0: コードファイルのパス
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("プロンプトとなるテキストファイルのパスを引数で指定してください。");
            return;
        }
        String inputText = """
                以下にJavaのコードを示しますので、ソースコードの内容を理解し、コードの説明を行ってください。
                なお、コードの説明は日本語で行ってください。また、コードの説明はコードの内容に基づいて行ってください。
                コードの説明を行う際、メソッドへの引数については、リテラルではなく型名で説明してください。
                        """;
        try {
            inputText += Files.readString(Paths.get(args[0]), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            System.out.println("ファイルの読み込みに失敗しました: " + e.getMessage());
            return;
        }
        if (inputText.isEmpty()) {
            System.out.println("プロンプトが空です。終了します。");
            return;
        }

        // Bedrock Runtimeクライアントの作成
        var client = BedrockRuntimeAsyncClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create("opeusr"))
                .region(Region.AP_NORTHEAST_1)
                .build();

        // モデルIDの設定
        var modelId = "anthropic.claude-3-5-sonnet-20240620-v1:0";

        // メッセージオブジェクトの作成
        var message = Message.builder()
                .content(ContentBlock.fromText(inputText))
                .role(ConversationRole.USER)
                .build();

        // 入力トークン数（日本語はざっくり1文字=1トークンとみなす）
        System.out.println("概算入力トークン数: " + inputText.length());

        // 出力トークン数をカウント
        final int[] outputTokenCount = {0};

        // 応答ストリームハンドラの作成
        var responseStreamHandler = ConverseStreamResponseHandler.builder()
                .subscriber(ConverseStreamResponseHandler.Visitor.builder()
                        .onContentBlockDelta(chunk -> {
                            String responseText = chunk.delta().text();
                            outputTokenCount[0] += responseText.length();
                            System.out.print(responseText);
                        }).build())
                .onError(err -> System.err.printf("Can't invoke '%s': %s", modelId, err.getMessage())).build();

        try {
            // メッセージ送信と応答の表示
            client.converseStream(request -> request.modelId(modelId)
                    .messages(message)
                    .inferenceConfig(config -> config
                            .maxTokens(4096) // 回答が途中で切れないよう最大トークン数を増やす
                            .temperature(0.5F)
                            .topP(0.9F)),
                    responseStreamHandler).get();
            System.out.println("\n概算出力トークン数: " + outputTokenCount[0]);
        } catch (ExecutionException | InterruptedException e) {
            System.err.printf("Can't invoke '%s': %s", modelId, e.getCause().getMessage());
        }
    }
}
