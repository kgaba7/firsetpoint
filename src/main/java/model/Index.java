package model;

/**
 * Created by Gabor Kiss on 2019. 02. 25.
 */
public class Index {
    private int arnam;
    private int motnam;
    private int name;
    private int meaval;
    private int acttim;

    public int getActtim() {
        return acttim;
    }

    public void setActtim(int acttim) {
        this.acttim = acttim;
    }

    public int getMotnam() {
        return motnam;
    }

    public void setMotnam(int motnam) {
        this.motnam = motnam;
    }

    public int getArnam() {
        return arnam;
    }

    public void setArnam(int arnam) {
        this.arnam = arnam;
    }

    public int getName() {
        return name;
    }

    public void setName(int name) {
        this.name = name;
    }

    public int getMeaval() {
        return meaval;
    }

    public void setMeaval(int meaval) {
        this.meaval = meaval;
    }
}
