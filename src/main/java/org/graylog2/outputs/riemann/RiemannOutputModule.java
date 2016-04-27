package org.graylog2.outputs.riemann;

import org.graylog2.plugin.PluginModule;

public class RiemannOutputModule extends PluginModule {
    @Override
    protected void configure() {
        addMessageOutput(RiemannOutput.class, RiemannOutput.Factory.class);
    }
}
