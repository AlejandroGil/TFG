package myRecommender;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Stream;
import org.ranksys.core.util.tuples.Tuple2id;

import es.uam.eps.ir.ranksys.nn.sim.Similarity;

public class ThresholdSimilarity implements Similarity{
	
	private double lowerThreshold;
	private double higherThreshold;
	protected final Similarity sim;
	
	public ThresholdSimilarity(Similarity sim, double lowerThreshold, double higherThreshold) {
		this.sim = sim;
		this.lowerThreshold = lowerThreshold;
		this.higherThreshold = higherThreshold;
	}
	
	
	@Override
	public Stream<Tuple2id> similarElems(int idx) {
		return sim.similarElems(idx).filter(e -> checkSimilarity(e.v2));
	}

	@Override
	public IntToDoubleFunction similarity(int idx) {
        return idx2 -> {
        	double s = sim.similarity(idx, idx2);

            if (checkSimilarity(s))
            	return s;
            return 0.0;
        };
	}
	
	private boolean checkSimilarity(double sim){		
		return (sim > lowerThreshold && sim <= higherThreshold);
	}
	
}
