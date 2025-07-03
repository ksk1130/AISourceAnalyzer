package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gemini APIにHTTPリクエストでチャットするクラス。
 */
@Command(name = "AppGemini", mixinStandardHelpOptions = true, description = "Chat with Gemini API via HTTP request.")
public class AppGemini implements Runnable {
    private static final Logger logger = LogManager.getLogger(AppGemini.class);

    @Option(names = { "--prompt" }, required = true, description = "ベースプロンプトファイルのパス")
    private String promptPath;

    @Option(names = { "--code" }, required = true, description = "コードファイルのパス")
    private String codePath;

    @Option(names = {
            "--endpoint" }, required = false, description = "エンドポイントURL", defaultValue = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent")
    private String endpoint;

    /**
     * メインの実行処理。プロンプト・コードをファイルから読み込み、Gemini APIにストリーミングでリクエストを送信し、レスポンスを標準出力に出力します。
     */
    @Override
    public void run() {
        String basePrompt = "";
        String codeText = "";
        // APIキーを環境変数から取得
        String apiKey = System.getenv("API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("環境変数 API_KEY が設定されていません。APIキーをセットしてください。");
            System.err.println("[ERROR] 環境変数 API_KEY が設定されていません。");
            return;
        }
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

        // Gemini API用リクエストJSON生成
        String requestBody = String.format("{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}", escapeJson(inputText));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "text/event-stream") // ストリーミングレスポンスを要求
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        logger.info("Gemini APIへリクエスト送信: {} (streaming)", endpoint);
        try {
            HttpResponse<java.io.InputStream> response = client.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            logger.info("HTTPステータス: {}", response.statusCode());
            if (response.statusCode() == 200) {
                // ストリーミングで部分的に出力
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.body(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String json = line.substring(6).trim();
                            if (json.equals("[DONE]"))
                                break;
                            printGeminiStreamChunk(json);
                        }
                    }
                    System.out.println();
                }
            } else {
                System.err.println("[ERROR] "
                        + new String(response.body().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Gemini APIリクエスト失敗: {}", e.getMessage());
        }
    }

    /**
     * Geminiストリーミングレスポンスの1チャンク（JSON）からテキスト部分のみを抽出して標準出力に出力します。
     * 
     * @param json ストリーミングで受信した1チャンク分のJSON文字列
     */
    private static void printGeminiStreamChunk(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            if (root.has("candidates")) {
                for (JsonNode candidate : root.get("candidates")) {
                    JsonNode parts = candidate.path("content").path("parts");
                    for (JsonNode part : parts) {
                        if (part.has("text")) {
                            System.out.print(part.get("text").asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 無視
        }
    }

    /**
     * Gemini APIの通常レスポンス（JSON）からテキスト部分のみを抽出して標準出力に出力します。
     * 
     * @param responseBody Gemini APIからのレスポンスボディ（JSON文字列）
     */
    private static void printGeminiResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            // Geminiの標準レスポンスからテキスト部分を抽出
            if (root.has("candidates")) {
                for (JsonNode candidate : root.get("candidates")) {
                    JsonNode parts = candidate.path("content").path("parts");
                    for (JsonNode part : parts) {
                        if (part.has("text")) {
                            System.out.print(part.get("text").asText());
                        }
                    }
                }
                System.out.println();
            } else {
                System.out.println(responseBody);
            }
        } catch (Exception e) {
            System.out.println(responseBody);
        }
    }

    /**
     * 指定したパスのファイルをUTF-8→Shift_JISの順で自動判別して読み込むユーティリティ。
     * 
     * @param path 読み込むファイルのパス
     * @return 読み込んだ文字列
     * @throws Exception 読み込みに失敗した場合
     */
    private static String tryReadStringWithEncodings(Path path) throws Exception {
        if (!Files.exists(path)) {
            throw new IOException("ファイルが存在しません: " + path);
        }
        Exception lastException = null;
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (IOException | UncheckedIOException e) {
            lastException = e instanceof IOException ? (IOException) e : new IOException(e);
        }
        try {
            return Files.readString(path, Charset.forName("Shift_JIS")).trim();
        } catch (IOException | UncheckedIOException e) {
            throw new Exception(path + " の読み込みに失敗しました（UTF-8/Shift_JIS両方）", lastException);
        }
    }

    /**
     * JSON文字列として安全に送信するため、特殊文字をエスケープします。
     * 
     * @param text エスケープ対象の文字列
     * @return エスケープ済み文字列
     */
    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /**
     * コマンドライン引数を受け取り、アプリケーションを実行します。
     * 
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new AppGemini()).execute(args);
        System.exit(exitCode);
    }
}
