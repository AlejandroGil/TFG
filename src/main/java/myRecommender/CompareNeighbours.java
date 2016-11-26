/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myRecommender;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 *
 * @author alejandrogil
 */
public class CompareNeighbours {

	public static void main(String args[]) throws IOException {
		String file1 = "Cosine-neighbours.txt";
		String file2 = "NMSLIB-neighbours.txt";
		String k = "500, 100, 50, 10";
		
		//args = new String[]{file1, file2, k};
		if (args.length == 3) {
			file1 = args[0];
			file2 = args[1];
			k = args[2];
		}

		/*---------------------- Reading neighbours --------------------*/
		Map<Integer, List<Integer>> file1Neighbours = fileToMap(file1);
		Map<Integer, List<Integer>> file2Neighbours = fileToMap(file2);

		System.out.println("Neighbours in " + file1 + ": " + file1Neighbours.get(0).size());
		System.out.println("Neighbours in " + file2 + ": " + file2Neighbours.get(0).size());

		List<Integer> knn = new ArrayList<>(Arrays.asList(k.split(",")).stream().map(String::trim).map(Integer::parseInt).collect(Collectors.toList()));
		/*List<Integer> knn = new ArrayList<>();
		for (String nn : k.split(",")) {
			knn.add(Integer.parseInt(nn.trim()));
		}*/

		/*
		 * For all users, we compare the list stored in each map of neighbours
		 * associated with a user
		 */
		Map<Integer, Double> commonRate = commonNeighbours(knn, file1Neighbours, file2Neighbours);
		for (Integer val : knn) {
			System.out.println("Hit Rate between " + file1 + " and " + file2 + " @" + val + " = "
					+ 100 * commonRate.get(val) + "%");
		}
	}

	private static Map<Integer, List<Integer>> fileToMap(String inputFile) throws IOException {

		BufferedReader inp = new BufferedReader(new FileReader(inputFile));

		Map<Integer, List<Integer>> map = new HashMap<>();

		String line = null;
		while ((line = inp.readLine()) != null) {
			String[] toks = line.split("\t");
			String user = toks[0];
			List<String> neighbours = new ArrayList<String>(Arrays.asList(toks[1].split(",")));
			map.put(Integer.parseInt(user), neighbours.stream().map(Integer::parseInt).collect(Collectors.toList()));
		}
		inp.close();

		return map;
	}

	private static Map<Integer, Double> commonNeighbours(List<Integer> knn, Map<Integer, List<Integer>> map,
			Map<Integer, List<Integer>> map2) {

		Int2DoubleOpenHashMap commonRateMap = new Int2DoubleOpenHashMap();
		commonRateMap.defaultReturnValue(0.0);

		Map<Integer, Double> result = new HashMap<>();

		knn.forEach(elem -> {
			map.entrySet().forEach(entry -> {

				if (map2.containsKey(entry.getKey())) {
					Set<Integer> common = new TreeSet<>(
							entry.getValue().stream().limit(elem).collect(Collectors.toSet()));

					common.retainAll(map2.get(entry.getKey()).stream().limit(elem).collect(Collectors.toSet()));
					// intersection size
					commonRateMap.addTo(1, common.size());
					// we will normalize by the maximum possible number of this intersection
					commonRateMap.addTo(2, elem);
				}
			});
			result.put(elem, commonRateMap.get(1) / commonRateMap.get(2));
			commonRateMap.clear();
		});

		return result;
	}
}
