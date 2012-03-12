package weka.timeseries;

import weka.core.Instances;
import weka.core.SerializedObject;
import weka.core.Utils;

import java.io.Serializable;

/**
 * Implementation of class TimeSeries
 * 
 * @author anilkpatro
 * @date Mar 22, 2004
 */
public abstract class TimeSeries implements Cloneable, Serializable
{
    /**
     * Analyzes time series data. Must initialize all fields of the timeseries
     * that are not being set via options (ie. multiple calls of analyze
     * must always lead to the same result). Must not change the dataset
     * in any way.
     *
     * @param data set of instances serving as training data
     * @throws Exception if the analysis has not been
     *                   done successfully
     */
    public abstract void analyze(Instances data) throws Exception;

    /**
     * Creates a new instance of a timeseries analyzer given it's class name and
     * (optional) arguments to pass to it's setOptions method. If the
     * associator implements OptionHandler and the options parameter is
     * non-null, the associator will have it's options set.
     *
     * @param analyzerName the fully qualified class name of the timeseries analyzer
     * @param options        an array of options suitable for passing to setOptions. May
     *                       be null.
     * @return the newly created associator, ready for use.
     * @throws Exception if the associator name is invalid, or the options
     *                   supplied are not acceptable to the associator
     */
    public static TimeSeries forName(String analyzerName,
                                     String[] options) throws Exception
    {
        return (TimeSeries) Utils.forName( TimeSeries.class,
                                           analyzerName,
                                           options );
    }

    /**
     * Creates copies of the current associator. Note that this method
     * now uses Serialization to perform a deep copy, so the Associator
     * object must be fully Serializable. Any currently built model will
     * now be copied as well.
     *
     * @param model an example associator to copy
     * @param num   the number of associators copies to create.
     * @return an array of associators.
     * @throws Exception if an error occurs
     */
    public static TimeSeries[] makeCopies(TimeSeries model,
                                          int num) throws Exception
    {
        if ( model == null ) {
            throw new Exception( "No model time series analysis set" );
        }
        TimeSeries[] analyzers = new TimeSeries[num];
        SerializedObject so = new SerializedObject( model );
        for ( int i = 0; i < analyzers.length; i++ ) {
            analyzers[i] = (TimeSeries) so.getObject();
        }
        return analyzers;
    }
}
