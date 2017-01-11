package myRecommender;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.item.sim.ItemSimilarity;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;

public class ThresholdItemSimilarity<I> extends ItemSimilarity<I> {

    public ThresholdItemSimilarity(FastPreferenceData<?, I> data, Similarity sim, double minTh, double maxTh) {
        super(data, new ThresholdSimilarity(sim, minTh, maxTh));
    }
}
