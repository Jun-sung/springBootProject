# 🏠 SAFE HOME

> 日本不動産取引データ × ハザードマップ × AIチャット  
> リスク情報と物件情報を統合した不動産情報プラットフォーム

<br>

## 📌 プロジェクト概要

**SAFE HOME** は、Spring Boot を活用したフルスタック個人開発プロジェクトです。  
日本では不動産情報とハザードリスク情報が別々のサービスに分かれており、  
**「価格は安いが災害リスクは高い地域」** などを一目で判断しづらいという課題を解決するために開発しました。

物件情報と災害リスクを一画面で統合確認できる UI を提供し、安全な住まい探しをサポートします。

<br>

## 🛠 技術スタック

### Backend
| 技術 | バージョン |
|------|-----------|
| Java | JDK 17 LTS |
| Spring Boot | v3.5.0 |
| Spring Security | Security 6 |
| MyBatis | v3.0.5 |

### Database
| 技術 | バージョン |
|------|-----------|
| MySQL | v8.0 |

### Frontend
| 技術 | 用途 |
|------|------|
| Leaflet.js | インタラクティブ地図 |
| Bootstrap | v5.3 |
| JavaScript | ES6+ |
| Thymeleaf | テンプレートエンジン |

### 外部 API
| API | 用途 |
|-----|------|
| Reinfolib API（国土交通省） | 不動産取引価格データ取得 |
| Gemini API 1.5 Flash | AI チャット・エリア推薦 |
| OpenStreetMap | 地図タイル |

<br>

## 💡 主要機能

### 🗺 物件情報の可視化
国土交通省の不動産取引価格データを Leaflet 地図上にポップアップ付きで表示。  
価格・築年数・間取りなどの多条件フィルターに対応。

### 🌊 ハザードマップ統合
洪水（6段階）・津波（5段階）・土砂災害（2段階）のリスクゾーンを  
GeoJSON ポリゴンとして地図上に重ね合わせ表示。

### 🤖 AI チャット相談（Gemini API）
ユーザーの希望条件（予算・通勤・災害リスク許容度）を入力すると、  
条件に合うエリアを会話形式で提案。日本語・韓国語の両言語に対応。

### 👥 ロールベースアクセス制御（RBAC）

| ロール | 利用可能機能 |
|--------|------------|
| **非会員 (GUEST)** | 物件マップ閲覧・ハザード情報確認・地域検索・AIチャット |
| **会員 (USER)** | 上記 ＋ Q&A 投稿・編集・削除、マイページ管理 |
| **管理者 (ADMIN)** | 上記 ＋ 全ユーザー管理・Q&A 全体管理・統計ダッシュボード |

<br>

## 🗃 DB 設計

```
USER               : 会員情報・認証・権限管理（BCrypt パスワードハッシュ）
QUESTION / ANSWER  : Q&A 掲示板（1:N 構造）
TRANSACTION_RAW    : Reinfolib API の不動産取引価格生データ
HAZARD_ZONE        : 洪水・津波・土砂災害リスク情報（GeoJSON LONGTEXT）
```

<br>

## 📁 プロジェクト構造

```
SpringBootProject/
├── main/
│   ├── java/
│   │   └── com/safehome/
│   │       ├── controller/     # リクエスト処理
│   │       ├── service/        # ビジネスロジック
│   │       ├── domain/         # エンティティ / DTO
│   │       └── mapper/         # MyBatis DB 連携
│   └── resources/
│       ├── templates/          # Thymeleaf HTML ビュー
│       └── application.yml     # 環境設定
└── test/
    └── java/com/safehome/      # 単体テスト
```

<br>

## ⚙️ 実行方法

### 1. 事前準備

- Java 17 以上
- MySQL 8.0
- 各種 API キー（Reinfolib / Gemini）

### 2. 設定ファイルの構成

`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/safehome
    username: YOUR_DB_USERNAME
    password: YOUR_DB_PASSWORD
  thymeleaf:
    cache: false

gemini:
  api:
    key: YOUR_GEMINI_API_KEY

reinfolib:
  api:
    key: YOUR_REINFOLIB_API_KEY
```

### 3. ビルド & 実行

```bash
# Maven
./mvnw spring-boot:run

# Gradle
./gradlew bootRun
```

<br>

## 🔧 Troubleshooting

### ISSUE 01 — MySQL データ長超過（Data Overflow）
タイルキー（`region_code`）が DB カラムの文字数制限を超えてデータが切り捨てられる現象が発生。  
**→ Prefix Mapping 戦略を導入**（`FLOOD→FL` 等）し、キー長を 60% 以上短縮。

### ISSUE 02 — 言語切替時に検索パラメータが消える（State Loss）
日/韓 言語切替の際、地図座標と検索フィルターのパラメータが初期化される問題が発生。  
**→ Dynamic Link Resolver を実装**し、`URLSearchParams` で現在のクエリ文字列を言語切替ボタンのリンクに動的に結合。

### ISSUE 03 — 非同期レスポンスの逆転現象（Race Condition）
素早いフィルター操作時、古いリクエストのレスポンスが最新の結果を上書きし、誤った情報が表示されていた。  
**→ Request ID バリデーションを導入**し、`AbortController` と固有リクエスト ID で最後のリクエストのみをレンダリング。

<br>

## 📚 学習目標 & 実装ポイント

```
✅ Controller → Service → Mapper の階層型アーキテクチャ設計
✅ Spring Security による RBAC・BCrypt 暗号化・セッション管理
✅ Reinfolib API 連携（データ取得 → 加工 → 地図表示）
✅ Gemini API を活用した AI チャット機能の実装
✅ GeoJSON ポリゴン描画・タイル座標変換など地理情報処理
✅ JVM / DB / 外部 API の 3 段階キャッシュ & 並列処理による最適化
```

<br>

## 🚀 今後の改善・拡張計画

| 項目 | 内容 |
|------|------|
| Redis 導入 | JVM ローカルキャッシュを Redis に移行し、スケールアウト時の整合性を確保 |
| JWT 認証への移行 | セッション方式からステートレス JWT に切り替え |
| WebSocket（Q&A） | STOMP + WebSocket でリアルタイムチャットを実装 |
| AWS デプロイ | EC2 + RDS + S3 構成で本番環境を構築 |

<br>

## 🙋 開発情報

| 項目 | 内容 |
|------|------|
| 開発者 | 高俊成（Ko Junsung） |
| 開発タイプ | 個人プロジェクト |
| 開発期間 | 2026.02.10 — 2026.02.20（10日間） |
| 学習目的 | Spring Boot 3 フルスタック開発の実践 |

<br>

## 📬 Contact

- GitHub: [@Jun-sung](https://github.com/Jun-sung)
- Repository: [SpringBootProject](https://github.com/Jun-sung/SpringBootProject)
