package itemBased;

import java.io.File;
import java.util.List;

import org.grouplens.lenskit.ItemRecommender;
import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.RecommenderBuildException;
import org.grouplens.lenskit.baseline.BaselineScorer;
import org.grouplens.lenskit.baseline.ItemMeanRatingItemScorer;
import org.grouplens.lenskit.baseline.UserMeanBaseline;
import org.grouplens.lenskit.baseline.UserMeanItemScorer;
import org.grouplens.lenskit.core.LenskitConfiguration;
import org.grouplens.lenskit.core.LenskitRecommender;
import org.grouplens.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.text.TextEventDAO;
import org.grouplens.lenskit.knn.item.ItemItemScorer;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.transform.normalize.DefaultUserVectorNormalizer;
import org.grouplens.lenskit.transform.normalize.UserVectorNormalizer;

public class LenskitTestIB {

	public static void main(String[] args) {
		
		
		LenskitConfiguration config = new LenskitConfiguration();
		config.bind(ItemScorer.class).to(ItemItemScorer.class);
		config.bind(BaselineScorer.class, ItemScorer.class).to(UserMeanItemScorer.class);
		config.bind(UserMeanBaseline.class, ItemScorer.class).to(ItemMeanRatingItemScorer.class);
		config.bind(UserVectorNormalizer.class).to(DefaultUserVectorNormalizer.class);
		
		config.bind(EventDAO.class).to(TextEventDAO.ratings(new File("src/main/resources/u.data"), ","));

		
		try {
			LenskitRecommender rec = LenskitRecommender.build(config);
			
			//Generating recommendations
			ItemRecommender irec = rec.getItemRecommender();
			
			//Recommending 10 items to user 42
			List<ScoredId> recommendations = irec.recommend(42, 10);
			
			System.out.println("\nRecomendation list:\n " + recommendations);
			
		} catch (RecommenderBuildException e) { e.printStackTrace();}

	}

}
