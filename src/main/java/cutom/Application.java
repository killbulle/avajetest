package cutom;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class Application {

    private final List<Strategy> controllers;

    @Inject
    public Application(List<Strategy> controllers) {
        this.controllers = controllers;
    }



    @Inject
    public Optional<Feature> feature;
    public void launch()
    {
        feature.ifPresent(feature1 -> feature1.launch());
    }

    public List<Strategy> getControllers() {
        return controllers;
    }

}
