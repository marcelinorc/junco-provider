What is Junco?
==============

Junco (JUN-int + CO-verage) is a Surefire provider that executes JUnit tests cases and calculate coverage for each test case using Jacoco. Junco simply connects to the Jacoco agent , dump the current coverage information and resets the hit counters. Doing this each time a test case is executed,  we may expect to obtain  the coverage information solely for the executed test case.
The previous information is obtained in a first test run. In posterior runs, Junco execute test in order so the test cases covering most a certain source location passes first. The location is described in a .JSON file passed as parameter. Such location is called Transplantation point.

Usage
=====

In doc/Junco usage.docx you can see a detailed description of junco usage