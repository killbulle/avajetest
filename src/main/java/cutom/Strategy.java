package cutom;

import java.util.HashMap;
import java.util.Map;

public abstract class Strategy {

    Map<String, String> getContext() {
        return new HashMap<>();
    }

    abstract public void  stop();


}
