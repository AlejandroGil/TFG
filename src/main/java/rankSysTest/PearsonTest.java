package rankSysTest;

import static org.ranksys.formats.parsing.Parsers.lp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.ranksys.formats.index.ItemsReader;
import org.ranksys.formats.index.UsersReader;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;

import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import es.uam.eps.ir.ranksys.nn.user.UserNeighborhoodRecommender;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.TopKUserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.UserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;
import es.uam.eps.ir.ranksys.rec.Recommender;
import myRecommender.PearsonUserSimilarity;

public class PearsonTest {

	public static void main(String[] args) throws IOException {
		String userPath = "src/main/resources/users.txt";
	    String itemPath = "src/main/resources/items.txt";
	    String trainDataPath = "src/main/resources/u1.base";
	    String testDataPath = "src/main/resources/u1.test";
	
	    /*Loading user and item indexes ("0", "1", "2"... etc)*/
	    FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
	    FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));
	    
	    /*Reading rating file*/
	    FastPreferenceData<Long, Long> trainData = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);
	    FastPreferenceData<Long, Long> testData = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(testDataPath, lp, lp), userIndex, itemIndex);
	    
	    Map<String, Supplier<Recommender<Long, Long>>> recMap = new HashMap<>();
	    
	    recMap.put("ub_simPC", () -> {
	        int k = 100;
	        int q = 1;
	
	        UserSimilarity<Long> sim = new PearsonUserSimilarity<>(trainData, true, -1.0);
	        UserNeighborhood<Long> neighborhood = new TopKUserNeighborhood<>(sim, k);
	        
	        System.out.println(sim.similarity(0, 1));
	        
	        return new UserNeighborhoodRecommender<>(trainData, neighborhood, q);
	    });
	}
}
