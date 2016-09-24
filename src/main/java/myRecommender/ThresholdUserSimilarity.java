package myRecommender;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;

public class ThresholdUserSimilarity<U> extends UserSimilarity<U> {

    public ThresholdUserSimilarity(FastPreferenceData<U, ?> data, Similarity sim, double minTh, double maxTh) {
        super(data, new ThresholdSimilarity(sim, minTh, maxTh));
    }
}
