package exception;

public class NoSuchRedirect extends RuntimeException {
    public NoSuchRedirect(String msg){
        super(msg);
    }
}
