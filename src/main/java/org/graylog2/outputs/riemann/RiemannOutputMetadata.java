package org.graylog2.outputs.riemann;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.Version;

import java.net.URI;

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
        return "TORCH GmbH";
    }

    @Override
    public URI getURL() {
        return URI.create("http://www.torch.sh");
    }

    @Override
    public Version getVersion() {
        return new Version(0, 93, 0);
    }

    @Override
    public String getDescription() {
        return "Output plugin that writes messages to a Riemann instance.";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(0, 93, 0);
    }
}
