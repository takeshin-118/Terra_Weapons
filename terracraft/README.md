# Terracraft

Minecraft Java 1.21.11 / Fabric 用の武器・祭壇MODです。

## 導入

`terracraft-0.1.0.jar` と `geckolib-fabric-1.21.11-5.4.2.jar` を、サーバーおよび参加する全クライアントの `mods` フォルダへ置きます。Fabric API 0.141.6+1.21.11 も必要です。

## 現在のコマンド

権限レベル3（管理者）で使用します。

```text
/terracraft weapon permission grant <player> nether_axe
/terracraft weapon permission revoke <player> nether_axe
/terracraft weapon permission check <player> nether_axe
/terracraft weapon permission list <player>
/terracraft config reload
```

権利はオーバーワールドの永続データとしてUUID単位で保存され、ItemStackには保存されません。

## 仮ID変更

正式ワールド開始前に `src/main/java/jp/shint/terracraft/TerracraftMod.java` の登録ID、レシピ、翻訳、モデルを同時に更新します。ワールドを使用した後のID変更では、既存アイテムとブロックを移行するデータ修正が必要です。

## 実装状況

ネザー斧と祭壇コアの仮アセット、権利データ、原木・自然生成葉の一括破壊、5×5祭壇の4方向構造判定、外部JSON設定の初回生成と再読込、供物消費と16tick儀式は実装済みです。携帯クラフトUI、祭壇GUI、GeckoLib BlockEntityアニメーション、権利なし属性差分、データ定義のタグ・BlockState対応、権利連動レシピ解放は未完了です。
