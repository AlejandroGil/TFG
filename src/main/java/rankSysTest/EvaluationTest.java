/* 
 * Copyright (C) 2015 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package rankSysTest;

import static org.ranksys.formats.parsing.Parsers.lp;

import java.util.HashMap;
import java.util.Map;

import org.ranksys.formats.preference.SimpleRatingPreferencesReader;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

import es.uam.eps.ir.ranksys.core.preference.ConcatPreferenceData;
import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.core.preference.SimplePreferenceData;
import es.uam.eps.ir.ranksys.diversity.sales.metrics.AggregateDiversityMetric;
import es.uam.eps.ir.ranksys.diversity.sales.metrics.GiniIndex;
import es.uam.eps.ir.ranksys.metrics.RecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.NDCG;
import es.uam.eps.ir.ranksys.metrics.basic.Precision;
import es.uam.eps.ir.ranksys.metrics.basic.Recall;
import es.uam.eps.ir.ranksys.metrics.rel.BinaryRelevanceModel;
import es.uam.eps.ir.ranksys.metrics.rel.NoRelevanceModel;

/**
 * Example main of metrics.
 *
 * @author Sa�l Vargas (saul.vargas@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EvaluationTest {

    public static void main(String[] args) throws Exception {
        String trainDataPath = "src/main/resources/ml-100k/u5.base";
        String testDataPath = "src/main/resources/ml-100k/u5.test";
        String recIn = "ub";
        Double threshold = 0.0;

        // USER - ITEM - RATING files for train and test
        PreferenceData<Long, Long> trainData = SimplePreferenceData.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp));
        PreferenceData<Long, Long> testData = SimplePreferenceData.load(SimpleRatingPreferencesReader.get().read(testDataPath, lp, lp));
        PreferenceData<Long, Long> totalData = new ConcatPreferenceData<>(trainData, testData);
        // EVALUATED AT CUTOFF 10
        int cutoff = 10;
        // BINARY RELEVANCE
        BinaryRelevanceModel<Long, Long> binRel = new BinaryRelevanceModel<>(false, testData, threshold);
        // NO RELEVANCE
        NoRelevanceModel<Long, Long> norel = new NoRelevanceModel<>();

        Map<String, SystemMetric<Long, Long>> sysMetrics = new HashMap<>();

        ////////////////////////
        // INDIVIDUAL METRICS //
        ////////////////////////
        Map<String, RecommendationMetric<Long, Long>> recMetrics = new HashMap<>();

        // PRECISION
        recMetrics.put("prec", new Precision<>(cutoff, binRel));
        // RECALL
        recMetrics.put("recall", new Recall<>(cutoff, binRel));
        // nDCG
        recMetrics.put("ndcg", new NDCG<>(cutoff, new NDCG.NDCGRelevanceModel<>(false, testData, threshold)));

        // AVERAGE VALUES OF RECOMMENDATION METRICS FOR ITEMS IN TEST
        int numUsers = testData.numUsersWithPreferences();
        recMetrics.forEach((name, metric) -> sysMetrics.put(name, new AverageRecommendationMetric<>(metric, numUsers)));

        ////////////////////
        // SYSTEM METRICS //
        ////////////////////
        sysMetrics.put("aggrdiv", new AggregateDiversityMetric<>(cutoff, norel));
        int numItems = totalData.numItemsWithPreferences();
        sysMetrics.put("gini", new GiniIndex<>(cutoff, numItems));

        RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(lp, lp);

        format.getReader(recIn).readAll().forEach(rec -> sysMetrics.values().forEach(metric -> metric.add(rec)));

        sysMetrics.forEach((name, metric) -> System.out.println(name + "\t" + metric.evaluate()));
    }
}