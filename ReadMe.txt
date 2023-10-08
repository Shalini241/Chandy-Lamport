How to Run the application
--------------------------
1.	Download the zip folder and copy it under the root directory
2.	Go inside the OS-Asg1 folder and run
	javac NodeWrapper.java
3.	Once the project is compiled, run the launcher script using
	./launcher.sh
5.	Program will wait for 10 seconds for all the nodes to start their server and then the node 0 will start the protocol.
6.	Once the protocol terminates, it will create .log files and .out files. Output files(.out) will have the snapshot information.
7.	At last, run the cleanFiles.sh to remove all the .log and .out files and cleanup.sh script to terminate all the java processes.