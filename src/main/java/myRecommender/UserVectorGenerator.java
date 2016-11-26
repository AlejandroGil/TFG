package myRecommender;

import static org.ranksys.formats.parsing.Parsers.lp;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.ranksys.formats.index.ItemsReader;
import org.ranksys.formats.index.UsersReader;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;

import com.google.common.collect.Multiset.Entry;

import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.TopKUserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.UserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;
import es.uam.eps.ir.ranksys.nn.user.sim.VectorCosineUserSimilarity;
import es.uam.eps.ir.ranksys.nn.user.sim.VectorJaccardUserSimilarity;

public class UserVectorGenerator {

	/**
	 *
	 * This class generates a file with the users and each item-rating. The rows
	 * represent the user and the columns represent the items (id)
	 * 
	 * @throws IOException
	 */

	public static void main(String[] args) throws IOException {

		String outfile;

		String userPath = "src/main/resources/ml-100k/users.txt";
		String itemPath = "src/main/resources/ml-100k/items.txt";
		String trainDataPath = "src/main/resources/ml-100k/u5.base";

		Map<Integer, Map<Integer, Double>> vectorRatings;

		/* Loading user and item indexes ("0", "1", "2"... etc) */
		FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
		FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));

		/* Reading rating file */
		FastPreferenceData<Long, Long> data = SimpleFastPreferenceData
				.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);

		/*
		 * if (args.length < 1){ System.out.println(
		 * "Parameters incorrect. Usage: outputFile"); System.exit(0); }
		 */

		outfile = "vectorRatings.txt";

		vectorRatings = new HashMap<>();

		data.getAllUidx().forEach(uIndex -> {

			HashMap<Integer, Double> aux = new HashMap<>();

			/*
			 * for each user we take all the items rated, adding them to the map
			 * aux -> {itemId - rating}
			 */
			data.getUidxPreferences(uIndex).forEach(p -> {
				aux.put(p.v1, p.v2);
				vectorRatings.put(uIndex, aux);
			});

			data.getAllIidx().forEach(iIndex -> {

				if (!aux.containsKey(iIndex))
					aux.put(iIndex, 0.0);
				vectorRatings.put(uIndex, aux);
			});
		});

		PrintStream out = new PrintStream(new File(outfile));

		vectorRatings.entrySet().stream().forEach(entry -> {

			entry.getValue().entrySet().forEach(entryValue -> {

				// System.out.println("User: " + entry.getKey() + " Item: " +
				// entryValue.getKey() + "Rating: " + entryValue.getValue());
				out.print(entryValue.getValue() + "\t");
			});
			out.println();
		});

		out.close();

		System.out.println("Done!");
	}
}
