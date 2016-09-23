package Erroneus;

import es.uam.eps.ir.ranksys.nn.neighborhood.Neighborhood;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import java.util.stream.Stream;
import org.ranksys.core.util.tuples.Tuple2id;

/**
 * Threshold neighborhood. Items with a similarity betwwen thresholds are kept
 * as neighbors.
 *
 * @author Alejandro Gil 
 * */

public class MyThesholdNeighborhood implements Neighborhood {

		private final Similarity sim;
	    private final double lowerThreshold;
	    private final double higherThreshold;

	    /**
	     * Constructor.
	     *
	     * @param sim similarity
	     * @param threshold minimum value to be considered as neighbor
	     */
	    public MyThesholdNeighborhood(Similarity sim, double lowerThreshold, double higherThreshold) {
	        this.sim = sim;
	        this.lowerThreshold = lowerThreshold;
	        this.higherThreshold = higherThreshold;
	    }

	    /**
	     * Returns the neighborhood of a user/index.
	     *
	     * @param idx user/index whose neighborhood is calculated
	     * @return stream of user/item-similarity pairs.
	     */
	    @Override
	    public Stream<Tuple2id> getNeighbors(int idx) {
	        return sim.similarElems(idx).filter(is -> is.v2 > lowerThreshold && is.v2 < higherThreshold);
	    }
}
