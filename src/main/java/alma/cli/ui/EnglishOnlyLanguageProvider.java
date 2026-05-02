package alma.cli.ui;

// Stub LanguageProvider used when the real LanguageDetector is not wired.
public final class EnglishOnlyLanguageProvider implements LanguageProvider {

    @Override
    public String sourceLanguage(String text) {
        return "en";
    }
}
