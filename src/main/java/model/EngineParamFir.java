package model;

import utils.Utils;

public class EngineParamFir {
    private String arnam;
    private String motnam;
    private String name;

    public String getFullName() {
        if (!Utils.isNullOrEmpty(getMotnam()) && !Utils.isNullOrEmpty(getName())) {
            return this.getMotnam() + "_" + this.getName();
        }
        return "";
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
}
