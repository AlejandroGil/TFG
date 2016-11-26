/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myRecommender;

import static org.ranksys.formats.parsing.Parsers.lp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.ranksys.formats.index.ItemsReader;
import org.ranksys.formats.index.UsersReader;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;

import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 *
 * @author alejandrogil
 */
public class CompareNeighbours {

	public static void main(String args[]) throws IOException {

		/*---------------------- Reading neighbours --------------------*/
		Map<Integer, List<Integer>> cosineNeighbours = fileToMap("Cosine-neighbours.txt");
		Map<Integer, List<Integer>> nmslibNeighbours = fileToMap("NMSLIB-neighbours.txt");

		System.out.println("Neighbours in input file: " + nmslibNeighbours.get(0).size());

		/*
		 * For all users, we compare the list stored in map of neighbours
		 * associated with a user (nmslib results) with the real neighbours of
		 * that user
		 */
		/*
		 * -------------------------------------------- Calculate common
		 * neighbours --------------------------------
		 */

		List<Integer> knn = Arrays.asList(500, 100, 50, 10);
		List<Double> commonRate = commonNeighbours(knn, cosineNeighbours, nmslibNeighbours);

		int i = 0;
		for (Integer val : knn) {

			System.out.println("Hit Rate with Cosnine @ " + val + " NN = "
					+ commonRate.get(i) / (val * cosineNeighbours.size()) * 100 + "%");
			i++;
		}

	}

	private static Map<Integer, List<Integer>> fileToMap(String inputFile) throws IOException {

		BufferedReader inp = new BufferedReader(new FileReader(inputFile));
		String line = inp.readLine();

		Map<Integer, List<Integer>> map = new HashMap<>();

		while (line != null) {
			StringBuffer sb = new StringBuffer();
			sb.append(line);
			sb.append(System.getProperty("line.separator"));

			int tabIndex = line.indexOf("\t");
			String user = line.substring(0, tabIndex);

			List<String> neighbours = new ArrayList<String>(Arrays.asList(line.substring(tabIndex + 1).split(",")));

			map.put(Integer.parseInt(user), neighbours.stream().map(Integer::parseInt).collect(Collectors.toList()));

			line = inp.readLine();
		}

		return map;
	}

	private static List<Double> commonNeighbours(List<Integer> knn, Map<Integer, List<Integer>> map,
			Map<Integer, List<Integer>> map2) {

		Int2DoubleOpenHashMap commonRateMap = new Int2DoubleOpenHashMap();
		commonRateMap.defaultReturnValue(0.0);

		List<Double> result = new ArrayList<>();

		knn.forEach(elem -> {
			map.entrySet().forEach(entry -> {

				if (map2.containsKey(entry.getKey())) {

					/* NMSLIB neighbours */
					List<Integer> common = new ArrayList<>(
							entry.getValue().stream().limit(elem).collect(Collectors.toList()));

					common.retainAll(map2.get(entry.getKey()).stream().limit(elem).collect(Collectors.toList()));
					commonRateMap.addTo(1, common.size());
				}
			});
			result.add(commonRateMap.get(1));
			commonRateMap.put(1, 0.0);
		});

		return result;
	}
}
