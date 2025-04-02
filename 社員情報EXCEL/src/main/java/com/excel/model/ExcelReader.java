package com.excel.model;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.excel.repository.ShainDAO;
import com.excel.util.ImportResult;

/**
 * Excelファイルの読み込みと検証を担当するクラス。
 */
public class ExcelReader {
    private static final List<String> EXPECTED_HEADERS = Arrays.asList(
        "社員番号", "氏名（フリガナ）", "氏名", "氏名（英字）", "在籍区分", "部門コード", "性別", "血液型", "生年月日"
    );
    private static final int REQUIRED_COLUMN_COUNT = EXPECTED_HEADERS.size();

    /**
     * Excelファイルの検証を行います。
     *
     * @param filePath Excelファイルのパス
     * @return 検証結果（エラーがない場合はvalid、エラーがあればエラーメッセージを含む）
     * @throws IOException ファイル読み込みエラーが発生した場合
     */
    public ImportResult validateFile(String filePath) throws IOException {
        List<String> errorMessages = new ArrayList<>();
        ImportResult result = new ImportResult();

        // ステップ1: ファイル拡張子のチェック
        if (!validateFileExtension(filePath, errorMessages)) {
            return setErrorResult(result, errorMessages);
        }

        try (FileInputStream file = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(file)) {

            // ステップ2: シートの存在チェック
            Sheet sheet = validateSheetExistence(workbook, errorMessages);
            if (sheet == null) {
                return setErrorResult(result, errorMessages);
            }

            // ステップ3: シートの列数をチェック
            if (!validateColumnCount(sheet, errorMessages)) {
                return setErrorResult(result, errorMessages);
            }

            // ステップ4: ヘッダー行の存在チェック
            Row headerRow = validateHeaderRow(sheet, errorMessages);
            if (headerRow == null) {
                return setErrorResult(result, errorMessages);
            }

            // ステップ5: ヘッダー内容のチェック
            if (!validateHeaderContent(headerRow, errorMessages)) {
                return setErrorResult(result, errorMessages);
            }

            return result;
        } catch (Exception e) {
            errorMessages.add("Excelファイルの読み込みに失敗しました: " + e.getMessage());
            return setErrorResult(result, errorMessages);
        }
    }
    
    /**
     * ファイル拡張子の検証を行います。
     *
     * @param filePath ファイルパス
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     * @return 検証が成功した場合はtrue、失敗した場合はfalse
     */
    private boolean validateFileExtension(String filePath, List<String> errorMessages) {
        if (!checkFileExtension(filePath)) {
            errorMessages.add("ファイル拡張子が正しくありません（.xlsxまたは.xlsが必要です）");
            return false;
        }
        return true;
    }
    
    /**
     * シートの存在を検証します。
     *
     * @param workbook Excelワークブック
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     * @return 検証が成功した場合はSheetオブジェクト、失敗した場合はnull
     */
    private Sheet validateSheetExistence(Workbook workbook, List<String> errorMessages) {
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            errorMessages.add("Excelファイルにシートが見つかりません");
            return null;
        }
        return sheet;
    }
    
    /**
     * シートの列数を検証します。
     *
     * @param sheet 検証するシート
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     * @return 検証が成功した場合はtrue、失敗した場合はfalse
     */
    private boolean validateColumnCount(Sheet sheet, List<String> errorMessages) {
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
            errorMessages.add("シートの列数が不足しています（必要な列数: " + REQUIRED_COLUMN_COUNT + "）");
            return false;
        }
        return true;
    }
    
    /**
     * ヘッダー行の存在を検証します。
     *
     * @param sheet 検証するシート
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     * @return 検証が成功した場合はヘッダー行、失敗した場合はnull
     */
    private Row validateHeaderRow(Sheet sheet, List<String> errorMessages) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            errorMessages.add("ヘッダー行が見つかりません");
            return null;
        }
        return headerRow;
    }
    
    /**
     * ヘッダー内容の検証を行います。
     *
     * @param headerRow ヘッダー行
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     * @return 検証が成功した場合はtrue、失敗した場合はfalse
     */
    private boolean validateHeaderContent(Row headerRow, List<String> errorMessages) {
        if (!checkHeaders(headerRow)) {
            errorMessages.add(buildHeaderErrorMessage());
            return false;
        }
        return true;
    }
    
    /**
     * ヘッダーエラーメッセージを作成します。
     *
     * @return フォーマットされたヘッダーエラーメッセージ
     */
    private String buildHeaderErrorMessage() {
        StringBuilder headerMessage = new StringBuilder("ヘッダー内容が正しくありません。期待されるヘッダー:\n");
        int headersPerLine = 3; // 1行に表示するヘッダーの数
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            headerMessage.append(EXPECTED_HEADERS.get(i));
            if (i < EXPECTED_HEADERS.size() - 1) {
                headerMessage.append(", ");
            }
            if ((i + 1) % headersPerLine == 0 || i == EXPECTED_HEADERS.size() - 1) {
                headerMessage.append("\n");
            }
        }
        return headerMessage.toString();
    }
    
    /**
     * エラーメッセージを設定し、結果を返します。
     *
     * @param result 検証結果
     * @param errorMessages エラーメッセージのリスト
     * @return エラーが設定された検証結果
     */
    private ImportResult setErrorResult(ImportResult result, List<String> errorMessages) {
        result.setErrorMessages(errorMessages);
        result.setValid(false);
        return result;
    }
    
    

    /**
     * Excelファイルを開き、最初のシートを読み込みます。
     *
     * @param filePath Excelファイルのパス
     * @return 最初のシート
     * @throws IOException ファイル読み込みエラーが発生した場合、またはファイルが無効な場合
     */
    public Sheet loadSheet(String filePath) throws IOException {
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
     * Excelシートから社員情報を読み込み、バリデーションを行います。
     *
     * @param sheet 読み込むExcelシート
     * @param validator バリデーションを行うShainValidator
     * @return バリデーション済みの社員情報リスト
     * @throws IOException 読み込みエラーが発生した場合
     */
    public List<ShainDAO> readAndValidateShainList(Sheet sheet, ShainValidator validator) throws IOException {
        List<ShainDAO> shainList = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            int rowNum = i + 1;
            // 行のバリデーション
            errorMessages.addAll(validator.validateRow(row, rowNum));
            if (!errorMessages.isEmpty()) {
                throw new IOException(buildErrorMessage(errorMessages));
            }

            // ShainDAOの作成
            ShainDAO newShain = createShainFromRow(row, rowNum, validator);
            if (newShain != null) {
                shainList.add(newShain);
            }
        }

        return shainList;
    }

    /**
     * シートの1行からShainDAOオブジェクトを作成します。
     *
     * @param row 処理する行
     * @param rowNum 行番号（エラーメッセージ用）
     * @param validator バリデーションを行うShainValidator
     * @return 作成されたShainDAOオブジェクト、エラーが発生した場合はnull
     */
    private ShainDAO createShainFromRow(Row row, int rowNum, ShainValidator validator) {
        RowData rowData = new RowData(row);
        java.util.Date birthDate = validator.validateAndParseDate(rowData.birthDateStr, rowNum, new ArrayList<>());

        return new ShainDAO(rowData.shainNo, rowData.shimeiKana, rowData.shimei, rowData.shimeiEiji,
                rowData.zaisekiKB, rowData.bumonCd, rowData.seibetsu, rowData.ketsuekiGata, birthDate);
    }

    /**
     * ファイルの拡張子が正しいかチェックします。
     *
     * @param filePath ファイルパス
     * @return 拡張子が.xlsxまたは.xlsの場合はtrue、それ以外の場合はfalse
     */
    private boolean checkFileExtension(String filePath) {
        return filePath.toLowerCase().endsWith(".xlsx") || filePath.toLowerCase().endsWith(".xls");
    }

    /**
     * Excelシートのヘッダーが期待される形式と一致するかチェックします。
     *
     * @param headerRow ヘッダー行
     * @return ヘッダーが正しい場合はtrue、不正な場合はfalse
     */
    private boolean checkHeaders(Row headerRow) {
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
    }

    /**
     * 行が空であるかどうかを判定します。
     *
     * @param row チェックする行
     * @return 行が空の場合はtrue、それ以外はfalse
     */
    public boolean isRowEmpty(Row row) {
        boolean isEmpty = true;

        // 各セルをチェックし、値が存在する場合は空ではないと判定
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
     * セルの値を取得します。
     *
     * @param cell 取得するセル
     * @return セルの値を文字列で返します。セルがnullの場合は空文字を返します。
     */
    public String getCellValue(Cell cell) {
        // セルがnullの場合は空文字を返す
        if (cell == null)
            return "";

        // 数式セルの場合は特別な処理を行う
        if (cell.getCellType() == CellType.FORMULA) {
            return getFormulaCellValue(cell);
        }

        // セルの型に応じて値を変換
        switch (cell.getCellType()) {
            case STRING:
                // 文字列セルの場合はそのまま返す
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // 数値セルの場合
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 日付形式の場合はyyyy/M/d形式に変換
                    return new SimpleDateFormat("yyyy/M/d").format(cell.getDateCellValue());
                } else {
                    // 通常の数値の場合は文字列に変換
                    return new BigDecimal(cell.getNumericCellValue()).toPlainString();
                }
            case BOOLEAN:
                // ブール値の場合は文字列に変換
                return String.valueOf(cell.getBooleanCellValue());
            default:
                // その他の場合は空文字を返す
                return "";
        }
    }

    /**
     * 数式セルの値を取得します。
     *
     * @param cell 数式が含まれているセル
     * @return 数式の計算結果を文字列で返します。
     */
    private String getFormulaCellValue(Cell cell) {
        // 数式を評価するためのEvaluatorを作成
        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        CellValue cellValue = evaluator.evaluate(cell);

        // 評価結果の型に応じて値を変換
        switch (cellValue.getCellType()) {
            case STRING:
                // 文字列の場合はそのまま返す
                return cellValue.getStringValue();
            case NUMERIC:
                // 数値の場合は小数点以下を考慮して変換
                double numericValue = cellValue.getNumberValue();
                return (numericValue % 1 == 0) ? String.valueOf((long) numericValue) : String.valueOf(numericValue);
            case BOOLEAN:
                // ブール値の場合は文字列に変換
                return String.valueOf(cellValue.getBooleanValue());
            default:
                // その他の場合は空文字を返す
                return "";
        }
    }

    /**
     * エラーメッセージのリストからエラーメッセージ文字列を作成します。
     *
     * @param errorMessages エラーメッセージのリスト
     * @return フォーマットされたエラーメッセージ文字列
     */
    private String buildErrorMessage(List<String> errorMessages) {
        // エラーメッセージが20件を超える場合、最初の20件のみ表示し、残りを省略表示
        if (errorMessages.size() > 20) {
            return "データエラー:\n" + String.join("\n", errorMessages.subList(0, 20))
                    + "\n・・・ほか " + (errorMessages.size() - 20) + " 件のエラーがあります";
        } else {
            return "データエラー:\n" + String.join("\n", errorMessages);
        }
    }

    /**
     * Excelシートの1行からデータを抽出するための内部クラス。
     */
    private class RowData {
        String shainNo;
        String shimeiKana;
        String shimei;
        String shimeiEiji;
        String zaisekiKB;
        String bumonCd;
        String seibetsu;
        String ketsuekiGata;
        String birthDateStr;

        /**
         * コンストラクタ。行からデータを抽出します。
         *
         * @param row 抽出する行
         */
        RowData(Row row) {
            // インスタンスメソッドを使ってセルの値を取得
            this.shainNo = getCellValue(row.getCell(0)).trim();
            this.shimeiKana = getCellValue(row.getCell(1)).trim();
            this.shimei = getCellValue(row.getCell(2)).trim();
            this.shimeiEiji = getCellValue(row.getCell(3)).trim();
            this.zaisekiKB = getCellValue(row.getCell(4)).trim();
            this.bumonCd = getCellValue(row.getCell(5)).trim();
            this.seibetsu = getCellValue(row.getCell(6)).trim();
            this.ketsuekiGata = getCellValue(row.getCell(7)).trim();
            this.birthDateStr = getCellValue(row.getCell(8)).trim();
        }
    }
}