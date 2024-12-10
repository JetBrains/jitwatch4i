# JITWatch4i

**JITWatch4i** is an IntelliJ IDEA plugin for analyzing and visualizing logs from the HotSpot JIT (Just-In-Time) compiler. It is built upon the foundations of [JitWatch](https://github.com/AdoptOpenJDK/jitwatch) by Chris Newland and [jitwatch-intellij](https://github.com/yole/jitwatch-intellij) by Dmitry Jemerov.

## Features
- **Log Parsing**: Parse and analyze HotSpot JIT compiler logs to identify performance optimizations and bottlenecks.
- **Visualization**: Gain insights into JIT compilation events through visualizations.
- **IntelliJ Integration**: Directly view C1/C2 assembly and bytecode for Java files opened in the IntelliJ editor

## Creating and Loading the Compilation Log

The simple option for creating and loading the compilation log is to enable the "Log compilation" option in the "JITWatch" tab of the run configuration settings.

If you enable the option, the plugin will create a HotSpot log in a temporary directory.

Alternatively, you can add the logging options to the VM options of your run configuration, and then load the log file manually. To enable logging, you need the following options:

-XX:+UnlockDiagnosticVMOptions
-XX:+TraceClassLoading
-XX:+LogCompilation
-XX:+PrintAssembly


## Credits
This plugin is based on:
- [JitWatch](https://github.com/AdoptOpenJDK/jitwatch) by Chris Newland.
- [jitwatch-intellij](https://github.com/yole/jitwatch-intellij) by Dmitry Jemerov

