package myRecommender;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.TransposedPreferenceData;
import es.uam.eps.ir.ranksys.nn.item.sim.ItemSimilarity;

/**
 *
 * @author Saúl Vargas (saul.vargas@uam.es)
 * 
 * @param <U> type of the users
 */
public class PearsonItemSimilarity<I> extends ItemSimilarity<I> {

    /**
     * Constructor.
     *
     * @param data preference data
     * @param alpha asymmetry factor, set to 0.5 to standard cosine.
     * @param dense true for array-based calculations, false to map-based
     */
    public PearsonItemSimilarity(FastPreferenceData<?, I> data, boolean dense, double threshold, boolean commonNorm) {
        super(data, new PearsonSimilarity(new TransposedPreferenceData<>(data), dense, threshold, commonNorm));
    }
    
}
