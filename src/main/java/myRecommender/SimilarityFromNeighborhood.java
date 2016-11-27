package myRecommender;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Stream;

import org.ranksys.core.util.tuples.Tuple2id;

import es.uam.eps.ir.ranksys.nn.neighborhood.Neighborhood;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;

public class SimilarityFromNeighborhood implements Similarity {
	
	private Neighborhood neigh;
	
	public SimilarityFromNeighborhood(Neighborhood neighborhood) {
		this.neigh = neighborhood;
	}
	
	@Override
	public Stream<Tuple2id> similarElems(int idx) {
		return neigh.getNeighbors(idx);
	}
	
	@Override
	public IntToDoubleFunction similarity(int idx) {
		return u2 -> neigh.getNeighbors(idx).filter(t -> (t.v1 == u2)).findFirst().get().v2();
	}
}
