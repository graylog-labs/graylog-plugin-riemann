package org.graylog2.outputs.riemann;

import com.aphyr.riemann.client.*;
import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.*;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class RiemannOutput implements MessageOutput{
    private static final String CK_RIEMANN_HOST = "riemann_host";
    private static final String CK_RIEMANN_PORT = "riemann_port";
    private static final String CK_RIEMANN_PROTOCOL = "riemann_protocol";
    private static final String CK_RIEMANN_TCP_BATCH = "tcp_batch_size";
    private static final String CK_EVENT_TTL = "event_ttl";
    private static final String CK_MAP_FIELDS = "map_fields";

    private static final Logger LOG = LoggerFactory.getLogger(RiemannOutput.class);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicBoolean disconnecting = new AtomicBoolean(false);
    private AtomicBoolean needReconnect = new AtomicBoolean(false);
    private Configuration configuration;
    private IRiemannClient riemannClient;

    Runnable reconnectHandler = new Runnable()
    {
        @Override
        public void run() {
            while (true) {
                try {
                    if (needReconnect.get()) {
                        reconnect();
                    }
                    Thread.sleep(5000);
                } catch (Exception e) {
                    LOG.error("Riemann reconnect handler crashed!");
                }
            }
        }
    };

    @Inject
    public RiemannOutput(@Assisted Stream stream, @Assisted Configuration configuration) throws MessageOutputConfigurationException {
        this.configuration = configuration;

        try {
            this.riemannClient = getClient(configuration.getString(CK_RIEMANN_PROTOCOL));
            disconnecting.set(false);
            needReconnect.set(false);
            riemannClient.connect();
        } catch (IOException e) {
            LOG.error("Can not connect to Riemann server " + configuration.getString(CK_RIEMANN_HOST), e);
        }

        new Thread(reconnectHandler).start();
        isRunning.set(true);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        try {
            if (!disconnecting.get() && !needReconnect.get()) {
                sendEvent(getEventFromMessage(message));
            }

        } catch (UnsupportedOperationException e) {
            // open issue https://github.com/aphyr/riemann-java-client/issues/38
        } catch (java.io.IOException e) {
            needReconnect.set(true);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Unable to send event to Riemann server");
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
        LOG.info("Stopping Riemann output");

        disconnecting.set(true);
        if (riemannClient != null) {
            riemannClient.close();
        }

        isRunning.set(false);
    }

    private void reconnect() {
        try {
            LOG.info("Trying to reconnect to Riemann server...");
            riemannClient.reconnect();
        } catch (Exception e) {
            LOG.error("Reconnect to Riemann server failed.");
            return;
        }
        needReconnect.set(false);
    }

    private IRiemannClient getClient(String protocol) throws IOException {
        switch (protocol) {
            case "TCP":
                try {
                    return new RiemannBatchClient(RiemannClient.tcp(configuration.getString(CK_RIEMANN_HOST),
                            configuration.getInt(CK_RIEMANN_PORT)), configuration.getInt(CK_RIEMANN_TCP_BATCH));
                } catch (UnsupportedJVMException e) {
                    LOG.error("JVM doesn't support batch client, falling back to slower implementation");
                    return RiemannClient.tcp(configuration.getString(CK_RIEMANN_HOST),
                            configuration.getInt(CK_RIEMANN_PORT));
                }
            case "UDP":
                return RiemannClient.udp(configuration.getString(CK_RIEMANN_HOST),
                        configuration.getInt(CK_RIEMANN_PORT));
            default:
                LOG.error("Unsupported Riemann protocol chosen");
                throw new ProtocolException("Unsupported protocol");
        }
    }

    private EventDSL getEventFromMessage(Message message) {
        Iterator messageFields = message.getFields().entrySet().iterator();
        Iterator messageStreams = message.getStreams().iterator();
        List<String> messageStreamNames = new ArrayList<>();

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
        return event;
    }

    private void sendEvent(EventDSL event) throws IOException {
        final IPromise response = event.send();
        if (response == null) {
            LOG.error("Can not send Riemann event");
        }

        if (configuration.getString(CK_RIEMANN_PROTOCOL).equals("TCP")) {
            try {
                //executor.execute(new DerefHandler(response));
            } catch (RejectedExecutionException e) {
                LOG.error("Riemann processing too slow, can not guarantee event delivery");
            }
        }
    }

    public interface Factory extends MessageOutput.Factory<RiemannOutput> {
        @Override
        RiemannOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends MessageOutput.Config {
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
                            CK_RIEMANN_TCP_BATCH, "TCP Batch Size", 50,
                            "Number of messages that get cached before writing to Riemann",
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
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Riemann Output", false, "", "An output plugin sending Riemann events over TCP or UDP");
        }
    }
}