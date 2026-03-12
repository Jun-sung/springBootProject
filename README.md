# 🏠 SafeHome

> 安全な住環境のための Spring Boot ベース Webサービス

<br>

## 📌 プロジェクト紹介

**SafeHome** は、Spring Boot を学習しながら制作した Webアプリケーションです。  
MVCパターンベースの構造と、Springの様々な機能（依存性注入・ORM・セキュリティ等）を実践しながら開発しました。

<br>

## 🛠 技術スタック

| 分類 | 技術 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Frontend | HTML5, CSS3, Bootstrap |
| Build Tool | Maven / Gradle |


<br>

## 📁 プロジェクト構造

```
SpringBootProject/
├── main/
│   ├── java/
│   │   └── com/safehome/       # コアビジネスロジック
│   │       ├── controller/     # リクエスト処理
│   │       ├── service/        # ビジネスロジック
│   │       ├── domain/         # エンティティ / DTO
│   │       └── mapper/         # DB連携
│   └── resources/
│       ├── templates/          # HTMLビュー
│       └── application.yml     # 環境設定
└── test/
    └── java/com/safehome/      # 単体テスト
```

<br>

## ⚙️ 実行方法

### 1. 事前準備

- Java 17 以上
- Maven または Gradle

### 2. 設定ファイルの構成

`src/main/resources/application.yml` または `application.properties`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/safehome
    username: YOUR_DB_USERNAME
    password: YOUR_DB_PASSWORD
  thymeleaf:
    cache: false
```

### 3. ビルド & 実行

```bash
# Maven
./mvnw spring-boot:run

# Gradle
./gradlew bootRun
```

<br>

## 💡 主要機能

- 🏠 住居情報の登録・照会
- 👤 会員登録 / ログイン（Spring Security）
- 📋 掲示板 CRUD
- 🔍 検索・フィルタリング機能

<br>

## 📚 学習目標 & 実装ポイント

```
✅ Spring Boot 3 プロジェクト構造の理解
✅ MVCパターンベースのWebアプリケーション設計
✅ Spring Security による認証・認可処理
✅ Thymeleaf テンプレートエンジンの活用
✅ JUnit を使った単体テストの作成
```

<br>

## 🙋 開発情報

| 項目 | 内容 |
|------|------|
| 開発タイプ | 個人学習プロジェクト |
| 開発期間 | - |
| 学習目的 | Spring Boot 3 ベースのWeb開発実践 |

<br>

## 📬 Contact

- GitHub: [@Jun-sung](https://github.com/Jun-sung)
