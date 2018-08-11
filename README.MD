[![Build Status](https://travis-ci.org/stepfair/java-jmx-mbean-dump.svg?branch=master)](https://travis-ci.org/stepfair/java-jmx-mbean-dump)

# Java agent for logging mbeans

## Build and Run

### Build

`./gradlew clean jar`

### Run Main (Single Execution)

`./gradlew clean runJar` or `java -jar path\to\java-jmx-mbean-dump-agent.jar`

### Run Agent (Periodic Execution)

`-javaagent:path\to\java-jmx-mbean-dump-agent.jar`

## Configuration

### Settings

|Setting |Default |Notes|
|---|---|---|
|csv.path |\path\to\jar\ |Where to write the mbeans.\<pid\>.csv (defaults to the jar location)|
|json.path |\path\to\jar\ |Where to read the mbeans.json config (defaults to the jar location or main resources if not found)|
|refresh.rate |60000 |Time in millis between reading and writing mbeans|
|log.level |INFO |Log level based on java.util.logging.Level (OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL)|

`-javaagent:path\to\java-jmx-mbean-dump-agent.jar -Drefresh.rate=30000 -Dlog.level=ALL -Dcsv.path=C:\data\out -Djson.path=C:\data\in`

### mbeans.json (example)

```json
{
  "java.lang:type=OperatingSystem": [
    "Name",
    "Arch",
    "Version"
  ],
  "java.lang:type=Threading": [
    "ThreadCount"
  ]
}
 ```
 
### mbeans.\<pid\>.csv (example)
|Object |Attribute |Value |Error |
|---|---|---|---|
|java.lang:type=OperatingSystem |Name |Windows 10 | |
|java.lang:type=OperatingSystem |Arch |amd64 | |
|java.lang:type=OperatingSystem |Version |10.0 | |
|java.lang:type=Threading |ThreadCount |5 | |