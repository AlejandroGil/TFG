package userBased;
import java.io.File;
import java.util.List;
import org.grouplens.lenskit.*;
import org.grouplens.lenskit.baseline.BaselineScorer;
import org.grouplens.lenskit.baseline.ItemMeanRatingItemScorer;
import org.grouplens.lenskit.baseline.UserMeanBaseline;
import org.grouplens.lenskit.baseline.UserMeanItemScorer;
import org.grouplens.lenskit.core.*;
import org.grouplens.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.text.TextEventDAO;
import org.grouplens.lenskit.knn.user.UserUserItemScorer;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.transform.normalize.DefaultUserVectorNormalizer;
import org.grouplens.lenskit.transform.normalize.MeanCenteringVectorNormalizer;
import org.grouplens.lenskit.transform.normalize.UserVectorNormalizer;
import org.grouplens.lenskit.transform.normalize.VectorNormalizer;

public class LensKitTestUB {

	//----------------- COLABORATIVE FILTERING - USER-BASED RECOMMENDATION -----------------//
	
	public static void main(String[] args) {

		LenskitConfiguration config = new LenskitConfiguration();
		
		// Use item-item CF to score items
		config.bind(ItemScorer.class).to(UserUserItemScorer.class);
		// let's use personalized mean rating as the baseline/fallback predictor.
		// 2-step process:
		// First, use the user mean rating as the baseline scorer
		config.bind(BaselineScorer.class, ItemScorer.class).to(UserMeanItemScorer.class);
		// Second, use the item mean rating as the base for user means
		config.bind(UserMeanBaseline.class, ItemScorer.class).to(ItemMeanRatingItemScorer.class);
		// and normalize ratings by baseline prior to computing similarities
		
		//-----> No normalized
		//config.bind(UserVectorNormalizer.class).to(BaselineSubtractingUserVectorNormalizer.class);
		
		
		//-----> Normalized
		config.bind(UserVectorNormalizer.class).to(DefaultUserVectorNormalizer.class);
				
		//Connecting the Data Source
		//config.bind(EventDAO.class).to(new SimpleFileRatingDAO(new File("ratings.csv"), ",")); -----> deprecated
	
		config.bind(EventDAO.class).to(TextEventDAO.ratings(new File("src/main/resources/ratings.csv"), ","));
		
		//Creating the recommender
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
