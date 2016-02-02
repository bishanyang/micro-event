package edu.cmu.ml.rtw.micro.event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.Annotation;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorSentence;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorTokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.AnnotationTypeNLPCat;
import edu.cmu.ml.rtw.micro.event.NarSystem;


public class EventExtractor implements AnnotatorTokenSpan<String> {
	private static final AnnotationType<?>[] REQUIRED_ANNOTATIONS = new AnnotationType<?>[] {
		AnnotationTypeNLP.TOKEN,
		AnnotationTypeNLP.SENTENCE,
		AnnotationTypeNLP.POS,
		AnnotationTypeNLP.LEMMA,
		AnnotationTypeNLP.DEPENDENCY_PARSE,
		AnnotationTypeNLP.CONSTITUENCY_PARSE,
		AnnotationTypeNLP.NER
		//AnnotationTypeNLPCat.NELL_CATEGORY
	};

	public static final AnnotationTypeNLP<String> EVENT_FRAME = new AnnotationTypeNLP<String>("nell-event", String.class, Target.TOKEN_SPAN);

	private static class Singleton {
		private static final EventExtractor INSTANCE = new EventExtractor();
	}

	public static EventExtractor getInstance() {
		return Singleton.INSTANCE;
	}

	private EventExtractor() {
		NarSystem.loadLibrary();
	}

	private native boolean initialize(String configFile);
	private native String  annotate(String inputData);

	@Override
	public String getName() {
		return "cmunell_event-0.0.1";
	}

	@Override
	public AnnotationType<String> produces() {
		return EVENT_FRAME;
	}

	@Override
	public AnnotationType<?>[] requires() {
		return REQUIRED_ANNOTATIONS;
	}

	@Override
	public boolean measuresConfidence() {
		return true;
	}

	@Override
	public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
		try {
			InputStream resource = EventExtractor.class.getResourceAsStream("/event.config");
			BufferedReader bfr = new BufferedReader(new InputStreamReader(resource));
			String line;
			String configStr = "";
			while ((line = bfr.readLine()) != null) {
				configStr += line + "\n";
			}
			bfr.close();
			
			if (!initialize(configStr))
				throw new IllegalStateException("Unable to initialize event tagger.");
			
		} catch (IOException ioe) {
		    throw new RuntimeException(ioe);
		}
		
		StringBuilder inputData = new StringBuilder();
		
		// Read NELL category output
		/*Collection<AnnotationTypeNLP<?>> col = new ArrayList<AnnotationTypeNLP<?>>();
		col.add(AnnotationTypeNLPCat.NELL_CATEGORY);
		List<Annotation> annotations = document.toMicroAnnotation(col).getAllAnnotations();	
		List<Triple<TokenSpan, String, Double>> annoSpans = new ArrayList<Triple<TokenSpan, String, Double>>();
		for (Annotation annotation : annotations) {
			//System.out.println(annotation.getSpanStart() + "\t" + annotation.getSpanEnd() + "\t" + annotation.getStringValue());	
			int startTokenIndex = -1;
			int endTokenIndex = -1;
			int sentenceIndex = -1;
			int sentCount = document.getSentenceCount();
			for (int j = 0; j < sentCount; j++) {
				int tokenCount = document.getSentenceTokenCount(j);
				for (int i = 0; i < tokenCount; i++) {
					if (document.getToken(j, i).getCharSpanStart() == annotation.getSpanStart())
						startTokenIndex = i;
					if (document.getToken(j, i).getCharSpanEnd() == annotation.getSpanEnd()) {
						endTokenIndex = i + 1;
						sentenceIndex = j;
						break;
					}
				}
			}
			if (startTokenIndex >= 0 && endTokenIndex >= 0) {
				annoSpans.add(new Triple<TokenSpan, String, Double>(new TokenSpan(document, sentenceIndex, startTokenIndex, endTokenIndex), 
						annotation.getStringValue(), annotation.getConfidence()));
			}
		}
		inputData.append("#begin NELL\n");
		for (Triple<TokenSpan, String, Double> span : annoSpans) {
	          inputData.append(span.getFirst().toString()+","+span.getSecond()+","+span.getThird().toString()+"\t");
	    }
		inputData.append("#begin NELL\n");
		*/
		
		for (int i = 0; i < document.getSentenceCount(); i++) {
			inputData.append("#begin sentence\n");
			
			List<PoSTag> tags = document.getSentencePoSTags(i);
		    List<String> words = document.getSentenceTokenStrs(i);
		   
		    String parseTree = document.getConstituencyParse(i).toString();
		    String depGraph = document.getDependencyParse(i).toString();
		    
		    StringBuilder sentStr = new StringBuilder();
		    for (String word : words) {
		    	sentStr.append(word).append(" ");
		    }
		    sentStr.setLength(sentStr.length() - 1);
		    inputData.append(sentStr.toString()).append("\n");
		    
		    StringBuilder posTagStr = new StringBuilder();
			for (PoSTag posTag : tags) {
				posTagStr.append(posTag).append(" ");
			}
			posTagStr.setLength(posTagStr.length() - 1);
			inputData.append(posTagStr.toString()).append("\n");
			
			StringBuilder sentLemmaStr = new StringBuilder();
			for (int j = 0; j < words.size(); ++j) {
				String lemmaWord = document.getTokenAnnotation(AnnotationTypeNLP.LEMMA, i, j);
		    	sentLemmaStr.append(lemmaWord).append(" ");
		    }
			sentLemmaStr.setLength(sentLemmaStr.length() - 1);
			inputData.append(sentLemmaStr.toString()).append("\n");
			
			// NER str
			List<Pair<TokenSpan, String>> nerSpans = document.getNer(i);
			for (Pair<TokenSpan, String> span : nerSpans) {
		          if (span.getSecond().toString().equals("O")) continue;
		          inputData.append(Integer.valueOf(span.getFirst().getStartTokenIndex()));
		          inputData.append(","+Integer.valueOf(span.getFirst().getEndTokenIndex()));
		          inputData.append(","+span.getSecond() + " ");
			}
		    inputData.append("\n");
			
			inputData.append(parseTree).append("\n");
			inputData.append(depGraph);
			
			inputData.append("#end sentence\n");
		}
		
		String outputData = annotate(inputData.toString());
		
		List<Triple<TokenSpan, String, Double>> events = new ArrayList<Triple<TokenSpan, String, Double>>();
		
		String[] eventFrames = outputData.split("\n");
		for (int i = 0; i < eventFrames.length; ++i) {
			String[] fields = eventFrames[i].split(",");
			if (fields.length != 5) continue;
	
			int sentIndex = Integer.valueOf(fields[0]);
			int tokenStart = Integer.valueOf(fields[1]);
			int tokenEnd = Integer.valueOf(fields[2]);
			String label = fields[3];
			double score = Double.valueOf(fields[4]);
			
			events.add(new Triple<TokenSpan, String, Double>(new TokenSpan(document, sentIndex, tokenStart, tokenEnd), 
					label, score));
		}
		
		return events;
	}
}
