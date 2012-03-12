/*
 * Created on Mar 13, 2004
 */
package weka.filters.unsupervised.attribute;

import weka.core.*;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Fourier Transform filter - takes an n attribute dataset (all numeric)
 * and return an 2*n attribute dataset with real and imaginary components
 * of Fourier Transform of the original attributes
 * @author anilkpatro
 */
public class FourierTransform
        extends Filter
        implements UnsupervisedFilter, OptionHandler
{
    /** Number of coeeficients to keep after transformation */
    private int m_numCoeffs = 5;
    /** Use FFT/DFT for Fourier Transformations */
    private boolean m_useFFT = false;
    /** Complex number helper for FTs */
    private AttFTHolder [] m_attsFT;

    // ---- ACCESSOR FUNCTIONS ----
    
    public int getNumCoeffs()
    {
        return m_numCoeffs;
    }

    public void setNumCoeffs(int numCoeffs)
    {
        m_numCoeffs = numCoeffs;
    }

    public boolean getUseFFT()
    {
        return m_useFFT;
    }

    public void setUseFFT(boolean use)
    {
        m_useFFT = use;
    }

    // ---- OPERATIONS ----
    
    /**
     * Sets the format of the input instances. If the filter is able to
     * determine the output format before seeing any input instances, it
     * does so here. This default implementation clears the output format
     * and output queue, and the new batch flag is set. Overriders should
     * call <code>super.setInputFormat(Instances)</code>
     *
     * @param instanceInfo an Instances object containing the input instance
     * structure (any instances contained in the object are ignored - only the
     * structure is required).
     * @return true if the outputFormat may be collected immediately
     * @exception Exception if the inputFormat can't be set successfully 
     */
    public boolean setInputFormat(Instances instanceInfo) throws Exception
    {
        super.setInputFormat( instanceInfo );

        for ( int i = 0; i < instanceInfo.numAttributes(); ++i )
        {
            if ( !instanceInfo.attribute( i ).isNumeric() ) {
                throw new UnsupportedAttributeTypeException( "All attributes must be numeric" );
            }
        }

        // Create the output buffer
        setOutputFormat();
        return true;
    }

    /**
     * Sets the format of output instances.
     */
    private void setOutputFormat()
    {
        // give names to the new attributes
        FastVector newAtts = new FastVector();
        String foName = null;
        for ( int i = 0; i < getInputFormat().numAttributes(); i++ )
        {
            String attName = getInputFormat().attribute( i ).name();
            foName = "'FT " + attName.replace( '\'', ' ' ).trim() + " (re)\'";
            Attribute newAttribX = new Attribute( foName );
            newAtts.addElement( newAttribX );

            foName = "'FT " + attName.replace( '\'', ' ' ).trim() + " (im)\'";
            Attribute newAttribY = new Attribute( foName );
            newAtts.addElement( newAttribY );
        }

        setOutputFormat( new Instances( getInputFormat().relationName(),
                                        newAtts, getNumCoeffs() ) );
    }

   /**
    * Signify that this batch of input to the filter is finished. If
    * the filter requires all instances prior to filtering, output()
    * may now be called to retrieve the filtered instances. Any
    * subsequent instances filtered should be filtered based on setting
    * obtained from the first batch (unless the inputFormat has been
    * re-assigned or new options have been set). This default
    * implementation assumes all instance processing occurs during
    * inputFormat() and input().
    *
    * @return true if there are instances pending output
    * @exception NullPointerException if no input structure has been defined,
    * @exception Exception if there was a problem finishing the batch.
    */
    public boolean batchFinished()
    {
        if ( getInputFormat() == null ) {
            throw new IllegalStateException( "No input instance format defined" );
        }

        // throw if all attributes are not numeric
        Instances instances = getInputFormat();
        if ( instances.numInstances() < getNumCoeffs() )
        {
            throw new IllegalStateException( "Number of coeffs cannot be greater " +
                                             "than the total number of instances" );
        }

        m_attsFT = new AttFTHolder [ instances.numAttributes() ];

        int nearestPower2;
        for ( int attr = 0; attr < instances.numAttributes(); ++attr ) {
            m_attsFT[attr] = new AttFTHolder();

            double [] array = instances.attributeToDoubleArray( attr );
            // get the nearest power of 2 for the FT
            for (nearestPower2 = 1; array.length > nearestPower2;
                 nearestPower2 <<= 1)
                ;
            
            // initialize the complex numbers
            m_attsFT[attr].re = new double[nearestPower2];
            m_attsFT[attr].im = new double[nearestPower2];
            int j = 0;
            for (int i=0; i<nearestPower2; ++i, ++j) {
                m_attsFT[attr].re[i] = (j < array.length) ? array[i] : 0;
                m_attsFT[attr].im[i] = 0;
            }

            // inplace FT
            if (m_useFFT) {
                computeFFT(m_attsFT[attr].re, m_attsFT[attr].im);
            } else {
                computeDFT(m_attsFT[attr].re, m_attsFT[attr].im);
            }
        }

        // set instances of the new dataset
        for (int i=0; i<getNumCoeffs(); ++i) {
            double [] vals = new double[instances.numAttributes() * 2];
            for (int j=0; j<instances.numAttributes(); ++j) {
                vals[2*j]   = m_attsFT[j].re[i];
                vals[2*j+1] = m_attsFT[j].im[i];
            }
            Instance inst = new Instance( instances.instance(i).weight(), vals );
            inst.setDataset( instances.instance(i).dataset() );
            push(inst);
        }

        flushInput();
        m_NewBatch = true;
        return ( numPendingOutput() != 0 );
    }

    /**
     * This computes an in-place complex-to-complex FFT x and y are the real
     * and imaginary arrays of 2^m points.
     * 
     * @param x real array
     * @param y imaginary array
     */
    private void computeFFT(double[] x, double[] y)
    {
        int numPoints = x.length;
        int logPoints = (int) (Math.log(numPoints) / Math.log(2));

        // Do the bit reversal
        int halfPoints = numPoints / 2;
        int rev = 0;
        for (int i = 0; i < numPoints - 1; i++) {
            if (i < rev) {
                // swap the numbers
                double tx = x[i];
                double ty = y[i];
                x[i] = x[rev];
                y[i] = y[rev];
                x[rev] = tx;
                y[rev] = ty;
            }
            int mask = halfPoints;
            while (mask <= rev) {
                rev -= mask;
                mask >>= 1;
            }
            rev += mask;
        }

        // Compute the FFT
        double c1 = -1.0;
        double c2 = 0.0;
        int step = 1;
        for (int level = 0; level < logPoints; level++) {
            int increm = step * 2;
            double u1 = 1.0;
            double u2 = 0.0;
            for (int j = 0; j < step; j++) {
                for (int i = j; i < numPoints; i += increm) {
                    // Butterfly
                    double t1 = u1 * x[i + step] - u2 * y[i + step];
                    double t2 = u1 * y[i + step] + u2 * x[i + step];
                    x[i + step] = x[i] - t1;
                    y[i + step] = y[i] - t2;
                    x[i] += t1;
                    y[i] += t2;
                }
                // U = exp ( - 2 PI j / 2 ^ level )
                double z = u1 * c1 - u2 * c2;
                u2 = u1 * c2 + u2 * c1;
                u1 = z;
            }
            c2 = Math.sqrt((1.0 - c1) / 2.0);
            c1 = Math.sqrt((1.0 + c1) / 2.0);

            step *= 2;
        }

        // Scaling for forward transform
        for (int i = 0; i < numPoints; i++) {
            x[i] /= numPoints;
            y[i] /= numPoints;
        }
    }

    /**
     * This computes an in-place complex-to-complex DFT x and y are the real
     * and imaginary arrays of 2^m points.
     * 
     * @param x real array
     * @param y imaginary array
     */
    private void computeDFT(double[] x, double[] y)
    {
        double arg;
        double cosarg, sinarg;
        double[] x2;
        double[] y2;

        int m = x.length;
        x2 = new double[m];
        y2 = new double[m];

        // Compute correlation
        for (int i = 0; i < m; i++) {
            x2[i] = 0;
            y2[i] = 0;
            arg = 2.0 * Math.PI * (double) i / (double) m;
            for (int k = 0; k < m; k++) {
                cosarg = Math.cos(k * arg);
                sinarg = Math.sin(k * arg);
                x2[i] += (x[k] * cosarg - y[k] * sinarg);
                y2[i] += (x[k] * sinarg + y[k] * cosarg);
            }
        }

        // Copy and scale the data back
        for (int i = 0; i < m; i++) {
            x[i] = x2[i] / (double) m;
            y[i] = y2[i] / (double) m;
        }
    }

   /**
    * Returns an enumeration of all the available options..
    *
    * @return an enumeration of all available options.
    */
    public Enumeration listOptions()
    {
        Vector newVector = new Vector( 2 );

        newVector.addElement( new Option( "\tSpecifies the number of coefficients to use.\n" +
                                          "\t(default = 5)",
                                          "r", 1, "-r <num>" ) );
        newVector.addElement( new Option( "\tUse FFT.\n" +
                                          "\t(default = false)",
                                          "F", 1, "-F <num>" ) );

        return newVector.elements();
    }

   /**
    * Sets the OptionHandler's options using the given list. All options
    * will be set (or reset) during this call (i.e. incremental setting
    * of options is not possible).
    *
    * @param options the list of options as an array of strings
    * @exception Exception if an option is not supported
    */
    public void setOptions(String[] options) throws Exception
    {
        String numCoeffs = Utils.getOption( 'r', options );
        if ( numCoeffs.length() != 0 ) {
            setNumCoeffs(Integer.parseInt( numCoeffs ));
        } else {
            setNumCoeffs( 10 );
        }

        String useFFT = Utils.getOption( 'F', options );
        if ( useFFT.length() != 0 ) {
            setUseFFT(useFFT == "True");
        } else {
            setUseFFT( false );
        }
    }

   /**
    * Gets the current option settings for the OptionHandler.
    *
    * @return the list of current option settings as an array of strings
    */
    public String[] getOptions()
    {
        String [] options = new String [4];
        int current = 0;

        options[current++] = "-r"; options[current++] = "" + getNumCoeffs();
        options[current++] = "-F"; options[current++] = "" + getUseFFT();
        return options;
    }

    /**
     * Entry point for testing filter
     */
    public static void main(String[] args)
    {
        try {
            if ( Utils.getFlag( 'b', args ) ) {
                Filter.batchFilterFile( new FourierTransform(), args );
            } else {
                Filter.filterFile( new FourierTransform(), args );
            }
        } catch (Exception ex) {
            System.out.println( ex.getMessage() );
        }
    }

    /**
     * Helper class
     */
    private class AttFTHolder
    {
        public double [] re;
        public double [] im;
    }
}
