package org.graylog2.outputs.riemann;

import com.google.common.collect.Lists;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Collection;

public class RiemannOutputPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new RiemannOutputMetadata();
    }

    @Override
    public Collection<PluginModule> modules () {
        return Lists.newArrayList((PluginModule) new RiemannOutputModule());
    }
}
