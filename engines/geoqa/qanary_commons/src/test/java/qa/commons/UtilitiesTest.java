package qa.commons;

import eu.wdaqua.qanary.utils.CoreNLPUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertEquals;

public class UtilitiesTest {
    private Logger logger = LoggerFactory.getLogger(RestTemplateCacheLiveTest.class);
    @Test
    public void testWordCount() {
        assertEquals (0, CoreNLPUtilities.wordcount(" "));
        assertEquals (4, CoreNLPUtilities.wordcount("My name is Sergios"));
        assertEquals (4, CoreNLPUtilities.wordcount(" My name	 is     Sergios"));
    }
}
