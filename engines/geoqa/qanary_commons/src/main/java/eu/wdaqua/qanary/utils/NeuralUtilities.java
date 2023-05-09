package eu.wdaqua.qanary.utils;

import java.io.File;
import java.util.Locale;
import com.robrua.nlp.bert.Bert;

public class NeuralUtilities {
    static Bert bert;
    static { // FIXME
//        bert = Bert.load("com/robrua/nlp/easy-bert/bert-uncased-L-12-H-768-A-12");
    }

    public static float computeCosSimilarity(float[] a, float[] b) {
        //todo: you might want to check they are the same size before proceeding

        float dotProduct = 0;
        float normASum = 0;
        float normBSum = 0;

        for(int i = 0; i < a.length; i ++) {
            dotProduct += a[i] * b[i];
            normASum += a[i] * a[i];
            normBSum += b[i] * b[i];
        }

        float eucledianDist = (float) (Math.sqrt(normASum) * Math.sqrt(normBSum));
        return dotProduct / eucledianDist;
    }

    public static float computeCosSimilarity(String a, String b) {
        // FIXME
//        float[] emb1 = bert.embedSequence(a.toLowerCase(Locale.ROOT));
//        float[] emb2 = bert.embedSequence(b.toLowerCase(Locale.ROOT));
//        return NeuralUtilities.computeCosSimilarity(emb1, emb2);
        return 0;
    }
}
