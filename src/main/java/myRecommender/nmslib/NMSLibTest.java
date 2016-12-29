package myRecommender.nmslib;

import static org.ranksys.formats.parsing.Parsers.lp;

import java.io.File;
import java.io.IOException;

import org.ranksys.formats.index.ItemsReader;
import org.ranksys.formats.index.UsersReader;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;

import cern.colt.Arrays;
import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import es.uam.eps.ir.ranksys.nn.item.ItemNeighborhoodRecommender;
import es.uam.eps.ir.ranksys.nn.item.neighborhood.ItemNeighborhood;
import es.uam.eps.ir.ranksys.nn.neighborhood.Neighborhood;
import es.uam.eps.ir.ranksys.nn.user.UserNeighborhoodRecommender;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.UserNeighborhood;
import es.uam.eps.ir.ranksys.rec.Recommender;

public class NMSLibTest {

	public static void main(String[] args) throws Exception {
		/*
		args = new String[]{"generate_transformation", "src/main/resources/ml-100k/u1.base", "src/main/resources/ml-100k/users.txt", "src/main/resources/ml-100k/items.txt",
				"0.0", "true"};
		*/
		/*
		args = new String[]{"generate_transformation", "src/main/resources/ml-100k/u1.base", "src/main/resources/ml-100k/users.txt", "src/main/resources/ml-100k/items.txt",
				"0.0", "false"};
		*/
		/*
		args = new String[]{"ubrec", "src/main/resources/ml-100k/u1.base", "src/main/resources/ml-100k/users.txt", "src/main/resources/ml-100k/items.txt",
				"0.0", "true",
				"20", "localhost", "10000"};
		*/
		args = new String[]{"ubrec", "src/main/resources/ml-100k/u1.base", "src/main/resources/ml-100k/users.txt", "src/main/resources/ml-100k/items.txt",
				"0.0", "true",
				"20", "ir.ii.uam.es", "10002"};

		String trainDataPath = args[1];
		String userPath = args[2];
		String itemPath = args[3];

		SimpleFastPreferenceData<Long, Long> data = getPreferenceData(trainDataPath, userPath, itemPath);

		double defaultValue = Double.parseDouble(args[4]);
		boolean useItemsAsSpace = Boolean.parseBoolean(args[5]);

		switch (args[0]) {
		case "generate_transformation": {
			System.out.println("generate_transformation trainfile userindex itemindex defValue useItemsAsSpace");
			String outfile = new File(trainDataPath).getName() + "__" + (useItemsAsSpace ? "item" : "user")
					+ "vectorRatings_" + defaultValue + ".txt";
			String outfileMapping = outfile + "_" + (useItemsAsSpace ? "users" : "items");

			DatamodelTransformation<Long> t = getTransformation(data, defaultValue, useItemsAsSpace);
			t.write(new File(outfileMapping), new File(outfile));
		}
			break;

		case "ubrec": {
			System.out.println("ubrec trainfile userindex itemindex defValue useItemsAsSpace k host port");
			int k = Integer.parseInt(args[6]);
			String host = args[7];
			int port = Integer.parseInt(args[8]);
			String queryTimeParams = "";

			DatamodelTransformation<Long> t = getTransformation(data, defaultValue, useItemsAsSpace);
			Neighborhood n = new NMSLibNeighborhood<>(t, k, host, port, queryTimeParams);
			UserNeighborhood<Long> neighborhood = new UserNeighborhood<Long>(data, n) {
			};

			//System.out.println(Arrays.toString(t.transform(1L)));
			Recommender<Long, Long> r = new UserNeighborhoodRecommender<>(data, neighborhood, 1);
			r.getRecommendation(1L, 15).getItems().stream().forEach(e -> System.out.println(e.v1 + ":" + e.v2));
		}
			break;

		case "ibrec": {
			System.out.println("ibrec trainfile userindex itemindex defValue useItemsAsSpace k host port");
			int k = Integer.parseInt(args[6]);
			String host = args[7];
			int port = Integer.parseInt(args[8]);
			String queryTimeParams = "";

			DatamodelTransformation<Long> t = getTransformation(data, defaultValue, useItemsAsSpace);
			Neighborhood n = new NMSLibNeighborhood<>(t, k, host, port, queryTimeParams);
			ItemNeighborhood<Long> neighborhood = new ItemNeighborhood<Long>(data, n) {
			};

			new ItemNeighborhoodRecommender<>(data, neighborhood, 1);
		}
			break;

		default:
			break;
		}
	}

	public static DatamodelTransformation<Long> getTransformation(String trainDataPath, String userPath,
			String itemPath, double defValue, boolean itemSpace) throws IOException {
		/* Loading user and item indexes ("0", "1", "2"... etc) */
		FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
		FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));

		/* Reading rating file */
		SimpleFastPreferenceData<Long, Long> data = SimpleFastPreferenceData
				.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);

		return new DatamodelTransformation<>(data, userIndex, itemIndex, defValue, itemSpace);
	}

	public static DatamodelTransformation<Long> getTransformation(SimpleFastPreferenceData<Long, Long> data,
			double defValue, boolean itemSpace) throws IOException {
		return new DatamodelTransformation<>(data, data, data, defValue, itemSpace);
	}

	public static SimpleFastPreferenceData<Long, Long> getPreferenceData(String trainDataPath, String userPath,
			String itemPath) throws IOException {
		/* Loading user and item indexes ("0", "1", "2"... etc) */
		FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
		FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));

		/* Reading rating file */
		SimpleFastPreferenceData<Long, Long> data = SimpleFastPreferenceData
				.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);

		return data;
	}
}
