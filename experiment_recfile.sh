#variar: fold(5), lp(2)

usersPath="users.txt"
itemsPath="items.txt"
q=1

#generating recommendations
for fold in 1 2 3 4 5
do
  trainPath=u$fold.base
  testPath=u$fold.test

  for k in 5 10 20 40 60 100
  do
    for lp in 1 2
    do
      #Parameters: recFile userPath itemPath trainData testData neighFile k q outfile
      java -jar experiment.jar recFile $usersPath $itemsPath $trainPath $testPath $trainPath\_l$lp\_NMSLIB_neighbors.txt $k $q recfile\_$trainPath\_l$lp\_NMSLIB_neighbors.txt
      #Parameters: eval recfile testdata outfile
      java -jar experiment.jar eval recfile\_$trainPath\_l$lp\_NMSLIB_neighbors.txt $testPath eval_recfile\_$trainPath\_l$lp\_NMSLIB_neighbors.txt
    done
  done
done
