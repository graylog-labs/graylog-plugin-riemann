Riemann Plugin for Graylog
==========================

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-riemann.svg)](https://travis-ci.org/Graylog2/graylog-plugin-riemann)

An output plugin for integrating [Riemann](http://riemann.io) with [Graylog](https://www.graylog.org).

**Required Graylog version:** 1.0 and later

## Installation

[Download the plugin](https://github.com/Graylog2/graylog-plugin-riemann/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Usage

You should now be able to add a Riemann output to your streams through the option `Manage outputs`.

![Screenshot: Riemann Output Settings](https://s3.amazonaws.com/graylog2public/images/plugin-riemann-settings.png)

The important parameters are the host address and port number to successfully establish a connection to Riemann.
Additionally  the plugin can send the log message as one JSON string or automatically extract every field as a Riemann custom event field.

You will now receive messages from this stream in Riemann.

## Build

This project is using Maven and requires Java 7 or higher.

You can build a plugin (JAR) with `mvn package`.

DEB and RPM packages can be build with `mvn jdeb:jdeb` and `mvn rpm:rpm` respectively.

## Plugin Release

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. TravisCI will build the release artifacts and upload to GitHub automatically.

## Credits

Thanks to Henrik Johansen and Region Syddanmark for sponsorship!
