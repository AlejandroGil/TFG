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
    
    private double threshold;
    
    /*Map containing the ratings of the users to calculate means and deviations*/
    protected Map<Integer, Stats> stats;

	public Pearson(FastPreferenceData<?, ?> data, boolean dense, double threshold) {

        this.data = data;
        this.dense = dense;
        this.threshold = threshold;
        
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

        final double n2a = (dense ? norm2Array[idx] : norm2Map.get(idx));

        return idx2 -> {
            Int2DoubleOpenHashMap tempMap = new Int2DoubleOpenHashMap();
            tempMap.defaultReturnValue(0.0);
            double prod = data.getUidxPreferences(idx2).filter(p -> map.containsKey(p.v1))
                    .mapToDouble(iv -> {
                    	double a = (map.get(iv.v1) - stats.get(idx).getMean());
                    	double b = (iv.v2 - stats.get(idx2).getMean());
                    	tempMap.addTo(idx, a*a);
                    	tempMap.addTo(idx2, b*b);
                    	System.out.println(tempMap);
                    	return a * b;
                    })
                    .sum();

            double n2a1 = n2a;
            double n2a2 = (dense ? norm2Array[idx2] : norm2Map.get(idx2));
            if(true){
            	n2a1 = tempMap.get(idx);
            	n2a2 = tempMap.get(idx2);
            }
            return sim(prod, n2a1, n2a2);
        };
	}


	@Override
	public Stream<Tuple2id> similarElems(int idx1) {

        if (data.useIteratorsPreferentially()) {
            if (dense) {
                double n2a = norm2Array[idx1];

                double[] productMap = getFasterProductArray(idx1);
                return range(0, productMap.length)
                        .filter(i -> productMap[i] > threshold)
                        .mapToObj(i -> {
                        	double n2a1 = n2a;
                        	double n2a2 = norm2Array[i];
	                        if(true){
	                        	Int2DoubleMap map = getNorm(idx1, i);
	                        	n2a1 = map.get(idx1);
	                        	n2a2 = map.get(i);
	                        }
	                        return tuple(i, sim(productMap[i], n2a1, n2a2));});
            } else {
                double n2a = norm2Map.get(idx1);

                return getFasterProductMap(idx1).int2DoubleEntrySet().stream()
                		.filter(e->e.getValue() > threshold)
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
                        .filter(i -> productMap[i] > threshold)
                        .mapToObj(i -> tuple(i, sim(productMap[i], n2a, norm2Array[i])));
            } else {
                double n2a = norm2Map.get(idx1);

                return getProductMap(idx1).int2DoubleEntrySet().stream()
                		.filter(e->e.getValue() > threshold)
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
	
	private Int2DoubleMap getNorm(int idx1, int idx2) {
        Int2DoubleOpenHashMap map = new Int2DoubleOpenHashMap();
        data.getUidxPreferences(idx1).forEach(iv -> map.put(iv.v1, iv.v2));

            Int2DoubleOpenHashMap tempMap = new Int2DoubleOpenHashMap();
            tempMap.defaultReturnValue(0.0);
            
            data.getUidxPreferences(idx2).filter(p -> map.containsKey(p.v1))
                    .forEach(iv -> {
                    	double a = (map.get(iv.v1) - stats.get(idx1).getMean());
                    	double b = (iv.v2 - stats.get(idx2).getMean());
                    	tempMap.addTo(idx1, a*a);
                    	tempMap.addTo(idx2, b*b);
                    });
            return tempMap;
    }
	
	private Int2DoubleMap getProductMap(int idx1) {
        Int2DoubleOpenHashMap productMap = new Int2DoubleOpenHashMap();
        productMap.defaultReturnValue(0.0);

        data.getUidxPreferences(idx1).forEach(ip -> {
            data.getIidxPreferences(ip.v1).forEach(up -> {
                productMap.addTo(up.v1, (ip.v2 - stats.get(idx1).getMean()) * (up.v2 - stats.get(up.v1).getMean()));
            });
        });

        productMap.remove(idx1);

        return productMap;
    }

    private double[] getProductArray(int idx1) {
        double[] productMap = new double[data.numUsers()];

        data.getUidxPreferences(idx1).forEach(ip -> {
            data.getIidxPreferences(ip.v1).forEach(up -> {
                productMap[up.v1] += (ip.v2 - stats.get(idx1).getMean()) * (up.v2 - stats.get(up.v1).getMean());
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
            int next = vidxs.nextInt();
            while (vidxs.hasNext()) {
                productMap.addTo(next, (iv - stats.get(uidx).getMean()) * (vvs.nextDouble() - stats.get(next).getMean()));
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
            
            int next = vidxs.nextInt();
            while (vidxs.hasNext()) {
                productMap[next] += (iv - stats.get(uidx).getMean()) * (vvs.nextDouble() - stats.get(next).getMean());
            }
        }

        productMap[uidx] = 0.0;

        return productMap;
    }
    
    protected abstract double sim(double product, double norm2A, double norm2B);

}
