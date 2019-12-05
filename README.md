# CallGraphEngine
## What is this repository
This repo is a fork of https://github.com/gousiosg/java-callgraph. The original app can generate both dynamica and static call graphs given a java project in form of jars. For the sake of our final project which relies on static analysis, I modified the static call graph generation in the original app to meet our needs. Now, the app supports outputing various information that includes: 1. class members: what classes include what methods. `contains:{class_name}\*{method_name}` 2. method dependency: what methods call into what other methods and the type of that call. `M:{method_name}*({call_type})*{method_name}` 3. class dependency: what classes depend on what other classes. `C:{class_name}*{class_name}`
## How?
The static analysis code will go through each function in the jar files and establish the caller-callee relationship with the help of Apache BCEL https://commons.apache.org/proper/commons-bcel/ which is a byte code analysis library that helps ocate methods, retrieve method names, and identify method calls.
## Input
The jar files of the two target systems, Tomcat 6.0 and 8.5, are in "tomcat60" and "tomcat85" respectively.
## Ouput
the output files are the txt files that start with "call_graph".
## How to Run?
in the root directory, do:
```
mvn install
// to generate call graph with method parameters populated 
java -jar target/javacg-0.1-SNAPSHOT-static.jar -n {input_dir} > {output_file_name} 
// to generate call graph with method parameters not populated, this is used for our project 
java -jar target/javacg-0.1-SNAPSHOT-static.jar -dn {input_dir} > {output_file_name}
```
