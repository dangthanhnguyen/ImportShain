package com.excel.model;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * このクラスは、社員データの検証ロジック（必須項目のチェックなど）をテストします。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // lenientモードを使用
public class ShainValidatorTest {

    @InjectMocks
    private ShainValidator shainValidator;

    @Mock
    private ExcelReader excelReader;

    @Mock
    private Row row;

    /**
     * 有効なデータが入力された行に対してエラーリストが空であることを検証します。
     */
    @Test
    void testValidateRow_ValidData_ReturnsEmptyErrorList() {
        when(excelReader.getCellValue(any())).thenAnswer(invocation -> {
            int index = invocation.getArgument(0, Cell.class).getColumnIndex();
            return switch (index) {
                case 0 -> "1234";
                case 1 -> "ヤマダ タロウ";
                case 2 -> "山田 太郎";
                case 3 -> "Yamada Taro";
                case 4 -> "在籍";
                case 5 -> "001";
                case 6 -> "男";
                case 7 -> "A";
                case 8 -> "1990/1/1";
                default -> "";
            };
        });

        when(row.getCell(anyInt())).thenAnswer(invocation -> {
            int index = invocation.getArgument(0);
            Cell cell = mock(Cell.class);
            when(cell.getColumnIndex()).thenReturn(index);
            when(cell.getCellType()).thenReturn(CellType.STRING);
            when(cell.getStringCellValue()).thenReturn(switch (index) {
                case 0 -> "1234";
                case 1 -> "ヤマダ タロウ";
                case 2 -> "山田 太郎";
                case 3 -> "Yamada Taro";
                case 4 -> "在籍";
                case 5 -> "001";
                case 6 -> "男";
                case 7 -> "A";
                case 8 -> "1990/1/1";
                default -> "";
            });
            return cell;
        });

        List<String> errors = shainValidator.validateRow(row, 2);

        assertTrue(errors.isEmpty());
    }

    /**
     * 社員番号が入力されていない場合にエラーメッセージが返されることを検証します。
     */
    @Test
    void testValidateRow_MissingShainNo_ReturnsError() {
        // すべての列（0から8）をモック
        when(row.getCell(anyInt())).thenAnswer(invocation -> {
            int index = invocation.getArgument(0);
            Cell cell = mock(Cell.class);
            when(cell.getColumnIndex()).thenReturn(index);
            when(cell.getCellType()).thenReturn(index == 0 ? CellType.BLANK : CellType.STRING);
            when(cell.getStringCellValue()).thenReturn(switch (index) {
                case 0 -> "";
                case 1 -> "ヤマダ タロウ";
                case 2 -> "山田 太郎";
                case 3 -> "Yamada Taro";
                case 4 -> "在籍";
                case 5 -> "001";
                case 6 -> "男";
                case 7 -> "A";
                case 8 -> "1990/1/1";
                default -> "";
            });
            return cell;
        });

        // すべての列に対してexcelReader.getCellValueをモック
        when(excelReader.getCellValue(any())).thenAnswer(invocation -> {
            int index = invocation.getArgument(0, Cell.class).getColumnIndex();
            return switch (index) {
                case 0 -> "";
                case 1 -> "ヤマダ タロウ";
                case 2 -> "山田 太郎";
                case 3 -> "Yamada Taro";
                case 4 -> "在籍";
                case 5 -> "001";
                case 6 -> "男";
                case 7 -> "A";
                case 8 -> "1990/1/1";
                default -> "";
            };
        });

        List<String> errors = shainValidator.validateRow(row, 2);

        assertFalse(errors.isEmpty());
        assertEquals("行 2: 社員番号未入力", errors.get(0));
    }
}