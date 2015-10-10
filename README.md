![Logo](../../wiki/images/sensorhub_logo_128.png)
SensorHub
===========================================================

SensorHub software allows one to easily build interoperable and evolutive sensor networks with advanced processing capabilities and based on open-standards for all data exchanges. These open-standards are mostly [OGC](http://www.opengeospatial.org) standards from the [Sensor Web Enablement](http://www.opengeospatial.org/projects/groups/sensorwebdwg) (SWE) initiative and are key to design sensor networks that can largely evolve with time (addition of new types of sensors, reconfigurations, etc.).

The framework allows one to connect any kind of sensors and actuators to a common bus via a simple yet generic driver API. Sensors can be connected through any available hardware interface such as [RS232/422](http://en.wikipedia.org/wiki/RS-232), [SPI](http://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus), [I2C](http://en.wikipedia.org/wiki/I%C2%B2C), [USB](http://en.wikipedia.org/wiki/USB), [Ethernet](http://en.wikipedia.org/wiki/Ethernet), [Wifi](http://en.wikipedia.org/wiki/Wi-Fi), [Bluetooth](http://en.wikipedia.org/wiki/Bluetooth), [ZigBee](http://en.wikipedia.org/wiki/ZigBee), [HTTP](http://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), etc... Once drivers are available for a specific sensor, it is automatically connected to the bus and it is then trivial to send commands and read data from it. An intuitive user interface allows the user to configure the network to suit its needs and more advanced processing capabilities are available via a plugin system.

SensorHub embeds the full power of OGC web services ([Sensor Observation Service](http://www.opengeospatial.org/standards/sos) or SOS, [Sensor Planning Service](http://www.opengeospatial.org/standards/sps) or SPS) to communicate with all connected sensors in the network as well as to provide robust metadata (owner, location and orientation, calibration, etc.) about them. Through these standard, several SensorHub instances can also communicate with each other to form larger networks.

Low level functions of SensorHub (send commands and read data from sensor) are coded efficiently and can be used on embedded hardware running [Java SE®](http://www.oracle.com/technetwork/java/javase), [Java ME®](http://www.oracle.com/technetwork/java/embedded/javame) or [Android®](http://www.android.com) while more advanced data processing capabilities are fully multi-threaded and can thus benefit from a more powerful hardware platform (e.g. multi-processor servers or even clusters).


## License

SensorHub is licensed under the [Mozilla Public License version 2.0](http://www.mozilla.org/MPL/2.0/).


## Using

Refer to the [Documentation Site](http://docs.opensensorhub.org/) for instructions on how to install and use SensorHub, as well as get the latest news.

You can also go to this [Demo Page](http://sensiasoft.net:8181/demo.html) to see SensorHub in action with a few example sensor streams (Video, GPS, predicted satellite positions, etc.).


## Building

SensorHub can be built using Maven either from the command line or within Eclipse. Please see the [Developer's Guide](http://docs.opensensorhub.org/dev-guide.html) for detailed instructions.


## Contributing

Refer to the [Developer's Guide](http://docs.opensensorhub.org/dev-guide.html) for instructions on how to setup your development environment in Eclipse.

You can also find documentation in the [Javadocs](http://docs.opensensorhub.org/sensorhub-core/apidocs) and [wiki](../../wiki/Home). 

Several sensor driver examples are also available in the source code to help you get started.
