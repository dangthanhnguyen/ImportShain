package com.excel.model;

import org.apache.poi.ss.usermodel.Row;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 社員情報のバリデーションを行うクラス。
 */
public class ShainValidator {
    private final ExcelReader excelReader;

    /**
     * コンストラクタ。
     */
    public ShainValidator() {
        this.excelReader = new ExcelReader();
    }

    /**
     * シートの1行をバリデーションします。
     *
     * @param row 処理する行
     * @param rowNum 行番号（エラーメッセージ用）
     * @return エラーメッセージのリスト（エラーがない場合は空リスト）
     */
    public List<String> validateRow(Row row, int rowNum) {
        List<String> errorMessages = new ArrayList<>();
        RowData rowData = new RowData(row);

        // 各列のバリデーションを実行
        validateShainNo(rowData.shainNo, rowNum, errorMessages);
        validateShimeiKana(rowData.shimeiKana, rowNum, errorMessages);
        validateShimei(rowData.shimei, rowNum, errorMessages);
        validateShimeiEiji(rowData.shimeiEiji, rowNum, errorMessages);
        validateZaisekiKB(rowData.zaisekiKB, rowNum, errorMessages);
        validateBumonCd(rowData.bumonCd, rowNum, errorMessages);
        validateSeibetsu(rowData.seibetsu, rowNum, errorMessages);
        validateKetsuekiGata(rowData.ketsuekiGata, rowNum, errorMessages);

        // 生年月日のバリデーション
        validateAndParseDate(rowData.birthDateStr, rowNum, errorMessages);

        return errorMessages;
    }

    /**
     * 社員番号のバリデーションを行います。
     *
     * @param shainNo 社員番号
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     */
    private void validateShainNo(String shainNo, int rowNum, List<String> errorMessages) {
        // 空欄チェック
        if (shainNo.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 社員番号未入力");
        } else if (!shainNo.matches("\\d*")) {
            // 数字のみであるかチェック
            errorMessages.add("行 " + rowNum + ": 社員番号が数字でない");
        }
        // 長さチェック（最大4桁）
        if (shainNo.length() > 4) {
            errorMessages.add("行 " + rowNum + ": 社員番号の桁数オーバー");
        }
    }

    /**
     * 氏名（フリガナ）のバリデーションを行います。
     *
     * @param shimeiKana 氏名（フリガナ）
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     */
    private void validateShimeiKana(String shimeiKana, int rowNum, List<String> errorMessages) {
        // 空欄チェック
        if (shimeiKana.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 氏名（フリガナ）未入力");
        }
        // 長さチェック（最大30文字）
        if (shimeiKana.length() > 30) {
            errorMessages.add("行 " + rowNum + ": 氏名（フリガナ）桁数オーバー");
        }
    }

    /**
     * 氏名のバリデーションを行います。
     *
     * @param shimei 氏名
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     */
    private void validateShimei(String shimei, int rowNum, List<String> errorMessages) {
        // 空欄チェック
        if (shimei.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 氏名未入力");
        }
        // 長さチェック（最大30文字）
        if (shimei.length() > 30) {
            errorMessages.add("行 " + rowNum + ": 氏名桁数オーバー");
        }
    }

    /**
     * 氏名（英字）のバリデーションを行います。
     *
     * @param shimeiEiji 氏名（英字）
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     */
    private void validateShimeiEiji(String shimeiEiji, int rowNum, List<String> errorMessages) {
        // 空欄チェック
        if (shimeiEiji.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 氏名（英字）未入力");
        }
        // 長さチェック（最大40文字）
        if (shimeiEiji.length() > 40) {
            errorMessages.add("行 " + rowNum + ": 氏名（英字）桁数オーバー");
        }
    }

    /**
     * 在籍区分のバリデーションを行います。
     *
     * @param zaisekiKB 在籍区分
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     */
    private void validateZaisekiKB(String zaisekiKB, int rowNum, List<String> errorMessages) {
        // 空欄チェック
        if (zaisekiKB.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 在籍区分未登録");
        }
    }

    /**
     * 部門コードのバリデーションを行います。
     *
     * @param bumonCd 部門コード
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     */
    private void validateBumonCd(String bumonCd, int rowNum, List<String> errorMessages) {
        // 空欄チェック
        if (bumonCd.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 部門コード未登録");
        }
    }

    /**
     * 性別のバリデーションを行います。
     *
     * @param seibetsu 性別
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     */
    private void validateSeibetsu(String seibetsu, int rowNum, List<String> errorMessages) {
        // 空欄チェック
        if (seibetsu.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 性別未登録");
        }
    }

    /**
     * 血液型のバリデーションを行います。
     *
     * @param ketsuekiGata 血液型
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     */
    private void validateKetsuekiGata(String ketsuekiGata, int rowNum, List<String> errorMessages) {
        // 空欄チェック
        if (ketsuekiGata.trim().isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 血液型未登録");
        }
    }

    /**
     * 日付文字列をバリデーションし、必要に応じてエラーメッセージを追加します。
     *
     * @param dateStr 日付文字列
     * @param rowNum 行番号（エラーメッセージ用）
     * @param errorMessages エラーメッセージのリスト（結果として追加される）
     * @return バリデーションが成功した場合はparseされたDate、失敗した場合はnull
     */
    public java.util.Date validateAndParseDate(String dateStr, int rowNum, List<String> errorMessages) {
        // 空欄チェック
        if (dateStr.isEmpty()) {
            errorMessages.add("行 " + rowNum + ": 生年月日が日付でない");
            return null;
        }

        // 日付形式のチェック（yyyy/M/d形式）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d");
        sdf.setLenient(false); // 厳密な日付チェックを有効化
        try {
            java.util.Date parsedDate = sdf.parse(dateStr);

            // 入力された文字列とフォーマット後の文字列が一致するか確認（不正な日付を防ぐ）
            String formattedBack = sdf.format(parsedDate);
            if (!dateStr.equals(formattedBack)) {
                errorMessages.add("行 " + rowNum + ": 生年月日形式エラー (yyyy/M/d)。値: " + dateStr);
                return null;
            }

            // 追加: 生年月日が現在日付より未来でないかチェック
            java.util.Date currentDate = new java.util.Date();
            if (parsedDate.after(currentDate)) {
                errorMessages.add("行 " + rowNum + ": 生年月日が未来の日付です。値: " + dateStr);
                return null;
            }

            return parsedDate;
        } catch (ParseException e) {
            errorMessages.add("行 " + rowNum + ": 生年月日が日付でない (yyyy/M/d)。値: " + dateStr);
            return null;
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
            // excelReaderを使ってセルの値を取得
            this.shainNo = excelReader.getCellValue(row.getCell(0)).trim();
            this.shimeiKana = excelReader.getCellValue(row.getCell(1)).trim();
            this.shimei = excelReader.getCellValue(row.getCell(2)).trim();
            this.shimeiEiji = excelReader.getCellValue(row.getCell(3)).trim();
            this.zaisekiKB = excelReader.getCellValue(row.getCell(4)).trim();
            this.bumonCd = excelReader.getCellValue(row.getCell(5)).trim();
            this.seibetsu = excelReader.getCellValue(row.getCell(6)).trim();
            this.ketsuekiGata = excelReader.getCellValue(row.getCell(7)).trim();
            this.birthDateStr = excelReader.getCellValue(row.getCell(8)).trim();
        }
    }
}