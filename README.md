# Terracraft Portable Recipe UI

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
