/*
 *   Copyright 2015 Calytrix Technologies
 *
 *   This file is part of hperf.
 *
 *   NOTICE:  All information contained herein is, and remains
 *            the property of Calytrix Technologies Pty Ltd.
 *            The intellectual and technical concepts contained
 *            herein are proprietary to Calytrix Technologies Pty Ltd.
 *            Dissemination of this information or reproduction of
 *            this material is strictly forbidden unless prior written
 *            permission is obtained from Calytrix Technologies Pty Ltd.
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package hperf;

import java.text.NumberFormat;

import org.apache.log4j.Logger;

public class Utils
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	/**
	 * Create a byte[] of the specified size with a predictable set of contents.
	 * This is used to so that we can have some measure of control over the size
	 * of attribute reflections or interactions when we send them.
	 * 
	 * Buffer contents are a cyclic count from 0-9. For example, a 13-byte buffer
	 * would return: `[0,1,2,3,4,5,6,7,8,9,0,1,2]`.
	 * 
	 * @param sizeInBytes The size to make the buffer
	 * @return Initialized buffer of the specified size containing a cylic set of
	 *         numbers from 0-9.
	 */
	public static byte[] generatePayload( int sizeInBytes )
	{
		// Stuff a basic buffer full of some data we can verify on the other side
		// The size of this is set in configuration and we use it to ensure that
		// all messages are of at least a certain size. In reality, the size is a
		// little bigger as we send some additional attributes/parameters, but we
		// can't have it all with perfectly sized updates now can we!
		byte[] payload = new byte[sizeInBytes];
		for( int i = 0; i < sizeInBytes; i++ )
			payload[i] = (byte)(i % 10);

		return payload;
	}

	/**
	 * Validates the given payload as being consistent with the scheme that we use
	 * to generate payloads in {@link #generatePayload(int)}. This is a cyclic list
	 * of numbers, from 0-9, repeating until the appropriate buffer size is filled.
	 * If there is a problem, we print this to the provided logger.
	 * 
	 * @param received The payload we received
	 * @param expectedSize How big we expect the array to be
	 * @param logger The logger to print our results to
	 */
	public static void verifyPayload( byte[] received, int expectedSize, Logger logger )
	{
		if( received.length != expectedSize )
		{
			logger.error( "Received data buffer of incorrect size: expected="+
			              expectedSize+", received="+received.length );
		}

		for( int i = 0; i < received.length; i++ )
		{
			byte expected = (byte)(i % 10);
			if( received[i] != expected )
			{
				logger.error( "Invalid data received. Index ["+i+"] was ["+received[i]+
				              "], expected ["+expected+"]" );
				return;
			}
		}
	}
	
	/**
	 * Convert the given size (in bytes) to a more human readable string. Returned values
	 * will be in the form: "16B", "16KB", "16MB", "16GB".
	 */
	public static String getSizeString( long size )
	{
		return getSizeString( size, 2 );
	}
	
	public static String getSizeString( long bytes, int decimalPlaces )
	{
		// let's see how much we have so we can figure out the right qualifier
		double totalkb = bytes / 1000;
		double totalmb = totalkb / 1000;
		double totalgb = totalmb / 1000;
		if( totalgb >= 1 )
			return String.format( "%4."+decimalPlaces+"fGB", totalgb );
		else if( totalmb >= 1 )
			return String.format( "%4."+decimalPlaces+"fMB", totalmb );
		else if( totalkb >= 1 )
			return String.format( "%4."+decimalPlaces+"fKB", totalkb );
		else
			return bytes+"b";
	}

	public static String getMegabytesPerSec( double bytes, int decimalPlaces )
	{
		// let's see how much we have so we can figure out the right qualifier
		double totalkb = bytes / 1000;
		double totalmb = totalkb / 1000;
		double totalgb = totalmb / 1000;
		if( totalgb > 1 )
			return String.format( "%5."+decimalPlaces+"fGB/s", totalgb );
		else if( totalmb > 1 )
			return String.format( "%5."+decimalPlaces+"fMB/s", totalmb );
		else
			return String.format( "%5."+decimalPlaces+"fKB/s", totalkb );
	}

	public static String getMegabitsPerSec( double bits )
	{
		// let's see how much we have so we can figure out the right qualifier
		double totalkb = bits / 1000;
		double totalmb = totalkb / 1000;
		double totalgb = totalmb / 1000;
		
		if( totalgb > 1 )
			return String.format( "%4.2f Gbits/s", totalgb );     /** 5.79 Gbits/s */
		else if( totalmb >= 100 )
			return String.format( "%4d Mbits/s", (long)totalmb ); /**  100 Mbits/s */
		else if( totalmb >= 10 )
			return String.format( "%4d Mbits/s", (long)totalmb ); /**   10 Mbits/s */
		else
			return String.format( "%4.2f Mbits/s", totalmb );     /**  1.0 Mbits/s */
	}

	/** Returns value as string with thousands separators */
	public static String getFormatted( int value )
	{
		return NumberFormat.getIntegerInstance().format( value );
	}

	/** 
	 * Sleep for the given milliseconds, throw an unchecked exception
	 * if we are interrupted for any reason.
	 */
	public static final void sleep( long millis )
	{
		sleep( millis, 0 );
	}

	/**
	 * Sleep for the given milliseconds and additional nanos. On most operating
	 * systems this should use the timer with the highest resolution available
	 * to the JVM. If you just want to sleep for some period of nanos, pass `0`
	 * for the millis value.
	 * 
	 * Throw an unchecked exception if we are interrupted for any reason
	 */
	public static final void sleep( long millis, int nanos )
	{
		try
		{
			Thread.sleep( millis, nanos );
		}
		catch( InterruptedException ie )
		{
			throw new RuntimeException( ie );
		}
	}

	/**
	 * Convenience method that will call `.wait()` on the given object and swallow the
	 * interrupted exception, returning immediately if it is generated. Condenses the
	 * code a little bit, making things a bit simpler and easier to read without the big
	 * try/catch guff.
	 */
	public static final void wait( Object waitObject )
	{
		synchronized( waitObject )
		{
			try
			{
				waitObject.wait();
			}
			catch( Exception e )
			{
				return;
			}
		}
	}
	
	///////////////////////////////////////////////////////////////
	// Int Conversion Methods                                    //
	///////////////////////////////////////////////////////////////
	public static byte[] intToBytes( int value )
	{
		return new byte[]
		{
		 	(byte)(value >>> 24),
		 	(byte)(value >>> 16),
		 	(byte)(value >>> 8),
		 	(byte)(value)
		};
	}

	public static void intToBytes( int value, byte[] buffer, int offset )
	{
		buffer[offset]   = (byte)( value >>> 24 );
		buffer[offset+1] = (byte)( value >>> 16 );
		buffer[offset+2] = (byte)( value >>> 8  );
		buffer[offset+3] = (byte)(value);
	}

	
	public static int bytesToInt( byte[] bytes )
	{
		return ((bytes[0] & 0xff) << 24) |
		       ((bytes[1] & 0xff) << 16) |
		       ((bytes[2] & 0xff) << 8 ) |
		        (bytes[3] & 0xff);
	}

	public static int bytesToInt( byte[] buffer, int offset )
	{
		return ((buffer[offset]   & 0xff) << 24) |
		       ((buffer[offset+1] & 0xff) << 16) |
		       ((buffer[offset+2] & 0xff) << 8 ) |
		        (buffer[offset+3] & 0xff);
	}

	///////////////////////////////////////////////////////////////
	// Long Conversion Methods                                   //
	///////////////////////////////////////////////////////////////
	public static byte[] longToBytes( long value )
	{
		return new byte[]
		{
		 	(byte)(value >>> 56),
		 	(byte)(value >>> 48),
		 	(byte)(value >>> 40),
		 	(byte)(value >>> 32),
		 	(byte)(value >>> 24),
		 	(byte)(value >>> 16),
		 	(byte)(value >>> 8),
		 	(byte)(value)
		};
	}

	public static void longToBytes( long value, byte[] buffer, int offset )
	{
		buffer[offset]   = (byte)( value >>> 56 );
		buffer[offset+1] = (byte)( value >>> 48 );
		buffer[offset+2] = (byte)( value >>> 40 );
		buffer[offset+3] = (byte)( value >>> 32 );
		buffer[offset+4] = (byte)( value >>> 24 );
		buffer[offset+5] = (byte)( value >>> 16 );
		buffer[offset+6] = (byte)( value >>> 8  );
		buffer[offset+7] = (byte)(value);
	}

	public static long bytesToLong( byte[] bytes )
	{
		return ((long)(bytes[0] & 0xff) << 56) |
		       ((long)(bytes[1] & 0xff) << 48) |
		       ((long)(bytes[2] & 0xff) << 40) |
		       ((long)(bytes[3] & 0xff) << 32) |
		       ((long)(bytes[4] & 0xff) << 24) |
		       ((long)(bytes[5] & 0xff) << 16) |
		       ((long)(bytes[6] & 0xff) << 8 ) |
		       ((long) bytes[7] & 0xff);
	}

	public static long bytesToLong( byte[] buffer, int offset )
	{
		return ((long)(buffer[offset]   & 0xff) << 56) |
		       ((long)(buffer[offset+1] & 0xff) << 48) |
		       ((long)(buffer[offset+2] & 0xff) << 40) |
		       ((long)(buffer[offset+3] & 0xff) << 32) |
		       ((long)(buffer[offset+4] & 0xff) << 24) |
		       ((long)(buffer[offset+5] & 0xff) << 16) |
		       ((long)(buffer[offset+6] & 0xff) << 8 ) |
		        ((long)buffer[offset+7] & 0xff);
	}
}
