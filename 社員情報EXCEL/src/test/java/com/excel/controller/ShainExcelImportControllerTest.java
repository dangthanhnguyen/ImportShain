package com.excel.controller;

import com.excel.util.ImportResult;
import com.excel.model.ShainExcelImportViewModel;
import com.excel.model.ShainExcelImportViewModel.ImportType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * このクラスは、社員Excelインポート処理を管理するコントローラの動作をテストします。
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ApplicationExtension.class)
public class ShainExcelImportControllerTest {

    @InjectMocks
    private ShainExcelImportController controller;

    @Mock
    private ShainExcelImportViewModel model;

    @Mock
    private Button endButton;

    @Mock
    private TextField folderPathField;

    @Mock
    private Button selectFolderButton;

    @Mock
    private RadioButton insertImport;

    @Mock
    private RadioButton insertAndUpdateImport;

    @Mock
    private RadioButton newImport;

    @Mock
    private ToggleGroup importMethodGroup;

    @Mock
    private Button executeButton;

    @Mock
    private Stage stage;

    @TempDir
    Path tempDir;

    /**
     * TestFXの初期化処理。
     * JavaFXアプリケーションをテスト用に初期化します。
     *
     * @param stage テスト用のステージ
     */
    @Start
    private void start(Stage stage) {
        this.stage = stage;
    }

    /**
     * テスト全体の初期化処理。
     * JavaFXプラットフォームを初期化します。
     */
    @BeforeAll
    static void initToolkit() {
        Platform.startup(() -> {});
    }

    /**
     * 各テストメソッドの前にコントローラを初期化します。
     * ここではモックの基本的な設定のみを行い、個別のスタブは各テストケースで設定します。
     */
    @BeforeEach
    void setUp() throws Exception {
        setField(controller, "model", model);
        setField(controller, "folderPathField", folderPathField);
        setField(controller, "selectFolderButton", selectFolderButton);
        setField(controller, "endButton", endButton);
        setField(controller, "executeButton", executeButton);
        setField(controller, "insertImport", insertImport);
        setField(controller, "insertAndUpdateImport", insertAndUpdateImport);
        setField(controller, "newImport", newImport);
        setField(controller, "importMethodGroup", importMethodGroup);
    }

    /**
     * リフレクションを使用してフィールドに値を割り当てるヘルパー メソッドです。
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * ファイルが選択され、テキストフィールドにパスが設定されることを検証します。
     */
    @Test
    void testHandleSelectExcelFile_FileSelected_SetsPath() {
        // 必要なスタブを設定
        when(selectFolderButton.getScene()).thenReturn(mock(javafx.scene.Scene.class));
        when(selectFolderButton.getScene().getWindow()).thenReturn(stage);

        String selectedFilePath = "path/to/file.xlsx";
        when(model.selectExcelFile(stage)).thenReturn(selectedFilePath);

        controller.handleSelectExcelFile();

        verify(folderPathField).setText(selectedFilePath);
    }

    /**
     * ファイルが選択されなかった場合、テキストフィールドが更新されないことを検証します。
     */
    @Test
    void testHandleSelectExcelFile_NoFileSelected_DoesNotSetPath() {
        // 必要なスタブを設定
        when(selectFolderButton.getScene()).thenReturn(mock(javafx.scene.Scene.class));
        when(selectFolderButton.getScene().getWindow()).thenReturn(stage);

        when(model.selectExcelFile(stage)).thenReturn(null);

        controller.handleSelectExcelFile();

        verify(folderPathField, never()).setText(anyString());
    }

    /**
     * ファイルパスが有効で、ユーザーが確認ダイアログで「はい」を選択した場合にインポート処理が実行されることを検証します。
     */
    @Test
    void testHandleExecuteAction_ValidPathAndConfirmed_ExecutesImport() throws IOException {
        Path tempFile = tempDir.resolve("file.xlsx");
        Files.createFile(tempFile);
        String filePath = tempFile.toString();
        when(folderPathField.getText()).thenReturn(filePath);

        ShainExcelImportController spyController = spy(controller);
        doReturn(true).when(spyController).showConfirmationDialog();
        doNothing().when(spyController).executeImport(anyString());

        spyController.handleExecuteAction();

        verify(spyController).executeImport(filePath);
    }

    /**
     * ユーザーが確認ダイアログで「いいえ」を選択した場合、インポート処理が実行されないことを検証します。
     */
    @Test
    void testHandleExecuteAction_ValidPathButNotConfirmed_DoesNotExecuteImport() throws IOException {
        Path tempFile = tempDir.resolve("file.xlsx");
        Files.createFile(tempFile);
        String filePath = tempFile.toString();
        when(folderPathField.getText()).thenReturn(filePath);

        ShainExcelImportController spyController = spy(controller);
        doReturn(false).when(spyController).showConfirmationDialog();

        spyController.handleExecuteAction();

        verify(spyController, never()).executeImport(anyString());
    }

    /**
     * ファイルパスが有効な場合にtrueが返されることを検証します。
     */
    @Test
    void testValidateFilePath_ValidPath_ReturnsTrue() throws IOException {
        Path tempFile = tempDir.resolve("file.xlsx");
        Files.createFile(tempFile);
        String filePath = tempFile.toString();

        boolean result = controller.validateFilePath(filePath);

        assertTrue(result);
    }

    /**
     * ファイルパスが空の場合に警告が表示され、falseが返されることを検証します。
     */
    @Test
    void testValidateFilePath_EmptyPath_ShowsAlertAndReturnsFalse() {
        String filePath = "";

        ShainExcelImportController spyController = spy(controller);
        doNothing().when(spyController).showAlert(any(), anyString(), anyString());

        boolean result = spyController.validateFilePath(filePath);

        assertFalse(result);
        verify(spyController).showAlert(Alert.AlertType.WARNING, "警告", "取込ファイルが存在しません。");
    }

    /**
     * ファイルが存在しない場合に警告が表示され、falseが返されることを検証します。
     */
    @Test
    void testValidateFilePath_NonExistentFile_ShowsAlertAndReturnsFalse() {
        String filePath = tempDir.resolve("nonexistent.xlsx").toString();

        ShainExcelImportController spyController = spy(controller);
        doNothing().when(spyController).showAlert(any(), anyString(), anyString());

        boolean result = spyController.validateFilePath(filePath);

        assertFalse(result);
        verify(spyController).showAlert(Alert.AlertType.WARNING, "警告", "取込ファイルが存在しません。");
    }

    /**
     * 追加のみのインポートが選択された場合に正しいインポートタイプが返されることを検証します。
     */
    @Test
    void testDetermineImportType_InsertOnlySelected_ReturnsInsertOnly() {
        // 必要なスタブを設定
        when(insertImport.isSelected()).thenReturn(true);

        ImportType result = controller.determineImportType();

        assertEquals(ImportType.INSERT_ONLY, result);
    }

    /**
     * インポートタイプが選択されていない場合にエラーが表示され、nullが返されることを検証します。
     */
    @Test
    void testDetermineImportType_NoSelection_ShowsErrorAndReturnsNull() {
        // 必要なスタブを設定
        when(insertImport.isSelected()).thenReturn(false);
        when(insertAndUpdateImport.isSelected()).thenReturn(false);
        when(newImport.isSelected()).thenReturn(false);

        ShainExcelImportController spyController = spy(controller);
        doNothing().when(spyController).showAlert(any(), anyString(), anyString());

        ImportType result = spyController.determineImportType();

        assertNull(result);
        verify(spyController).showAlert(Alert.AlertType.ERROR, "エラー", "インポート方法が選択されていません。");
    }

    /**
     * インポートが成功し、エラーがない場合に成功メッセージが表示されることを検証します。
     */
    @Test
    void testHandleImportSuccess_NoErrors_ShowsSuccessMessage() {
        Task<ImportResult> importTask = mock(Task.class);
        Dialog<Void> progressDialog = mock(Dialog.class);
        ImportResult result = new ImportResult();
        result.setInserted(5);
        result.setUpdated(3);
        when(importTask.getValue()).thenReturn(result);

        ShainExcelImportController spyController = spy(controller);
        doNothing().when(spyController).showAlert(any(), anyString(), anyString());

        spyController.handleImportSuccess(importTask, progressDialog);

        verify(progressDialog).close();
        verify(spyController).showAlert(eq(Alert.AlertType.INFORMATION), eq("完了"), contains("追加: 5 件\n更新: 3 件\n合計: 8 件"));
    }

    /**
     * インポートが成功したがエラーがある場合にエラーメッセージが表示されることを検証します。
     */
    @Test
    void testHandleImportSuccess_WithErrors_ShowsErrorMessage() {
        Task<ImportResult> importTask = mock(Task.class);
        Dialog<Void> progressDialog = mock(Dialog.class);
        ImportResult result = new ImportResult();
        result.setErrorMessages(Arrays.asList("エラー1", "エラー2"));
        when(importTask.getValue()).thenReturn(result);

        ShainExcelImportController spyController = spy(controller);
        doNothing().when(spyController).showAlert(any(), anyString(), anyString());

        spyController.handleImportSuccess(importTask, progressDialog);

        verify(progressDialog).close();
        verify(spyController).showAlert(eq(Alert.AlertType.ERROR), eq("エラー"), contains("以下のエラーが発生しました:\nエラー1\nエラー2"));
    }

    /**
     * インポートが失敗した場合にエラーメッセージが表示されることを検証します。
     */
    @Test
    void testHandleImportFailure_ShowsErrorMessage() {
        Task<ImportResult> importTask = mock(Task.class);
        Dialog<Void> progressDialog = mock(Dialog.class);
        when(importTask.getException()).thenReturn(new RuntimeException("インポートエラー"));
        when(importTask.getValue()).thenReturn(null);

        ShainExcelImportController spyController = spy(controller);
        doNothing().when(spyController).showAlert(any(), anyString(), anyString());

        spyController.handleImportFailure(importTask, progressDialog);

        verify(progressDialog).close();
        verify(spyController).showAlert(eq(Alert.AlertType.ERROR), eq("エラー"), eq("インポートエラー"));
    }
}