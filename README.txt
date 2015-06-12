Distributed Resource Manager

This is a Distributed Resource Manager that supports concurrent transactions with ACID properties.

Instructions:

- copy your submissions directory to ./submissions

cd project/test.part2
cp -r ../../submissions/* ../transaction
rm -r ../transaction/Client.java
cp -f Client.java ../transaction/

cd ../lockmgr
make

cd ../transaction
make clean
make server
make client

rmiregistry -J-classpath -J.. 3676 &

cd ../test.part2
setenv CLASSPATH .:gnujaxp.jar
javac RunTests.java

# make sure the following "rmiPort" number is the same as the
# "RMIREGPORT" in ../transaction/Makefile
java -DrmiPort=2219 RunTests MASTER.xml

- the results will be put in ./results/grades.txt

- If you want, you can modify the file project/test.part2/MASTER.xml
  to change the scripts you want to test.

- You are STRONGLY suggested to run the scripts ONE BY ONE.

- Structure:

   o RunTest.java parses the MASTER.xml.  For each line, it activates
     "Client.java" by passing the script name under the "scripts" directory.

   o Client.java starts the necessary RMI modules, such as:

	TransactionManager  RMIName,
  	ResourceManager.RMINameFlights,
  	ResourceManager.RMINameRooms,
  	ResourceManager.RMINameCars,
  	ResourceManager.RMINameCustomers,
  	WorkflowController.RMIName

     Then it reads and parses the script file, and interpret each line
     to take the corresponding action.
