package com.excel.repository;

import com.excel.util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 社員情報をデータベースに操作するためのリポジトリクラス。
 * 社員情報の登録、更新、削除、存在確認などの操作を提供します。
 */
public class ShainRepository {

    /**
     * 社員情報を更新します。
     * shain_noに基づいて社員情報を更新し、updated_dateフィールドを現在の日付（CURRENT_DATE）に設定します。
     * 
     * @param shain 社員情報（ShainDAOオブジェクト）
     * @return 更新されたレコード数（通常は1または0）
     * @throws SQLException データベース操作中にエラーが発生した場合
     */
    public int updateShain(ShainDAO shain) throws SQLException {
        String sql = """
                    UPDATE shain
                    SET shimei_kana = ?,
                        shimei = ?,
                        shimei_eiji = ?,
                        zaiseki_kb = ?,
                        bumon_cd = ?,
                        seibetsu = ?,
                        ketsueki_gata = ?,
                        birth_date = ?,
                        update_date = CURRENT_DATE
                    WHERE shain_no = ?
                """;

        try (Connection conn = DbConnectionUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shain.getShimeiKana());
            pstmt.setString(2, shain.getShimei());
            pstmt.setString(3, shain.getShimeiEiji());
            pstmt.setString(4, shain.getZaisekiKB());
            pstmt.setString(5, shain.getBumonCode());
            pstmt.setString(6, shain.getSeibetsu());
            pstmt.setString(7, shain.getKetsuekiGata());
            pstmt.setDate(8, shain.getBirthDate() != null ? new java.sql.Date(shain.getBirthDate().getTime()) : null);
            pstmt.setString(9, shain.getShainNo());

            return pstmt.executeUpdate();
        }
    }

    /**
     * 社員情報を一括で更新します。
     * shain_noに基づいて社員情報を更新し、updated_dateフィールドを現在の日付（CURRENT_DATE）に設定します。
     * 
     * @param shainList 社員情報のリスト（ShainDAOオブジェクトのリスト）
     * @return 更新されたレコード数
     * @throws SQLException データベース操作中にエラーが発生した場合
     */
    public int updateShainBatch(List<ShainDAO> shainList) throws SQLException {
        // リストが空またはnullの場合は0を返す
        if (shainList == null || shainList.isEmpty()) {
            return 0;
        }

        // リストに1件しかない場合は単一更新メソッドを呼び出す
        if (shainList.size() == 1) {
            return updateShain(shainList.get(0));
        }

        String sql = """
                    UPDATE shain
                    SET shimei_kana = ?,
                        shimei = ?,
                        shimei_eiji = ?,
                        zaiseki_kb = ?,
                        bumon_cd = ?,
                        seibetsu = ?,
                        ketsueki_gata = ?,
                        birth_date = ?,
                        update_date = CURRENT_DATE
                    WHERE shain_no = ?
                """;

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // 各社員情報をバッチに追加
            for (ShainDAO shain : shainList) {
                pstmt.setString(1, shain.getShimeiKana());
                pstmt.setString(2, shain.getShimei());
                pstmt.setString(3, shain.getShimeiEiji());
                pstmt.setString(4, shain.getZaisekiKB());
                pstmt.setString(5, shain.getBumonCode());
                pstmt.setString(6, shain.getSeibetsu());
                pstmt.setString(7, shain.getKetsuekiGata());
                pstmt.setDate(8, shain.getBirthDate() != null ? new java.sql.Date(shain.getBirthDate().getTime()) : null);
                pstmt.setString(9, shain.getShainNo());
                pstmt.addBatch(); // バッチに追加
            }

            // バッチを実行
            int[] results = pstmt.executeBatch();
            int totalUpdated = 0;
            // 各更新結果を合計
            for (int result : results) {
                if (result > 0) {
                    totalUpdated += result;
                }
            }
            return totalUpdated;
        }
    }

    /**
     * 社員情報を新規登録します。created_dateフィールドを現在の日付（CURRENT_DATE）に設定します。
     * 
     * @param shain 社員情報（ShainDAOオブジェクト）
     * @return 登録されたレコード数（通常は1）
     * @throws SQLException データベース操作中にエラーが発生した場合
     */
    public int insertShain(ShainDAO shain) throws SQLException {
        String sql = """
                    INSERT INTO shain (
                        shain_no,
                        shimei_kana,
                        shimei,
                        shimei_eiji,
                        zaiseki_kb,
                        bumon_cd,
                        seibetsu,
                        ketsueki_gata,
                        birth_date,
                        created_date
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                """;

        try (Connection conn = DbConnectionUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shain.getShainNo());
            pstmt.setString(2, shain.getShimeiKana());
            pstmt.setString(3, shain.getShimei());
            pstmt.setString(4, shain.getShimeiEiji());
            pstmt.setString(5, shain.getZaisekiKB());
            pstmt.setString(6, shain.getBumonCode());
            pstmt.setString(7, shain.getSeibetsu());
            pstmt.setString(8, shain.getKetsuekiGata());
            pstmt.setDate(9, shain.getBirthDate() != null ? new java.sql.Date(shain.getBirthDate().getTime()) : null);

            return pstmt.executeUpdate();
        }
    }

    /**
     * 社員情報を一括で新規登録します。created_dateフィールドを現在の日付（CURRENT_DATE）に設定します。
     * 
     * @param shainList 社員情報のリスト（ShainDAOオブジェクトのリスト）
     * @return 登録されたレコード数
     * @throws SQLException データベース操作中にエラーが発生した場合
     */
    public int insertShainBatch(List<ShainDAO> shainList) throws SQLException {
        // リストが空またはnullの場合は0を返す
        if (shainList == null || shainList.isEmpty()) {
            return 0;
        }

        // リストに1件しかない場合は単一登録メソッドを呼び出す
        if (shainList.size() == 1) {
            return insertShain(shainList.get(0));
        }

        String sql = """
                    INSERT INTO shain (
                        shain_no,
                        shimei_kana,
                        shimei,
                        shimei_eiji,
                        zaiseki_kb,
                        bumon_cd,
                        seibetsu,
                        ketsueki_gata,
                        birth_date,
                        created_date
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                """;

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // 各社員情報をバッチに追加
            for (ShainDAO shain : shainList) {
                pstmt.setString(1, shain.getShainNo());
                pstmt.setString(2, shain.getShimeiKana());
                pstmt.setString(3, shain.getShimei());
                pstmt.setString(4, shain.getShimeiEiji());
                pstmt.setString(5, shain.getZaisekiKB());
                pstmt.setString(6, shain.getBumonCode());
                pstmt.setString(7, shain.getSeibetsu());
                pstmt.setString(8, shain.getKetsuekiGata());
                pstmt.setDate(9, shain.getBirthDate() != null ? new java.sql.Date(shain.getBirthDate().getTime()) : null);
                pstmt.addBatch(); // バッチに追加
            }

            // バッチを実行
            int[] results = pstmt.executeBatch();
            int totalInserted = 0;
            // 各登録結果を合計
            for (int result : results) {
                if (result > 0) {
                    totalInserted += result;
                }
            }
            return totalInserted;
        }
    }

    /**
     * 社員テーブルから全てのデータを削除します。
     * 
     * @return 削除されたレコード数
     * @throws SQLException データベース操作中にエラーが発生した場合
     */
    public int deleteAllShain() throws SQLException {
        String sql = "DELETE FROM shain";

        try (Connection conn = DbConnectionUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            return pstmt.executeUpdate();
        }
    }

    /**
     * 社員番号がデータベースに存在するかどうかを確認します。
     * 
     * @param shainNo 社員番号
     * @return 存在する場合は true、存在しない場合は false
     * @throws SQLException データベース操作中にエラーが発生した場合
     */
    public boolean isShainExists(String shainNo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM shain WHERE shain_no = ?";

        try (Connection conn = DbConnectionUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shainNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    /**
     * すべての社員番号を取得します。
     * 
     * @return 社員番号のセット
     * @throws SQLException データベース操作中にエラーが発生した場合
     */
    public Set<String> getAllShainNos() throws SQLException {
        Set<String> shainNos = new HashSet<>();
        String sql = "SELECT shain_no FROM shain";
        try (Connection conn = DbConnectionUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    shainNos.add(rs.getString("shain_no"));
                }
            }
        }
        return shainNos;
    }

    /**
     * 指定された社員番号リストの中にデータベースに存在する社員番号を返します。
     * 
     * @param shainNos 社員番号のリスト
     * @return 存在する社員番号のセット（存在しない場合は空のセット）
     * @throws SQLException データベース操作中にエラーが発生した場合
     */
    public Set<String> checkExistingShainNos(List<String> shainNos) throws SQLException {
        Set<String> existingShainNos = new HashSet<>();
        // リストが空またはnullの場合は空のセットを返す
        if (shainNos == null || shainNos.isEmpty()) {
            return existingShainNos;
        }

        // 動的にSQLを構築：IN句のプレースホルダをリストのサイズ分生成
        String sql = "SELECT shain_no FROM shain WHERE shain_no IN ("
                   + String.join(",", Collections.nCopies(shainNos.size(), "?")) + ")";
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // 各社員番号をプレースホルダに設定
            for (int i = 0; i < shainNos.size(); i++) {
                pstmt.setString(i + 1, shainNos.get(i));
            }
            // クエリを実行し、存在する社員番号を取得
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                existingShainNos.add(rs.getString("shain_no"));
            }
        }
        return existingShainNos;
    }
}