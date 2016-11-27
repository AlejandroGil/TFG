package myRecommender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ranksys.core.util.tuples.Tuple2id;

import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.nn.neighborhood.Neighborhood;

public class FileUserNeighborhood implements Neighborhood {

	private Map<Integer, List<Tuple2id>> neighborsFromFile;

	public FileUserNeighborhood(FastUserIndex<Long> uIndex, File fileNeighborhood)
			throws FileNotFoundException, IOException {
		neighborsFromFile = new HashMap<>();
		// read the file
		BufferedReader in = new BufferedReader(new FileReader(fileNeighborhood));
		String line = null;
		while ((line = in.readLine()) != null) {
			// format: userid \t n1:s(u,n1),n2:s(u,n2),...
			// or: userid \t n1,n2,...
			String[] toks = line.split("\t");
			Long userid = Long.parseLong(toks[0]);
			int uidx = uIndex.user2uidx(userid);

			List<String> temp = Arrays.asList(toks[1].split(",")).stream().map(String::trim)
					.collect(Collectors.toList());
			List<Tuple2id> neighbors = new ArrayList<>();
			IntStream.range(0, temp.size()).forEach(idx -> {
				String s = temp.get(idx);
				if (s.contains(":")) {
					String[] t = s.split(":");
					neighbors.add(new Tuple2id(uIndex.user2uidx(Long.parseLong(t[0])), Double.parseDouble(t[1])));
				} else {
					// if similarity is not stored in file, then we use the ranking of the neighbors: sim = 1 / rank
					neighbors.add(new Tuple2id(uIndex.user2uidx(Long.parseLong(s)), 1.0 / (idx + 1)));
				}
			});
		}
		in.close();
	}

	@Override
	public Stream<Tuple2id> getNeighbors(int idx) {
		return neighborsFromFile.getOrDefault(idx, new ArrayList<>()).stream();
	}
}
