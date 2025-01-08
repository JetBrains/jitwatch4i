# JITWatch4i

![Screenshot_20241219_222045](https://github.com/user-attachments/assets/3a20e4ee-3f01-4243-9370-455d5b6fe9c9)

Analyzes and visualizes logs from the HotSpot JIT (Just-In-Time) compiler,  built upon the foundations of [JitWatch](https://github.com/AdoptOpenJDK/jitwatch) by Chris Newland and [jitwatch-intellij](https://github.com/yole/jitwatch-intellij) by Dmitry Jemerov.

#### Features

- **Main Panel**: Lists compiled methods organized in a class tree, shows compilation times, optimization levels (L1–L4) and compiler type.
- **Code Integration**: Displays bytecode and assembly code side-by-side with the original Java source, with syntax highlighting.

#### Additional Features

- **Tops** :  displays methods ranked by various metrics, including the largest generated code, longest compilation times, most frequent deoptimizations, and failed inlining attempts.
- **Comp. Activity**: shows compiler thread activity over time, with compilations represented by rectangles sized by generated code. Compilations are selectable.
- **Time Line**:  shows a timeline of compilations categorized by optimization levels (L1–L4), illustrating how compilation activity changes over time. Levels L1+L2+L3 are compiled by C1, L4 is compiled by C2.
- **Histo**:  provides histograms to analyze the distribution of compiled code sizes, compilation durations, and sizes of inlined methods.
- **FreeCC**:  tracks the allocated and free memory in the Code Cache over time.
- **CCLayout**:  displays a detailed layout of the Code Cache memory.
- **Comp. Chain**:  shows the compilation structure of methods, including inlined methods and external calls.
- **Code Suggest**:  identifies performance-critical areas such as:
    - **Hot Methods**: shows info about methods that were not inlined, with reasons for inlining failures.
    - **Branch Prediction**: identifies unpredictable branches that impact performance.

### Creating and Loading the Compilation Log

The simple option for creating and loading the compilation log is to enable the "Log compilation" option in the run configuration settings.

If you enable the option, the plugin will create a HotSpot log in a temporary directory.

Alternatively, you can add the logging options to the VM options of your run configuration, and then load the log file manually. To enable logging, you need the following options:
```
-XX:+UnlockDiagnosticVMOptions
-XX:+TraceClassLoading
-XX:+LogCompilation
-XX:+PrintAssembly
```

### Credits
This plugin is based on:
- [JitWatch](https://github.com/AdoptOpenJDK/jitwatch) by Chris Newland.
- [jitwatch-intellij](https://github.com/yole/jitwatch-intellij) by Dmitry Jemerov
