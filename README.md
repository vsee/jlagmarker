# Interaction Lag Detection in Interactive Workloads

JLagmarker automatically detects user interactions and corresponding
system responses (interaction lag) in screen recordings of interactive workloads.

It produces a profile of user interaction lag for a given workload and system
configuration. Different interaction lag profiles of the same workload can be 
compared to analyse how different system configurations affect user perceived
system response times.

Read the the corresponding [paper](http://homepages.inf.ed.ac.uk/s1160444/docs/measuring_qoe_IISWC2014.pdf) 
for more details and a case study. It was published at [IISWC'14](http://www.iiswc.org/iiswc2014/) 
and awarded the conference's **Best Paper Award**.

## Building the Project

### Dependencies

* Java 8

### Build

Build project and jar files with:

```$ ./gradlew build```

Clean with:

```$ ./gradlew clean```

Build Eclipse project with:

```$ ./gradlew eclipse```

then import existing project into eclipse

