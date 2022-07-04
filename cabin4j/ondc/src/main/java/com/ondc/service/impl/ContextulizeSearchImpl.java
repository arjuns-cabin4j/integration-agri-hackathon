package com.ondc.service.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cabin4j.suite.platform.services.GlobalPropertiesService;
import com.google.cloud.translate.v3.DetectLanguageRequest;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.Translation;
import com.ondc.factory.POSFactory;
import com.ondc.factory.SearchTranslatorFactory;
import com.ondc.service.ContextulizeSearch;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.WhitespaceTokenizer;

@Service
public class ContextulizeSearchImpl implements ContextulizeSearch {

	private static final String EN_LANG = "en";

	private static final String AGRO = "AGRO";

	private static final String NOUN_PROPER = "NNP";

	private static final String NOUN_SINGULAR = "NNS";

	private static final String NOUN = "NN";

	private static final String TEXT_PLAIN = "text/plain";

	/*
	 * codes for
	 * hindi,marathi,bengali,gujarati,kannada,malay,malayalam,marathi,tamil,telugu
	 * and urdu.
	 */

	private static final List<String> langs = List.of("hi", "mr", "bn", "gu", "kn", "ms", "ml", "mr", "ta", "te", "ur");

	private final Logger LOG = LoggerFactory.getLogger(ContextulizeSearchImpl.class);

	@Autowired
	private GlobalPropertiesService globalPropertiesService;

	@Autowired
	private SearchTranslatorFactory searchTranslatorFactory;

	@Autowired
	private POSFactory posFactory;

	@Override
	public String detectLang(String text) throws IOException {

		DetectLanguageRequest request = DetectLanguageRequest.newBuilder()
				.setParent(searchTranslatorFactory.getLocationName().toString()).setMimeType(TEXT_PLAIN)
				.setContent(text).build();

		return searchTranslatorFactory.getTranslationServiceClient().detectLanguage(request).getLanguages(0)
				.getLanguageCode();

	}

	@Override
	public String translate(String text, String source, String target) throws IOException {

		TranslateTextRequest translate = TranslateTextRequest.newBuilder()
				.setParent(searchTranslatorFactory.getLocationName().toString()).setMimeType(TEXT_PLAIN)
				.setSourceLanguageCode(source).setTargetLanguageCode(target).addContents(text).build();
		StringBuilder result = new StringBuilder();
		for (Translation translation : searchTranslatorFactory.getTranslationServiceClient().translateText(translate)
				.getTranslationsList()) {
			result.append(translation.getTranslatedText());

		}

		return result.toString();

	}

	@Override
	public String getContext(String text) throws IOException {

		WhitespaceTokenizer whitespaceTokenizer = WhitespaceTokenizer.INSTANCE;
		String[] tokens = whitespaceTokenizer.tokenize(text);
		List<POSTaggerME> posTaggers = posFactory.getPosTaggers();
		String[][] tags = new String[posTaggers.size()][tokens.length];
		for (int i = 0; i < posTaggers.size(); i++) {
			tags[i] = posTaggers.get(i).tag(tokens);
		}

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < posTaggers.size(); i++) {
			for (int j = 0; j < tokens.length; j++) {
				if (tags[i][j].equalsIgnoreCase(NOUN) || tags[i][j].equalsIgnoreCase(NOUN_PROPER)
						|| tags[i][j].equalsIgnoreCase(AGRO) || tags[i][j].equalsIgnoreCase(NOUN_SINGULAR)) {
					result.append(tokens[j]).append(StringUtils.SPACE);
				}
			}
		}
		LOG.info("Tokes = '{}'", Arrays.toString(tokens));

		for (String[] s : tags) {
			LOG.info("Tags = '{}'", Arrays.toString(s));
		}

		return result.toString();

	}

	@Override
	public String contextulizedSearch(String text, boolean enableContext) {
		try {
			String srcLang = detectLang(text);
			String result;
			if (langs.contains(srcLang)) {
				if (text.chars().anyMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.DEVANAGARI)) {
					result = translate(text, srcLang, EN_LANG);
				} else {
					result = translate(translate(text, EN_LANG, srcLang), srcLang, EN_LANG);
				}

			} else {
				if (!srcLang.equals(EN_LANG)) {
					LOG.error("Unsupported language '{}' detected parsing query as it is", srcLang);
				}
				result = text;
			}
			if (enableContext)
				return normalizeText(getContext(result));
			else
				return normalizeText(result);
		} catch (Exception e) {
			LOG.error("Exception occurred while performning Contextulized Search for '{}'", text, e);
		}
		return StringUtils.EMPTY;
	}

	protected POSModel getModel() throws FileNotFoundException, IOException {
		try (InputStream modelIn = new FileInputStream(
				globalPropertiesService.getPropertyFromCache("search.nlp.model.path", null))) {
			POSModel model = new POSModel(modelIn);
			return model;
		}
	}

	@Override
	public String normalizeText(String search) {
		Set<String> text = new HashSet<>();
		outer: for (String s : search.split(" ")) {
			for (String i : globalPropertiesService.getPropertyFromCache("nlp.context.ignore", null).split(",")) {
				if (i.equalsIgnoreCase(s))
					continue outer;
			}
			text.add(s);
		}
		StringBuilder sb = new StringBuilder();
		for (String s : text) {
			sb.append(s).append(StringUtils.SPACE);
		}
		return sb.toString().trim();
	}

}
