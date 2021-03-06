package org.elasticsearch.index.analysis;

/**
 * Joe Linn
 * 1/17/2015
 */
public enum URLPart {
    PROTOCOL,
    HOST,
    PATH,
    REF,
    QUERY,
    PORT,
    WHOLE;

    public static URLPart fromString(String part) {
        for (URLPart urlPart : URLPart.values()) {
            if (urlPart.name().equalsIgnoreCase(part)) {
                return urlPart;
            }
        }
        throw new IllegalArgumentException(String.format("Unrecognized URL part: %s", part));
    }
}
