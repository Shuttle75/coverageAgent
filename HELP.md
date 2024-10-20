# Getting Started

Java agent, for checking test coverage, when tests run on TS

When agent is run, 
  on start appeared message - "Agent is running"
  at the end appeared message - "Running Shutdown Hook. Unload report"

### Run Command

-javaagent:/home/shuttle/IdeaProjects/coverageAgent/target/get-methods-1.0-SNAPSHOT.jar=/report.log;com.package.details

/report.log;com.package - agent arguments, separated by ;, where:
               /report.log - path of report
       com.package.details - path of scanned and filtered classes

### Reference Documentation
For further reference, please consider the following sections:

* [The java Command](https://docs.oracle.com/en/java/javase/22/docs/specs/man/java.html)
* [Package java.lang.instrument](https://docs.oracle.com/javase/10/docs/api/java/lang/instrument/package-summary.html)
* [Byte Buddy](https://bytebuddy.net/)
