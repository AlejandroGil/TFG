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
    public double similarity(int idx1, int idx2) {
        if (sim.similarity(idx1, idx2) > lowerThreshold && sim.similarity(idx1, idx2) < higherThreshold)
        	return sim.similarity(idx1).applyAsDouble(idx2);
        
        return 0.0;
    }

	@Override
	public Stream<Tuple2id> similarElems(int idx) {
		return sim.similarElems(idx);
	}

	@Override
	public IntToDoubleFunction similarity(int idx) {
		return sim.similarity(idx);		//No se que devolver
	}
	

}
