package io.wisocn.sink;
import java.util.List;
import java.util.Map;

public interface Sink<E> {

    String getType();

    String getName();

    void submit(List<Map<String, Object>> record);


    void forceFlush();

}
