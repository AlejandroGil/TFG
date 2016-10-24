#variar: fold (5), k (5, 10, 20, 40, 60, 100), tr (3), norm (2), sim (12)

usersPath = users.txt
itemsPath = items.txt
q = 1

#generating recommendations
for fold in 1 2 3 4 5
trainPath = u$fold.base
testPath = u$fold.test
do
  for k in 5 10 20 40 60 100
  do
    for transform in STD MD Z
    do
      for normalize in true false
      do
        for sim in cosine cosine_th_0.3 cosine_th_0.5 jaccard jaccard_th_0.3 jaccard_th_0.5 pearson pearsoncn pearson_th_0 pearsoncn_th_0 pearson_th_0.5 pearsoncn_th_0.5
          java -jar experiment.jar ub $usersPath $itemsPath $trainPath $testPath ub_$sim_$normalize_$transform_$k_fold$fold $sim $transform $normalize $k $q
      done
    done
  done
done
