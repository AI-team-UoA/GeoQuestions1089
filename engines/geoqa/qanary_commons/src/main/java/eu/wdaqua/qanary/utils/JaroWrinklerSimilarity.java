package eu.wdaqua.qanary.utils;

import info.debatty.java.stringsimilarity.JaroWinkler;

public class JaroWrinklerSimilarity implements Similarity {
    JaroWinkler jw = new JaroWinkler();
    @Override
    public double computeSimilarity(String a, String b) {
        return jw.similarity(a, b);
    }
}
