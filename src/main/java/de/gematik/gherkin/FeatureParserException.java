package de.gematik.gherkin;

public class FeatureParserException extends RuntimeException {

    private static final long serialVersionUID = -5184487815608514494L;

    public FeatureParserException(final String s) {
        super(s);
    }

    public FeatureParserException(final String s, final Throwable t) {
        super(s, t);
    }
}
