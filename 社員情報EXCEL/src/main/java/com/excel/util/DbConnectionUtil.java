package com.excel.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * データベース接続を管理するユーティリティクラス。
 * PostgreSQLデータベースへの接続を提供します。
 */
public class DbConnectionUtil {
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    /**
     * 静的初期化ブロック。
     * 設定ファイルを読み込み、PostgreSQL JDBCドライバーのロードを行います。
     */
    static {
        Properties props = new Properties();
        try (InputStream input = DbConnectionUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find config.properties in classpath");
            }
            props.load(input);

            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");

            if (URL == null || USER == null || PASSWORD == null) {
                throw new RuntimeException("Missing required database configuration in config.properties");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
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
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}