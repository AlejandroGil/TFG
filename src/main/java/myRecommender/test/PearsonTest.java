package myRecommender.test;

import static org.jooq.lambda.tuple.Tuple.tuple;

import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import es.uam.eps.ir.ranksys.nn.item.sim.ItemSimilarity;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;
import myRecommender.PearsonItemSimilarity;
import myRecommender.PearsonUserSimilarity;

public class PearsonTest {

	@Before
	public void loadData() {

	}

	@Test
	public void simpleTestUserSim() {
		FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(Stream.of(1L,2L));
		FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(Stream.of(1L,2L,3L,4L));
		FastPreferenceData<Long, Long> data = SimpleFastPreferenceData.load(Stream.of(
				// u
				tuple(1L, 1L, 2.0),
				tuple(1L, 2L, 3.0),
				tuple(1L, 3L, 4.0),
				tuple(1L, 4L, 4.0),
				// v
				tuple(2L, 1L, 2.0),
				tuple(2L, 2L, 5.0),
				tuple(2L, 3L, 5.0),
				tuple(2L, 4L, 8.0)
				), userIndex, itemIndex);
		UserSimilarity<Long> sim = new PearsonUserSimilarity<>(data, true, 0.0, false);
		double s = sim.similarity(1L, 2L);
		Assert.assertEquals(0.86, s, 0.01);
	}

	@Test
	public void simpleTestItemSim() {
		FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(Stream.of(1L,2L,3L,4L));
		FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(Stream.of(1L,2L));
		FastPreferenceData<Long, Long> data = SimpleFastPreferenceData.load(Stream.of(
				// i
				tuple(1L, 1L, 2.0),
				tuple(2L, 1L, 3.0),
				tuple(3L, 1L, 4.0),
				tuple(4L, 1L, 4.0),
				// j
				tuple(1L, 2L, 2.0),
				tuple(2L, 2L, 5.0),
				tuple(3L, 2L, 5.0),
				tuple(4L, 2L, 8.0)
				), userIndex, itemIndex);
		ItemSimilarity<Long> sim = new PearsonItemSimilarity<>(data, true, 0.0, false);
		double s = sim.similarity(1L, 2L);
		Assert.assertEquals(0.86, s, 0.01);
	}
}
