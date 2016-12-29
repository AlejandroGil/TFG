package myRecommender.nmslib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.ranksys.core.util.tuples.Tuple2id;

import es.uam.eps.ir.ranksys.nn.neighborhood.Neighborhood;
import thrift.QueryService;
import thrift.ReplyEntry;

/**
 * Neighborhood based on NMSLIB approximate KNN.
 * It depends on a DatamodelTransformation, the same one that should be instantiated
 * to generate the files that will be passed to the Query Server (using Thrift).
 * 
 * @author Alejandro
 *
 * @param <T> type of users and items (the same type!)
 */
public class NMSLibNeighborhood<T> implements Neighborhood {

	private final DatamodelTransformation<T> transformation;
	private final int k;
	private QueryService.Client client;

	public NMSLibNeighborhood(DatamodelTransformation<T> transformation, int k, String host, int port,
			String queryTimeParams) {
		this.transformation = transformation;
		this.k = k;

		// init client
		try {
			TTransport transport = new TSocket(host, port);
			transport.open();
			TProtocol protocol = new TBinaryProtocol(transport);
			this.client = new QueryService.Client(protocol);
		} catch (TTransportException e) {
			e.printStackTrace();
			this.client = null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		// Close transport/socket !
		client.getInputProtocol().getTransport().close();
	}

	@Override
	public Stream<Tuple2id> getNeighbors(int idx) {
		if (client != null) {
			//T target = transformation.getIdFromIndex(idx);
			T target = transformation.getIdFromRankSys(idx);
			double[] vector = transformation.transform(target);
			//String queryObj = Arrays.asList(vector).stream().map(Object::toString).collect(Collectors.joining("\t"));
			String queryObj = Arrays.stream(vector).mapToObj(Double::toString).collect(Collectors.joining("\t"));
			//System.out.println("Query:" + queryObj);
			List<Tuple2id> neighbors = new ArrayList<>();
			try {
				List<ReplyEntry> res = client.knnQuery(k + 1, queryObj, false, false);
				res.forEach(elem -> {
					T id = transformation.getIdFromIndex(elem.getId());
					Integer index = transformation.getRankSysIndex(id);
					if (id != target) {
						neighbors.add(new Tuple2id(index, elem.dist));
					}
				});
			} catch (TException e) {
				e.printStackTrace();
			}
			return neighbors.stream();
		}
		return Stream.empty();
	}
}
