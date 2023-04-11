# maven-build-profiler

## What's this?

This is a [Maven extension](https://maven.apache.org/guides/mini/guide-using-extensions.html) that tracks execution times for each plugin and lifecycle phase and sends them (along with some other performance-related info like CPU count, Maven options, some JVM options etc.) as JSON to a REST servlet that persists those metrics to PostgreSQL database.

I've included a tiny web frontend for exploring the data a bit but obviously one could write a much more fancy one.

# Technologies used

Maven, Spring Boot, Apache Wicket, D3.js, PostgreSQL

# Requirements

## Building / Running

- Maven 3.3.x 
- JDK >= 17

# How to build

Just run 


```
    mvn clean install
```

## Integration into build process

Note that the Maven extension configured here requires JDK >= 17 and Maven 3.3.x

- Step 1: Create a .mvn folder in your project's top-level directory and create a file named 'extensions.xml' with the following content inside of it:

```
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
      <groupId>de.code-sourcery.maven.buildprofiler</groupId>
      <artifactId>maven-build-profiler-extension</artifactId>
      <version>1.0.3</version>
    </extension>
</extensions>
```

- Step 2: Edit your top-level pom.xml file and define a profile like the following 

```
  <profiles>
    <profile>
      <id>tracking</id>
      <properties>
        <buildTimeTrackingEnabled>true</buildTimeTrackingEnabled>
        <buildTimeTrackingProject>Project A</buildTimeTrackingProject>
        <buildTimeTrackingBranch>Branch #1</buildTimeTrackingBranch>
        <buildTimeTrackingServerUrl>http://localhost:8080/mavenBuildProfiler/api/receive</buildTimeTrackingServerUrl>
      </properties>
    </profile>
  </profiles>
```

You'll obviously want to adjust those values AND you need to have already installed the server-side for it to actually work. If you're using GIT for version control, the extension will automatically include the currently checked-out GIT SHA1 into the generated report.

Actually tracking the build times is then just a matter of running something like

```
    mvn -Ptracking clean install
```

Build times for each plugin, project and Maven lifecycle phase are tracked continuously and all sent in a single HTTP request at the very end of a successful build. Failed builds will not send any information to the server.

# Server-side installation

You'll need

- JDK >= 17
- PostgreSQL >= 12 (older versions will probably work just fine)
- (a servlet container that is NOT yet using the new Jakarta J2EE packages, for example Tomcat <= 9)

You can either deploy the server-side part (which is just a WAR file) in a servlet container of your choosing OR you can just run it using an embedded Jetty courtesy of Spring Boot. You'll need to have a correctly initialized PostgreSQL database either way 

## Setting up the database

First, you'll need to create the database itself using a PostgreSQL admin user account that has CREATE DATABASE permissions like so

```
    cat database/create_database.sql | psql -U<admin user> -h <your DB host>
```

You most likely want to modify the password and/or username in create_database.sql. Make sure to adjust the mavenBuildProfiler.properties configuration file (see below) accordingly if you do so.

Afterwards, you'll have to create the actual database schema by executing all the SQL files inside database/updates in temporal order like so.

```
    cat database/initial_schema.sql | psql -Uprofiler -h <your DB host> mavenbuildprofiler
```

Again, you'd have to adjust the PostgreSQL user name etc. if you changed them during the previous step.

## WAR file deployment

You'll find the WAR file in war/target/mavenBuildProfiler.war
After copying the file, you'll need to edit and put config/mavenBuildProfiler.properties on your classpath along with a suitable log4j2.xml configuration.
