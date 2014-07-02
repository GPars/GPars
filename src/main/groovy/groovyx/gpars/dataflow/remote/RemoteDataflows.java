package groovyx.gpars.dataflow.remote;

import groovyx.gpars.dataflow.DataflowVariable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteDataflows {
    private static Map<String, DataflowVariable<?>> publishedVariables = new ConcurrentHashMap<>();

    public static <T> void publish(DataflowVariable<T> variable, String name) {
        publishedVariables.put(name, variable);
    }

    public static DataflowVariable<?> get(String name) {
        return publishedVariables.get(name);
    }
}
