package groovyx.gpars.activeobject;

/**
 * @author Kirill Vergun <code@o-nix.me>
 * @since 14.11.14
 */
public interface ActorWithExceptionHandler {
    Object recoverFromException(String methodName, Exception e);
}