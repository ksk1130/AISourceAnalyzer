package org.example;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * ChatModelFactoryを利用してストリーミングチャットを実行するクラス。
 */
@Command(name = "App", mixinStandardHelpOptions = true, description = "Source Analysis AI using LangChain4j")
public class App implements Runnable {
    /**
     * ロガーインスタンス。
     */
    private static final Logger logger = LogManager.getLogger(App.class);

    @Option(names = { "--prompt" }, required = true, description = "ベースプロンプトファイルのパス")
    private String promptPath;

    @Option(names = { "--code" }, required = true, description = "コードファイルのパス")
    private String codePath;

    @Option(names = { "--prop" }, required = false, description = "プロパティファイルのパス")
    private String propPath;

    // モデルIDをenumで管理
    enum ModelId {
        CLAUDE_3_5_SONNET("anthropic.claude-3-5-sonnet-20240620-v1:0"),
        CLAUDE_3_5_SONNET_v2("apac.anthropic.claude-3-5-sonnet-20241022-v2:0"),
        CLAUDE_3_7_SONNET("apac.anthropic.claude-3-7-sonnet-20250219-v1:0"),
        CLAUDE_4_0_SONNET("apac.anthropic.claude-sonnet-4-20250514-v1:0");

        private final String value;

        ModelId(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @Override
    public void run() {
        String basePrompt = "";
        String codeText = "";
        try {
            logger.info("ベースプロンプトファイル: {}", promptPath);
            logger.info("コードファイル: {}", codePath);

            basePrompt = tryReadStringWithEncodings(Paths.get(promptPath));
            codeText = tryReadStringWithEncodings(Paths.get(codePath));
        } catch (Exception e) {
            logger.error("ファイルの読み込みに失敗しました: {}", e.getMessage());
            return;
        }
        String inputText = basePrompt + "\n" + codeText;
        if (inputText.isEmpty()) {
            logger.warn("プロンプトが空です。終了します。");
            return;
        }

        // ChatModelFactoryを使ってBedrock用モデルを生成
        StreamingChatModel model = null;

        String modelId = ModelId.CLAUDE_3_5_SONNET.getValue();
        String region = "ap-northeast-1";
        String profileOrApiKey = null;

        if (propPath != null) {
            logger.info("プロパティファイルからパラメータを読み込み: {}", propPath);
            model = ChatModelFactory.createFromProperties(
                    ChatModelFactory.Provider.BEDROCK,
                    modelId,
                    region,
                    profileOrApiKey,
                    propPath);
        } else {
            model = ChatModelFactory.create(
                    ChatModelFactory.Provider.BEDROCK,
                    modelId,
                    region,
                    profileOrApiKey);
        }

        // 入力トークン数（日本語はざっくり1文字=1トークンとみなす）
        logger.info("概算入力トークン数: {}", inputText.length());

        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
        logger.info("使用するモデル: {}", model.provider().name());

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
        System.out.println(); // 最後の出力を改行

        // 概算出力トークン数を表示
        int outputTokenCount = 0;
        try {
            outputTokenCount = futureChatResponse.get().toString().length();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } // ここでは単純に文字数をトークン数とみなす
        logger.info("概算出力トークン数: {}", outputTokenCount);
    }

    /**
     * 指定したパスのファイルをUTF-8→Shift_JISの順で自動判別して読み込むユーティリティ
     * 
     * @param path 読み込むファイルのパス
     * @return 読み込んだ文字列
     * @throws Exception 読み込みに失敗した場合
     */
    private static String tryReadStringWithEncodings(Path path) throws Exception {
        // ファイルが存在しない場合は例外を発生させる
        if (!Files.exists(path)) {
            throw new IOException("ファイルが存在しません: " + path);
        }

        Exception lastException = null;

        // UTF-8で試行
        try {
            logger.info("ファイルをUTF-8で読み込み: {}", path);
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (IOException | UncheckedIOException e) {
            logger.debug("UTF-8での読み込みに失敗: {}", e.getMessage());
            lastException = e instanceof IOException ? (IOException) e : new IOException(e);
        }

        // Shift_JISで試行
        try {
            logger.info("UTF-8での読み込みに失敗。Shift_JISで再試行: {}", path);
            return Files.readString(path, Charset.forName("Shift_JIS")).trim();
        } catch (IOException | UncheckedIOException e) {
            logger.debug("Shift_JISでの読み込みに失敗: {}", e.getMessage());
            throw new Exception(path + " の読み込みに失敗しました（UTF-8/Shift_JIS両方）", lastException);
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
