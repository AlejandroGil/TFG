files=`ls eval_*`
summary=summary_eval
rm $summary
for file in $files
do
  awk '{print FILENAME"\t"$0}' $file >> $summary
done

cat eval_* > summary2_eval
