package ee.ttu.idk0071.sentiment.factories;

import org.springframework.stereotype.Component;

import ee.ttu.idk0071.sentiment.lib.analysis.api.SentimentAnalyzer;
import ee.ttu.idk0071.sentiment.lib.analysis.impl.ViveknSentimentAnalyzer;

@Component
public class AnalyzerFactory {
	public SentimentAnalyzer getAnalyzer() {
		return new ViveknSentimentAnalyzer();
	}
}
