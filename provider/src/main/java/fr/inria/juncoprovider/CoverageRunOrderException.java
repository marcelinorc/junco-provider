package fr.inria.juncoprovider;

/**
 * An exception for the many causes of failure of the coverage order
 *
 * Created by marcel on 22/03/14.
 */
public class CoverageRunOrderException extends Exception {

    public CoverageRunOrderException(String message) {
        super(message);
    }

}
