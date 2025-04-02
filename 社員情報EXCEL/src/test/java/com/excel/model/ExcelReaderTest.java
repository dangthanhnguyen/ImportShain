package com.excel.model;

import com.excel.repository.ShainDAO;
import com.excel.util.ImportResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * このクラスは、Excelファイルの読み込み、検証、社員データの解析機能をテストします。
 */
@ExtendWith(MockitoExtension.class)
public class ExcelReaderTest {

	@InjectMocks
	private ExcelReader excelReader;

	@Mock
	private ShainValidator shainValidator;

	private static final List<String> EXPECTED_HEADERS = Arrays.asList("社員番号", "氏名（フリガナ）", "氏名", "氏名（英字）", "在籍区分",
			"部門コード", "性別", "血液型", "生年月日");

	/**
	 * 各テストメソッドの前にテスト環境をセットアップします。
	 */
	@BeforeEach
	void setUp() {
		excelReader = new ExcelReader();
	}

	/**
	 * 有効なExcelファイルが正しく検証され、エラーメッセージが空であることを検証します。
	 *
	 * @throws IOException ファイル操作中にエラーが発生した場合
	 */
	@Test
	void testValidateFile_ValidFile_ReturnsValidResult() throws IOException {
		String filePath = "src/test/resources/valid_excel.xlsx";
		createValidExcelFile(filePath);

		ImportResult result = excelReader.validateFile(filePath);

		assertTrue(result.isValid());
		assertTrue(result.getErrorMessages().isEmpty());
	}

	/**
	 * 無効なファイル拡張子（.xlsxまたは.xls以外）が検出され、エラーメッセージが返されることを検証します。
	 *
	 * @throws IOException ファイル操作中にエラーが発生した場合
	 */
	@Test
	void testValidateFile_InvalidExtension_ReturnsError() throws IOException {
		String filePath = "src/test/resources/invalid.txt";

		ImportResult result = excelReader.validateFile(filePath);

		assertFalse(result.isValid());
		assertEquals("ファイル拡張子が正しくありません（.xlsxまたは.xlsが必要です）", result.getErrorMessages().get(0));
	}

	/**
	 * 有効なExcelファイルからシートが正しく読み込まれることを検証します。
	 *
	 * @throws IOException ファイル操作中にエラーが発生した場合
	 */
	@Test
	void testLoadSheet_ValidFile_ReturnsSheet() throws IOException {
		String filePath = "src/test/resources/valid_excel.xlsx";
		createValidExcelFile(filePath);

		Sheet sheet = excelReader.loadSheet(filePath);

		assertNotNull(sheet);
		assertEquals(0, sheet.getSheetName().indexOf("Sheet"));
	}

	/**
	 * 有効なデータが含まれるシートから社員リストが正しく読み込まれ、検証されることを確認します。
	 *
	 * @throws IOException ファイル操作中にエラーが発生した場合
	 */
	@Test
	void testReadAndValidateShainList_ValidData_ReturnsShainList() throws IOException {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet();
		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
			headerRow.createCell(i).setCellValue(EXPECTED_HEADERS.get(i));
		}
		Row dataRow = sheet.createRow(1);
		dataRow.createCell(0).setCellValue("1234");
		dataRow.createCell(1).setCellValue("ヤマダ タロウ");
		dataRow.createCell(2).setCellValue("山田 太郎");
		dataRow.createCell(3).setCellValue("Yamada Taro");
		dataRow.createCell(4).setCellValue("在籍");
		dataRow.createCell(5).setCellValue("001");
		dataRow.createCell(6).setCellValue("男");
		dataRow.createCell(7).setCellValue("A");
		dataRow.createCell(8).setCellValue("1990/1/1");

		when(shainValidator.validateRow(any(Row.class), eq(2))).thenReturn(new ArrayList<>());
		when(shainValidator.validateAndParseDate(eq("1990/1/1"), eq(2), any())).thenReturn(new java.util.Date());

		List<ShainDAO> shainList = excelReader.readAndValidateShainList(sheet, shainValidator);

		assertEquals(1, shainList.size());
		assertEquals("1234", shainList.get(0).getShainNo());
	}

	/**
	 * テスト用の有効なExcelファイルを作成します。 ヘッダー行を含むExcelファイルを指定されたパスに保存します。
	 *
	 * @param filePath 作成するExcelファイルのパス
	 * @throws IOException ファイル操作中にエラーが発生した場合
	 */
	private void createValidExcelFile(String filePath) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet();
		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
			headerRow.createCell(i).setCellValue(EXPECTED_HEADERS.get(i));
		}
		try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
			workbook.write(fileOut);
		}
		workbook.close();
	}
}