package cutom;

import io.avaje.inject.PreDestroy;
import jakarta.inject.Singleton;

import java.util.Map;

@Singleton
public class Strat2 extends Strategy {

  @Override
  Map<String, String> getContext() {
    Map<String, String> context = super.getContext();
    context.put("something", "2");
    return context;
  }

  @Override
  @PreDestroy
  public void stop() {
    System.out.println("terminating strat2");
  }
}
