#variar: fold (5), k (5, 10, 20, 40, 60, 100), tr (3), norm (2), sim (12)

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
    for transform in STD MD Z
    do
      for normalize in false
      do
        for sim in cosine cosine_th_0.3 cosine_th_0.5 jaccard jaccard_th_0.3 jaccard_th_0.5 pearson pearsoncn pearson_th_0 pearsoncn_th_0 pearson_th_0.5 pearsoncn_th_0.5
        do
          #Parameters: ub userPath itemPath trainData testData outfile sim transf norm k q [alpha]
          java -jar experiment.jar ub $usersPath $itemsPath $trainPath $testPath ub\_$sim\_$normalize\_$transform\_$kfold$fold $sim $transform $normalize $k $q
          #Parameters: eval recfile testdata outfile
          java -jar experiment.jar eval ub\_$sim\_$normalize\_$transform\_$k\_fold\_$fold $testPath eval_ub\_$sim\_$normalize\_$transform\_$k\_fold$fold
        done
      done
    done
  done
done
