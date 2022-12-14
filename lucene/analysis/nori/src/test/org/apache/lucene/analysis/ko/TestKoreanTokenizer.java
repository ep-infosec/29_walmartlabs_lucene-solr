/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.analysis.ko;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockGraphTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ko.KoreanTokenizer.DecompoundMode;
import org.apache.lucene.analysis.ko.dict.UserDictionary;
import org.apache.lucene.analysis.ko.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.ko.tokenattributes.ReadingAttribute;

public class TestKoreanTokenizer extends BaseTokenStreamTestCase {
  private Analyzer analyzer, analyzerWithPunctuation, analyzerUnigram, analyzerDecompound, analyzerDecompoundKeep, analyzerReading;

  public static UserDictionary readDict() {
    InputStream is = TestKoreanTokenizer.class.getResourceAsStream("userdict.txt");
    if (is == null) {
      throw new RuntimeException("Cannot find userdict.txt in test classpath!");
    }
    try {
      try {
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        return UserDictionary.open(reader);
      } finally {
        is.close();
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    UserDictionary userDictionary = readDict();
    analyzer = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new KoreanTokenizer(newAttributeFactory(), userDictionary,
            DecompoundMode.NONE, false);
        return new TokenStreamComponents(tokenizer, tokenizer);
      }
    };
    analyzerWithPunctuation = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new KoreanTokenizer(newAttributeFactory(), userDictionary,
            DecompoundMode.NONE, false, false);
        return new TokenStreamComponents(tokenizer, tokenizer);
      }
    };
    analyzerUnigram = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new KoreanTokenizer(newAttributeFactory(), userDictionary,
            DecompoundMode.NONE, true);
        return new TokenStreamComponents(tokenizer, tokenizer);
      }
    };
    analyzerDecompound = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new KoreanTokenizer(newAttributeFactory(), userDictionary,
            DecompoundMode.DISCARD, false);
        return new TokenStreamComponents(tokenizer);
      }
    };
    analyzerDecompoundKeep = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new KoreanTokenizer(newAttributeFactory(), userDictionary,
            DecompoundMode.MIXED, false);
        return new TokenStreamComponents(tokenizer);
      }
    };
    analyzerReading = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new KoreanTokenizer(newAttributeFactory(), userDictionary,
            DecompoundMode.NONE, false);
        KoreanReadingFormFilter filter = new KoreanReadingFormFilter(tokenizer);
        return new TokenStreamComponents(tokenizer, filter);
      }
    };
  }

  public void testSpaces() throws IOException {
    assertAnalyzesTo(analyzer, "??????        ?????????         ???",
        new String[]{"??????", "??????", "???", "???"},
        new int[]{0, 10, 12, 22},
        new int[]{2, 12, 13, 23},
        new int[]{1, 1, 1, 1}
    );
    assertPartsOfSpeech(analyzer, "?????? ?????????         ???",
        new POS.Type[] { POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME },
        new POS.Tag[] { POS.Tag.NNG, POS.Tag.NNG, POS.Tag.J, POS.Tag.NNB },
        new POS.Tag[] { POS.Tag.NNG, POS.Tag.NNG, POS.Tag.J, POS.Tag.NNB }
    );
  }

  public void testPartOfSpeechs() throws IOException {
    assertAnalyzesTo(analyzer, "?????? ????????? ???",
        new String[]{"??????", "??????", "???", "???"},
        new int[]{0, 3, 5, 7},
        new int[]{2, 5, 6, 8},
        new int[]{1, 1, 1, 1}
    );
    assertPartsOfSpeech(analyzer, "?????? ????????? ???",
        new POS.Type[] { POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME },
        new POS.Tag[] { POS.Tag.NNG, POS.Tag.NNG, POS.Tag.J, POS.Tag.NNB },
        new POS.Tag[] { POS.Tag.NNG, POS.Tag.NNG, POS.Tag.J, POS.Tag.NNB }
    );
  }

  public void testPartOfSpeechsWithPunc() throws IOException {
    assertAnalyzesTo(analyzerWithPunctuation, "?????? ????????? ???!",
        new String[]{"??????", " ", "??????", "???", " ", "???", "!"},
        new int[]{0, 2, 3, 5, 6, 7, 8, 9},
        new int[]{2, 3, 5, 6, 7, 8, 9, 11},
        new int[]{1, 1, 1, 1, 1, 1, 1, 1}
    );
    assertPartsOfSpeech(analyzerWithPunctuation, "?????? ????????? ???!",
        new POS.Type[] { POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME },
        new POS.Tag[] { POS.Tag.NNG, POS.Tag.SP, POS.Tag.NNG, POS.Tag.J, POS.Tag.SP, POS.Tag.NNB, POS.Tag.SF },
        new POS.Tag[] { POS.Tag.NNG, POS.Tag.SP, POS.Tag.NNG, POS.Tag.J, POS.Tag.SP, POS.Tag.NNB, POS.Tag.SF }
    );
  }

  public void testFloatingPointNumber() throws IOException {
    assertAnalyzesTo(analyzerWithPunctuation, "10.1 ?????? ?????????",
        new String[]{"10", ".", "1", " ", "??????", " ", "?????????"},
        new int[]{0, 2, 3, 4, 5, 7, 8},
        new int[]{2, 3, 4, 5, 7, 8, 11},
        new int[]{1, 1, 1, 1, 1, 1, 1}
    );

    assertAnalyzesTo(analyzer, "10.1 ?????? ?????????",
        new String[]{"10", "1", "??????", "?????????"},
        new int[]{0, 3, 5, 8},
        new int[]{2, 4, 7, 11},
        new int[]{1, 1, 1, 1}
    );
  }

  public void testPartOfSpeechsWithCompound() throws IOException {
    assertAnalyzesTo(analyzer, "?????????????????? ??????, ??????, ??????",
        new String[]{"???????????????", "???", "??????", "??????", "??????"},
        new int[]{0, 5, 7, 11, 15},
        new int[]{5, 6, 9, 13, 17},
        new int[]{1, 1, 1, 1, 1}
    );

    assertPartsOfSpeech(analyzer,"?????????????????? ??????, ??????, ??????",
        new POS.Type[]{POS.Type.COMPOUND, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.NNG, POS.Tag.J, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNP},
        new POS.Tag[]{POS.Tag.NNG, POS.Tag.J, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNP}
    );

    assertAnalyzesTo(analyzerDecompound, "?????????????????? ??????, ??????, ??????",
        new String[]{"?????????", "??????", "???", "??????", "??????", "??????"},
        new int[]{0, 3, 5, 7, 11, 15},
        new int[]{3, 5, 6, 9, 13, 17},
        new int[]{1, 1, 1, 1, 1, 1}
    );

    assertAnalyzesTo(analyzerDecompoundKeep, "?????????????????? ??????, ??????, ??????",
        new String[]{"???????????????", "?????????", "??????", "???", "??????", "??????", "??????"},
        new int[]{0, 0, 3, 5, 7, 11, 15},
        new int[]{5, 3, 5, 6, 9, 13, 17},
        null,
        new int[]{1, 0, 1, 1, 1, 1, 1},
        new int[]{2, 1, 1, 1, 1, 1, 1}
    );

    assertPartsOfSpeech(analyzerDecompound,"?????????????????? ??????, ??????, ??????",
        new POS.Type[]{POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.NNG, POS.Tag.NNG, POS.Tag.J, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNP},
        new POS.Tag[]{POS.Tag.NNG, POS.Tag.NNG, POS.Tag.J, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNP}
    );

    assertPartsOfSpeech(analyzerDecompoundKeep,"?????????????????? ??????, ??????, ??????",
        new POS.Type[]{POS.Type.COMPOUND, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.NNG, POS.Tag.NNG, POS.Tag.NNG, POS.Tag.J, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNP},
        new POS.Tag[]{POS.Tag.NNG, POS.Tag.NNG, POS.Tag.NNG, POS.Tag.J, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNP}
    );
  }

  public void testPartOfSpeechsWithInflects() throws IOException {
    assertAnalyzesTo(analyzer, "?????????",
        new String[]{"?????????"},
        new int[]{0},
        new int[]{3},
        new int[]{1}
    );

    assertPartsOfSpeech(analyzer, "?????????",
        new POS.Type[]{POS.Type.INFLECT},
        new POS.Tag[]{POS.Tag.VV},
        new POS.Tag[]{POS.Tag.E}
    );

    assertAnalyzesTo(analyzerDecompound, "?????????",
        new String[]{"?????????", "???"},
        new int[]{0, 0},
        new int[]{3, 3},
        new int[]{1, 1}
    );

    assertAnalyzesTo(analyzerDecompoundKeep, "?????????",
        new String[]{"?????????", "?????????", "???"},
        new int[]{0, 0, 0},
        new int[]{3, 3, 3},
        null,
        new int[]{1, 0, 1},
        new int[]{2, 1, 1}
    );

    assertPartsOfSpeech(analyzerDecompound, "?????????",
        new POS.Type[]{POS.Type.MORPHEME, POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.VV, POS.Tag.E},
        new POS.Tag[]{POS.Tag.VV, POS.Tag.E}
    );

    assertPartsOfSpeech(analyzerDecompoundKeep, "?????????",
        new POS.Type[]{POS.Type.INFLECT, POS.Type.MORPHEME, POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.VV, POS.Tag.VV, POS.Tag.E},
        new POS.Tag[]{POS.Tag.E, POS.Tag.VV, POS.Tag.E}
    );
  }

  public void testUnknownWord() throws IOException {
    assertAnalyzesTo(analyzer,"2018 ?????? ?????????????????????",
        new String[]{"2018", "??????", "??????", "?????????", "??????"},
        new int[]{0, 5, 8, 10, 13},
        new int[]{4, 7, 10, 13, 15},
        new int[]{1, 1, 1, 1, 1});

    assertPartsOfSpeech(analyzer,"2018 ?????? ?????????????????????",
        new POS.Type[]{POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.SN, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNG},
        new POS.Tag[]{POS.Tag.SN, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNG}
    );

    assertAnalyzesTo(analyzerUnigram,"2018 ?????? ?????????????????????",
        new String[]{"2", "0", "1", "8", "??????", "??????", "?????????", "??????"},
        new int[]{0, 1, 2, 3, 5, 8, 10, 13},
        new int[]{1, 2, 3, 4, 7, 10, 13, 15},
        new int[]{1, 1, 1, 1, 1, 1, 1, 1});

    assertPartsOfSpeech(analyzerUnigram,"2018 ?????? ?????????????????????",
        new POS.Type[]{POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME,},
        new POS.Tag[]{POS.Tag.SY, POS.Tag.SY, POS.Tag.SY, POS.Tag.SY, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNG},
        new POS.Tag[]{POS.Tag.SY, POS.Tag.SY, POS.Tag.SY, POS.Tag.SY, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNP, POS.Tag.NNG}
    );
  }

  public void testReading() throws IOException {
    assertReadings(analyzer, "????????????", "????????????");
    assertReadings(analyzer, "????????????", "????????????");
    assertReadings(analyzer, "?????????", new String[] {null});
    assertAnalyzesTo(analyzerReading,"????????????",
        new String[]{"????????????"},
        new int[]{0},
        new int[]{4},
        new int[]{1});
    assertAnalyzesTo(analyzerReading,"????????????",
        new String[]{"????????????"},
        new int[]{0},
        new int[]{4},
        new int[]{1});
    assertAnalyzesTo(analyzerReading,"?????????",
        new String[]{"?????????"},
        new int[]{0},
        new int[]{3},
        new int[]{1});
  }

  public void testUserDict() throws IOException {
    assertAnalyzesTo(analyzer, "c++ ??????????????? ??????",
        new String[]{"c++", "???????????????", "??????"},
        new int[]{0, 4, 10},
        new int[]{3, 9, 12},
        new int[]{1, 1, 1}
    );

    assertPartsOfSpeech(analyzer, "c++ ??????????????? ??????",
        new POS.Type[]{POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.NNG, POS.Tag.NNG, POS.Tag.NNG},
        new POS.Tag[]{POS.Tag.NNG, POS.Tag.NNG, POS.Tag.NNG}
    );

    assertAnalyzesTo(analyzerDecompound, "??????????????????",
        new String[]{"??????", "??????", "??????"},
        new int[]{0, 2, 4},
        new int[]{2, 4, 6},
        new int[]{1, 1, 1}
    );

    assertPartsOfSpeech(analyzerDecompound, "??????????????????",
        new POS.Type[]{POS.Type.MORPHEME, POS.Type.MORPHEME, POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.NNG, POS.Tag.NNG, POS.Tag.NNG},
        new POS.Tag[]{POS.Tag.NNG, POS.Tag.NNG, POS.Tag.NNG}
    );

    assertAnalyzesTo(analyzer, "??????????????????",
        new String[]{"??????????????????"},
        new int[]{0},
        new int[]{6},
        new int[]{1}
    );

    assertAnalyzesTo(analyzer, "21??????????????????",
        new String[]{"21??????????????????"},
        new int[]{0},
        new int[]{8},
        new int[]{1}
    );
  }

  public void testInterpunct() throws IOException {
    assertAnalyzesTo(analyzer, "????????????????????????????????????????????????????????????????????????",
        new String[]{"??????", "??????", "?????????", "??????", "??????", "??????", "??????", "??????", "??????"},
        new int[]{0, 3, 6, 10, 12, 15, 18, 20, 22},
        new int[]{2, 5, 9, 12, 14, 17, 20, 22, 24},
        new int[]{1, 1, 1, 1,   1,  1,  1,  1,  1}
    );
  }

  /** blast some random strings through the tokenizer */
  public void testRandomStrings() throws Exception {
    checkRandomData(random(), analyzer, 500*RANDOM_MULTIPLIER);
    checkRandomData(random(), analyzerUnigram, 500*RANDOM_MULTIPLIER);
    checkRandomData(random(), analyzerDecompound, 500*RANDOM_MULTIPLIER);
  }

  /** blast some random large strings through the tokenizer */
  public void testRandomHugeStrings() throws Exception {
    Random random = random();
    checkRandomData(random, analyzer, 20*RANDOM_MULTIPLIER, 8192);
    checkRandomData(random, analyzerUnigram, 20*RANDOM_MULTIPLIER, 8192);
    checkRandomData(random, analyzerDecompound, 20*RANDOM_MULTIPLIER, 8192);
  }

  public void testRandomHugeStringsMockGraphAfter() throws Exception {
    // Randomly inject graph tokens after KoreanTokenizer:
    Random random = random();
    Analyzer analyzer = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new KoreanTokenizer(newAttributeFactory(), null, DecompoundMode.MIXED, false);
        TokenStream graph = new MockGraphTokenFilter(random(), tokenizer);
        return new TokenStreamComponents(tokenizer, graph);
      }
    };
    checkRandomData(random, analyzer, 20*RANDOM_MULTIPLIER, 8192);
    analyzer.close();
  }

  public void testCombining() throws IOException {
    assertAnalyzesTo(analyzer, "?????????????????? ??????????",
        new String[]{"??????????????????", "??????????"},
        new int[]{0, 10},
        new int[]{9, 15},
        new int[]{1, 1}
    );
    assertPartsOfSpeech(analyzer, "?????????????????? ??????????",
        new POS.Type[]{POS.Type.MORPHEME, POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.SL, POS.Tag.SL},
        new POS.Tag[]{POS.Tag.SL, POS.Tag.SL}
    );

    assertAnalyzesTo(analyzer, "ka??k??t??????a??k??",
        new String[]{"ka??k??t??????a??k??"},
        new int[]{0},
        new int[]{13},
        new int[]{1}
    );
    assertPartsOfSpeech(analyzer, "ka??k??t??????a??k??",
        new POS.Type[]{POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.SL},
        new POS.Tag[]{POS.Tag.SL}
    );

    assertAnalyzesTo(analyzer, "?????????",
        new String[]{"?????????"},
        new int[]{0},
        new int[]{4},
        new int[]{1}
    );
    assertPartsOfSpeech(analyzer, "?????????",
        new POS.Type[]{POS.Type.MORPHEME},
        new POS.Tag[]{POS.Tag.SL},
        new POS.Tag[]{POS.Tag.SL}
    );
  }

  private void assertReadings(Analyzer analyzer, String input, String... readings) throws IOException {
    try (TokenStream ts = analyzer.tokenStream("ignored", input)) {
      ReadingAttribute readingAtt = ts.addAttribute(ReadingAttribute.class);
      ts.reset();
      for(String reading : readings) {
        assertTrue(ts.incrementToken());
        assertEquals(reading, readingAtt.getReading());
      }
      assertFalse(ts.incrementToken());
      ts.end();
    }
  }

  private void assertPartsOfSpeech(Analyzer analyzer, String input, POS.Type[] posTypes, POS.Tag[] leftPosTags, POS.Tag[] rightPosTags) throws IOException {
    assert posTypes.length == leftPosTags.length && posTypes.length == rightPosTags.length;
    try (TokenStream ts = analyzer.tokenStream("ignored", input)) {
      PartOfSpeechAttribute partOfSpeechAtt = ts.addAttribute(PartOfSpeechAttribute.class);
      ts.reset();
      for (int i = 0; i < posTypes.length; i++) {
        POS.Type posType = posTypes[i];
        POS.Tag leftTag = leftPosTags[i];
        POS.Tag rightTag = rightPosTags[i];
        assertTrue(ts.incrementToken());
        assertEquals(posType, partOfSpeechAtt.getPOSType());
        assertEquals(leftTag, partOfSpeechAtt.getLeftPOS());
        assertEquals(rightTag, partOfSpeechAtt.getRightPOS());
      }
      assertFalse(ts.incrementToken());
      ts.end();
    }
  }

}
