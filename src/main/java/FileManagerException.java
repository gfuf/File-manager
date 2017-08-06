public class FileManagerException extends Exception {
    String detail;

    public FileManagerException(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return detail;
    }
}
