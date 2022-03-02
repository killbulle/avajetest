package cutom;

import io.avaje.config.Config;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;

import java.util.Optional;

@Factory
public class FeatureFactory {

    @Bean
    public Feature create() {
        if (Config.getBool("strat.togglefeaturesA")) {
            return getA();
        }

        return null;
    }

    private Feature getA() {
        return new Feature() {
            @Override
            public void launch() {
                System.out.println("AAAAA");
            }
        };
    }

}
