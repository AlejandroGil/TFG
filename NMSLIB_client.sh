mvn compile -X exec:java -Dexec.mainClass=thrift.Client -Dexec.args="-p 10000 -a localhost -k 500 -i u1.base__vectorRatings.txt -out u1.base__NMSLIB_neighbors.txt"
