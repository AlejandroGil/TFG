package myRecommender.nmslib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ranksys.core.preference.PointWisePreferenceData;

import es.uam.eps.ir.ranksys.core.preference.IdPref;
import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;

/**
 * 
 * 
 * @author Alejandro
 *
 * @param <T> type of users and items (the same type!)
 */
public class DatamodelTransformation<T> {

	private double defaultValue;
	private boolean itemSpace;
	private Map<Integer, T> indexIdMapping;
	private Map<T, Integer> idRanksysIndexMapping;
	private List<T> representationSpace;
	private PointWisePreferenceData<T, T> datamodel;

	public DatamodelTransformation(PointWisePreferenceData<T, T> data, FastUserIndex<T> userIndex, FastItemIndex<T> itemIndex,
			double defValue, boolean useItemsAsSpace) {
		this.datamodel = data;
		this.defaultValue = defValue;
		this.itemSpace = useItemsAsSpace;

		indexIdMapping = new HashMap<>();
		idRanksysIndexMapping = new HashMap<>();
		if (itemSpace) {
			// represent users with respect to items
			representationSpace = datamodel.getAllItems().sorted().collect(Collectors.toList());
			datamodel.getAllUsers().forEach(u -> {
				idRanksysIndexMapping.put(u, userIndex.user2uidx(u));
				indexIdMapping.put(indexIdMapping.size(), u);
			});
		} else {
			// represent items with respect to users
			representationSpace = datamodel.getAllUsers().sorted().collect(Collectors.toList());
			datamodel.getAllItems().forEach(i -> {
				idRanksysIndexMapping.put(i, itemIndex.item2iidx(i));
				indexIdMapping.put(indexIdMapping.size(), i);
			});
		}
	}

	public T getIdFromIndex(Integer idx) {
		return indexIdMapping.get(idx);
	}

	public T getIdFromRankSys(int idx) {
		T id = null;
		id = idRanksysIndexMapping.entrySet().stream().filter(e -> e.getValue() == idx).findFirst().get().getKey();
		return id;
	}

	public Integer getRankSysIndex(T id) {
		return idRanksysIndexMapping.get(id);
	}

	public double[] transform(T id) {
		double[] vector = new double[representationSpace.size()];
		IntStream.range(0, vector.length).forEach(idx -> {
			T coord = representationSpace.get(idx);
			T u = null;
			T i = null;
			// get corresponding coordinates
			if (itemSpace) {
				u = id;
				i = coord;
			} else {
				u = coord;
				i = id;
			}
			//
			double p = defaultValue;
			Optional<? extends IdPref<T>> pref = datamodel.getPreference(u, i);
			if (pref.isPresent()) {
				p = pref.get().v2();
			}
			vector[idx] = p;
		});
		return vector;
	}

	public void write(File mappingFile, File generationFile) throws FileNotFoundException {
		PrintStream out = new PrintStream(generationFile);
		PrintStream outMapping = new PrintStream(mappingFile);

		indexIdMapping.keySet().stream().sorted().forEach(idx -> {
			T id = getIdFromIndex(idx);
			outMapping.println(idx + "\t" + id);

			double[] vector = transform(id);

			IntStream.range(0, vector.length).forEach(i -> {
				out.print(vector[i] + "\t");
			});
			out.println();
		});

		out.close();
		outMapping.close();
	}
}
