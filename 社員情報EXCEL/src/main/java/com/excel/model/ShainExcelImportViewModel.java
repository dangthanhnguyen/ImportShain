package com.excel.model;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.excel.util.DbConnectionUtil;
import com.excel.util.ImportResult;
import com.excel.repository.ShainDAO;
import com.excel.repository.ShainRepository;

/**
 * 社員情報をExcelファイルからインポートするためのモデルクラス。
 * Excelファイルの読み込み、バリデーション、データベースへの挿入・更新処理を管理します。
 */
public class ShainExcelImportViewModel {
	private final ShainRepository shainRepository;
	private final ShainValidator shainValidator;
	private final ExcelReader excelReader;
	private final FileChooser fileChooser;

	/**
	 * コンストラクタ。ShainRepository、ShainValidator、ExcelReader, FileChooserを初期化します。
	 */
	public ShainExcelImportViewModel() {
		this.shainRepository = new ShainRepository();
		this.shainValidator = new ShainValidator();
		this.excelReader = new ExcelReader();
		this.fileChooser = new FileChooser();
	}

	/**
	 * 必要な依存関係（ExcelReader、ShainValidator、ShainRepository、FileChooser）を注入して初期化します。
	 *
	 * @param excelReader Excelファイルの読み込みを担当する
	 * @param shainValidator 社員データの検証を担当する
	 * @param shainRepository 社員データの永続化を担当する
	 * @param fileChooser ファイル選択ダイアログを提供する
	 */
	public ShainExcelImportViewModel(ExcelReader excelReader, ShainValidator shainValidator,
	        ShainRepository shainRepository, FileChooser fileChooser) {
	    this.excelReader = excelReader;
	    this.shainValidator = shainValidator;
	    this.shainRepository = shainRepository;
	    this.fileChooser = fileChooser;
	}

	/**
	 * Excelファイルを選択し、そのパスを取得します。
	 *
	 * @param stage 現在のウィンドウ
	 * @return 選択されたExcelファイルのパス。ファイルが選択されなかった場合はnull。
	 */
	public String selectExcelFile(Stage stage) {
		fileChooser.setTitle("Excelファイルを選択してください");

		fileChooser.getExtensionFilters()
				.addAll(new FileChooser.ExtensionFilter("Excelファイル (*.xlsx, *.xls)", "*.xlsx", "*.xls"));

		File selectedFile = fileChooser.showOpenDialog(stage);

		return (selectedFile != null) ? selectedFile.getAbsolutePath() : null;
	}

	/**
	 * インポート処理用のTaskを作成し、実行します。
	 *
	 * @param filePath         インポートするExcelファイルのパス
	 * @param importType       インポートタイプ (INSERT_ONLY, INSERT_AND_UPDATE, NEW)
	 * @param progressCallback 処理中の進捗を通知するためのコールバック
	 * @return 実行中のTask
	 */
	public Task<ImportResult> executeImport(String filePath, ImportType importType,
			Consumer<ImportResult> progressCallback) {
		Task<ImportResult> importTask = createImportTask(filePath, importType, progressCallback);

		new Thread(importTask).start();

		return importTask;
	}

	/**
	 * インポート用のTaskを作成します。
	 *
	 * @param filePath         インポートするExcelファイルのパス
	 * @param importType       インポートタイプ
	 * @param progressCallback 進捗を通知するためのコールバック
	 * @return 作成されたTask
	 */
	private Task<ImportResult> createImportTask(String filePath, ImportType importType,
			Consumer<ImportResult> progressCallback) {
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
	 * インポートタイプを定義する列挙型。
	 */
	public enum ImportType {
		INSERT_ONLY, INSERT_AND_UPDATE, NEW
	}

	/**
	 * Excelファイルから社員情報をインポートします（追加のみ）。 社員番号が重複している場合や既に存在する場合、処理を中止し、エラーを返します。
	 *
	 * @param filePath         Excelファイルのパス
	 * @param progressCallback 処理済みのレコード数を通知するためのコールバック
	 * @return インポート結果（追加数、更新数、エラーメッセージ）
	 * @throws IOException  ファイル読み込みエラーが発生した場合
	 * @throws SQLException データベース操作エラーが発生した場合
	 */
	public ImportResult importInsertOnly(String filePath, Consumer<ImportResult> progressCallback)
			throws IOException, SQLException {
		ImportResult result = new ImportResult();
		List<String> errorMessages = new ArrayList<>();

		// ステップ1: ファイルの検証とデータの読み込み
		List<ShainDAO> shainList = validateAndLoadData(filePath, errorMessages, true);
		if (!errorMessages.isEmpty()) {
			result.setErrorMessages(errorMessages);
			result.setValid(false);
			throw new SQLException(buildErrorMessage(errorMessages));
		}

		// ステップ2: データベースにデータを挿入
		importInsertOnlyToDatabase(shainList, result, progressCallback);

		return result;
	}

	/**
	 * データをデータベースに挿入します（追加のみ）。
	 *
	 * @param shainList        挿入する社員情報のリスト
	 * @param result           インポート結果（結果として更新される）
	 * @param progressCallback 進捗を通知するためのコールバック
	 * @throws SQLException データベース操作エラーが発生した場合
	 */
	private void importInsertOnlyToDatabase(List<ShainDAO> shainList, ImportResult result,
			Consumer<ImportResult> progressCallback) throws SQLException {
		try (Connection conn = DbConnectionUtil.getConnection()) {
			// トランザクション開始：自動コミットを無効化
			conn.setAutoCommit(false);
			try {
				// バッチ処理でデータを挿入
				int inserted = shainRepository.insertShainBatch(shainList);
				result.setInserted(inserted);

				// 進捗を通知
				if (progressCallback != null) {
					progressCallback.accept(result);
				}

				// トランザクションをコミット
				conn.commit();
			} catch (SQLException e) {
				// エラーが発生した場合、トランザクションをロールバック
				conn.rollback();
				throw e;
			}
		}
	}

	/**
	 * Excelファイルから社員情報をインポートします（追加・更新）。 社員番号が既に存在する場合は更新、存在しない場合は追加します。
	 *
	 * @param filePath         Excelファイルのパス
	 * @param progressCallback 処理済みのレコード数を通知するためのコールバック
	 * @return 処理結果（追加数、更新数）
	 * @throws IOException  ファイル読み込みエラーが発生した場合
	 * @throws SQLException データベース操作エラーが発生した場合
	 */
	public ImportResult importInsertAndUpdate(String filePath, Consumer<ImportResult> progressCallback)
			throws IOException, SQLException {
		ImportResult result = new ImportResult();
		List<String> errorMessages = new ArrayList<>();

		// ステップ1: ファイルの検証とデータの読み込み
		List<ShainDAO> shainList = validateAndLoadData(filePath, errorMessages, false);
		if (!errorMessages.isEmpty()) {
			result.setErrorMessages(errorMessages);
			result.setValid(false);
			throw new SQLException(buildErrorMessage(errorMessages));
		}

		// ステップ2: データベースで社員番号の存在を一括チェック
		List<String> shainNos = shainList.stream().map(ShainDAO::getShainNo).toList();
		Set<String> existingShainNos = shainRepository.checkExistingShainNos(shainNos);

		// ステップ3: データベースにデータを挿入・更新
		importInsertAndUpdateToDatabase(shainList, existingShainNos, result, progressCallback);

		return result;
	}

	/**
	 * データをデータベースに挿入・更新します。
	 *
	 * @param shainList        社員情報のリスト
	 * @param existingShainNos 既存の社員番号のセット
	 * @param result           インポート結果（結果として更新される）
	 * @param progressCallback 進捗を通知するためのコールバック
	 * @throws SQLException データベース操作エラーが発生した場合
	 */
	private void importInsertAndUpdateToDatabase(List<ShainDAO> shainList, Set<String> existingShainNos,
			ImportResult result, Consumer<ImportResult> progressCallback) throws SQLException {
		try (Connection conn = DbConnectionUtil.getConnection()) {
			// トランザクション開始：自動コミットを無効化
			conn.setAutoCommit(false);
			try {
				// データを「挿入用」と「更新用」に分類
				List<ShainDAO> toInsert = new ArrayList<>();
				List<ShainDAO> toUpdate = new ArrayList<>();
				classifyShainRecords(shainList, existingShainNos, toInsert, toUpdate);

				// 更新処理：既存のレコードを更新
				if (!toUpdate.isEmpty()) {
					int updated = shainRepository.updateShainBatch(toUpdate);
					result.setUpdated(updated);
				}

				// 挿入処理：新しいレコードを挿入
				if (!toInsert.isEmpty()) {
					int inserted = shainRepository.insertShainBatch(toInsert);
					result.setInserted(inserted);
				}

				// 進捗を通知
				if (progressCallback != null) {
					progressCallback.accept(result);
				}

				// トランザクションをコミット
				conn.commit();
			} catch (SQLException e) {
				// エラーが発生した場合、トランザクションをロールバック
				conn.rollback();
				throw e;
			}
		}
	}

	/**
	 * Excelファイルから社員情報をインポートします（全件入替）。 既存のデータを全て削除し、新しいデータを挿入します。
	 *
	 * @param filePath         Excelファイルのパス
	 * @param progressCallback 処理済みのレコード数を通知するためのコールバック
	 * @return 挿入結果（追加数、更新数）
	 * @throws IOException  ファイル読み込みエラーが発生した場合
	 * @throws SQLException データベース操作エラーが発生した場合
	 */
	public ImportResult importNew(String filePath, Consumer<ImportResult> progressCallback)
			throws IOException, SQLException {
		ImportResult result = new ImportResult();
		List<String> errorMessages = new ArrayList<>();

		// ステップ1: ファイルの検証とデータの読み込み
		List<ShainDAO> shainList = validateAndLoadData(filePath, errorMessages, false);
		if (!errorMessages.isEmpty()) {
			result.setErrorMessages(errorMessages);
			result.setValid(false);
			throw new SQLException(buildErrorMessage(errorMessages));
		}

		// ステップ2: データベースのデータを全削除し、新しいデータを挿入
		importNewToDatabase(shainList, result, progressCallback);

		return result;
	}

	/**
	 * データベースのデータを全削除し、新しいデータを挿入します。
	 *
	 * @param shainList        挿入する社員情報のリスト
	 * @param result           インポート結果（結果として更新される）
	 * @param progressCallback 進捗を通知するためのコールバック
	 * @throws SQLException データベース操作エラーが発生した場合
	 */
	private void importNewToDatabase(List<ShainDAO> shainList, ImportResult result,
			Consumer<ImportResult> progressCallback) throws SQLException {
		try (Connection conn = DbConnectionUtil.getConnection()) {
			// トランザクション開始：自動コミットを無効化
			conn.setAutoCommit(false);
			try {
				// 既存のデータを全て削除
				shainRepository.deleteAllShain();

				// 新しいデータを挿入
				if (!shainList.isEmpty()) {
					int inserted = shainRepository.insertShainBatch(shainList);
					result.setInserted(inserted);
				}

				// 進捗を通知
				if (progressCallback != null) {
					progressCallback.accept(result);
				}

				// トランザクションをコミット
				conn.commit();
			} catch (SQLException e) {
				// エラーが発生した場合、トランザクションをロールバック
				conn.rollback();
				throw e;
			}
		}
	}

	/**
	 * Excelファイルの検証を行い、データを読み込みます。
	 *
	 * @param filePath      Excelファイルのパス
	 * @param errorMessages エラーメッセージのリスト（結果として追加される）
	 * @param checkExisting 既存の社員番号をチェックするかどうか
	 * @return 読み込まれた社員情報のリスト
	 * @throws IOException  ファイル読み込みエラーが発生した場合
	 * @throws SQLException データベース操作エラーが発生した場合
	 */
	private List<ShainDAO> validateAndLoadData(String filePath, List<String> errorMessages, boolean checkExisting)
			throws IOException, SQLException {
		// ステップ1: ファイルの検証
		ImportResult fileValidation = excelReader.validateFile(filePath);
		if (!fileValidation.isValid()) {
			errorMessages.addAll(fileValidation.getErrorMessages());
			throw new IOException(buildErrorMessage(errorMessages));
		}

		// ステップ2: シートの読み込み
		Sheet sheet = excelReader.loadSheet(filePath);

		// ステップ3: シート内で社員番号の重複チェック
		ImportResult sheetValidation = validateSheetData(sheet);
		if (!sheetValidation.isValid()) {
			errorMessages.addAll(sheetValidation.getErrorMessages());
			throw new SQLException(buildErrorMessage(errorMessages));
		}

		// ステップ4: データベース内で社員番号の存在チェック（必要な場合）
		if (checkExisting) {
			ImportResult existingCheck = checkExistingShainNos(sheet);
			if (!existingCheck.isValid()) {
				errorMessages.addAll(existingCheck.getErrorMessages());
				throw new SQLException(buildErrorMessage(errorMessages));
			}
		}

		// ステップ5: シートのデータを読み込み、バリデーションを行い、ShainDAOリストを作成
		return excelReader.readAndValidateShainList(sheet, shainValidator);
	}

	/**
	 * 社員番号がデータベースに存在するかどうかを事前にチェックします。
	 *
	 * @param sheet チェックするExcelシート
	 * @return 検証結果（エラーがない場合はvalid、存在する場合はエラーメッセージを含む）
	 * @throws SQLException データベース操作エラーが発生した場合
	 */
	private ImportResult checkExistingShainNos(Sheet sheet) throws SQLException {
		List<String> shainNos = new ArrayList<>();
		List<String> errorMessages = new ArrayList<>();
		ImportResult result = new ImportResult();

		// シートから社員番号を収集
		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			Row row = sheet.getRow(i);
			if (row == null || excelReader.isRowEmpty(row)) {
				continue;
			}
			String shainNo = excelReader.getCellValue(row.getCell(0)).trim();
			if (!shainNo.isEmpty()) {
				shainNos.add(shainNo);
			}
		}

		// データベースで社員番号の存在を一括チェック
		Set<String> existingShainNos = shainRepository.checkExistingShainNos(shainNos);

		// 存在する社員番号に対してエラーメッセージを追加
		for (int i = 0; i < shainNos.size(); i++) {
			String shainNo = shainNos.get(i);
			if (existingShainNos.contains(shainNo)) {
				errorMessages.add("社員番号 " + shainNo + "が既に存在しています");
			}
		}

		if (!errorMessages.isEmpty()) {
			result.setErrorMessages(errorMessages);
			result.setValid(false);
		}

		return result;
	}

	/**
	 * 社員情報を「挿入用」と「更新用」に分類します。
	 *
	 * @param shainList        社員情報のリスト
	 * @param existingShainNos 既存の社員番号のセット
	 * @param toInsert         挿入用のリスト（結果として追加される）
	 * @param toUpdate         更新用のリスト（結果として追加される）
	 */
	private void classifyShainRecords(List<ShainDAO> shainList, Set<String> existingShainNos, List<ShainDAO> toInsert,
			List<ShainDAO> toUpdate) {
		for (ShainDAO shain : shainList) {
			if (existingShainNos.contains(shain.getShainNo())) {
				toUpdate.add(shain);
			} else {
				toInsert.add(shain);
			}
		}
	}

	/**
	 * シートのデータを検証します（重複チェックとバリデーション）。
	 *
	 * @param sheet チェックするシート
	 * @return 検証結果（エラーがない場合はvalid、エラーがあればエラーメッセージを含む）
	 */
	private ImportResult validateSheetData(Sheet sheet) {
		List<String> errorMessages = new ArrayList<>();
		Map<String, List<Integer>> shainNoMap = new HashMap<>();
		ImportResult result = new ImportResult();

		// 各行をチェック（ヘッダー行は除外）
		for (Row row : sheet) {
			if (row == null || row.getRowNum() == 0 || excelReader.isRowEmpty(row)) {
				continue;
			}

			int rowNum = row.getRowNum() + 1;
			String shainNo = excelReader.getCellValue(row.getCell(0)).trim();

			// 社員番号が空でない場合、重複チェック用にマップに追加
			if (!shainNo.trim().isEmpty()) {
				shainNoMap.computeIfAbsent(shainNo, k -> new ArrayList<>()).add(rowNum);
			}

			// 行のバリデーションを実行
			errorMessages.addAll(shainValidator.validateRow(row, rowNum));
		}

		// 社員番号の重複をチェックし、エラーメッセージを追加
		for (Map.Entry<String, List<Integer>> entry : shainNoMap.entrySet()) {
			List<Integer> rows = entry.getValue();
			if (rows.size() > 1) {
				// 重複している行番号をメッセージに追加
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

		if (!errorMessages.isEmpty()) {
			result.setErrorMessages(errorMessages);
			result.setValid(false);
		}

		return result;
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
			return "データエラー:\n" + String.join("\n", errorMessages.subList(0, 20)) + "\n・・・ほか "
					+ (errorMessages.size() - 20) + " 件のエラーがあります";
		} else {
			return "データエラー:\n" + String.join("\n", errorMessages);
		}
	}
}