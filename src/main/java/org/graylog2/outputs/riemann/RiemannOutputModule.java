package org.graylog2.outputs.riemann;

import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.outputs.MessageOutput;

public class RiemannOutputModule extends PluginModule {
    @Override
    protected void configure() {
        final MapBinder<String, MessageOutput.Factory<? extends MessageOutput>> outputMapBinder = outputsMapBinder();
        installOutput(outputMapBinder, RiemannOutput.class, RiemannOutput.Factory.class);
    }
}
