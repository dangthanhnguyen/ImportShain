package com.excel;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class ApplicationMain extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // FXMLをロード
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/importshainview.fxml"));
            AnchorPane root = loader.load();

            // シーンを作成し、ウィンドウのサイズを設定
            Scene scene = new Scene(root, 1024, 700);

            // ステージ（ウィンドウ）を設定
            primaryStage.setTitle("社員情報EXCEL取込");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
