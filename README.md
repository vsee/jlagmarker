# Interaction Lag Detection in Interactive Workloads

<div align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.en.html" target="_blank">
    <img src="https://img.shields.io/badge/license-GNU%20GPL%20v3-blue.svg?style=flat">
  </a>
</div>

**JLagmarker** automatically detects user interactions and corresponding
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

## Executing Sample Workload

This [archive](https://drive.google.com/open?id=0BziCZ9zd_KatQm9UTVhqanFoMnM) 
contains all the necessary data to execute jlagmarker
for an example workload. The workload was recorded on a 
*Qualcomm Dragonboard APQ8074* running *Android Jelly Bean version 4.2.2* 
with *Linux kernel 3.4.0*.

**NOTE: Further information on how to collect the provided workload 
data can be found in the [publication](http://homepages.inf.ed.ac.uk/s1160444/docs/measuring_qoe_IISWC2014.pdf) 
mentioned above or my [dissertation](http://homepages.inf.ed.ac.uk/s1160444/publications.html).**

The data archive contains:

* a video of the screen output during workload execution (dataset01.ts)
* a description of touch screen input event timings (userinput_dataset01.csv)
* a sample configuration file for [lag end suggestions](#lag-end-suggestion) (suggester_config_dataset01.csv)

### Executing Interaction Lag Annotation

### Executing Interaction Lag Detection

### Executing Framedump



## JLagmarker Detailed Description

### Lag End Suggestion


## License

Copyright 2017 Volker Seeker <volker@seekerscience.co.uk>.

Released under the terms of the GPLv3 license. See [LICENSE.txt](/LICENSE.txt)
for details.

