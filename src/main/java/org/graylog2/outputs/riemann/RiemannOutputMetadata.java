package org.graylog2.outputs.riemann;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;

public class RiemannOutputMetadata implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return RiemannOutput.class.getCanonicalName();
    }

    @Override
    public String getName() {
        return "Riemann Output Plugin";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc.";
    }

    @Override
    public URI getURL() {
        return URI.create("https://www.graylog.org");
    }

    @Override
    public Version getVersion() {
        return new Version(1, 1, 3);
    }

    @Override
    public String getDescription() {
        return "Output plugin that writes messages to a Riemann instance.";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(2, 0, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return EnumSet.of(ServerStatus.Capability.SERVER);
    }
}
