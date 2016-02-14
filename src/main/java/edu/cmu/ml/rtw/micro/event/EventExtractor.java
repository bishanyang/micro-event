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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

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

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

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
	
	private void outputBratAnno(DocumentNLP document, List<AceEvent> aceEvents, 
			HashMap<String, AceEntity> aceEntities, String outputPath) {
		try {
			File txtfile = new File(outputPath + document.getName().replace(' ', '_') + ".txt");
			//System.out.println("Event: "+outputPath + document.getName());
			FileWriter txtfw = new FileWriter(txtfile.getAbsoluteFile());
			BufferedWriter txtbw = new BufferedWriter(txtfw);
			txtbw.write(document.getOriginalText());
			txtbw.close();
			txtfw.close();
			
			File annfile = new File(outputPath + document.getName().replace(' ', '_') + ".ann");
			FileWriter annfw = new FileWriter(annfile.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(annfw);
			
			for (Map.Entry<String,AceEntity> entry : aceEntities.entrySet()) {
			    String key = entry.getKey();
			    AceEntity en = entry.getValue();
			    bw.write(en.toString() + "\n");
			}
			
			for (AceEvent event : aceEvents) {
				bw.write(event.toTriggerString() + "\n");
				bw.write(event.toEventString() + "\n");
			}
			
			bw.close();
			annfw.close();
			
		} catch (IOException ioe) {
		    throw new RuntimeException(ioe);
		}
	}

	@Override
	public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
		String bratPath = "";
		try {
			InputStream resource = EventExtractor.class.getResourceAsStream("/event.config");
			BufferedReader bfr = new BufferedReader(new InputStreamReader(resource));
			String line;
			String configStr = "";
			while ((line = bfr.readLine()) != null) {
				configStr += line + "\n";
				String[] fields = line.split("=");
				if (fields.length > 1 && fields[0].equals("bratpath")) {
					bratPath = fields[1];
				}
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
		    inputData.append("Sent: ").append(sentStr.toString()).append("\n");
		    
		    StringBuilder posTagStr = new StringBuilder();
			for (PoSTag posTag : tags) {
				posTagStr.append(posTag).append(" ");
			}
			posTagStr.setLength(posTagStr.length() - 1);
			inputData.append("POS: ").append(posTagStr.toString()).append("\n");
			
			StringBuilder sentLemmaStr = new StringBuilder();
			for (int j = 0; j < words.size(); ++j) {
				String lemmaWord = document.getTokenAnnotation(AnnotationTypeNLP.LEMMA, i, j);
		    	sentLemmaStr.append(lemmaWord).append(" ");
		    }
			sentLemmaStr.setLength(sentLemmaStr.length() - 1);
			inputData.append("Lemma: ").append(sentLemmaStr.toString()).append("\n");
			
			// NER str
			inputData.append("NER: ");
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
		
		/*try {
			File txtfile = new File(bratPath + document.getName().replace(' ', '_') + ".orig.txt");
			//System.out.println("Event: "+outputPath + document.getName());
			FileWriter txtfw = new FileWriter(txtfile.getAbsoluteFile());
			BufferedWriter txtbw = new BufferedWriter(txtfw);
			//txtbw.write(inputData.toString());
			txtbw.write(document.getOriginalText());
			txtbw.close();
			txtfw.close();
			
			txtfile = new File(bratPath + document.getName().replace(' ', '_') + ".txt");
			//System.out.println("Event: "+outputPath + document.getName());
			txtfw = new FileWriter(txtfile.getAbsoluteFile());
			txtbw = new BufferedWriter(txtfw);
			//txtbw.write(inputData.toString());
			txtbw.write(document.getText());
			txtbw.close();
			txtfw.close();
		} catch (IOException ioe) {
		    throw new RuntimeException(ioe);
		}
		*/
		
		String outputData = annotate(inputData.toString());
		
		List<Triple<TokenSpan, String, Double>> events = new ArrayList<Triple<TokenSpan, String, Double>>();
		
		int sentIndex = 0;
		int tokenStart = 0;
		int tokenEnd = 0;
		double score = 0.0;
		
		List<AceEvent> aceEvents = new ArrayList<AceEvent>();
		HashMap<String, AceEntity> aceEntities = new HashMap<String, AceEntity>();
		
		int entityId = 1;
		int eventId = 1;
		String[] eventFrames = outputData.split("\n");
		for (int i = 0; i < eventFrames.length; ++i) {
			String[] fields = eventFrames[i].split("\t");
			if (fields.length == 0) continue;
	
			String[] subfields = fields[0].split(",");
			if (subfields.length != 5) continue;
			
			// Read event trigger
			sentIndex = Integer.valueOf(subfields[0]);
			tokenStart = Integer.valueOf(subfields[1]);
			tokenEnd = Integer.valueOf(subfields[2]);
			String eventType = subfields[3];
			score = Double.valueOf(subfields[4]);
			TokenSpan eventTrigger = new TokenSpan(document, sentIndex, tokenStart, tokenEnd);
			
			//System.out.println(document.getName() + " " + sentIndex + " " + document.getSentenceTokenCount(sentIndex)+" "+ tokenStart + " " + tokenEnd);
			//System.out.println(document.getSentence(sentIndex));
			
			String eventLabel = eventType + "=" + eventTrigger.toString();
			
			AceEvent eventInst = new AceEvent();
			eventInst.triggerId = "T" + Integer.toString(entityId);
			entityId += 1;
			eventInst.eventId = "E" + Integer.toString(eventId);
			eventId += 1;
			eventInst.eventType = eventType;
			eventInst.charSpan.setFirst(document.getToken(sentIndex, tokenStart).getCharSpanStart());
			eventInst.charSpan.setSecond(document.getToken(sentIndex, tokenEnd-1).getCharSpanEnd());
			eventInst.spanStr = eventTrigger.toString();
			
			// Read event arguments
			for (int j = 1; j < fields.length; ++j) {
				String[] splits = fields[j].split(",");
				if (splits.length != 6) continue;
				
				sentIndex = Integer.valueOf(splits[0]);
				tokenStart = Integer.valueOf(splits[1]);
				tokenEnd = Integer.valueOf(splits[2]);
				String roleType = splits[3];
				String nerType = splits[4];
				TokenSpan arg = new TokenSpan(document, sentIndex, tokenStart, tokenEnd);
				eventLabel += "; " + roleType + "=" + arg.toString();
				
				String key = Integer.toString(sentIndex) + "#" + Integer.toString(tokenStart) + "#" + Integer.toString(tokenEnd);
				String enID = "";
				if (!aceEntities.containsKey(key)) {
					AceEntity entityInst = new AceEntity();
					entityInst.entityId = "T" + Integer.toString(entityId);
					entityId += 1;
					
					entityInst.entityType = nerType;
					entityInst.charSpan.setFirst(document.getToken(sentIndex, tokenStart).getCharSpanStart());
					entityInst.charSpan.setSecond(document.getToken(sentIndex, tokenEnd-1).getCharSpanEnd());
					entityInst.spanStr = arg.toString();
					aceEntities.put(key, entityInst);
					
					enID = entityInst.entityId;
				} else {
					enID = aceEntities.get(key).entityId;
				}
				
				eventInst.AddArgument(enID, roleType);
			}
			
			events.add(new Triple<TokenSpan, String, Double>(eventTrigger, eventLabel, score));
			
			aceEvents.add(eventInst);
		}
		
		// output brat annotations
		if (bratPath != "" && aceEvents.size() > 0)
			outputBratAnno(document, aceEvents, aceEntities, bratPath);
		
		return events;
	}
}
