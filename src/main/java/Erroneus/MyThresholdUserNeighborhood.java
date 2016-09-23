package Erroneus;

import es.uam.eps.ir.ranksys.nn.user.neighborhood.UserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;

/**
 * Threshold user neighborhood. See {@link MyThresholdNeighborhood}.
 *
 * @author Alejandro Gil Vargas
 * 
 * @param <U> type of the users
 */

public class MyThresholdUserNeighborhood<U> extends UserNeighborhood<U> {

	 /**
     * Constructor
     *
     * @param sim user similarity
     * @param threshold minimum value to be considered as neighbor
     */
    public MyThresholdUserNeighborhood(UserSimilarity<U> sim, double lowerThreshold, double higherThreshold) {
        super(sim, new MyThesholdNeighborhood(sim, lowerThreshold, higherThreshold));
    }
}
