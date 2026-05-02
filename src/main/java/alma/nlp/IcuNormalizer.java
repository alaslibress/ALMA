package alma.nlp;

import com.ibm.icu.text.Normalizer2;
import dev.langchain4j.agent.tool.Tool;

public final class IcuNormalizer {

    private static final Normalizer2 NFC = Normalizer2.getNFCInstance();

    @Tool("Normalizes Unicode to NFC form. Useful before comparing or hashing strings with diacritics.")
    public String normalizeUnicode(String text) {
        if (text == null) {
            return "";
        }
        return NFC.normalize(text);
    }
}
