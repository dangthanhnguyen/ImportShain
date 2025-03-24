package com.excel.controller;

import com.excel.util.ImportResult;
import com.excel.model.ShainExcelImportModel;
import com.excel.model.ShainExcelImportModel.ImportType;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

public class ShainExcelImportController {

    // 社員Excelインポートモデルのインスタンス
    private final ShainExcelImportModel model = new ShainExcelImportModel();

    @FXML
    private Button endButton; // 終了ボタン

    @FXML
    private TextField folderPathField; // 選択したExcelファイルのパスを表示するテキストフィールド

    @FXML
    private Button selectFolderButton; // Excelファイルを選択するボタン

    @FXML
    private RadioButton insertImport; // 追加のみのインポートを選択するラジオボタン

    @FXML
    private RadioButton insertAndUpdateImport; // 追加・更新のインポートを選択するラジオボタン

    @FXML
    private RadioButton newImport; // 全件入替のインポートを選択するラジオボタン

    @FXML
    private ToggleGroup importMethodGroup; // インポート方法を選択するためのラジオボタングループ

    @FXML
    private Button executeButton; // 実行ボタン

    /**
     * コントローラの初期化メソッド。
     * 各ボタンにイベントハンドラを設定します。
     */
    @FXML
    private void initialize() {
        // 終了ボタンのクリックイベントを設定
        endButton.setOnAction(event -> closeWindow());

        // 参照ボタンのクリックイベントを設定（Excelファイル選択）
        selectFolderButton.setOnAction(event -> handleSelectExcelFile());

        // 実行ボタンのクリックイベントを設定（インポート処理開始）
        executeButton.setOnAction(event -> handleExecuteAction());
    }

    /**
     * 現在のウィンドウを閉じます。
     */
    private void closeWindow() {
        Stage stage = (Stage) endButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Excelファイルを選択し、選択したファイルパスをUIに反映します。
     */
    private void handleSelectExcelFile() {
        Stage stage = (Stage) selectFolderButton.getScene().getWindow();
        // モデルからファイル選択ダイアログを表示し、選択されたファイルパスを取得
        String selectedFilePath = model.selectExcelFile(stage);

        // 選択されたファイルパスをテキストフィールドに表示
        if (selectedFilePath != null) {
            folderPathField.setText(selectedFilePath);
        }
    }

    /**
     * 実行ボタンのクリックイベントを処理します。
     * 確認ダイアログを表示し、ユーザーの選択に応じてインポート処理を実行します。
     */
    @FXML
    private void handleExecuteAction() {
        String filePath = folderPathField.getText().trim();

        // ファイルパスが空の場合、警告を表示
        if (filePath.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "警告", "実行する前にExcelファイルを選択してください。");
            return;
        }

        // ファイルが存在しない、またはファイルでない場合、警告を表示
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            showAlert(Alert.AlertType.WARNING, "警告", "実行する前に有効な Excel ファイルを選択してください。");
            return;
        }

        // 確認ダイアログを表示
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("確認");
        confirmationAlert.setHeaderText("インポートを実行しますか？");
        confirmationAlert.setContentText("この操作は元に戻せません。よろしいですか？");

        // 確認ダイアログのボタンを「はい」と「いいえ」に設定
        ButtonType buttonTypeYes = new ButtonType("はい");
        ButtonType buttonTypeNo = new ButtonType("いいえ");
        confirmationAlert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

        // ユーザーの選択を待つ
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        // ユーザーが「はい」を選択した場合、インポート処理を実行
        if (result.isPresent() && result.get() == buttonTypeYes) {
            executeImport(filePath);
        }
        // ユーザーが「いいえ」を選択した場合、何もしない
    }

    /**
     * インポート処理を実行します。
     * 処理中にプログレスインジケータを表示し、追加および更新のレコード数をリアルタイムで更新します。
     * @param filePath インポートするExcelファイルのパス
     */
    private void executeImport(String filePath) {
        // 処理中ダイアログを作成
        Dialog<Void> progressDialog = createProgressDialog();

        // 選択されたインポートタイプを判定
        ImportType importType = determineImportType();
        if (importType == null) {
            // インポートタイプが選択されていない場合、ダイアログを閉じて終了
            progressDialog.close();
            return;
        }

        // 処理中ダイアログを表示
        progressDialog.show();

        // インポート処理を実行し、結果を処理
        runImportAndHandleResult(filePath, importType, progressDialog);
    }

    /**
     * 処理中ダイアログを作成します。
     * プログレスインジケータと追加・更新レコード数を表示するラベルを含みます。
     * @return 設定済みのダイアログ
     */
    private Dialog<Void> createProgressDialog() {
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle("処理中");
        progressDialog.setHeaderText("処理中...");

        // ダイアログのサイズを設定（幅400px、高さ300px）
        progressDialog.getDialogPane().setPrefSize(400, 300);

        // プログレスインジケータ（回転アイコン）を作成し、サイズを大きく設定
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(100, 100); // サイズを100x100pxに設定

        // 追加および更新レコード数を表示するラベルを作成
        Label insertLabel = new Label("追加: 0 件");
        Label updateLabel = new Label("更新: 0 件");

        // ラベルのフォントサイズを大きく設定
        insertLabel.setStyle("-fx-font-size: 18px;"); // フォントサイズ18px
        updateLabel.setStyle("-fx-font-size: 18px;"); // フォントサイズ18px

        // ラベルをダイアログに保存（後の処理で使用）
        progressDialog.getDialogPane().setUserData(new Label[]{insertLabel, updateLabel});

        // ダイアログのレイアウトを作成（間隔20pxで配置）
        VBox dialogContent = new VBox(20, progressIndicator, insertLabel, updateLabel);
        dialogContent.setAlignment(javafx.geometry.Pos.CENTER);
        progressDialog.getDialogPane().setContent(dialogContent);

        // ダイアログをモーダルに設定（メインウィンドウとのインタラクションをブロック）
        progressDialog.initModality(Modality.APPLICATION_MODAL);
        progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        return progressDialog;
    }

    /**
     * 選択されたラジオボタンに基づいてインポートタイプを判定します。
     * @return インポートタイプ（選択されていない場合はnull）
     */
    private ImportType determineImportType() {
        if (insertImport.isSelected()) {
            return ImportType.INSERT_ONLY;
        } else if (insertAndUpdateImport.isSelected()) {
            return ImportType.INSERT_AND_UPDATE;
        } else if (newImport.isSelected()) {
            return ImportType.NEW;
        } else {
            // インポートタイプが選択されていない場合、エラーを表示
            showAlert(Alert.AlertType.ERROR, "エラー", "インポート方法が選択されていません。");
            return null;
        }
    }

    /**
     * モデルからexecuteImportメソッドを呼び出し、インポート処理の結果を処理します。
     * @param filePath インポートするExcelファイルのパス
     * @param importType インポートタイプ
     * @param progressDialog 処理中ダイアログ（処理完了時に閉じる）
     */
    private void runImportAndHandleResult(String filePath, ImportType importType, Dialog<Void> progressDialog) {
        // ダイアログからラベルを取得
        Label[] labels = (Label[]) progressDialog.getDialogPane().getUserData();
        Label insertLabel = labels[0];
        Label updateLabel = labels[1];

        // モデルからインポートタスクを取得し、進捗をリアルタイムで更新
        Task<ImportResult> importTask = model.executeImport(filePath, importType, importResult -> {
            // JavaFXスレッドでUIを更新
            Platform.runLater(() -> {
                insertLabel.setText("追加: " + importResult.getInsertedCount() + " 件");
                updateLabel.setText("更新: " + importResult.getUpdatedCount() + " 件");
            });
        });

        // タスクが成功した場合の処理
        importTask.setOnSucceeded(event -> {
            progressDialog.close();
            ImportResult result = importTask.getValue();
            StringBuilder message = new StringBuilder();

            // エラーメッセージがある場合、エラーを表示
            if (result.getErrorMessages() != null && !result.getErrorMessages().isEmpty()) {
                message.append("以下のエラーが発生しました:\n");
                message.append(String.join("\n", result.getErrorMessages()));
                showAlert(Alert.AlertType.ERROR, "エラー", message.toString());
            } else {
                // エラーがない場合、処理結果を表示
                message.append("処理が完了しました。\n")
                       .append("追加: ").append(result.getInsertedCount()).append(" 件\n")
                       .append("更新: ").append(result.getUpdatedCount()).append(" 件\n")
                       .append("合計: ").append(result.getTotalProcessed()).append(" 件");
                showAlert(Alert.AlertType.INFORMATION, "完了", message.toString());
            }
        });

        // タスクが失敗した場合の処理
        importTask.setOnFailed(event -> {
            progressDialog.close();
            Throwable e = importTask.getException();
            StringBuilder message = new StringBuilder();

            // 外れ値からエラーメッセージを取得
            message.append(e.getMessage());

            // ImportResultからエラーメッセージを取得（可能であれば）
            try {
                ImportResult result = importTask.getValue();
                if (result != null && result.getErrorMessages() != null && !result.getErrorMessages().isEmpty()) {
                    message.append("\n\n詳細なエラー:\n");
                    message.append(String.join("\n", result.getErrorMessages()));
                }
            } catch (Exception ex) {
                // タスクが失敗した場合、結果を取得できない可能性がある
                message.append("\nエラーの詳細を取得できませんでした。");
            }

            showAlert(Alert.AlertType.ERROR, "エラー", message.toString());
        });
    }

    /**
     * アラートダイアログを表示します。
     * @param type アラートの種類（情報、エラー、警告など）
     * @param title アラートのタイトル
     * @param message 表示するメッセージ
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}