package eu.wdaqua.qanary.utils;

public class BertEmbeddingSimilarity implements Similarity {
    @Override
    public double computeSimilarity(String a, String b) {
        return NeuralUtilities.computeCosSimilarity(a, b);
    }
}
