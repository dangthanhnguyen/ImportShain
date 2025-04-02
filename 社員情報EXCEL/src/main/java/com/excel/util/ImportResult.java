package com.excel.util;

import java.util.ArrayList;
import java.util.List;

/**
 * インポート処理の結果を格納するクラス。
 * インポート中に追加されたレコード数、更新されたレコード数、エラーメッセージを管理します。
 * また、検証結果（valid/invalid）も管理します。
 */
public class ImportResult {
    private int insertedCount; // 追加されたレコード数
    private int updatedCount;  // 更新されたレコード数
    private List<String> errorMessages; // エラーメッセージのリスト
    private boolean valid; // 検証が成功したかどうか

    /**
     * コンストラクタ。
     * 追加レコード数、更新レコード数を0に初期化し、エラーメッセージリストを空のリストに設定します。
     * デフォルトでvalidをtrueに設定します。
     */
    public ImportResult() {
        this.insertedCount = 0;
        this.updatedCount = 0;
        this.errorMessages = new ArrayList<>();
        this.valid = true; // デフォルトでtrue
    }

    /**
     * 追加されたレコード数を取得します。
     *
     * @return 追加されたレコード数
     */
    public int getInsertedCount() {
        return insertedCount;
    }

    /**
     * 追加されたレコード数を設定します。
     *
     * @param inserted 追加されたレコード数
     */
    public void setInserted(int inserted) {
        this.insertedCount = inserted;
    }

    /**
     * 更新されたレコード数を取得します。
     *
     * @return 更新されたレコード数
     */
    public int getUpdatedCount() {
        return updatedCount;
    }

    /**
     * 更新されたレコード数を設定します。
     *
     * @param updated 更新されたレコード数
     */
    public void setUpdated(int updated) {
        this.updatedCount = updated;
    }

    /**
     * 追加されたレコード数を1増加させます。
     */
    public void incrementInserted() {
        this.insertedCount++;
    }

    /**
     * 更新されたレコード数を1増加させます。
     */
    public void incrementUpdated() {
        this.updatedCount++;
    }

    /**
     * エラーメッセージのリストを取得します。
     *
     * @return エラーメッセージのリスト
     */
    public List<String> getErrorMessages() {
        return errorMessages;
    }

    /**
     * エラーメッセージのリストを設定します。
     *
     * @param errorMessages エラーメッセージのリスト
     */
    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    /**
     * 処理されたレコードの総数（追加＋更新）を取得します。
     *
     * @return 追加されたレコード数と更新されたレコード数の合計
     */
    public int getTotalProcessed() {
        return insertedCount + updatedCount;
    }

    /**
     * 検証が成功したかどうかを取得します。
     *
     * @return 検証が成功した場合はtrue、失敗した場合はfalse
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 検証が成功したかどうかを設定します。
     *
     * @param valid 検証が成功したかどうか
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }
}