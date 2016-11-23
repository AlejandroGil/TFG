/**
 * Non-metric Space Library
 *
 * Authors: Bilegsaikhan Naidan (https://github.com/bileg), Leonid Boytsov (http://boytsov.info).
 * With contributions from Lawrence Cayton (http://lcayton.com/) and others.
 *
 * For the complete list of contributors and further details see:
 * https://github.com/searchivarius/NonMetricSpaceLib
 *
 * Copyright (c) 2015
 *
 * This code is released under the
 * Apache License Version 2.0 http://www.apache.org/licenses/.
 *
 */
package thrift;

import com.google.common.collect.Lists;
import org.apache.commons.cli.*;

import org.apache.thrift.TException;
import org.apache.thrift.transport.*;

import org.apache.thrift.protocol.*;

import java.io.*;
import java.util.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

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
import java.util.stream.Collectors;
import myRecommender.PearsonUserSimilarity;
import static org.ranksys.formats.parsing.Parsers.lp;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;


public class Client {
	enum SearchType {
		kKNNSearch, kRangeSearch
	};

	private final static String PORT_SHORT_PARAM = "p";
	private final static String PORT_LONG_PARAM = "port";
	private final static String PORT_DESC = "TCP/IP server port number";

	private final static String INPUT_SHORT_PARAM = "i";
	private final static String INPUT_LONG_PARAM = "input";
	private final static String INPUT_DESC = "Input file";

	private final static String HOST_SHORT_PARAM = "a";
	private final static String HOST_LONG_PARAM = "addr";
	private final static String HOST_DESC = "TCP/IP server address";

	private final static String K_SHORT_PARAM = "k";
	private final static String K_LONG_PARAM = "knn";
	private final static String K_DESC = "k for k-NN search";

	private final static String R_SHORT_PARAM = "r";
	private final static String R_LONG_PARAM = "range";
	private final static String R_DESC = "range for the range search";

	private final static String QUERY_TIME_SHORT_PARAM = "t";
	private final static String QUERY_TIME_LONG_PARAM = "queryTimeParams";
	private final static String QUERY_TIME_DESC = "Query time parameters";

	private final static String RET_OBJ_SHORT_PARAM = "o";
	private final static String RET_OBJ_LONG_PARAM = "retObj";
	private final static String RET_OBJ_DESC = "Return string representation of found objects?";

	private final static String RET_EXTERN_ID_SHORT_PARAM = "e";
	private final static String RET_EXTERN_ID_LONG_PARAM = "retExternId";
	private final static String RET_EXTERN_ID_DESC = "Return external IDs?";
        
	static void Usage(String err) {
		System.err.println("Error: " + err);
		System.err.println(String.format(
				"Usage: \n" + "-%s [%s] arg \t\t\t %s \n" + "-%s [%s] arg \t\t\t %s \n" + "-%s [%s] arg \t\t\t %s \n"
						+ "-%s [%s] arg \t\t\t %s \n" + "-%s [%s] arg \t\t\t %s \n" + "-%s [%s] arg \t %s \n"
						+ "-%s [%s] \t\t %s \n" + "-%s [%s] \t\t\t %s \n",
				PORT_SHORT_PARAM, PORT_LONG_PARAM, PORT_DESC, INPUT_SHORT_PARAM, INPUT_LONG_PARAM, INPUT_DESC,
				HOST_SHORT_PARAM, HOST_LONG_PARAM, HOST_DESC, K_SHORT_PARAM, K_LONG_PARAM, K_DESC, R_SHORT_PARAM,
				R_LONG_PARAM, R_DESC, QUERY_TIME_SHORT_PARAM, QUERY_TIME_LONG_PARAM, QUERY_TIME_DESC,
				RET_EXTERN_ID_SHORT_PARAM, RET_EXTERN_ID_LONG_PARAM, RET_EXTERN_ID_DESC, RET_OBJ_SHORT_PARAM,
				RET_OBJ_LONG_PARAM, RET_OBJ_DESC

		));
		System.exit(1);
	}

	public static void main(String args[]) throws IOException {

		Options opt = new Options();

		Option o = new Option(PORT_SHORT_PARAM, PORT_LONG_PARAM, true, PORT_DESC);
		o.setRequired(true);
		opt.addOption(o);
		o = new Option(INPUT_SHORT_PARAM, INPUT_LONG_PARAM, true, INPUT_DESC);
		o.setRequired(true);
		opt.addOption(o);
		o = new Option(HOST_SHORT_PARAM, HOST_LONG_PARAM, true, HOST_DESC);
		o.setRequired(true);
		opt.addOption(o);
		opt.addOption(K_SHORT_PARAM, K_LONG_PARAM, true, K_DESC);
		opt.addOption(R_SHORT_PARAM, R_LONG_PARAM, true, R_DESC);
		opt.addOption(QUERY_TIME_SHORT_PARAM, QUERY_TIME_LONG_PARAM, true, QUERY_TIME_DESC);
		opt.addOption(RET_OBJ_SHORT_PARAM, RET_OBJ_LONG_PARAM, false, RET_OBJ_DESC);
		opt.addOption(RET_EXTERN_ID_SHORT_PARAM, RET_EXTERN_ID_LONG_PARAM, false, RET_EXTERN_ID_DESC);

		CommandLineParser parser = new org.apache.commons.cli.GnuParser();

		int userId = 0;
		/* Map to store userId, neighborsIds */
		HashMap<Integer, List<Integer>> nmsNeighbors = new HashMap<Integer, List<Integer>>();

		try {
			CommandLine cmd = parser.parse(opt, args);

			String host = cmd.getOptionValue(HOST_SHORT_PARAM);

			String inputFile = cmd.getOptionValue(INPUT_SHORT_PARAM);
			BufferedReader inp = new BufferedReader(new FileReader(inputFile));

			String tmp = null;

			tmp = cmd.getOptionValue(PORT_SHORT_PARAM);

			int port = -1;

			try {
				port = Integer.parseInt(tmp);
			} catch (NumberFormatException e) {
				Usage("Port should be integer!");
			}

			boolean retObj = cmd.hasOption(RET_OBJ_SHORT_PARAM);
			boolean retExternId = cmd.hasOption(RET_EXTERN_ID_SHORT_PARAM);

			String queryTimeParams = cmd.getOptionValue(QUERY_TIME_SHORT_PARAM);
			if (null == queryTimeParams)
				queryTimeParams = "";

			SearchType searchType = SearchType.kKNNSearch;
			int k = 0;
			double r = 0;

			if (cmd.hasOption(K_SHORT_PARAM)) {
				if (cmd.hasOption(R_SHORT_PARAM)) {
					Usage("Range search is not allowed if the KNN search is specified!");
				}
				tmp = cmd.getOptionValue(K_SHORT_PARAM);
				try {
					k = Integer.parseInt(tmp);
				} catch (NumberFormatException e) {
					Usage("K should be integer!");
				}
				searchType = SearchType.kKNNSearch;
			} else if (cmd.hasOption(R_SHORT_PARAM)) {
				if (cmd.hasOption(K_SHORT_PARAM)) {
					Usage("KNN search is not allowed if the range search is specified!");
				}
				searchType = SearchType.kRangeSearch;
				tmp = cmd.getOptionValue(R_SHORT_PARAM);
				try {
					r = Double.parseDouble(tmp);
				} catch (NumberFormatException e) {
					Usage("The range value should be numeric!");
				}
			} else {
				Usage("One has to specify either range or KNN-search parameter");
			}

			String separator = System.getProperty("line.separator");

			try {

				TTransport transport = new TSocket(host, port);
				transport.open();

				TProtocol protocol = new TBinaryProtocol(transport);
				QueryService.Client client = new QueryService.Client(protocol);

				String line = inp.readLine();

				while (line != null) {
					StringBuffer sb = new StringBuffer();
					sb.append(line);
					sb.append(separator);

					String queryObj = sb.toString();

					if (!queryTimeParams.isEmpty())
						client.setQueryTimeParams(queryTimeParams);

					List<ReplyEntry> res = null;

					//long t1 = System.nanoTime();

					if (searchType == SearchType.kKNNSearch) {
						//System.out.println(String.format("Running a %d-NN search", k));
						res = client.knnQuery(k, queryObj, retExternId, retObj);
					} else {
						// System.out.println(String.format("Running a range
						// search (r=%g)", r));
						res = client.rangeQuery(r, queryObj, retExternId, retObj);
					}

					res = res.stream().filter(v -> v.getDist() > 0.0).collect(Collectors.toList());

					List <Integer> neighbors = new ArrayList<Integer>();
                                        
                                        res.forEach(elem -> {
                                            neighbors.add(elem.getId());
                                        });
                                        
					nmsNeighbors.put(userId, neighbors);

					userId++;
					queryObj = null;
					line = inp.readLine();
				}
                                
                                
                                String userPath = "src/main/resources/ml-100k/users.txt";
                                String itemPath = "src/main/resources/ml-100k/items.txt";
                                String trainDataPath = "src/main/resources/ml-100k/u1.base";

                                /*Loading user and item indexes ("0", "1", "2"... etc)*/
                                FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
                                FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));

                                /*Reading rating file*/
                                FastPreferenceData<Long, Long> data = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);

                                int q = 1;

                                UserSimilarity<Long> sim = new VectorCosineUserSimilarity<>(data, 0.5 ,true);
                                UserNeighborhood<Long> neighborhood = new TopKUserNeighborhood<>(sim, k);
                                
                                Int2DoubleOpenHashMap commonRateMap = new Int2DoubleOpenHashMap();
                                commonRateMap.defaultReturnValue(0.0);

                                List<Integer> knn = Arrays.asList(k,100,50,10);
                                List<Double> result = new ArrayList<>();
                               
                                Map<Integer, Map<Integer, Double>> auxNeighbours = new HashMap<>();  
                                /*We need to sort neighbours by similarity*/
                                data.getAllUidx().forEach(uIdx ->{
                                    neighborhood.getNeighbors(uIdx).forEach(val -> {
                                        
                                        if (!auxNeighbours.containsKey(uIdx)){
                                            
                                            Map<Integer, Double> aux = new HashMap<>();
                                            aux.put(val.v1, val.v2);
                                            auxNeighbours.put(uIdx, aux);
                                        }
                                        else {
                                            auxNeighbours.get(uIdx).put(val.v1, val.v2);
                                        }
                                    });
                                });
                                                           
                                Map<Integer, Map<Integer, Double>> orderedNeighbours = new HashMap<>();  
                                    /*Sorting map by value*/
                                auxNeighbours.entrySet().forEach(entry -> {
                                                                        
                                    orderedNeighbours.put(entry.getKey(), entry.getValue().entrySet().stream()
                                    .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                                    .collect(Collectors.toMap(
                                      Map.Entry::getKey, 
                                      Map.Entry::getValue, 
                                      (e1, e2) -> e1, 
                                      LinkedHashMap::new)));
                                });
                                        
                                        
                                
                                
                                /*For all users, we compare the list stored in map of neighbours associated with a user (nmslib results) 
                                with the real neighbours of that user */
                                knn.forEach(elem ->{

                                    data.getAllUidx().forEach(uIndex ->{
                                        nmsNeighbors.entrySet().forEach(entry -> {

                                            if(entry.getKey() == uIndex){
                                                
                                                /*NMSLIB neighbours*/
                                                List<Integer> common = new ArrayList<>(entry.getValue().stream().limit(elem).collect(Collectors.toList()));
                                                List<Integer> aux = new ArrayList<>();
                                                
                                                /*Cosine neighbours*/
                                                orderedNeighbours.get(uIndex).entrySet().stream().limit(elem).forEach(val -> {
                                                    
                                                    aux.add(val.getKey());
                                                });
                                                
                                                common.retainAll(aux);
                                                commonRateMap.addTo(1, common.size());
                                            }
                                        });
                                    });

                                    result.add(commonRateMap.get(1));
                                    commonRateMap.put(1, 0.0);
                                });
                                                                
                                int i = 0;
                                
                                for (Integer val : knn){
                                    
                                    System.out.println("Hit Rate with Cosnine @ " + val + " NN = " + result.get(i)/(val * data.getAllUidx().count()) * 100 + "%");
                                    i++;
                                } 
                                
				System.out.println("Done! Closing connection");

				transport.close(); // Close transport/socket !
			} catch (TException te) {
				System.err.println("Apache Thrift exception: " + te);
				te.printStackTrace();
			}

		} catch (ParseException e) {
			Usage("Cannot parse arguments");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
