package myRecommender;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Stream;

import org.apache.commons.math3.analysis.function.Pow;
import org.ranksys.core.util.tuples.Tuple2id;

import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.ranksys.nn.sim.VectorSimilarity;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

public class PearsonCorrelationSimilarity implements Similarity {


	private FastPreferenceData<?, ?> data;
	private boolean dense;
	
	/**
     * Cached normalization for when dense is false.
     */
	private Int2DoubleMap norm2Map;
	
	/**
     * Cached normalization for when dense is true.
     */
    protected final double[] norm2Array;
    
    /*Map containing the ratings of the users to calculate means and deviations*/
    protected Map<Integer, Stats> stats;

	public PearsonCorrelationSimilarity(FastPreferenceData<?, ?> data, boolean dense) {

        this.data = data;
        this.dense = dense;
        
        stats = new HashMap<>();
        data.getAllUidx().forEach(uIndex -> {
            Stats s = new  Stats();
            stats.put(uIndex, s);
            data.getUidxPreferences(uIndex).forEach(p -> {
                s.accept(p.v2);
            });
        });
        
        if (data.useIteratorsPreferentially()) {
            if (dense) {
                this.norm2Map = null;
                this.norm2Array = new double[data.numUsers()];
                data.getUidxWithPreferences().forEach(idx -> norm2Array[idx] = getFasterNorm2(idx));
            } else {
                this.norm2Map = new Int2DoubleOpenHashMap();
                this.norm2Array = null;
                norm2Map.defaultReturnValue(0.0);
                data.getUidxWithPreferences().forEach(idx -> norm2Map.put(idx, getFasterNorm2(idx)));
            }
        } else {
            if (dense) {
                this.norm2Map = null;
                this.norm2Array = new double[data.numUsers()];
                data.getUidxWithPreferences().forEach(idx -> norm2Array[idx] = getNorm2(idx));
            } else {
                this.norm2Map = new Int2DoubleOpenHashMap();
                this.norm2Array = null;
                norm2Map.defaultReturnValue(0.0);
                data.getUidxWithPreferences().forEach(idx -> norm2Map.put(idx, getNorm2(idx)));
            }
        }
    
	}

	@Override
	public IntToDoubleFunction similarity(int idx) {


        Int2DoubleOpenHashMap map = new Int2DoubleOpenHashMap();
        data.getUidxPreferences(idx).forEach(iv -> map.put(iv.v1, iv.v2));

        double n2a = norm2Map.get(idx);

        return idx2 -> {
            double prod = data.getUidxPreferences(idx2)
                    .mapToDouble(iv -> (map.get(iv.v1) - stats.get(iv.v1).getMean()) * (iv.v2 - stats.get().getMean()))
                    .sum();

            return sim(prod, n2a, norm2Map.get(idx2));
        };
	}

	@Override
	public Stream<Tuple2id> similarElems(int idx) {
		// TODO Auto-generated method stub
		return null;
	}

	
	private double getFasterNorm2(int uidx) {
		DoubleIterator ivs = data.getUidxVs(uidx);
	    double sum = 0;
	    while (ivs.hasNext()) {
	    	double iv = ivs.nextDouble();
	            sum += Math.pow(iv - stats.get(uidx).getMean(), 2);
	        }
	        return sum;
	    }
	
	private double getNorm2(int idx) {
        return data.getUidxPreferences(idx)
                .mapToDouble(IdxPref::v2)
                .map(x -> Math.pow(x - stats.get(idx).getMean(), 2))
                .sum();
    }
}
