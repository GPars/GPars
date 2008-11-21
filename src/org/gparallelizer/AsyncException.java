package org.gparallelizer;

import java.util.List;

/**
 * This class wraps multiple exception, which occured in concurrently run code inside one of the <i>AsyncInvokerUtil</i> methods.
 * @see AsyncInvokerUtil
 *
 * @author Vaclav Pech
 * Date: Nov 17, 2008
 */
public class AsyncException extends RuntimeException {
    private final List<Throwable> concurrentExceptions;

    public AsyncException(String message, List<Throwable> concurrentExceptions) {
        super(message);
        this.concurrentExceptions = concurrentExceptions;
    }

    public List<Throwable> getConcurrentExceptions() {
        return concurrentExceptions;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " " + buildMessage();
    }

    @Override
    public String toString() {
        return buildMessage();
    }

    private String buildMessage() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AsyncException");
        sb.append("{concurrentExceptions=").append("[\n");
        for (final Throwable cuncurrentException : concurrentExceptions) {
            sb.append(cuncurrentException.toString()).append("\n");
        }
        sb.append("]}");
        return sb.toString();
    }
}
