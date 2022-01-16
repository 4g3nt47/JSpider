# JSpider

JSpider is an advanced, multi-threaded Java library for crawling websites. It can be used as a command line program with all it's primary features. JSpider's `JSpider.start()` function is non-blocking, which allows the invoking code to access URLs live as they are captured (see the `JSpider.getOutputQueue()` function).

Starting with version 1.1, you can extend the functionalities of JSpider by creating custom plugins. See the [plugins](/src/com/umarabdul/jspider/plugin/) directory for some samples.

## Build.

* `$ git clone https://github.com/4g3nt47/JSpider.git`
* `$ cd JSpider`
* `$ ant fat-jar`
* `$ java -jar jspider.jar --help`
