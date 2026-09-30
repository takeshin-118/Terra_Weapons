# Terra_Weapons

Minecraft Java 1.21.11 / Fabric 用の Terracraft 本体と携帯クラフト追加MODを管理するリポジトリです。

## 構成

- `terracraft/`: MOD本体。武器、祭壇と儀式、権限管理、伐採処理、設定、コマンド、携帯クラフト、素材を含みます。
- ルートの `src/`: 携帯クラフトのレシピブック追加MODと画像素材です。
- `terracraft/docs/`: 本体の設定編集ガイドです。

本体と追加MODは別々のJARとしてビルドします。Java 21以上を用意し、リポジトリのルートで実行してください。

```powershell
.\terracraft\gradlew.bat -p terracraft build
.\gradlew.bat build
```

成果物は `terracraft/build/libs/terracraft-0.1.0.jar` と `build/libs/terracraft-portable-recipe-ui-0.1.0.jar` です。Fabric Loader、Fabric API、GeckoLib が必要です。本体の機能・設定については `terracraft/src/` と `terracraft/docs/` を参照してください。

## 携帯クラフト追加MOD

Terracraft の携帯クラフト用 Fabric クライアント UI です。`R`（設定で変更可）で開きます。

## UI

- 最初の画面は「書斎の本棚」。木製書棚、左右のランタン、中央の「テラクラのレシピ」をクリックして詳細を開きます。
- 詳細は見開き本です。左ページにレシピ名・完成品プレビュー・戻るボタン、右ページに素材・所持数/必要数・解放条件・作成ボタンを表示します。
- 足りない素材と未解放条件は赤色です。すべて満たすと「作成する」が金色になり、クリックできます。

画面はクライアント側の表示と入力だけを担当します。所持素材、解放状態、消費、完成品の付与は既存のサーバー側 `PortableRecipeNetworking` が引き続き検証・処理します。

## ビルド

Java 21 を用意して、このディレクトリで実行します。

```powershell
.\gradlew.bat build
```

成果物は `build\libs\terracraft-portable-recipe-ui-<version>.jar` に生成されます。実行環境には Terracraft 本体と Fabric API が必要です。
