package org.graylog2.outputs.riemann;

import com.aphyr.riemann.Proto;
import com.aphyr.riemann.client.EventDSL;
import com.aphyr.riemann.client.RiemannClient;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.*;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class RiemannOutput implements MessageOutput{
    private static final String CK_RIEMANN_HOST = "riemann_host";
    private static final String CK_RIEMANN_PORT = "riemann_port";
    private static final String CK_RIEMANN_PROTOCOL = "riemann_protocol";
    private static final String CK_EVENT_TTL = "event_ttl";
    private static final String CK_MAP_FIELDS = "map_fields";

    private static final Logger LOG = LoggerFactory.getLogger(RiemannOutput.class);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Configuration configuration;
    private RiemannClient riemannClient;

    @Override
    public void initialize(Configuration config) {
        this.configuration = config;

        try {
            if (configuration.getString(CK_RIEMANN_PROTOCOL).equals("TCP")) {
                this.riemannClient = RiemannClient.tcp(configuration.getString(CK_RIEMANN_HOST),
                        configuration.getInt(CK_RIEMANN_PORT));
            } else if (configuration.getString(CK_RIEMANN_PROTOCOL).equals("UDP")) {
                this.riemannClient = RiemannClient.udp(configuration.getString(CK_RIEMANN_HOST),
                        configuration.getInt(CK_RIEMANN_PORT));
            } else {
                throw new ProtocolException("Unsupported protocol");
            }
            riemannClient.connect();
        } catch (IOException e) {
            LOG.error("Can not connect to Riemann server " + configuration.getString(CK_RIEMANN_HOST), e);
        }

        isRunning.set(true);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        Iterator messageFields = message.getFields().entrySet().iterator();
        Iterator messageStreams = message.getStreams().iterator();
        List<String> messageStreamNames = new ArrayList<>();

        try {
            EventDSL event = riemannClient.event()
                    .host(message.getSource())
                    .time(message.getFieldAs(DateTime.class, "timestamp").getMillis())
                    .description(message.getMessage())
                    .ttl(configuration.getInt(CK_EVENT_TTL));

            while (messageStreams.hasNext()) {
                Stream stream = (Stream) messageStreams.next();
                messageStreamNames.add(stream.getTitle());
            }
            if (! messageStreamNames.isEmpty()) {
                event.tags(messageStreamNames);
            }

            if (configuration.getBoolean(CK_MAP_FIELDS)) {
                while (messageFields.hasNext()) {
                    Map.Entry pair = (Map.Entry) messageFields.next();
                    event.attribute(String.valueOf(pair.getKey()), String.valueOf(pair.getValue()));
                }
            }

            final Proto.Msg response = event.send().deref(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!response.getOk()) {
                throw new TimeoutException("Timed out");
            }
        } catch (UnsupportedOperationException e) {
            // open issue https://github.com/aphyr/riemann-java-client/issues/38
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Can not send event to Riemann server");
        }
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
        }

    }

    @Override
    public void stop() {
        LOG.info("Stopping");
        if (riemannClient != null) {
            riemannClient.close();
        }
        isRunning.set(false);
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest configurationRequest = new ConfigurationRequest();

        configurationRequest.addField(new TextField(
                        CK_RIEMANN_HOST, "Riemann Host", "",
                        "Hostname of your Riemann instance",
                        ConfigurationField.Optional.NOT_OPTIONAL)
        );

        configurationRequest.addField(new NumberField(
                        CK_RIEMANN_PORT, "Riemann Port", 5555,
                        "Port of your Riemann instance",
                        ConfigurationField.Optional.OPTIONAL)
        );

        final Map<String, String> protocols = ImmutableMap.of(
                "TCP", "TCP",
                "UDP", "UDP");
        configurationRequest.addField(new DropdownField(
                        CK_RIEMANN_PROTOCOL, "Riemann Protocol", "TCP", protocols,
                        "Protocol that should be used to talk to Riemann",
                        ConfigurationField.Optional.OPTIONAL)
        );

        configurationRequest.addField(new NumberField(
                        CK_EVENT_TTL, "Event TTL", 60,
                        "Time in seconds that an event is considered vaild",
                        ConfigurationField.Optional.OPTIONAL)
        );

        configurationRequest.addField(new BooleanField(
                        CK_MAP_FIELDS, "Create custom event fields", true,
                        "Convert message fields automatically to custom event fields")
        );

        return configurationRequest;
    }

    @Override
    public String getName() {
        return "Riemann Output";
    }

    @Override
    public String getHumanName() {
        return getName();
    }

    @Override
    public String getLinkToDocs() {
        return "http://graylog2.org";
    }

}