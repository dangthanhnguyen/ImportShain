package com.excel.repository;

import java.util.Date;

public class ShainDAO {
    private String shainNo;      // 社員番号
    private String shimeiKana;   // 氏名カナ
    private String shimei;       // 氏名
    private String shimeiEiji;   // 氏名英語
    private String zaisekiKB;    // 在籍区分
    private String bumonCode;    // 部門コード
    private String seibetsu;     // 性別
    private String ketsuekiGata; // 血液型
    private Date birthDate;       // 誕生日
    private Date createDate;      // 作成日
    private Date updateDate;      // 更新日

    /**
     * 全てのパラメータを持つコンストラクタ。
     * @param shainNo 社員番号
     * @param shimeiKana 氏名カナ
     * @param shimei 氏名
     * @param shimeiEiji 氏名英語
     * @param zaisekiKB 在籍区分
     * @param bumonCode 部門コード
     * @param seibetsu 性別
     * @param ketsuekiGata 血液型
     * @param birthDate 誕生日
     * @param createDate 作成日
     * @param updateDate 更新日
     */
    public ShainDAO(String shainNo, String shimeiKana, String shimei, String shimeiEiji, 
                    String zaisekiKB, String bumonCode, String seibetsu, String ketsuekiGata, 
                    Date birthDate, Date createDate, Date updateDate) {
        this.shainNo = shainNo;
        this.shimeiKana = shimeiKana;
        this.shimei = shimei;
        this.shimeiEiji = shimeiEiji;
        this.zaisekiKB = zaisekiKB;
        this.bumonCode = bumonCode;
        this.seibetsu = seibetsu;
        this.ketsuekiGata = ketsuekiGata;
        this.birthDate = birthDate;
        this.createDate = createDate;
        this.updateDate = updateDate;
    }

    /**
     * パラメータなしのコンストラクタ。
     * 全てのフィールドをnullで初期化します。
     */
    public ShainDAO() {
    }

    /**
     * 作成日と更新日を除くパラメータを持つコンストラクタ。
     * 作成日と更新日はnullで初期化されます。
     * @param shainNo 社員番号
     * @param shimeiKana 氏名カナ
     * @param shimei 氏名
     * @param shimeiEiji 氏名英語
     * @param zaisekiKB 在籍区分
     * @param bumonCode 部門コード
     * @param seibetsu 性別
     * @param ketsuekiGata 血液型
     * @param birthDate 誕生日
     */
    public ShainDAO(String shainNo, String shimeiKana, String shimei, String shimeiEiji, 
                    String zaisekiKB, String bumonCode, String seibetsu, String ketsuekiGata,Date birthDate) {
        this.shainNo = shainNo;
        this.shimeiKana = shimeiKana;
        this.shimei = shimei;
        this.shimeiEiji = shimeiEiji;
        this.zaisekiKB = zaisekiKB;
        this.bumonCode = bumonCode;
        this.seibetsu = seibetsu;
        this.seibetsu = seibetsu;
        this.ketsuekiGata = ketsuekiGata;
        this.birthDate = birthDate;
    }

    /**
     * 社員番号を取得します。
     * @return 社員番号
     */
    public String getShainNo() {
        return shainNo;
    }

    /**
     * 社員番号を設定します。
     * @param shainNo 社員番号
     */
    public void setShainNo(String shainNo) {
        this.shainNo = shainNo;
    }

    /**
     * 氏名カナを取得します。
     * @return 氏名カナ
     */
    public String getShimeiKana() {
        return shimeiKana;
    }

    /**
     * 氏名カナを設定します。
     * @param shimeiKana 氏名カナ
     */
    public void setShimeiKana(String shimeiKana) {
        this.shimeiKana = shimeiKana;
    }

    /**
     * 氏名を取得します。
     * @return 氏名
     */
    public String getShimei() {
        return shimei;
    }

    /**
     * 氏名を設定します。
     * @param shimei 氏名
     */
    public void setShimei(String shimei) {
        this.shimei = shimei;
    }

    /**
     * 氏名英語を取得します。
     * @return 氏名英語
     */
    public String getShimeiEiji() {
        return shimeiEiji;
    }

    /**
     * 氏名英語を設定します。
     * @param shimeiEiji 氏名英語
     */
    public void setShimeiEiji(String shimeiEiji) {
        this.shimeiEiji = shimeiEiji;
    }

    /**
     * 在籍区分を取得します。
     * @return 在籍区分
     */
    public String getZaisekiKB() {
        return zaisekiKB;
    }

    /**
     * 在籍区分を設定します。
     * @param zaisekiKB 在籍区分
     */
    public void setZaisekiKB(String zaisekiKB) {
        this.zaisekiKB = zaisekiKB;
    }

    /**
     * 部門コードを取得します。
     * @return 部門コード
     */
    public String getBumonCode() {
        return bumonCode;
    }

    /**
     * 部門コードを設定します。
     * @param bumonCode 部門コード
     */
    public void setBumonCode(String bumonCode) {
        this.bumonCode = bumonCode;
    }

    /**
     * 性別を取得します。
     * @return 性別
     */
    public String getSeibetsu() {
        return seibetsu;
    }

    /**
     * 性別を設定します。
     * @param seibetsu 性別
     */
    public void setSeibetsu(String seibetsu) {
        this.seibetsu = seibetsu;
    }
    
    /**
     * 血液型を取得します。
     * @return 血液型
     */
    public String getKetsuekiGata() {
        return ketsuekiGata;
    }
    
    /**
     * 血液型を設定します。
     * @param ketsuekiGata 血液型
     */
    public void setKetsuekiGata(String ketsuekiGata) {
        this.ketsuekiGata = ketsuekiGata;
    }

    /**
     * 誕生日を取得します。
     * @return 誕生日
     */
    public Date getBirthDate() {
        return birthDate;
    }

    /**
     * 誕生日を設定します。
     * @param birthDate 誕生日
     */
    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    /**
     * 作成日を取得します。
     * @return 作成日
     */
    public Date getCreateDate() {
        return createDate;
    }

    /**
     * 作成日を設定します。
     * @param createDate 作成日
     */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    /**
     * 更新日を取得します。
     * @return 更新日
     */
    public Date getUpdateDate() {
        return updateDate;
    }

    /**
     * 更新日を設定します。
     * @param updateDate 更新日
     */
    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    /**
     * オブジェクトの文字列表現を返します。
     * @return オブジェクトの文字列表現
     */
    @Override
    public String toString() {
        return "ShainDAO{" +
                "shainNo='" + shainNo + '\'' +
                ", shimeiKana='" + shimeiKana + '\'' +
                ", shimei='" + shimei + '\'' +
                ", shimeiEiji='" + shimeiEiji + '\'' +
                ", zaisekiKB='" + zaisekiKB + '\'' +
                ", bumonCode='" + bumonCode + '\'' +
                ", seibetsu='" + seibetsu + '\'' +
                ", ketsuekiGata='" + ketsuekiGata + '\'' +
                ", birthDate=" + birthDate +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                '}';
    }
}