package com.excel.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * データベース接続を管理するユーティリティクラス。
 * PostgreSQLデータベースへの接続を提供します。
 */
public class DbConnectionUtil {
    // データベース接続情報
    private static final String URL = "jdbc:postgresql://localhost:5432/shainDB"; // 接続URL
    private static final String USER = "postgres"; // ユーザー名
    private static final String PASSWORD = "solekia"; // パスワード

    /**
     * 静的初期化ブロック。
     * PostgreSQL JDBCドライバーのロードを行います。
     */
    static {
        try {
            // JDBCドライバーをロード
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            // ドライバーが見つからない場合、例外をスロー
            throw new RuntimeException("PostgreSQL JDBC Driver not found!", e);
        }
    }

    /**
     * データベースへの接続を取得します。
     *
     * @return データベース接続（Connectionオブジェクト）
     * @throws SQLException データベース接続に失敗した場合
     */
    public static Connection getConnection() throws SQLException {
        // 設定されたURL、ユーザー名、パスワードを使用して接続を確立
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}