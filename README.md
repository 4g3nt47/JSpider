# JSpider
JSpider is an advanced, multi-threaded Java library for crawling websites.
It can be used as a command line program with all it's primary features.
JSpider's start() function is non-blocking, which allow the invoking code to
access URLs live as they are captured (see the getOutputQueue() function).

Starting with version 1.1, you can extend the functionalities of JSpider by
creating custom plugins. See the [plugins](https://github.com/UmarAbdul01/JSpider/plugin/)
directory for some samples.

## Dependencies.
* [JBrowser](https://github.com/UmarAbdul01/JBrowser)
* [ArgParser](https://github.com/UmarAbdul01/ArgParser)

## Build.

* `$ git clone https://github.com/UmarAbdul01/JSpider.git`
* `$ cd JSpider`
* `$ ant fat-jar`
* $ java -jar jspider.jar --help
