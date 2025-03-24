package com.excel.repository;

import com.excel.util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ShainRepository {
    /**
     * 社員情報を更新します。
     * shain_noに基づいて社員情報を更新し、updated_dateフィールドを現在の日付（CURRENT_DATE）に設定します。
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

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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
     * 社員情報を新規登録します。
     * created_dateフィールドを現在の日付（CURRENT_DATE）に設定します。
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

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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
     * 社員テーブルから全てのデータを削除します。
     * @return 削除されたレコード数
     * @throws SQLException データベース操作中にエラーが発生した場合
     */
    public int deleteAllShain() throws SQLException {
        String sql = "DELETE FROM shain";

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            return pstmt.executeUpdate();
        }
    }
    
    /**
     * 社員番号がデータベースに存在するかどうかを確認します。
     * @param shainNo 社員番号
     * @return 存在する場合は true、存在しない場合は false
     * @throws SQLException データベース操作中にエラーが発生した場合
     */
    public boolean isShainExists(String shainNo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM shain WHERE shain_no = ?";

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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
}