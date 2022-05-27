package model;

import java.time.LocalTime;

public class FirModel {
    private String arnam;
    private String motnam;
    private String name;
    private Integer colNum;
    private Double prevValue;
    private Double actValue;
    private Double sumValue;
    private LocalTime timeLimit;

    public FirModel(String arnam, String motnam, String name) {
        this.arnam = arnam;
        this.motnam = motnam;
        this.name = name;
        this.setTimeLimit(LocalTime.of(0, 15));
        this.colNum = -1;
        this.prevValue = 0d;
        this.actValue = 0d;
        this.sumValue = 0d;
    }

    public String getArnam() {
        return arnam;
    }

    public void setArnam(String arnam) {
        this.arnam = arnam;
    }

    public String getMotnam() {
        return motnam;
    }

    public void setMotnam(String motnam) {
        this.motnam = motnam;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getColNum() {
        return colNum;
    }

    public void setColNum(Integer colNum) {
        this.colNum = colNum;
    }

    public Double getPrevValue() {
        return prevValue;
    }

    public void setPrevValue(Double prevValue) {
        this.prevValue = prevValue;
    }

    public Double getSumValue() {
        return sumValue;
    }

    public void setSumValue(Double sumValue) {
        this.sumValue = sumValue;
    }

    public Double getActValue() {
        return actValue;
    }

    public void setActValue(Double actValue) {
        this.actValue = actValue;
    }

    public LocalTime getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(LocalTime timeLimit) {
        this.timeLimit = timeLimit;
    }
}
