package myRecommender;

import static java.lang.Math.pow;

import java.util.HashMap;
import java.util.Map;

import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.item.neighborhood.ItemNeighborhood;
import es.uam.eps.ir.ranksys.nn.item.sim.ItemSimilarity;
import es.uam.eps.ir.ranksys.rec.fast.FastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import myRecommender.MyUserNeighborhoodRecommender.TRANSFORM;
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
public class MyItemNeighborhoodRecommender<U, I> extends FastRankingRecommender<U, I> {
	
	protected TRANSFORM t;
    /**
     * Preference data.
     */
    protected final FastPreferenceData<U, I> data;

    /**
     * User neighborhood.
     */
    protected final ItemNeighborhood<U> neighborhood;
    
    
    protected final ItemSimilarity<U> similarity;

    /**
     * Exponent of the similarity.
     */
    protected final int q;
    
    /*Map containing the ratings of the users to calculate means and deviations*/
    protected Map<Integer, Stats> stats;
    protected boolean normalize;


    /**
     * Constructor.
     *
     * @param data preference data
     * @param neighborhood user neighborhood
     * @param q exponent of the similarity
     */
    public MyItemNeighborhoodRecommender(FastPreferenceData<U, I> data, ItemNeighborhood<U> neighborhood, int q, ItemSimilarity<U> sim, TRANSFORM std, boolean normalize) {
        super(data, data);
        this.data = data;
        this.neighborhood = neighborhood;
        this.q = q;
        
        this.similarity = sim;
        this.normalize = normalize;
        this.t = std;
        
        stats = new HashMap<>();
        data.getAllIidx().forEach(iIndex -> {
            Stats s = new  Stats();
            stats.put(iIndex, s);
            data.getIidxPreferences(iIndex).forEach(p -> {
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
        
        data.getUidxPreferences(uidx).forEach(jp -> {
        	int i = jp.v1;

        	neighborhood.getNeighbors(i).forEach(is -> {
            	int j = is.v1;
            	double sim = similarity.similarity(i, j);
            	
                double w = pow(sim, q);
                cMap.addTo(1, Math.abs(w));

            	double t3 = 0.0;
            	switch (t) {
    			case STD:
    				t3 = jp.v2;
    				break;
    			case MC:
    				t3 = jp.v2 - stats.get(j).getMean();
    				break;
    			case Z:
    				t3 = (jp.v2 - stats.get(j).getMean())/stats.get(j).getStandardDeviation();
    				break;

    			default:
    				break;
    			}
                double p = w * t3;
                scoresMap.addTo(j, p);
                count.addTo(j, 1);
            });
        });

        Int2DoubleOpenHashMap scoresMap2 = new Int2DoubleOpenHashMap();
        scoresMap2.defaultReturnValue(0.0);
        scoresMap.forEach((i,v) -> {

            double t1 = 0.0;
            switch (t) {
    		case STD:
    			break;
    		case MC:
    		case Z:
    			t1 = stats.get(i).getMean();
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
    			t2 = stats.get(i).getStandardDeviation();
    			break;

    		default:
    			break;
    		}
        	
            final double b = normalize ? t2 / cMap.get(1) : t2;
        	
        	double s = t1 + b * v;
        	scoresMap2.addTo(i, s);
        });

        return scoresMap2;
    }
}