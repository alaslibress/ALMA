package alma.cli.ui;

@FunctionalInterface
public interface LanguageProvider {

    // Returns ISO-639-1 code (en, es, pt, ...).
    String sourceLanguage(String text);
}
