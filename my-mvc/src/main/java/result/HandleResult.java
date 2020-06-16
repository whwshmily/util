package result;

public class HandleResult {
    private String content;
    private Object object;

    public HandleResult() {
    }

    public HandleResult(String content, Object object) {
        this.content = content;
        this.object = object;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
