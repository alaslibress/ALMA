package alma.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class JsonExtractorTest {

    @Test
    void leavesCleanJsonAlone() {
        String raw = "{\"a\":1}";
        assertThat(JsonExtractor.clean(raw)).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripsJsonFence() {
        String raw = "```json\n{\"a\":1}\n```";
        assertThat(JsonExtractor.clean(raw)).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripsPlainFence() {
        String raw = "```\n{\"a\":1}\n```";
        assertThat(JsonExtractor.clean(raw)).isEqualTo("{\"a\":1}");
    }

    @Test
    void slicesProseWrapped() {
        String raw = "Sure, here you go: {\"a\":1} cheers!";
        assertThat(JsonExtractor.clean(raw)).isEqualTo("{\"a\":1}");
    }

    @Test
    void slicesFencedAndProseWrapped() {
        String raw = "Sure thing:\n```json\n{\n  \"a\": 1\n}\n```\nLet me know.";
        String result = JsonExtractor.clean(raw);
        assertThat(result).startsWith("{");
        assertThat(result).endsWith("}");
        assertThat(result).contains("\"a\": 1");
    }

    @Test
    void handlesNullInput() {
        assertThat(JsonExtractor.clean(null)).isEmpty();
    }

    @Test
    void cleanReturnsRawStrippedWhenBracesMissing() {
        assertThat(JsonExtractor.clean("hello world")).isEqualTo("hello world");
    }

    @Test
    void cleanHandlesUnclosedFenceGracefully() {
        String raw = "```json\n{\"a\":1}";
        String result = JsonExtractor.clean(raw);
        assertThat(result).isEqualTo("{\"a\":1}");
    }

    @Test
    void parseOrThrowParsesFencedRecord() throws JsonProcessingException {
        String raw = "```json\n{\"a\":1}\n```";
        SmallRecord parsed = JsonExtractor.parseOrThrow(raw, AlmaJackson.mapper(), SmallRecord.class);
        assertThat(parsed.a()).isEqualTo(1);
    }

    @Test
    void parseOrThrowThrowsJsonProcessingExceptionOnGarbage() {
        String raw = "this is not json at all";
        assertThatThrownBy(() ->
                JsonExtractor.parseOrThrow(raw, AlmaJackson.mapper(), SmallRecord.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    private record SmallRecord(int a) {}
}
