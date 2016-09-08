package lenskitTest;
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
import org.grouplens.lenskit.knn.user.UserUserItemScorer;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.transform.normalize.DefaultUserVectorNormalizer;
import org.grouplens.lenskit.transform.normalize.UserVectorNormalizer;

public class LensKitTestUB_IB {

	//----------------- COLABORATIVE FILTERING - USER-BASED RECOMMENDATION -----------------//
	
	public static void main(String[] args) {

		LenskitConfiguration configUB = new LenskitConfiguration();
		LenskitConfiguration configIB = new LenskitConfiguration();
		
		// Use item-item CF to score items
		configUB.bind(ItemScorer.class).to(UserUserItemScorer.class);
		configIB.bind(ItemScorer.class).to(ItemItemScorer.class);
		
		// let's use personalized mean rating as the baseline/fallback predictor.
		// 2-step process:
		// First, use the user mean rating as the baseline scorer
		configUB.bind(BaselineScorer.class, ItemScorer.class).to(UserMeanItemScorer.class);
		configIB.bind(BaselineScorer.class, ItemScorer.class).to(UserMeanItemScorer.class);

		// Second, use the item mean rating as the base for user means
		configUB.bind(UserMeanBaseline.class, ItemScorer.class).to(ItemMeanRatingItemScorer.class);
		configIB.bind(UserMeanBaseline.class, ItemScorer.class).to(ItemMeanRatingItemScorer.class);

		// and normalize ratings by baseline prior to computing similarities
		
		//-----> No normalized
		//config.bind(UserVectorNormalizer.class).to(BaselineSubtractingUserVectorNormalizer.class);
		
		
		//-----> Normalized
		configUB.bind(UserVectorNormalizer.class).to(DefaultUserVectorNormalizer.class);
		configIB.bind(UserVectorNormalizer.class).to(DefaultUserVectorNormalizer.class);
				
		//Connecting the Data Source
		//config.bind(EventDAO.class).to(new SimpleFileRatingDAO(new File("ratings.csv"), ",")); -----> deprecated
	
		configUB.bind(EventDAO.class).to(TextEventDAO.ratings(new File("src/main/resources/ratings.data"), "	"));
		configIB.bind(EventDAO.class).to(TextEventDAO.ratings(new File("src/main/resources/ratings.data"), "	"));
		
		//Creating the recommender
		try {
			LenskitRecommender recUB = LenskitRecommender.build(configUB);
			LenskitRecommender recIB = LenskitRecommender.build(configIB);
		
		//Generating recommendations
		ItemRecommender ubRec = recUB.getItemRecommender();
		ItemRecommender ibRec = recIB.getItemRecommender();
		
		//Recommending 10 items to user 42
		List<ScoredId> ubRecs = ubRec.recommend(42, 10);
		List<ScoredId> ibRecs = ibRec.recommend(42, 10);
		
		System.out.println("\nUser-based recomendation list:\n " + ubRecs);
		System.out.println("\nItem-based recomendation list:\n " + ibRecs);
		
		} catch (RecommenderBuildException e) { e.printStackTrace();}
	}
}
