Riemann output plugin
=====================

An output plugin for integrating [Riemann](http://riemann.io) with [Graylog](https://www.graylog.org).

## Instructions

#### Step 1: Installing the plugin

Copy the `.jar` file that you received to your Graylog plugin directory which is configured in your `server.conf` configuration file using the `plugin_dir` variable. Restart the `graylog-server` process to load the plugin.

Note that you should do this for every `graylog-server` instance you are running.

#### Step 2: Configuring the plugin

You should now be able to add a Riemann output to your streams through the option `Manage outputs`.

![Screenshot: Riemann Output Settings](https://s3.amazonaws.com/graylog2public/images/plugin-riemann-settings.png)

The important parameters are the host address and port number to successfully establish a connection to Riemann.
Additionally  the plugin can send the log message as one JSON string or automatically extract every field as a Reimann custom event field.

You will now receive messages from this stream in Riemann.

## Build

This project is using Maven and requires Java 7 or higher.

You can build the plugin (JAR) with `mvn package`. DEB and RPM packages can be build with `mvn jdeb:jdeb` and `mvn rpm:rpm` respectively.

## Credits

Thanks to Henrik Johansen and Region Syddanmark for sponsorship!
