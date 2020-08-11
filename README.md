[![build status](https://travis-ci.com/marschwar/dependeny-visualization.svg?branch=master)](https://travis-ci.com/marschwar)
[![build status](https://ci.appveyor.com/api/projects/status/github/marschwar/depvis?svg=true&branch=master)](https://ci.appveyor.com/project/marschwar/depvis)

# dependency-visualization
This is somewhat of a side project so do not expect frequent updates just yet.

At a client project I needed a way to visualize dependencies between projects java classes. As I could not find
 anything I liked within 10 minutes of searching the web, I decided to create my own.

## Prerequisites
* Java 8 or higher
* (optional) the **dot** tool from the [Graphviz](https://www.graphviz.org/) tool suite to transform generated`.dot
` files to a graph image

## Build from source

    ./gradlew clean assemble

This will create a `depvis-<version>-all.jar` in the `./build/libs` folder.

## Usage

    java -jar depvis-<version>-all.jar depvis-<version>.jar 

will create a dependency graph for depvis itself printing the html source to the console.

```
Usage: <main class> [options] <directory or jar file>
  Options:
    --help
      Show all available options
    --output-dir, -o
      The output directory in which to store the generated output file. If not 
      specified, the output is written to STDOUT.
    --includes, -i
      A regular expression pattern of types to include.You can specify 
      multiple patterns by repeating the -i option. Default: ".*"
    --excludes, -e
      A regular expression pattern of types to exclude. You can specify 
      multiple patterns by repeating the -e option. Default: "java.*"
    --format, -f
      The output format. Supported formats are:
        CYTOSCAPE_JS - html page using cytoscape.js to render the graph.
        DOT - a dot file that can be transformed to other formats using the dot tool.
      Default: CYTOSCAPE_JS
      Possible Values: [CYTOSCAPE_JS, DOT]
    --cycles-only
      If specified only classes that are part of a cyclic dependency are 
      shown. 
      Default: false
    --show-self-references
      If specified classes that only contain self references are shown. By 
      default these are omitted.
      Default: false
```

## Thanks & Links
* [Cytoscape](https://cytoscape.org/) and [Cytoscape.js](https://js.cytoscape.org/)
* [Graphviz](https://www.graphviz.org/)
* [jdeps](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jdeps.html) - command line tool to extract
 dependencies of java modules, packages and classes