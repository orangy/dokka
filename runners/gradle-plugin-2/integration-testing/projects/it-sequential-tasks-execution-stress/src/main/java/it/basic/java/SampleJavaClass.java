package it.basic.java;

import it.basic.PublicClass;

/**
 * This class is, unlike {@link PublicClass}, written in Java
 */
@SuppressWarnings("unused")
public class SampleJavaClass {

    /**
     * @return Empty instance of {@link PublicClass}
     */
    public PublicClass publicDocumentedFunction() {
        return new PublicClass();
    }
}
