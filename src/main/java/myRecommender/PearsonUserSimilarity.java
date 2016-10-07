package myRecommender;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;

/**
 * Vector cosine user similarity. See {@link VectorCosineSimilarity}.
 *
 * @author Saúl Vargas (saul.vargas@uam.es)
 * 
 * @param <U> type of the users
 */
public class PearsonUserSimilarity<U> extends UserSimilarity<U> {

    /**
     * Constructor.
     *
     * @param data preference data
     * @param alpha asymmetry factor, set to 0.5 to standard cosine.
     * @param dense true for array-based calculations, false to map-based
     */
    public PearsonUserSimilarity(FastPreferenceData<U, ?> data, boolean dense, double threshold, boolean commonNorm) {
        super(data, new PearsonSimilarity(data, dense, threshold, commonNorm));
    }
    
}
