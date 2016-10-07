package myRecommender;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;

public class PearsonSimilarity extends Pearson {

    /**
     * Constructor.
     *
     * @param data preference data
     * @param alpha asymmetry of the similarity, set to 0.5 for symmetry
     * @param dense true for array-based calculations, false to map-based
     */
    public PearsonSimilarity(FastPreferenceData<?, ?> data, boolean dense, double threshold, boolean commonNorm) {
        super(data, dense, threshold, commonNorm);
    }

    @Override
    protected double sim(double product, double norm2A, double norm2B) {
    	return product / Math.sqrt(norm2A * norm2B);
    }

}
