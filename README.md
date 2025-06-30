# LangChain4jを利用したソースコード解析AI

LangChain4jライブラリを使用してBedrockとAzure OpenAIでソースコード解析を行うAIアプリケーションです。

## 概要

このプロジェクトは以下の機能を提供します：

- **AWS Bedrock**および**Azure OpenAI**での統一されたストリーミングチャット実装
- **ファイルベース**のプロンプト・コード読み込み（UTF-8/Shift_JIS自動判別）
- **名前付きコマンドライン引数**（picocli使用）
- **日本語対応**（文字化け対策、ログ出力）
- **トークン数概算表示**

## 必要な環境

- **Java 21以上**
- **AWS CLI**設定済み（Bedrockアクセス権限）
- **Gradle 8.14**（included wrapper使用可能）

## 主要なクラス

### ChatModelFactory
BedrockとAzure OpenAIのチャットモデルを生成するファクトリークラス。
- プロバイダー切り替え（BEDROCK/AZURE_OPENAI）
- LLMパラメータ指定（maxTokens, temperature, topP）
- プロパティファイルからの設定読み込み

### App
メインアプリケーション。picocliを使用した名前付き引数処理。
- `--prompt`: ベースプロンプトファイル
- `--code`: 解析対象コードファイル  
- `--prop`: LLMパラメータ設定ファイル（オプション）

### ConverseStream
AWS Bedrock Runtime APIを直接使用した実装（参考用）。

## ビルドと実行

### 1. 依存関係のダウンロードとビルド

```bash
# プロジェクトのビルド
.\gradlew build

# 依存JARファイルをlibsディレクトリにコピー
.\gradlew copyDependencies
```

### 2. 基本実行（フルJDK使用）

```bash
# コマンドでの実行例
java -cp "app\build\classes\java\main;libs\*" ^
     -Dfile.encoding=UTF-8 ^
     org.example.RunStreamChatWithLangChain4j ^
     --prompt=sample_prompt.txt ^
     --code=app\src\main\java\org\example\ChildClass.java

# ラッパースクリプトを使用して実行
.\exec.bat --prompt=sample_prompt.txt --code=app\src\main\java\org\example\ChildClass.java
```

## 設定

### LLMパラメータ設定ファイル（例：config.properties）

```properties
maxTokens=4096
temperature=0.7
topP=0.9
```

実行時に`--prop=config.properties`で指定可能。

## ファイル構成

```
bedrock_gettingstarted/
├── app/
│   ├── build.gradle                 # Gradle設定
│   └── src/main/java/org/example/
│       ├── ChatModelFactory.java    # モデル生成ファクトリー
│       ├── RunStreamChatWithLangChain4j.java  # メインアプリ
│       ├── ConverseStream.java      # Bedrock直接実装（参考）
│       └── ChildClass.java          # サンプルコード
├── libs/                            # 依存JARファイル
├── exec.bat                         # 実行スクリプト
├── sample_prompt.txt                # サンプルプロンプト
└── README.md                        # このファイル
```

## 使用ライブラリ

- **LangChain4j** 1.1.0 - AI/LLM統合フレームワーク
- **AWS SDK for Java** 2.x - Bedrock Runtime API
- **picocli** 4.7.5 - コマンドライン引数解析
- **Log4j2** 2.20.0 - ログ出力

## ライセンス

このプロジェクトはサンプル実装です。商用利用時は各ライブラリのライセンスを確認してください。

## 参考

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [AWS Bedrock Developer Guide](https://docs.aws.amazon.com/bedrock/)
- [picocli User Manual](https://picocli.info/)
