package weka.timeseries;

import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.FourierTransform;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Date;

/**
 * Implementation of class SimilarityAnalysis
 * @author anilkpatro
 */
public class SimilarityAnalysis extends TimeSeries implements OptionHandler
{
    /** tolerance value for similarity */
    private double m_epsilon = 10;
    /** number of coefficients to use after FT */
    private int m_numCoeffs = 3;
    /** use FFT/DFT for FT */
    private boolean m_useFFT = false;
    /** range of values considred as template */
    private Range m_rangeTemplates = new Range( "first-5" );

    /** similarity matrices */
    private double[][] m_distancesFreq;
    private double[][] m_distancesTime;
    /** the actual data */
    private Instances m_data;

    /** timing information */
    private Date m_DFTTime;
    private Date m_FTEuclideanTime;
    private Date m_EuclideanTime;

    /**
     * Constructor
     */
    public SimilarityAnalysis()
    {
        m_rangeTemplates.setUpper( 100 );
    }

    // ---- ACCESSORS ----
    
    public boolean getUseFFT()
    {
        return m_useFFT;
    }

    public void setUseFFT(boolean useFFT)
    {
        m_useFFT = useFFT;
    }

    public String getTemplateRange()
    {
        return m_rangeTemplates.getRanges();
    }

    public void setTemplateRange(String rangeTemplates)
    {
        m_rangeTemplates.setRanges( rangeTemplates );
    }

    public double getEpsilon()
    {
        return m_epsilon;
    }

    public void setEpsilon(double epsilon)
    {
        m_epsilon = epsilon;
    }

    public int getNumCoeffs()
    {
        return m_numCoeffs;
    }

    public void setNumCoeffs(int numCoeffs)
    {
        m_numCoeffs = numCoeffs;
    }

    // ---- OPERATIONS ----
    
    /**
     * Analyze the time series data. The similarity matrices are created
     * and filled with euclidean distances based on the tolerance values
     * for similarity.
     *
     * @param data data to be analyzed
     */
    public void analyze(Instances data) throws Exception
    {
        m_data = data;
        m_rangeTemplates.setUpper( data.numAttributes() );

        Date startFT = new Date();

        // compute fourier transform
        FourierTransform dftFilter = new FourierTransform();
        dftFilter.setInputFormat( data );
        dftFilter.setNumCoeffs( getNumCoeffs() );
        dftFilter.setUseFFT( getUseFFT() );
        Instances fourierdata = Filter.useFilter( data, dftFilter );

        Date endFT = new Date();

        // time taken for FT
        m_DFTTime = new Date(endFT.getTime() - startFT.getTime());

        int numdim = data.numAttributes();
        m_distancesFreq = new double[numdim][numdim];
        m_distancesTime = new double[numdim][numdim];

        long ftDistTime = 0;
        long tDistTime = 0;

        // compute similarity matrices
        for ( int i = 0; i < data.numAttributes(); ++i ) {
            for ( int j = 0; j < i; j++ ) {
                // not for template sequences
                if (m_rangeTemplates.isInRange(i) && m_rangeTemplates.isInRange(j))
                    continue;

                Date startFTDist = new Date();

                // Compute the Euclidean distance between 2 dims using FT
                double[] reCT = fourierdata.attributeToDoubleArray( 2 * i );
                double[] imCT = fourierdata.attributeToDoubleArray( 2 * i + 1 );

                double[] reCS = fourierdata.attributeToDoubleArray( 2 * j );
                double[] imCS = fourierdata.attributeToDoubleArray( 2 * j + 1 );

                m_distancesFreq[i][j] = computeEuclidean( reCT, imCT, reCS, imCS );

                // if found similar using FT
                if ( m_distancesFreq[i][j] <= m_epsilon ) {
                    // then compute normal Euclidean distances between the 2 dims
                    double[] x = data.attributeToDoubleArray( i );
                    double[] y = data.attributeToDoubleArray( j );

                    m_distancesTime[i][j] = computeEuclidean( x, y );
                }

                Date endFTDist = new Date();

                // time taken for computing similarity based on FT
                ftDistTime += (endFTDist.getTime() - startFTDist.getTime());


                Date startDist = new Date();

                // compute similarity matrices (brute force)
                double[] x = data.attributeToDoubleArray( i );
                double[] y = data.attributeToDoubleArray( j );

                computeEuclidean( x, y );

                Date endDist = new Date();
                // time taken for computing similarity based brute force method
                tDistTime += (endDist.getTime() - startDist.getTime());

            }
        }

        m_FTEuclideanTime = new Date(ftDistTime);
        m_EuclideanTime = new Date(tDistTime);
    }

    /**
     * Compute Euclidean distance between two sequences having complex
     * numbers
     *
     * @param reCT real part of first sequence
     * @param imCT imaginary part of first sequence
     * @param reCS real part of second sequence
     * @param imCS imaginary part of second sequence
     */
    private double computeEuclidean(double[] reCT, double[] imCT,
                                    double[] reCS, double[] imCS)
    {
        double sum = 0;
        for ( int i = 0; i < reCT.length; ++i ) {
            double diffre = reCT[i] - reCS[i];
            double diffim = imCT[i] - imCS[i];
            double mod = Math.sqrt(diffre*diffre + diffim*diffim);
            sum += mod * mod;
        }
        return Math.sqrt( sum );
    }

    /**
     * Compute Euclidean distance between two sequences
     *
     * @param x first sequence
     * @param y second sequence
     */
    private double computeEuclidean(double[] x, double[] y)
    {
        double sum = 0;
        for ( int i = 0; i < x.length; ++i ) {
            sum += ( x[i] - y[i] ) * ( x[i] - y[i] );
        }
        return Math.sqrt( sum );
    }

   /**
    * Returns an enumeration of all the available options..
    *
    * @return an enumeration of all available options.
    */
    public Enumeration listOptions()
    {
        Vector newVector = new Vector( 6 );

        newVector.addElement( new Option( "\tSpecify a set of attributes which form the template."
                                          + "\n\tEg. 1,3,5-7."
                                          , "T", 1, "-T <start set>" ) );
        newVector.addElement( new Option( "\tEpsilon.\n" +
                                          "\t(default = 10)",
                                          "e", 1, "-e <num>" ) );
        newVector.addElement( new Option( "\tSpecifies the number of coefficients to use.\n" +
                                          "\t(default = 3)",
                                          "r", 1, "-r <num>" ) );
        newVector.addElement( new Option( "\tUse FFT for calculation of DFTs\n" +
                                          "\t(default = false)",
                                          "f", 0, "-f <true|false>" ) );

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
        String optionString = Utils.getOption( 'T', options );
        if ( optionString.length() != 0 ) {
            setTemplateRange( optionString );
        }

        String epsilon = Utils.getOption( 'e', options );
        if ( epsilon.length() != 0 ) {
            setEpsilon( Integer.parseInt( epsilon ) );
        } else {
            setEpsilon( 10 );
        }

        String numCoeffs = Utils.getOption( 'r', options );
        if ( numCoeffs.length() != 0 ) {
            setNumCoeffs( Integer.parseInt( numCoeffs ) );
        } else {
            setNumCoeffs( 3 );
        }

        String useFFT = Utils.getOption( 'f', options );
        if ( epsilon.length() != 0 ) {
            setUseFFT( useFFT == "True" );
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
        String[] options = new String[8];
        int current = 0;

        options[current++] = "-T";
        options[current++] = "" + getTemplateRange();
        options[current++] = "-e";
        options[current++] = "" + getEpsilon();
        options[current++] = "-r";
        options[current++] = "" + getNumCoeffs();
        options[current++] = "-f";
        options[current++] = "" + getUseFFT();
        return options;
    }

    /**
     * Output
     *
     * @return generated output
     */
    public String toString()
    {
        StringBuffer text = new StringBuffer();

        text.append( "\nTime Series Similarity Analysis\n=======\n\n" );
        text.append( "Tolerance: " + m_epsilon + '\n' );
        text.append( "Number of coefficients after DFT: " + m_numCoeffs + '\n' );

        text.append( "\nFreq-domain Distance Matrix:\n" );

        text.append("            ");
        for (int i=0; i<m_data.numAttributes(); ++i) {
            int spc = 12-m_data.attribute(i).name().length();
            for (int j=0; j<spc/2; ++j) text.append(" ");
            text.append( m_data.attribute( i ).name());
            //for (int j=0; j<spc/2; ++j) text.append(" ");
            text.append("\t");
        }
        text.append ("\n");
        for (int i=0; i<m_data.numAttributes(); ++i) {
            for (int j=-1; j<i; ++j) {
                if (j == -1) {
                    text.append(m_data.attribute(i).name() + "\t");
                } else {
                    text.append( Utils.doubleToString( m_distancesFreq[i][j], 12, 4) + "\t" );
                }
            }
            text.append ("\n");
        }

        text.append( "\nActual Distance Matrix:\n" );

        text.append("            ");
        for (int i=0; i<m_data.numAttributes(); ++i) {
            int spc = 12-m_data.attribute(i).name().length();
            for (int j=0; j<spc/2; ++j) text.append(" ");
            text.append( m_data.attribute( i ).name());
            //for (int j=0; j<spc/2; ++j) text.append(" ");
            text.append("\t");
        }
        text.append ("\n");
        for (int i=0; i<m_distancesTime.length; ++i) {
            for (int j=-1; j<i; ++j) {
                if (j == -1) {
                    text.append(m_data.attribute(i).name() + "\t");
                } else {
                    text.append( Utils.doubleToString( m_distancesTime[i][j], 12, 4) + "\t" );
                }
            }
            text.append ("\n");
        }
        text.append ("\n");

        text.append ("\nTemplate Similarity:\n======\n");
        int [] range = m_rangeTemplates.getSelection();
        for (int i=0; i<range.length; ++i) {
            int row = range[i];
            for (int col=range[range.length-1]+1; col<m_data.numAttributes(); ++col) {
                if (m_distancesFreq[col][row] <= m_epsilon)
                    if (m_distancesTime[col][row] <= m_epsilon)
                    {
                        text.append("Sequence '" + m_data.attribute(col).name() +
                                    "' seems to be similar to template sequence '" +
                                    m_data.attribute(row).name() + "'\n");
                    }
            }
        }

        text.append ("\nSeries Similarity:\n======\n");
        for (int row=range[range.length-1]+1; row<m_data.numAttributes(); ++row) {
            for (int col=range[range.length-1]+1; col<row; ++col) {
                if (m_distancesFreq[row][col] <= m_epsilon)
                    if (m_distancesTime[row][col] <= m_epsilon)
                    {
                        text.append("Sequence '" + m_data.attribute(col).name() +
                                    "' seems to be similar to sequence '" +
                                    m_data.attribute(row).name() + "'\n");
                    }
            }
        }

        text.append ("\nTiming:\n======\n");
        text.append("Time taken to compute DFTs: " + m_DFTTime.getTime() + "\n");
        text.append("Time taken to compute distances using DFTs: " + m_FTEuclideanTime.getTime() + "\n");
        text.append("Time taken to compute distances: " + m_EuclideanTime.getTime() + "\n");


        return text.toString();
    }

    /**
     * Main method for testing this class.
     */
    public static void main(String[] options)
    {

        String trainFileString;
        StringBuffer text = new StringBuffer();
        SimilarityAnalysis simi = new SimilarityAnalysis();
        Reader reader;

        try {
            text.append( "\n\nTime Series Similarity Analysis options:\n\n" );
            text.append( "-t <training file>\n" );
            text.append( "\tThe name of the training file.\n" );
            Enumeration enum__ = simi.listOptions();
            while ( enum__.hasMoreElements() ) {
                Option option = (Option) enum__.nextElement();
                text.append( option.synopsis() + '\n' );
                text.append( option.description() + '\n' );
            }
            trainFileString = Utils.getOption( 't', options );
            if ( trainFileString.length() == 0 ) {
                throw new Exception( "No training file given!" );
            }
            simi.setOptions( options );
            reader = new BufferedReader( new FileReader( trainFileString ) );
            simi.analyze( new Instances( reader ) );
            System.out.println( simi );
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println( "\n" + e.getMessage() + text );
        }
    }
}
