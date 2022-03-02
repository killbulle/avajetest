package cutom;

import io.avaje.config.Config;
import io.avaje.inject.BeanScope;

import java.util.List;

import static java.lang.Thread.sleep;


class Main {

    public static void main(String[] args) {
        try (BeanScope beanScope = BeanScope
                .newBuilder()
                .withShutdownHook(true).build()) {
            Application application = beanScope.get(Application.class);
            List<? extends Strategy> strategies = application.getControllers();
            System.out.println(strategies.size());
            application.launch();
            String value = Config.get("strat.fooName");
            String foo = Config.get("strat.fooprops");
            System.out.println( foo );
            System.out.println(value);
            System.out.println(Config.get("strat.togglefeatures"));
            Config.onChange("strat.togglefeatures", newValue -> {
                System.out.println("change");
                application.getControllers().get(0).stop();
            });

        Thread t = new Thread(() -> {
            while (true)
            {
                try {
                    sleep(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();
        while(true)
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        }
    }


}
