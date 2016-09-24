package myRecommender;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Stream;
import org.ranksys.core.util.tuples.Tuple2id;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class ThresholdSimilarity implements Similarity{
	
	private double lowerThreshold;
	private double higherThreshold;
	protected final Similarity sim;
	private FastPreferenceData<?, ?> data;
	
	public ThresholdSimilarity(FastPreferenceData<?, ?> data, Similarity sim, double lowerThreshold, double higherThreshold) {

		this.data = data;
		this.sim = sim;
		this.lowerThreshold = lowerThreshold;
		this.higherThreshold = higherThreshold;
	}
	
	
	@Override
	public Stream<Tuple2id> similarElems(int idx) {
		return sim.similarElems(idx);
	}

	@Override
	public IntToDoubleFunction similarity(int idx) {
		
		IntSet set = new IntOpenHashSet();
        data.getUidxPreferences(idx).map(IdxPref::v1).forEach(set::add);

        return idx2 -> {
            int product = (int) data.getUidxPreferences(idx2)
                    .map(IdxPref::v1)
                    .filter(set::contains)
                    .count();

            if (checkThresHold(product, lowerThreshold, higherThreshold))
            	return product;
            return 0.0;
        };
	}
	
	private boolean checkThresHold(double sim, double lowerThreshold, double higherThreshold){
		
		return (sim > lowerThreshold && sim < higherThreshold);
	}
	
}
