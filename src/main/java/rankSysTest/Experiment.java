package rankSysTest;

import static org.ranksys.formats.parsing.Parsers.lp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import org.ranksys.formats.index.ItemsReader;
import org.ranksys.formats.index.UsersReader;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.core.preference.SimplePreferenceData;
import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.RecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.NDCG;
import es.uam.eps.ir.ranksys.metrics.basic.Precision;
import es.uam.eps.ir.ranksys.metrics.basic.Recall;
import es.uam.eps.ir.ranksys.metrics.rel.BinaryRelevanceModel;
import es.uam.eps.ir.ranksys.nn.user.UserNeighborhoodRecommender;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.TopKUserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.UserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;
import es.uam.eps.ir.ranksys.nn.user.sim.VectorCosineUserSimilarity;
import es.uam.eps.ir.ranksys.nn.user.sim.VectorJaccardUserSimilarity;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilterRecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import myRecommender.FileUserNeighborhood;
import myRecommender.MyUserNeighborhoodRecommender;
import myRecommender.MyUserNeighborhoodRecommender.TRANSFORM;
import myRecommender.PearsonSimilarity;
import myRecommender.PearsonUserSimilarity;
import myRecommender.ThresholdUserSimilarity;

public class Experiment {
	private static final int NUM_RECS_PER_USER = 100;
	private static final double EVAL_THRESHOLD = 0.0;

	public static void main(String[] args) throws Exception {

		// args = new String[]{"out_neighs",
		// "src/main/resources/ml-100k/users.txt",
		// "src/main/resources/ml-100k/items.txt",
		// "src/main/resources/ml-100k/u1.base",
		// "u1.base__cosine_neighbors.txt", "cosine", "false", "500", "0"};

		if (args.length == 0) {
			System.out.println("Parameters incorrect -> try split/ub/eval as first parameter");
			System.exit(0);
		}

		if (args[0].equals("ub")) {
			if (args.length < 11) {
				System.out.println(
						"Parameters incorrect -> ub userPath itemPath trainData testData outfile sim transf norm k q [alpha]");
				System.exit(0);
			}
		} else if (args[0].equals("eval")) {
			if (args.length != 4) {
				System.out.println("Parameters incorrect -> eval recfile testdata outfile");
				System.exit(0);
			}
			
		} else if (args[0].equals("recFile")) {
			if (args.length != 9) {
				System.out.println("Parameters incorrect -> recFile userPath itemPath trainData testData neighFile k q outfile");
				System.exit(0);
			}
		} else if (args[0].equals("out_neighs")) {
			if (args.length < 9) {
				System.out.println(
						"Parameters incorrect -> out_neighs userPath itemPath trainData outfile sim norm k q [alpha]");
				System.exit(0);
			}
		}

		/*
		 * else if(args[0].equals("split")) if (args.length != )
		 * System.out.println("Parameters incorrect -> ");
		 */

		switch (args[0]) {
		case "split":
			// RIVAL
			break;

		case "ub": {
			System.out
					.println("Parameters: ub userPath itemPath trainData testData outfile sim transf norm k q [alpha]");
			String userPath = args[1];
			String itemPath = args[2];
			String trainDataPath = args[3];
			String testDataPath = args[4];

			/* Loading user and item indexes ("0", "1", "2"... etc) */
			FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
			FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));

			/* Reading rating file */
			FastPreferenceData<Long, Long> trainData = SimpleFastPreferenceData
					.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);
			FastPreferenceData<Long, Long> testData = SimpleFastPreferenceData
					.load(SimpleRatingPreferencesReader.get().read(testDataPath, lp, lp), userIndex, itemIndex);

			double alpha = 0.5;
			try {
				alpha = Double.parseDouble(args[11]);
			} catch (Exception e) {
				// nothing
			}
			int k = Integer.parseInt(args[9]);
			int q = Integer.parseInt(args[10]);
			boolean norm = Boolean.parseBoolean(args[8]);

			boolean dense = false;

			String simName = args[6];
			UserSimilarity<Long> sim = userSimilarityFactory(userIndex, itemIndex, trainData, alpha, dense, simName);

			UserNeighborhood<Long> neighborhood = new TopKUserNeighborhood<>(sim, k);
			TRANSFORM tr = null;
			switch (args[7]) {
			case "MC":
				tr = TRANSFORM.MC;
				break;

			case "STD":
				tr = TRANSFORM.STD;
				break;

			case "Z":
				tr = TRANSFORM.Z;
				break;

			default:
				break;
			}

			Recommender<Long, Long> recommender = new MyUserNeighborhoodRecommender<>(trainData, neighborhood, q, sim,
					tr, norm);
			String outfile = args[5];
			generateRecommendations(recommender, outfile, userIndex, itemIndex, trainData, testData, NUM_RECS_PER_USER);
			// en script, variar: fold (5), k (5, 10, 20, 40, 60, 100), tr (3),
			// norm (2), sim (12)
		}
			break;

			/*Write neighbours to outfile*/
		case "out_neighs": {
			System.out.println("Parameters: out_neighs userPath itemPath trainData outfile sim k [alpha]");
			String userPath = args[1];
			String itemPath = args[2];
			String trainDataPath = args[3];
			String outFile = args[4];

			/* Loading user and item indexes ("0", "1", "2"... etc) */
			FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
			FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));

			/* Reading rating file */
			FastPreferenceData<Long, Long> trainData = SimpleFastPreferenceData
					.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);

			double alpha = 0.5;
			try {
				alpha = Double.parseDouble(args[7]);
			} catch (Exception e) {
				// nothing
			}
			int k = Integer.parseInt(args[6]);

			boolean dense = false;

			String simName = args[5];
			UserSimilarity<Long> sim = userSimilarityFactory(userIndex, itemIndex, trainData, alpha, dense, simName);
			UserNeighborhood<Long> neighborhood = new TopKUserNeighborhood<>(sim, k);

			Map<Long, Map<Long, Double>> auxNeighbours = new HashMap<>();
			simToMap(neighborhood, auxNeighbours);
			// by default, we want the similarities in the file
			neighboursToFile(outFile, auxNeighbours, true);
		}
			break;

		case "eval": {
			System.out.println("Parameters: eval recfile testdata outfile");
			String testDataPath = args[2];
			String recIn = args[1];
			Double threshold = EVAL_THRESHOLD;
			String outfile = args[3];

			// USER - ITEM - RATING files for train and test
			PreferenceData<Long, Long> testData = SimplePreferenceData
					.load(SimpleRatingPreferencesReader.get().read(testDataPath, lp, lp));
			// BINARY RELEVANCE
			BinaryRelevanceModel<Long, Long> binRel = new BinaryRelevanceModel<>(false, testData, threshold);

			Map<String, SystemMetric<Long, Long>> sysMetrics = new HashMap<>();

			////////////////////////
			// INDIVIDUAL METRICS //
			////////////////////////
			Map<String, RecommendationMetric<Long, Long>> recMetrics = new HashMap<>();

			for (int cutoff : new int[] { 1, 5, 10, 20, 50, 100 }) {
				// PRECISION
				recMetrics.put("prec@" + cutoff, new Precision<>(cutoff, binRel));
				// RECALL
				recMetrics.put("recall@" + cutoff, new Recall<>(cutoff, binRel));
				// nDCG
				recMetrics.put("ndcg@" + cutoff,
						new NDCG<>(cutoff, new NDCG.NDCGRelevanceModel<>(false, testData, threshold)));
			}

			// AVERAGE VALUES OF RECOMMENDATION METRICS FOR ITEMS IN TEST
			int numUsers = testData.numUsersWithPreferences();
			recMetrics.forEach(
					(name, metric) -> sysMetrics.put(name, new AverageRecommendationMetric<>(metric, numUsers)));

			RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(lp, lp);

			format.getReader(recIn).readAll().forEach(rec -> sysMetrics.values().forEach(metric -> metric.add(rec)));

			sysMetrics.forEach((name, metric) -> System.out.println(name + "\t" + metric.evaluate()));

			PrintStream out = new PrintStream(new File(outfile));
			sysMetrics.forEach((name, metric) -> out.println(recIn + "\t" + name + "\t" + metric.evaluate()));
			out.close();
		}
			System.out.println("\nDone!");
			break;

		case "recFile": {
			System.out.println("Parameters: recFile userPath itemPath trainData testData neighFile k q outfile");
			String userPath = args[1];
			String itemPath = args[2];
			String trainDataPath = args[3];
			String testDataPath = args[4];

			/* Loading user and item indexes ("0", "1", "2"... etc) */
			FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
			FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));

			/* Reading rating file */
			FastPreferenceData<Long, Long> trainData = SimpleFastPreferenceData
					.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);
			FastPreferenceData<Long, Long> testData = SimpleFastPreferenceData
					.load(SimpleRatingPreferencesReader.get().read(testDataPath, lp, lp), userIndex, itemIndex);

			File fileNeighborhood = new File(args[5]);
			int k = Integer.parseInt(args[6]);
			FileUserNeighborhood fileUserNeighborhood = new FileUserNeighborhood(userIndex, fileNeighborhood, k);
			
			int q = Integer.parseInt(args[7]);

			UserNeighborhood<Long> userNeighborhood = new UserNeighborhood<Long>(userIndex, fileUserNeighborhood) {
			};

			Recommender<Long, Long> recommender = new UserNeighborhoodRecommender<Long, Long>(trainData,
					userNeighborhood, q);

			String outfile = args[8];
			generateRecommendations(recommender, outfile, userIndex, itemIndex, trainData, testData, NUM_RECS_PER_USER);
		}
			break;

		default:
			System.out.println("Parameters not recognized, try split/ub/eval as first parameter");
			break;
		}
	}

	public static UserSimilarity<Long> userSimilarityFactory(FastUserIndex<Long> userIndex,
			FastItemIndex<Long> itemIndex, FastPreferenceData<Long, Long> trainData, double alpha, boolean dense,
			String simName) throws IOException {
		UserSimilarity<Long> sim = null;
		switch (simName) {
		case "cosine":
			sim = new VectorCosineUserSimilarity<>(trainData, alpha, dense);
			break;
		case "cosine_th_0.3":
			sim = new ThresholdUserSimilarity<>(trainData, new VectorCosineUserSimilarity<>(trainData, alpha, dense),
					0.3, 1.0);
			break;
		case "cosine_th_0.5":
			sim = new ThresholdUserSimilarity<>(trainData, new VectorCosineUserSimilarity<>(trainData, alpha, dense),
					0.5, 1.0);
			break;
		case "jaccard":
			sim = new VectorJaccardUserSimilarity<>(trainData, dense);
			break;
		case "jaccard_th_0.3":
			sim = new ThresholdUserSimilarity<>(trainData, new VectorJaccardUserSimilarity<>(trainData, dense), 0.3,
					1.0);
			break;
		case "jaccard_th_0.5":
			sim = new ThresholdUserSimilarity<>(trainData, new VectorJaccardUserSimilarity<>(trainData, dense), 0.5,
					1.0);
			break;
		case "pearson":
			sim = new PearsonUserSimilarity<>(trainData, dense, -1.0, false);
			break;
		case "pearsoncn":
			sim = new PearsonUserSimilarity<>(trainData, dense, -1.0, true);
			break;
		case "pearson_th_0":
			sim = new ThresholdUserSimilarity<>(trainData, new PearsonSimilarity(trainData, dense, 0.0, false), 0.0,
					1.0);
			break;
		case "pearsoncn_th_0":
			sim = new ThresholdUserSimilarity<>(trainData, new PearsonSimilarity(trainData, dense, 0.0, true), 0.0,
					1.0);
			break;
		case "pearson_th_0.5":
			sim = new ThresholdUserSimilarity<>(trainData, new PearsonSimilarity(trainData, dense, 0.0, false), 0.5,
					1.0);
			break;
		case "pearsoncn_th_0.5":
			sim = new ThresholdUserSimilarity<>(trainData, new PearsonSimilarity(trainData, dense, 0.0, true), 0.5,
					1.0);
			break;
		default:
			break;
		}

		return sim;
	}

	private static void generateRecommendations(Recommender<Long, Long> rec, String outfile,
			FastUserIndex<Long> userIndex, FastItemIndex<Long> itemIndex, FastPreferenceData<Long, Long> trainData,
			FastPreferenceData<Long, Long> testData, int maxLength) throws IOException {
		Set<Long> targetUsers = testData.getUsersWithPreferences().collect(Collectors.toSet());

		/* OUTPUT FORMAT -> userid itemid score */
		RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(lp, lp);
		Function<Long, IntPredicate> filter = FastFilters.notInTrain(trainData);

		RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(userIndex, itemIndex,
				targetUsers.stream(), filter, maxLength);

		try (RecommendationFormat.Writer<Long, Long> writer = format.getWriter(outfile)) {
			runner.run(rec, writer);
		}

		System.out.println("\nDone!");
	}

	private static void simToMap(UserNeighborhood<Long> neighborhood, Map<Long, Map<Long, Double>> map) {
		neighborhood.getAllUsers().forEach(u -> {
			neighborhood.getNeighbors(u).forEach(val -> {
				if (!map.containsKey(u)) {

					Map<Long, Double> aux = new HashMap<>();
					aux.put(val.v1, val.v2);
					map.put(u, aux);
				} else {
					map.get(u).put(val.v1, val.v2);
				}
			});
		});
	}

	private static void neighboursToFile(String output, Map<Long, Map<Long, Double>> map, boolean outputSim)
			throws FileNotFoundException {

		PrintStream out = new PrintStream(new File(output));

		map.entrySet().stream().forEach(entry -> {
			// out.println(entry.getKey() + "\t" +
			// entry.getValue().keySet().stream().map(Object::toString).collect(Collectors.joining(",")));
			Map<Long, Double> neighbours = entry.getValue();
			// sort by (reverse) value
			out.println(entry.getKey() + "\t"
					+ neighbours.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
							.map(e -> e.getKey().toString() + (outputSim ? ":" + e.getValue() : ""))
							.collect(Collectors.joining(",")));
		});
		out.close();
	}
}
