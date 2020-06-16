package exception;

import annotation.RequestMapping;

public class RequestMappingException extends RuntimeException {
    public RequestMappingException(String msg){
        super(msg);
    }
}
