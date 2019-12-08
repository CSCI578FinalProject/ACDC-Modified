# CallGraphEngine
## What is this repository
This repo is our team's modified version of ACDC, which we call the finer ACDC with BFS Proximity. For more conceptual descriptions, please refer to the rationale document in the documentation repo.
## How?
There are two modifications in the code:
1) We changed the input from class-level dependencies to method-level dependencies (call graph). This changed is done through changing the run configuration and arguements, not the code itself.
2) We added the BFS proximity patch in `RSFOutput.java`. The idea is that, when we are traversing the graph using BFS, we would like to break down "functional black boxes" or simply the "raw clusters" further into "functional sections." The way we do this is that in the BFS order, we will consider consequtive nodes that belong to the same cluster as a functional section. For example, consider that we have two clusters A and B, and their members are {a1, a2, a3...} and {b1, b2, b3...}. If the BFS order of those nodes is a1, a2, a3, b1, b2, b3, a4, a5, b4, then, we will have functional section A1 = {a1, a2, a3}, A2 = {a4, a5}, B1 = {b1, b2, b3}, B2 = {b4}. Notice that this can be written as a ACDC pattern, so that we do not need to mess with `RSFOutput.java`. However, given the tight time limit, we chose this rather hacky implementation. This implementation is equivelent to writing a new pattern that does the same thing.
## Input
The input `call_graph_{version}_no_param_modified.rsf` files are located in `CS578-arcade/tomcat/`. Notice to switch different input files, currently you need to re-hardcode the input and output directories as indicated above. This is due to the time constraints of this project. I will rewrite a launch configuration after Friday.
## Ouput
the output files are `60.rsf` and `85.rsf` in `CS578-arcade/tomcat/` for tomcat version 6.0 and 8.5 respectively.
## How to Run?
Choose launch configuration `ACDC` with arguments `tomcat/{intputfile}.rsf tomcat/{outputfile}.rsf`.
