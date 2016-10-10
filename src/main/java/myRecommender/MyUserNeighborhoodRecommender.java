package myRecommender;

import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.fast.FastRankingRecommender;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.UserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import static java.lang.Math.pow;
import java.util.HashMap;
import java.util.Map;
/**
 * User-based nearest neighbors recommender.
 * 
 * F. Aiolli. Efficient Top-N Recommendation for Very Large Scale Binary Rated
 * Datasets. RecSys 2013.
 * 
 * Paolo Cremonesi, Yehuda Koren, and Roberto Turrin. Performance of 
 * recommender algorithms on top-n recommendation tasks. RecSys 2010.
 * 
 * C. Desrosiers, G. Karypis. A comprehensive survey of neighborhood-based 
 * recommendation methods. Recommender Systems Handbook.
 *
 * @author Alejandro Gil
 *
 * @param <U> type of the users
 * @param <I> type of the items
 */
public class MyUserNeighborhoodRecommender<U, I> extends FastRankingRecommender<U, I> {
	
	public static enum TRANSFORM{
		STD,
		MC,
		Z
	}

	protected TRANSFORM t;
    /**
     * Preference data.
     */
    protected final FastPreferenceData<U, I> data;

    /**
     * User neighborhood.
     */
    protected final UserNeighborhood<U> neighborhood;
    
    
    protected final UserSimilarity<U> similarity;

    /**
     * Exponent of the similarity.
     */
    protected final int q;
    
    /*Map containing the ratings og the users to calculate means and deviations*/
    protected Map<Integer, Stats> stats;
    protected boolean normalize;


    /**
     * Constructor.
     *
     * @param data preference data
     * @param neighborhood user neighborhood
     * @param q exponent of the similarity
     * @param sim similarity of users
     * @param tr type of transformation (standard, mean centeringo or z-score)
     * @param normalize choose whether the ratings are normalized or not
     */
    public MyUserNeighborhoodRecommender(FastPreferenceData<U, I> data, UserNeighborhood<U> neighborhood, int q, UserSimilarity<U> sim, TRANSFORM tr, boolean normalize) {
        super(data, data);
        this.data = data;
        this.neighborhood = neighborhood;
        this.q = q;
        
        this.similarity = sim;
        this.normalize = normalize;
        this.t = tr;
        
        stats = new HashMap<>();
        data.getAllUidx().forEach(uIndex -> {
            Stats s = new  Stats();
            stats.put(uIndex, s);
            data.getUidxPreferences(uIndex).forEach(p -> {
                s.accept(p.v2);
            });
        });
        
    }

    /**
     * Returns a map of item-score pairs.
     *
     * @param uidx index of the user whose scores are predicted
     * @return a map of item-score pairs
     */
    @Override
    public Int2DoubleMap getScoresMap(int uidx) {
        Int2DoubleOpenHashMap cMap = new Int2DoubleOpenHashMap();
        cMap.defaultReturnValue(0.0);
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);
        Int2DoubleOpenHashMap count = new Int2DoubleOpenHashMap();
        count.defaultReturnValue(0.0);

        double t1 = 0.0;
    	switch (t) {
		case STD:
			break;
		case MC:
		case Z:
			t1 = stats.get(uidx).getMean();
			break;

		default:
			t1 = 0.0;
			break;
		}
        
        double t2 = 1.0;
    	switch (t) {
		case STD:
		case MC:
			t2 = 1.0;
			break;
		case Z:
			t2 = stats.get(uidx).getStandardDeviation();
			break;

		default:
			break;
		}
        
    	neighborhood.getNeighbors(uidx).forEach(vs -> {

        	double sim = similarity.similarity(uidx, vs.v1);
        	
            double w = pow(sim, q);
            cMap.addTo(1, Math.abs(w));
            
            data.getUidxPreferences(vs.v1).forEach(iv -> {
            	
            	double t3 = 0.0;
            	switch (t) {
    			case STD:
    				t3 = iv.v2;
    				break;
    			case MC:
    				t3 = iv.v2 - stats.get(vs.v1).getMean();
    				break;
    			case Z:
    				t3 = (iv.v2 - stats.get(vs.v1).getMean())/stats.get(vs.v1).getStandardDeviation();
    				break;

    			default:
    				break;
    			}
                double p = w * t3;
                scoresMap.addTo(iv.v1, p);
                count.addTo(iv.v1, 1);
            });
        });
       
        final double b = normalize ? t2 / cMap.get(1) : t2;
        final double a = t1;

        Int2DoubleOpenHashMap scoresMap2 = new Int2DoubleOpenHashMap();
        scoresMap2.defaultReturnValue(0.0);
        scoresMap.forEach((k,v) -> {
        	double s = a + b * v;
        	scoresMap2.addTo(k, s);
        });

        return scoresMap2;
    }    
}
