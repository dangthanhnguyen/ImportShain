package com.excel.model;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.excel.util.DbConnectionUtil;
import com.excel.util.ImportResult;
import com.excel.repository.ShainDAO;
import com.excel.repository.ShainRepository;

public class ShainExcelImportModel {
    private final ShainRepository shainRepository;

    private static final List<String> EXPECTED_HEADERS = Arrays.asList(
        "社員番号", "氏名（フリガナ）", "氏名", "氏名（英字）", "在籍区分", "部門コード", "性別", "血液型", "生年月日"
    );
    private static final int REQUIRED_COLUMN_COUNT = EXPECTED_HEADERS.size();

    public ShainExcelImportModel() {
        this.shainRepository = new ShainRepository();
    }

    /**
     * Excelファイルを選択し、そのパスを取得する
     *
     * @param stage 現在のウィンドウ
     * @return 選択されたExcelファイルのパス (nullの場合、ファイルが選択されなかった)
     */
    public String selectExcelFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Excelファイルを選択してください");

        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excelファイル (*.xlsx, *.xls)", "*.xlsx", "*.xls")
        );

        File selectedFile = fileChooser.showOpenDialog(stage);

        return (selectedFile != null) ? selectedFile.getAbsolutePath() : null;
    }

    /**
     * インポート処理用のTaskを作成し、実行します。
     * @param filePath インポートするExcelファイルのパス
     * @param importType インポートタイプ (INSERT_ONLY, INSERT_AND_UPDATE, NEW)
     * @param progressCallback 処理中の進捗を通知するためのコールバック
     * @return 実行中のTask
     */
    public Task<ImportResult> executeImport(String filePath, ImportType importType, Consumer<ImportResult> progressCallback) {
        Task<ImportResult> importTask = createImportTask(filePath, importType, progressCallback);

        new Thread(importTask).start();

        return importTask;
    }

    /**
     * インポート用のTaskを作成します。
     * @param filePath インポートするExcelファイルのパス
     * @param importType インポートタイプ
     * @param progressCallback 進捗を通知するためのコールバック
     * @return 作成されたTask
     */
    private Task<ImportResult> createImportTask(String filePath, ImportType importType, Consumer<ImportResult> progressCallback) {
        return new Task<>() {
            @Override
            protected ImportResult call() throws Exception {
                switch (importType) {
                    case INSERT_ONLY:
                        return importInsertOnly(filePath, progressCallback);
                    case INSERT_AND_UPDATE:
                        return importInsertAndUpdate(filePath, progressCallback);
                    case NEW:
                        return importNew(filePath, progressCallback);
                    default:
                        throw new UnsupportedOperationException("インポート方法が選択されていません。");
                }
            }
        };
    }

    /**
     * インポートタイプを定義する列挙型
     */
    public enum ImportType {
        INSERT_ONLY,
        INSERT_AND_UPDATE,
        NEW
    }

    /**
     * Excelファイルから社員情報をインポートします（追加のみ）。
     * 社員番号が重複している場合や既に存在する場合、処理を中止し、エラーを返します。
     * @param filePath Excelファイルのパス
     * @param progressCallback 処理済みのレコード数を通知するためのコールバック
     * @return インポート結果（追加数、更新数、エラーメッセージ）
     * @throws IOException ファイル読み込みエラーが発生した場合
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public ImportResult importInsertOnly(String filePath, Consumer<ImportResult> progressCallback) throws IOException, SQLException {
        List<String> duplicateShainNo = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        ImportResult result = new ImportResult();

        if (!checkExcelFile(filePath)) {
            errorMessages.add("ファイルが正しくありません");
            result.setErrorMessages(errorMessages);
            throw new IOException(buildErrorMessage(errorMessages));
        }

        // Excelシートを読み込む
        Sheet sheet = validateAndLoadSheet(filePath);

        // 社員番号の重複チェック（isCheckで事前にチェック）
        errorMessages.addAll(isCheck(sheet));
        if (!errorMessages.isEmpty()) {
            result.setErrorMessages(errorMessages);
            throw new SQLException(buildErrorMessage(errorMessages));
        }

        // 社員番号がデータベースに存在するかどうかを事前にチェック
        errorMessages.addAll(checkExistingShainNo(sheet));
        if (!errorMessages.isEmpty()) {
            result.setErrorMessages(errorMessages);
            throw new SQLException(buildErrorMessage(errorMessages));
        }

        // すべての行を事前にバリデーション
        List<ShainDAO> shainList = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            ShainDAO newShain = createShainFromRow(row, i + 1, errorMessages);
            if (newShain == null) {
                continue;
            }
            shainList.add(newShain);
            duplicateShainNo.add(newShain.getShainNo());
        }

        // エラーがあれば処理を中止
        if (!errorMessages.isEmpty()) {
            result.setErrorMessages(errorMessages);
            throw new SQLException(buildErrorMessage(errorMessages));
        }

        // エラーがなければデータベースに挿入
        try (Connection conn = DbConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);

            for (ShainDAO shain : shainList) {
                shainRepository.insertShain(shain);
                result.incrementInserted();

                // 進捗を通知
                if (progressCallback != null) {
                    progressCallback.accept(result);
                }
            }

            // 正常に処理が完了した場合のみコミット
            conn.commit();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Excelファイルの読み込みに失敗しました: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 社員番号がデータベースに既に存在するかどうかをチェックします。
     * @param sheet チェックするシート
     * @return エラーメッセージのリスト（エラーがない場合は空リスト）
     * @throws SQLException データベース操作エラーが発生した場合
     */
    private List<String> checkExistingShainNo(Sheet sheet) throws SQLException {
        List<String> errorMessages = new ArrayList<>();
        List<String> shainNos = new ArrayList<>();

        // すべての行から社員番号を取得
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            String shainNo = getCellValue(row.getCell(0)).trim();
            if (!shainNo.isEmpty()) {
                shainNos.add(shainNo);
            }
        }

        // データベースに存在する社員番号をチェック
        for (String shainNo : shainNos) {
            if (shainRepository.isShainExists(shainNo)) {
                errorMessages.add("社員番号 " + shainNo + "が既に存在しています");
            }
        }

        return errorMessages;
    }

    /**
     * Excelファイルから社員情報をインポートします（追加・更新）。
     * 社員番号が既に存在する場合は更新、存在しない場合は追加します。
     * @param filePath Excelファイルのパス
     * @param progressCallback 処理済みのレコード数を通知するためのコールバック
     * @return 処理結果（追加数、更新数）
     * @throws IOException ファイル読み込みエラーが発生した場合
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public ImportResult importInsertAndUpdate(String filePath, Consumer<ImportResult> progressCallback) throws IOException, SQLException {
        List<String> duplicateShainNo = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        ImportResult result = new ImportResult();
        
        if (!checkExcelFile(filePath)) {
            errorMessages.add("ファイルが正しくありません");
            result.setErrorMessages(errorMessages);
            throw new IOException(buildErrorMessage(errorMessages));
        }

        Sheet sheet = validateAndLoadSheet(filePath);

        errorMessages.addAll(isCheck(sheet));
        if (!errorMessages.isEmpty()) {
            throw new SQLException(buildErrorMessage(errorMessages));
        }

        try (Connection conn = DbConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);

            result = processRowsInsertAndUpdate(sheet, duplicateShainNo, errorMessages, progressCallback);

            if (!errorMessages.isEmpty()) {
                throw new SQLException(buildErrorMessage(errorMessages));
            }

            conn.commit();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Excelファイルの読み込みに失敗しました: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Excelファイルから社員情報をインポートします（全件入替）。
     * 既存のデータを全て削除し、新しいデータを挿入します。
     * @param filePath Excelファイルのパス
     * @param progressCallback 処理済みのレコード数を通知するためのコールバック
     * @return 挿入結果（追加数、更新数）
     * @throws IOException ファイル読み込みエラーが発生した場合
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public ImportResult importNew(String filePath, Consumer<ImportResult> progressCallback) throws IOException, SQLException {
        List<String> duplicateShainNo = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        ImportResult result = new ImportResult();

        if (!checkExcelFile(filePath)) {
            errorMessages.add("ファイルが正しくありません");
            result.setErrorMessages(errorMessages);
            throw new IOException(buildErrorMessage(errorMessages));
        }

        Sheet sheet = validateAndLoadSheet(filePath);

        errorMessages.addAll(isCheck(sheet));
        if (!errorMessages.isEmpty()) {
            throw new SQLException(buildErrorMessage(errorMessages));
        }

        try (Connection conn = DbConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);

            shainRepository.deleteAllShain();

            result = processRowsNew(sheet, duplicateShainNo, errorMessages, progressCallback);

            if (!errorMessages.isEmpty()) {
                throw new SQLException(buildErrorMessage(errorMessages));
            }

            conn.commit();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Excelファイルの読み込みに失敗しました: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Excelファイルを開き、シートを検証して返します。
     * @param filePath Excelファイルのパス
     * @return 最初のシート
     * @throws IOException ファイル読み込みエラーが発生した場合、またはファイルが無効な場合
     */
    private Sheet validateAndLoadSheet(String filePath) throws IOException {
        try (FileInputStream file = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("Excelファイルにシートが見つかりません。");
            }

            return sheet;
        }
    }

    /**
     * Excelファイルが正しい形式と構造を持っているかチェックします。
     * @param filePath Excelファイルへのパス
     * @return ファイルが正しい場合はtrue、不正な場合はfalse
     * @throws IOException ファイルの読み込み中にエラーが発生した場合
     */
    private boolean checkExcelFile(String filePath) throws IOException {
        if (!filePath.toLowerCase().endsWith(".xlsx") && !filePath.toLowerCase().endsWith(".xls")) {
            return false;
        }

        try (FileInputStream file = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return false;
            }

            int maxColumns = 0;
            for (Row row : sheet) {
                if (row != null) {
                    int columns = row.getLastCellNum();
                    if (columns > maxColumns) {
                        maxColumns = columns;
                    }
                }
            }
            if (maxColumns < REQUIRED_COLUMN_COUNT) {
                return false;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return false;
            }

            List<String> actualHeaders = new ArrayList<>();
            for (int i = 0; i < REQUIRED_COLUMN_COUNT; i++) {
                String header = getCellValue(headerRow.getCell(i)).trim();
                actualHeaders.add(header);
            }

            for (int i = 0; i < REQUIRED_COLUMN_COUNT; i++) {
                String expectedHeader = EXPECTED_HEADERS.get(i);
                String actualHeader = actualHeaders.get(i);
                if (!expectedHeader.equals(actualHeader)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * シートの各行を処理し、データをデータベースに挿入または更新します（追加・更新）。
     * @param sheet 処理するシート
     * @param duplicateShainNo Excelファイル内で既に出現した社員番号のリスト
     * @param errorMessages エラーメッセージのリスト
     * @param progressCallback 処理済みのレコード数を通知するためのコールバック
     * @return 処理結果（追加数、更新数）
     * @throws SQLException データベース操作エラーが発生した場合
     */
    private ImportResult processRowsInsertAndUpdate(Sheet sheet, List<String> duplicateShainNo, List<String> errorMessages, Consumer<ImportResult> progressCallback) throws SQLException {
        ImportResult result = new ImportResult();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            String shainNo = getCellValue(row.getCell(0)).trim();

            if (duplicateShainNo.contains(shainNo)) {
                errorMessages.add("行 " + (i + 1) + ": 社員番号 " + shainNo + " がExcelファイル内で重複しています");
                continue;
            }

            ShainDAO newShain = createShainFromRow(row, i + 1, errorMessages);
            if (newShain == null) {
                continue;
            }

            if (shainRepository.isShainExists(shainNo)) {
                shainRepository.updateShain(newShain);
                result.incrementUpdated();
            } else {
                shainRepository.insertShain(newShain);
                result.incrementInserted();
            }

            duplicateShainNo.add(shainNo);

            if (progressCallback != null) {
                progressCallback.accept(result);
            }
        }

        return result;
    }

    /**
     * シートの各行を処理し、データをデータベースに挿入します（全件入替）。
     * @param sheet 処理するシート
     * @param duplicateShainNo Excelファイル内で既に出現した社員番号のリスト
     * @param errorMessages エラーメッセージのリスト
     * @param progressCallback 処理済みのレコード数を通知するためのコールバック
     * @return 挿入結果（追加数、更新数）
     * @throws SQLException データベース操作エラーが発生した場合
     */
    private ImportResult processRowsNew(Sheet sheet, List<String> duplicateShainNo, List<String> errorMessages, Consumer<ImportResult> progressCallback) throws SQLException {
        ImportResult result = new ImportResult();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            String shainNo = getCellValue(row.getCell(0)).trim();

            if (duplicateShainNo.contains(shainNo)) {
                errorMessages.add("行 " + (i + 1) + ": 社員番号 " + shainNo + " がExcelファイル内で重複しています");
                continue;
            }

            ShainDAO newShain = createShainFromRow(row, i + 1, errorMessages);
            if (newShain == null) {
                continue;
            }

            shainRepository.insertShain(newShain);
            result.incrementInserted();
            duplicateShainNo.add(shainNo);

            if (progressCallback != null) {
                progressCallback.accept(result);
            }
        }

        return result;
    }

    /**
     * 日付文字列をバリデーションし、必要に応じてエラーメッセージを追加します。
     * @param dateStr 日付文字列
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト
     * @return バリデーションが成功した場合はparseされたDate、失敗した場合はnull
     */
    private static java.util.Date validateAndParseDate(String dateStr, int rowNum, List<String> errorMessages) {
        if (dateStr.isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 生年月日を入力してください");
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d");
        sdf.setLenient(false);
        try {
            java.util.Date parsedDate = sdf.parse(dateStr);

            String formattedBack = sdf.format(parsedDate);
            if (!dateStr.equals(formattedBack)) {
                errorMessages.add("行 " + rowNum + ": 生年月日形式エラー (yyyy/M/d)。値: " + dateStr);
                return null;
            }
            return parsedDate;
        } catch (ParseException e) {
            errorMessages.add("行 " + rowNum + ": 生年月日形式エラー (yyyy/M/d)。値: " + dateStr);
            return null;
        }
    }

    /**
     * 1行のデータをバリデーションします。
     * @param row 処理する行
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト
     * @return バリデーションが成功した場合はtrue、失敗した場合はfalse
     */
    private static boolean validateRow(Row row, int rowNum, List<String> errorMessages) {
        String shainNo = getCellValue(row.getCell(0)).trim();
        String shimeiKana = getCellValue(row.getCell(1)).trim();
        String shimei = getCellValue(row.getCell(2)).trim();
        String shimeiEiji = getCellValue(row.getCell(3)).trim();
        String zaisekiKB = getCellValue(row.getCell(4)).trim();
        String bumonCd = getCellValue(row.getCell(5)).trim();
        String seibetsu = getCellValue(row.getCell(6)).trim();
        String ketsuekiGata = getCellValue(row.getCell(7)).trim();
        String birthDateStr = getCellValue(row.getCell(8)).trim();

        boolean isValid = true;

        // 社員番号のバリデーション
        if (shainNo.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 社員番号を入力してください");
            isValid = false;
        } else if (!shainNo.matches("\\d*")) {
            errorMessages.add("行 " + rowNum + ": 社員番号は数字のみ入力してください");
            isValid = false;
        }
        if (shainNo.length() > 4) {
            errorMessages.add("行 " + rowNum + ": 社員番号は最大4桁の数字で入力してください");
            isValid = false;
        }

        // 氏名（フリガナ）のバリデーション
        if (shimeiKana.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 氏名（フリガナ）を入力してください");
            isValid = false;
        }
        if (shimeiKana.length() > 30) {
            errorMessages.add("行 " + rowNum + ": 氏名（フリガナ）は30文字以内で入力してください");
            isValid = false;
        }

        // 氏名のバリデーション
        if (shimei.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 氏名を入力してください");
            isValid = false;
        }
        if (shimei.length() > 30) {
            errorMessages.add("行 " + rowNum + ": 氏名は30文字以内で入力してください");
            isValid = false;
        }

        // 氏名（英字）のバリデーション
        if (shimeiEiji.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 氏名（英字）を入力してください");
            isValid = false;
        }
        if (shimeiEiji.length() > 40) {
            errorMessages.add("行 " + rowNum + ": 氏名（英字）は40文字以内で入力してください");
            isValid = false;
        }

        // 在籍区分のバリデーション
        if (zaisekiKB.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 在籍区分を入力してください");
            isValid = false;
        }

        // 部門コードのバリデーション
        if (bumonCd.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 部門コードを入力してください");
            isValid = false;
        }

        // 性別のバリデーション
        if (seibetsu.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 性別を入力してください");
            isValid = false;
        }

        // 血液型のバリデーション
        if (ketsuekiGata.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 血液型を入力してください");
            isValid = false;
        }

        // 誕生日のバリデーション
        java.util.Date birthDate = validateAndParseDate(birthDateStr, rowNum, errorMessages);
        if (birthDate == null) {
            isValid = false;
        }

        return isValid;
    }

    /**
     * シートの1行からShainDAOオブジェクトを作成します。
     * @param row 処理する行
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト
     * @return 作成されたShainDAOオブジェクト、エラーが発生した場合はnull
     */
    private ShainDAO createShainFromRow(Row row, int rowNum, List<String> errorMessages) {
        // バリデーションを実行
        if (!validateRow(row, rowNum, errorMessages)) {
            return null;
        }

        // バリデーションが成功した場合、データを取得してShainDAOを作成
        String shainNo = getCellValue(row.getCell(0)).trim();
        String shimeiKana = getCellValue(row.getCell(1)).trim();
        String shimei = getCellValue(row.getCell(2)).trim();
        String shimeiEiji = getCellValue(row.getCell(3)).trim();
        String zaisekiKB = getCellValue(row.getCell(4)).trim();
        String bumonCd = getCellValue(row.getCell(5)).trim();
        String seibetsu = getCellValue(row.getCell(6)).trim();
        String ketsuekiGata = getCellValue(row.getCell(7)).trim();
        String birthDateStr = getCellValue(row.getCell(8)).trim();

        java.util.Date birthDate = validateAndParseDate(birthDateStr, rowNum, errorMessages);

        return new ShainDAO(shainNo, shimeiKana, shimei, shimeiEiji, zaisekiKB, bumonCd, seibetsu, ketsuekiGata, birthDate);
    }

    /**
     * エラーメッセージのリストからエラーメッセージ文字列を作成します。
     * @param errorMessages エラーメッセージのリスト
     * @return フォーマットされたエラーメッセージ文字列
     */
    private String buildErrorMessage(List<String> errorMessages) {
        if (errorMessages.size() > 20) {
            return "データエラー:\n" + String.join("\n", errorMessages.subList(0, 20))
                    + "\n・・・ほか " + (errorMessages.size() - 20) + " 件のエラーがあります";
        } else {
            return "データエラー:\n" + String.join("\n", errorMessages);
        }
    }

    /**
     * 行が空であるかどうかを判定する。
     * 
     * @param row チェックする行
     * @return 行が空の場合は true、それ以外は false
     */
    private static boolean isRowEmpty(Row row) {
        boolean isEmpty = true;

        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && getCellValue(cell).trim().length() > 0) {
                isEmpty = false;
                break;
            }
        }

        return isEmpty;
    }

    /**
     * セルの値を取得する。
     * 
     * @param cell 取得するセル
     * @return セルの値を文字列で返す。セルがnullの場合は空文字を返す。
     */
    private static String getCellValue(Cell cell) {
        if (cell == null)
            return "";

        if (cell.getCellType() == CellType.FORMULA) {
            return getFormulaCellValue(cell);
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy/M/d").format(cell.getDateCellValue());
                } else {
                    return new BigDecimal(cell.getNumericCellValue()).toPlainString();
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /**
     * 数式セルの値を取得する。
     * 
     * @param cell 数式が含まれているセル
     * @return 数式の計算結果を文字列で返す。
     */
    private static String getFormulaCellValue(Cell cell) {
        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        CellValue cellValue = evaluator.evaluate(cell);

        switch (cellValue.getCellType()) {
            case STRING:
                return cellValue.getStringValue();
            case NUMERIC:
                double numericValue = cellValue.getNumberValue();
                return (numericValue % 1 == 0) ? String.valueOf((long) numericValue) : String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cellValue.getBooleanValue());
            default:
                return "";
        }
    }

    /**
     * Excelシートのデータをチェックします。
     * @param sheet チェックするシート
     * @return エラーメッセージのリスト（エラーがない場合は空リスト）
     */
    public static List<String> isCheck(Sheet sheet) {
        List<String> errorMessages = new ArrayList<>();
        Map<String, List<Integer>> shainNoMap = new HashMap<>();

        // 各行をバリデーション
        for (Row row : sheet) {
            if (row == null || row.getRowNum() == 0 || isRowEmpty(row)) {
                continue;
            }

            int rowNum = row.getRowNum() + 1;
            String shainNo = getCellValue(row.getCell(0)).trim();

            // 社員番号の重複チェック用に記録
            if (!shainNo.trim().isEmpty()) {
                shainNoMap.computeIfAbsent(shainNo, k -> new ArrayList<>()).add(rowNum);
            }

            // 行のバリデーション
            validateRow(row, rowNum, errorMessages);
        }

        // 社員番号の重複チェック
        for (Map.Entry<String, List<Integer>> entry : shainNoMap.entrySet()) {
            List<Integer> rows = entry.getValue();
            if (rows.size() > 1) {
                StringBuilder message = new StringBuilder();
                message.append("社員番号 ").append(entry.getKey()).append(" が重複しています: 行 ");
                for (int i = 0; i < rows.size(); i++) {
                    message.append(rows.get(i));
                    if (i < rows.size() - 1) {
                        message.append(" と ");
                    }
                }
                errorMessages.add(message.toString());
            }
        }

        return errorMessages;
    }
}