package uk.ac.rothamsted.kg.rdf2pg.pgmaker;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * General configuration properties for rdf2pg. This apply overall, contrary to 
 * {@link ConfigItem}, which are per-entity type.
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>14 Jul 2026</dd></dl>
 *
 */
@Component @Qualifier ( "generalConfig" )
public class GeneralConfig
{
	private int maxStringLength = 7000;
	private int maxSetSize = 100;
	private BigValueMode bigValueMode = BigValueMode.IGNORE;
	private String bigValueTrailer = "...[truncated]";
	
	/**
	 * What to do when a property value is too big.
	 * 
	 * {@link GeneralConfig#getMaxStringLength()} and {@link GeneralConfig#getMaxSetSize()} are defined mainly 
	 * to support targets like Neo4j, which have problems with making range indexes for long strings and/or big 
	 * lists.
	 */
	public enum BigValueMode
	{
		/**
		 * In this mode, strings longer than {@link GeneralConfig#getMaxStringLength()} are truncated to
		 * this length minus the length of {@link GeneralConfig#getBigValueTrailer()}, for obvious reasons.
		 * 
		 * In case of a multi-value properties, the max length possible is divided by the number of string 
		 * values in the property.
		 */
		TRUNCATE,
		
		/**
		 * this is like {@link #TRUNCATE}, but if truncation is not possible for a property value, it is ignored. 
		 */
		TRUNCATE_OR_IGNORE,
		
		/**
		 * In this mode, values exceeding {@link GeneralConfig#getMaxStringLength()} and/or 
		 * {@link GeneralConfig#getMaxSetSize()} will cause an error and the whole rdf2pg conversion will fail.
		 */
		ERROR,
		
		/**
		 * In this mode, no string or set length is enforced. This is the default in most targets; in targets
		 * like Neo4j, {@link #TRUNCATE_OR_IGNORE} is used as default instead.
		 */
		IGNORE
	}
	

	public BigValueMode getBigValueMode ()
	{
		return bigValueMode;
	}
	
	public void setBigValueMode ( BigValueMode bigValueMode )
	{
		this.bigValueMode = bigValueMode;
	}

	/**
	 * The max length for a single sting value, or for the sum of values of a multi-value string property
	 * (in rdf2pg, they're always sets). If a value is bigger than this, the behaviour depends on 
	 * {@link #getBigValueMode()}.
	 * 
	 */
	public int getMaxStringLength ()
	{
		return maxStringLength;
	}

	public void setMaxStringLength ( int maxStringLength )
	{
		this.maxStringLength = maxStringLength;
	}

	/**
	 * The max elements allowed in a multi-value property. Unless {@link #getBigValueMode()} 
	 * is set to {@link BigValueMode#IGNORE}, if a property has more values than this, 
	 * an error is raised and the whole rdf2pg conversion fails.
	 */
	public int getMaxSetSize ()
	{
		return maxSetSize;
	}

	public void setMaxSetSize ( int maxSetSize )
	{
		this.maxSetSize = maxSetSize;
	}

	/**
	 * Trailer appended to a truncated string, default is "...[truncated]".
	 * Nothing is appended if this is null or empty.
	 * 
	 */
	public String getBigValueTrailer ()
	{
		return bigValueTrailer;
	}

	public void setBigValueTrailer ( String bigValueTrailer )
	{
		this.bigValueTrailer = bigValueTrailer;
	}

	@Override
	public String toString ()
	{
		return String.format ( 
			"GeneralConfig{maxStringLength: %s, maxSetSize: %s, bigValueMode: %s, bigValueTrailer: %s}",
			maxStringLength, maxSetSize, bigValueMode, bigValueTrailer
		);
	}
}
