# Martini Standalone 

## Table of Contents
1. [What is Martini Standalone?](#what)
1. [How does Martini Standalone work?](#how)
	1. [How do I run my suite using Martini Standalone?](#how-execute)
1. [Where can I find more information?](#info)
	1. [Wiki](#wiki)

### What is Martini Standalone? <a name="what"></a>

Martini Standalone is a Java command-line harness for executing [Martini](https://github.com/qas-guru/martini-core) 
test scenarios.

### How does Martini Standalone work? <a name="how"></a>

The library leverages [Cedric Beust](https://beust.com/weblog/)'s [JCommander](http://jcommander.org/) to 
collect command-line switches. It then configures and starts a 
[Spring Framework](https://spring.io/projects/spring-framework) application, subsequently
locating and executing requested scenarios.

#### How do I run my suite using Martini Standalone? <a name="how-execute"></a>
The Main entry-point class has a help switch detailing available options

	java -cp martini-standalone-4.0.0-JDK10.jar guru.qas.martini.standalone.Main -help


### Where can I find more information? <a name="info"></a>

#### In Progress: [Martini Standalone Wiki](https://github.com/qas-guru/martini-standalone/wiki) <a name="wiki"></a>
#### In Progress: [__Martini - swank software testing in Java__](https://leanpub.com/martini) <a name="book"></a>