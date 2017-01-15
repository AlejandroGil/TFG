package myRecommender.nmslib;

import static org.ranksys.formats.parsing.Parsers.lp;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.ranksys.formats.index.ItemsReader;
import org.ranksys.formats.index.UsersReader;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;

import es.uam.eps.ir.ranksys.core.preference.IdPref;
import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;

public class UserVectorGenerator {

	/**
	 *
	 * This class generates a file with the users and each item-rating. The rows
	 * represent the user and the columns represent the items (id)
	 * 
	 * @throws IOException
	 */

	public static void main(String[] args) throws IOException {

		String trainDataPath = "src/main/resources/ml-100k/u1.base";
		String outfile = new File(trainDataPath).getName() +"__vectorRatings.txt";
		String outfileUsers = outfile + "_users";
		String userPath = "src/main/resources/ml-100k/users.txt";
		String itemPath = "src/main/resources/ml-100k/items.txt";
		double defaultValue = 0.0;

		/* Loading user and item indexes ("0", "1", "2"... etc) */
		FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
		FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));

		/* Reading rating file */
		SimpleFastPreferenceData<Long, Long> data = SimpleFastPreferenceData
				.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);

		// get all items in order (important so that every user is represented
		// in the same way)
		List<Long> items = itemIndex.getAllItems().sorted().collect(Collectors.toList());

		PrintStream out = new PrintStream(new File(outfile));
		PrintStream outUsers = new PrintStream(new File(outfileUsers));

		data.getAllUsers().forEach(u -> {
			outUsers.println(u);

			items.forEach(i -> {
				double p = defaultValue;
				Optional<? extends IdPref<Long>> pref = data.getPreference(u, i);
				if (pref.isPresent()) {
					p = pref.get().v2();
				}
				out.print(p + "\t");
			});
			out.println();
		});

		out.close();
		outUsers.close();

		System.out.println("Done!");
	}
}
