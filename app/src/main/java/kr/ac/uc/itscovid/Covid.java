package kr.ac.uc.itscovid;

public class Covid {
    private String gubun;
    private Integer defCnt;
    private Integer localOccCnt;
    private String stdDay;

    public String getGubun(){return gubun;}

    public Integer getDefCnt(){return defCnt;}

    public Integer getLocalOccCnt(){return localOccCnt; }

    public String getStdDay(){return stdDay;}

    public Covid(String gubun, Integer defCnt, Integer localOccCnt, String stdDay){
        this.gubun = gubun;
        this.defCnt = defCnt;
        this.localOccCnt = localOccCnt;
        this.stdDay = stdDay;
    }
}
