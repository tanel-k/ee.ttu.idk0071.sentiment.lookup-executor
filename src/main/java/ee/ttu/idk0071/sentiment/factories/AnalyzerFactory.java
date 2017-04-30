package ee.ttu.idk0071.sentiment.factories;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import ee.ttu.idk0071.sentiment.lib.analysis.api.SentimentAnalyzer;
import ee.ttu.idk0071.sentiment.lib.analysis.impl.TextProcessingComAnalyzer;
import ee.ttu.idk0071.sentiment.lib.analysis.impl.ViveknSentimentAnalyzer;

@Component
public class AnalyzerFactory {
	private static final List<Class<? extends SentimentAnalyzer>> ANALYZER_CLASS_POOL = new CopyOnWriteArrayList<>();

	static {
		ANALYZER_CLASS_POOL.add(ViveknSentimentAnalyzer.class);
		ANALYZER_CLASS_POOL.add(TextProcessingComAnalyzer.class);
	}

	public static class NoAvailableAnalyzersException extends Exception {
		private static final long serialVersionUID = -1047495033674479734L;
	}

	public SentimentAnalyzer getFirstAvailable() throws NoAvailableAnalyzersException {
		for (Class<? extends SentimentAnalyzer> analyzerClass : ANALYZER_CLASS_POOL) {
			try {
				Constructor<? extends SentimentAnalyzer> constructor = analyzerClass.getConstructor();
				SentimentAnalyzer analyzer = constructor.newInstance();
				if (analyzer.isAvailable()) {
					return analyzer;
				}
			} catch (Throwable t) {
				continue;
			}
			
		}
		
		throw new NoAvailableAnalyzersException();
	}
}
