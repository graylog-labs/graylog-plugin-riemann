package org.graylog2.outputs.riemann;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Collection;
import java.util.Collections;

public class RiemannOutputPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new RiemannOutputMetadata();
    }

    @Override
    public Collection<PluginModule> modules() {
        return Collections.singleton(new RiemannOutputModule());
    }
}
