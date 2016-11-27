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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

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

	private final static String OUTPUT_SHORT_PARAM = "out";
	private final static String OUTPUT_LONG_PARAM = "output";
	private final static String OUTPUT_DESC = "Output file";

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
				"Usage: \n" + "-%s [%s] arg \t\t\t %s \n" + "-%s [%s] arg \t\t\t %s \n" + "-%s [%s] arg \t\t\t %s \n" + "-%s [%s] arg \t\t\t %s \n"
						+ "-%s [%s] arg \t\t\t %s \n" + "-%s [%s] arg \t\t\t %s \n" + "-%s [%s] arg \t %s \n"
						+ "-%s [%s] \t\t %s \n" + "-%s [%s] \t\t\t %s \n",
				PORT_SHORT_PARAM, PORT_LONG_PARAM, PORT_DESC, INPUT_SHORT_PARAM, INPUT_LONG_PARAM, INPUT_DESC,
				OUTPUT_SHORT_PARAM, OUTPUT_LONG_PARAM, OUTPUT_DESC,
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
		o = new Option(OUTPUT_SHORT_PARAM, OUTPUT_LONG_PARAM, true, OUTPUT_DESC);
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

		try {
			CommandLine cmd = parser.parse(opt, args);

			String host = cmd.getOptionValue(HOST_SHORT_PARAM);

			String inputFile = cmd.getOptionValue(INPUT_SHORT_PARAM);
			
			BufferedReader inp = new BufferedReader(new FileReader(inputFile));

			// read the user mapping
			Map<Integer, Long> userMapping = new HashMap<>();
			BufferedReader in = new BufferedReader(new FileReader(inputFile + "_users"));
			String line = null;
			int index = 0;
			while ((line = in.readLine()) != null) {
				long id = Long.parseLong(line);
				userMapping.put(index, id);
				index++;
			}
			in.close();

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

			/* Map to store userId, neighborsIds */
			Map<Long, List<Long>> nmsNeighbors = new HashMap<>();

			String separator = System.getProperty("line.separator");
			try {
				TTransport transport = new TSocket(host, port);
				transport.open();

				TProtocol protocol = new TBinaryProtocol(transport);
				QueryService.Client client = new QueryService.Client(protocol);

				int nLine = 0;
				while ((line = inp.readLine()) != null) {
					StringBuffer sb = new StringBuffer();
					sb.append(line);
					sb.append(separator);

					String queryObj = sb.toString();

					if (!queryTimeParams.isEmpty())
						client.setQueryTimeParams(queryTimeParams);

					List<ReplyEntry> res = null;

					// long t1 = System.nanoTime();

					if (searchType == SearchType.kKNNSearch) {
						// System.out.println(String.format("Running a %d-NN
						// search", k));
						res = client.knnQuery(k, queryObj, retExternId, retObj);
					} else {
						// System.out.println(String.format("Running a range
						// search (r=%g)", r));
						res = client.rangeQuery(r, queryObj, retExternId, retObj);
					}

					res = res.stream().filter(v -> v.getDist() > 0.0).collect(Collectors.toList());

					List<Long> neighbors = new ArrayList<>();

					res.forEach(elem -> {
						// the returned ids start in zero by default, so we need to transform that value to a proper id
						neighbors.add(userMapping.get(elem.getId()));
					});

					nmsNeighbors.put(userMapping.get(nLine), neighbors);

					nLine++;
				}
				
				inp.close();

				/*------------------------------------------ Exporting ------------------------------------------------------*/
				String outfile = cmd.getOptionValue(OUTPUT_SHORT_PARAM);//"NMSLIB-neighbours.txt";

				PrintStream out = new PrintStream(new File(outfile));
				nmsNeighbors.entrySet().stream().forEach(entry -> {
					out.println(entry.getKey() + "\t" + entry.getValue().stream().map(Object::toString)
							.collect(Collectors.joining(",")).toString());
				});
				out.close();

				System.out.println("Done! Neighborhood files created succesfuly. Closing connection");

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
