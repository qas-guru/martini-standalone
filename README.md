# Martini Standalone 

## Table of Contents
1. [What is Martini Standalone?](#what)
1. [How does Martini Standalone work?](#how)
1. [Where can I find more information?](#info)

### What is Martini Standalone? <a name="what"></a>

Martini Standalone is a Java command-line harness for executing [Martini](https://github.com/qas-guru/martini-core) 
test scenarios.

### How does Martini Standalone work? <a name="how"></a>

The library leverages [Cedric Beust](https://beust.com/weblog/)'s [JCommander](http://jcommander.org/) to 
collect command-line switches. It then configures and starts a 
[Spring Framework](https://spring.io/projects/spring-framework) application, subsequently
locating and executing requested scenarios.

#### In Progress: [Martini Standalone Wiki](https://github.com/qas-guru/martini-standalone/wiki)