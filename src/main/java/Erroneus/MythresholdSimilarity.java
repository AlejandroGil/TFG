package Erroneus;

import static java.lang.Math.pow;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.sim.VectorSimilarity;

/**
 * Vector cosine similarity. As in Cremonesi's paper. Can be asymmetric if alpha != 0.5.
 *
 * sim(v, w) = v * w / ((v * v)^alpha (w * w)^(1 - alpha))
 * <br>
 * F. Aiolli. Efficient Top-N Recommendation for Very Large Scale Binary Rated Datasets. RecSys 2013.
 * <br>
 * P. Cremonesi, Y. Koren, and R. Turrin. Performance of recommender algorithms on top-N recommendation tasks. RecSys 2010.
 *
 * @author Saúl Vargas (saul.vargas@uam.es)
 */
public class MythresholdSimilarity extends VectorSimilarity {

    private final double alpha;
	private double lowerThreshold;
	private double higherThreshold;

    /**
     * Constructor.
     *
     * @param data preference data
     * @param alpha asymmetry of the similarity, set to 0.5 for symmetry
     * @param dense true for array-based calculations, false to map-based
     */
    public MythresholdSimilarity(FastPreferenceData<?, ?> data, double alpha, boolean dense, double lowerThreshold, double higherThreshold) {
        super(data, dense);
        this.alpha = alpha;
        this.lowerThreshold = lowerThreshold;
        this.higherThreshold = higherThreshold;
    }

    @Override
    protected double sim(double product, double norm2A, double norm2B) {
    	
        double result = product / (pow(norm2A, alpha) * pow(norm2B, 1.0 - alpha));
        
        if (result > lowerThreshold && result < higherThreshold)
        	return result;
        return 0.0;
    }

}