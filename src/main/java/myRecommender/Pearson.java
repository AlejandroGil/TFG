package myRecommender;

import static java.util.stream.IntStream.range;
import static org.ranksys.core.util.tuples.Tuples.tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Stream;

import org.ranksys.core.util.tuples.Tuple2id;

import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;

public abstract class Pearson implements Similarity {


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

	public Pearson(FastPreferenceData<?, ?> data, boolean dense) {

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
                    .mapToDouble(iv -> (map.get(iv.v1) - stats.get(idx).getMean()) * (iv.v2 - stats.get(idx2).getMean()))
                    .sum();

            return sim(prod, n2a, norm2Map.get(idx2));
        };
	}


	@Override
	public Stream<Tuple2id> similarElems(int idx1) {

        if (data.useIteratorsPreferentially()) {
            if (dense) {
                double n2a = norm2Array[idx1];

                double[] productMap = getFasterProductArray(idx1);
                return range(0, productMap.length)
                        .filter(i -> productMap[i] != 0.0)
                        .mapToObj(i -> tuple(i, sim(productMap[i], n2a, norm2Array[i])));
            } else {
                double n2a = norm2Map.get(idx1);

                return getFasterProductMap(idx1).int2DoubleEntrySet().stream()
                        .map(e -> {
                            int idx2 = e.getIntKey();
                            double coo = e.getDoubleValue();
                            double n2b = norm2Map.get(idx2);
                            return tuple(idx2, sim(coo, n2a, n2b));
                        });
            }
        } else {
            if (dense) {
                double n2a = norm2Array[idx1];

                double[] productMap = getProductArray(idx1);
                return range(0, productMap.length)
                        .filter(i -> productMap[i] != 0.0)
                        .mapToObj(i -> tuple(i, sim(productMap[i], n2a, norm2Array[i])));
            } else {
                double n2a = norm2Map.get(idx1);

                return getProductMap(idx1).int2DoubleEntrySet().stream()
                        .map(e -> {
                            int idx2 = e.getIntKey();
                            double coo = e.getDoubleValue();
                            double n2b = norm2Map.get(idx2);
                            return tuple(idx2, sim(coo, n2a, n2b));
                        });
            }
        }

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
	
	private Int2DoubleMap getProductMap(int idx1) {
        Int2DoubleOpenHashMap productMap = new Int2DoubleOpenHashMap();
        productMap.defaultReturnValue(0.0);

        data.getUidxPreferences(idx1).forEach(ip -> {
            data.getIidxPreferences(ip.v1).forEach(up -> {
                productMap.addTo(up.v1, ip.v2 * up.v2);
            });
        });

        productMap.remove(idx1);

        return productMap;
    }

    private double[] getProductArray(int idx1) {
        double[] productMap = new double[data.numUsers()];

        data.getUidxPreferences(idx1).forEach(ip -> {
            data.getIidxPreferences(ip.v1).forEach(up -> {
                productMap[up.v1] += ip.v2 * up.v2;
            });
        });

        productMap[idx1] = 0.0;

        return productMap;
    }

	private Int2DoubleMap getFasterProductMap(int uidx) {
        Int2DoubleOpenHashMap productMap = new Int2DoubleOpenHashMap();
        productMap.defaultReturnValue(0.0);

        IntIterator iidxs = data.getUidxIidxs(uidx);
        DoubleIterator ivs = data.getUidxVs(uidx);
        while (iidxs.hasNext()) {
            int iidx = iidxs.nextInt();
            double iv = ivs.nextDouble();
            IntIterator vidxs = data.getIidxUidxs(iidx);
            DoubleIterator vvs = data.getIidxVs(iidx);
            while (vidxs.hasNext()) {
                productMap.addTo(vidxs.nextInt(), iv * vvs.nextDouble());
            }
        }

        productMap.remove(uidx);

        return productMap;
    }

    private double[] getFasterProductArray(int uidx) {
        double[] productMap = new double[data.numUsers()];

        IntIterator iidxs = data.getUidxIidxs(uidx);
        DoubleIterator ivs = data.getUidxVs(uidx);
        while (iidxs.hasNext()) {
            int iidx = iidxs.nextInt();
            double iv = ivs.nextDouble();
            IntIterator vidxs = data.getIidxUidxs(iidx);
            DoubleIterator vvs = data.getIidxVs(iidx);
            while (vidxs.hasNext()) {
                productMap[vidxs.nextInt()] += iv * vvs.nextDouble();
            }
        }

        productMap[uidx] = 0.0;

        return productMap;
    }
    
    protected abstract double sim(double product, double norm2A, double norm2B);

}
