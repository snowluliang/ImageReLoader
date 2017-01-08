package snow.com.imagereloader.bean;


public class FolderBean {

    /**
     * 文件路径
     */
    private String dir;
    //第一张图片路径
    private String firstImgPath;

    private String name;

    private int count;

    public FolderBean() {
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;

        int lastIndexOf = this.dir.lastIndexOf("/");
        this.name = this.dir.substring(lastIndexOf);

    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getName() {
        return name;
    }


    public FolderBean(int count, String dir, String firstImgPath, String name) {
        this.count = count;
        this.dir = dir;
        this.firstImgPath = firstImgPath;
        this.name = name;
    }
}
