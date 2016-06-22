/*
 *	MixingAudioInputStream.java
 *
 *	This file is part of jsresources.org
 *
 *	This code follows an idea of Paul Sorenson.
 */

/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
|<---            this code is formatted to fit into 80 columns             --->|
*/

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/*
 * This is a class of Tritonus. It's not one of the best ideas to use it here.
 * However, we really don't want to reimplement its functionality here.
 * You need to have tritonus_share.jar in the classpath. 
 * Get it from http://www.tritonus.org .  
 */
//import org.tritonus.share.sampled.TConversionTool;



/**
 * Mixing of multiple AudioInputStreams to one AudioInputStream. This class
 * takes a collection of AudioInputStreams and mixes them together. Being a
 * subclass of AudioInputStream itself, reading from instances of this class
 * behaves as if the mixdown result of the input streams is read.
 * 
 * @author Matthias Pfisterer
 */
public class MixingAudioInputStream
	extends		AudioInputStream
{
	private static final boolean	DEBUG = false;

	private List			m_audioInputStreamList;



	public MixingAudioInputStream(AudioFormat audioFormat, Collection audioInputStreams)
	{
		super(new ByteArrayInputStream(new byte[0]),
		      audioFormat,
		      AudioSystem.NOT_SPECIFIED);
		if (DEBUG) { out("MixingAudioInputStream.<init>(): begin"); }
		m_audioInputStreamList = new ArrayList(audioInputStreams);
		if (DEBUG)
		{
			out("MixingAudioInputStream.<init>(): stream list:");
			for (int i = 0; i < m_audioInputStreamList.size(); i++)
			{
				out("  " + m_audioInputStreamList.get(i));
			}
		}
		if (DEBUG) { out("MixingAudioInputStream.<init>(): end"); }
	}



	/**
	   The maximum of the frame length of the input stream is calculated and returned.
	   If at least one of the input streams has length
	   <code>AudioInputStream.NOT_SPECIFIED</code>, this value is returned.
	*/
	public long getFrameLength()
	{
		long	lLengthInFrames = 0;
		Iterator	streamIterator = m_audioInputStreamList.iterator();
		while (streamIterator.hasNext())
		{
			AudioInputStream	stream = (AudioInputStream) streamIterator.next();
			long	lLength = stream.getFrameLength();
			if (lLength == AudioSystem.NOT_SPECIFIED)
			{
				return AudioSystem.NOT_SPECIFIED;
			}
			else
			{
				lLengthInFrames = Math.max(lLengthInFrames, lLength);
			}
		}
		return lLengthInFrames;
	}



	public int read()
		throws	IOException
	{
		if (DEBUG) { out("MixingAudioInputStream.read(): begin"); }
		int	nSample = 0;
		Iterator	streamIterator = m_audioInputStreamList.iterator();
		while (streamIterator.hasNext())
		{
			AudioInputStream	stream = (AudioInputStream) streamIterator.next();
			int	nByte = stream.read();
			if (nByte == -1)
			{
				/*
				  The end of this stream has been signaled.
				  We remove the stream from our list.
				*/
				streamIterator.remove();
				continue;
			}
			else
			{
				/*
				  what about signed/unsigned?
				*/
				nSample += nByte;
			}
		}
		if (DEBUG) { out("MixingAudioInputStream.read(): end"); }
		return (byte) (nSample & 0xFF);
	}
	
	public static void main (String[]args){
	//	read();
	}



	public int read(byte[] abData, int nOffset, int nLength)
		throws	IOException
	{
		if (DEBUG)
		{
			out("MixingAudioInputStream.read(byte[], int, int): begin");
			out("MixingAudioInputStream.read(byte[], int, int): requested length: " + nLength);
		}
		int	nChannels = getFormat().getChannels();
		int	nFrameSize = getFormat().getFrameSize();
		/*
		  This value is in bytes. Note that it is the storage size.
		  It may be four bytes for 24 bit samples.
		*/
		int	nSampleSize = nFrameSize / nChannels;
		boolean	bBigEndian = getFormat().isBigEndian();
		AudioFormat.Encoding	encoding = getFormat().getEncoding();
		if (DEBUG)
		{
			out("MixingAudioInputStream.read(byte[], int, int): channels: " + nChannels);
			out("MixingAudioInputStream.read(byte[], int, int): frame size: " + nFrameSize);
			out("MixingAudioInputStream.read(byte[], int, int): sample size (bytes, storage size): " + nSampleSize);
			out("MixingAudioInputStream.read(byte[], int, int): big endian: " + bBigEndian);
			out("MixingAudioInputStream.read(byte[], int, int): encoding: " + encoding);
		}
		byte[]	abBuffer = new byte[nFrameSize];
		int[]	anMixedSamples = new int[nChannels];
		for (int nFrameBoundry = 0; nFrameBoundry < nLength; nFrameBoundry += nFrameSize)
		{
			if (DEBUG) { out("MixingAudioInputStream.read(byte[], int, int): frame boundry: " + nFrameBoundry); }
			for (int i = 0; i < nChannels; i++)
			{
				anMixedSamples[i] = 0;
			}
			Iterator	streamIterator = m_audioInputStreamList.iterator();
			while (streamIterator.hasNext())
			{
				AudioInputStream	stream = (AudioInputStream) streamIterator.next();
				if (DEBUG)
				{
					out("MixingAudioInputStream.read(byte[], int, int): AudioInputStream: " + stream);
				}
				int	nBytesRead = stream.read(abBuffer, 0, nFrameSize);
				if (DEBUG)
				{
					out("MixingAudioInputStream.read(byte[], int, int): bytes read: " + nBytesRead);
				}
				/*
				  TODO: we have to handle incomplete reads.
				*/
				if (nBytesRead == -1)
				{
					/*
					  The end of the current stream has been signaled.
					  We remove it from the list of streams.
					*/
					streamIterator.remove();
					continue;
				}
				for (int nChannel = 0; nChannel < nChannels; nChannel++)
				{
					int	nBufferOffset = nChannel * nSampleSize;
					int	nSampleToAdd = 0;
					if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED))
					{
						switch (nSampleSize)
						{
						case 1:
							nSampleToAdd = abBuffer[nBufferOffset];
							break;
						case 2:
							nSampleToAdd = bytesToInt16(abBuffer, nBufferOffset, bBigEndian);
							break;
						case 3:
							nSampleToAdd = bytesToInt24(abBuffer, nBufferOffset, bBigEndian);
							break;
						case 4:
							nSampleToAdd = bytesToInt32(abBuffer, nBufferOffset, bBigEndian);
							break;
						}
					}
					// TODO: pcm unsigned
					else if (encoding.equals(AudioFormat.Encoding.ALAW))
					{
						nSampleToAdd = alaw2linear(abBuffer[nBufferOffset]);
					}
					else if (encoding.equals(AudioFormat.Encoding.ULAW))
					{
						nSampleToAdd = ulaw2linear(abBuffer[nBufferOffset]);
					}
					anMixedSamples[nChannel] += nSampleToAdd;
				} // loop over channels
			} // loop over streams
			if (DEBUG)
			{
				out("MixingAudioInputStream.read(byte[], int, int): starting to write to buffer passed by caller");
			}
			for (int nChannel = 0; nChannel < nChannels; nChannel++)
			{
				if (DEBUG)
				{
					out("MixingAudioInputStream.read(byte[], int, int): channel: " + nChannel);
				}
				int	nBufferOffset = nOffset + nFrameBoundry /* * nFrameSize*/ + nChannel * nSampleSize;
				if (DEBUG)
				{
					out("MixingAudioInputStream.read(byte[], int, int): buffer offset: " + nBufferOffset);
				}
				if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED))
				{
					switch (nSampleSize)
					{
					case 1:
						abData[nBufferOffset] = (byte) anMixedSamples[nChannel];
						break;
					case 2:
						intToBytes16(anMixedSamples[nChannel], abData, nBufferOffset, bBigEndian);
						break;
					case 3:
						intToBytes24(anMixedSamples[nChannel], abData, nBufferOffset, bBigEndian);
						break;
					case 4:
						intToBytes32(anMixedSamples[nChannel], abData, nBufferOffset, bBigEndian);
						break;
					}
				}
				// TODO: pcm unsigned
				else if (encoding.equals(AudioFormat.Encoding.ALAW))
				{
					abData[nBufferOffset] = linear2alaw((short) anMixedSamples[nChannel]);
				}
				else if (encoding.equals(AudioFormat.Encoding.ULAW))
				{
					abData[nBufferOffset] = linear2ulaw(anMixedSamples[nChannel]);
				}
			} // (final) loop over channels
		} // loop over frames
		if (DEBUG)
		{
			out("MixingAudioInputStream.read(byte[], int, int): end");
		}
		// TODO: return a useful value
		return nLength;
	}



	/**
	   calls skip() on all input streams. There is no way to assure that the number of
	   bytes really skipped is the same for all input streams. Due to that, this
	   method always returns the passed value. In other words: the return value
	   is useless (better ideas appreciated).
	*/
	public long skip(long lLength)
		throws	IOException
	{
		Iterator	streamIterator = m_audioInputStreamList.iterator();
		while (streamIterator.hasNext())
		{
			AudioInputStream	stream = (AudioInputStream) streamIterator.next();
			stream.skip(lLength);
		}
		return lLength;
	}



	/**
	   The minimum of available() of all input stream is calculated and returned.
	*/
	public int available()
		throws	IOException
	{
		int	nAvailable = 0;
		Iterator	streamIterator = m_audioInputStreamList.iterator();
		while (streamIterator.hasNext())
		{
			AudioInputStream	stream = (AudioInputStream) streamIterator.next();
			nAvailable = Math.min(nAvailable, stream.available());
		}
		return nAvailable;
	}



	public void close()
		throws	IOException
	{
		// TODO: should we close all streams in the list?
	}



	/**
	   Calls mark() on all input streams.
	*/
	public void mark(int nReadLimit)
	{
		Iterator	streamIterator = m_audioInputStreamList.iterator();
		while (streamIterator.hasNext())
		{
			AudioInputStream	stream = (AudioInputStream) streamIterator.next();
			stream.mark(nReadLimit);
		}
	}


	/**
	   Calls reset() on all input streams.
	*/
	public void reset()
		throws	IOException
	{
		Iterator	streamIterator = m_audioInputStreamList.iterator();
		while (streamIterator.hasNext())
		{
			AudioInputStream	stream = (AudioInputStream) streamIterator.next();
			stream.reset();
		}
	}



	/**
	   returns true if all input stream return true for markSupported().
	*/
	public boolean markSupported()
	{
		Iterator	streamIterator = m_audioInputStreamList.iterator();
		while (streamIterator.hasNext())
		{
			AudioInputStream	stream = (AudioInputStream) streamIterator.next();
			if (! stream.markSupported())
			{
				return false;
			}
		}
		return true;
	}



	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
	
	public static void convertSign8(byte[] buffer, int byteOffset, int sampleCount) {
		sampleCount+=byteOffset;
		for (int i=byteOffset; i<sampleCount; i++) {
			buffer[i]+=128;
		}
	}

	public static void swapOrder16(byte[] buffer, int byteOffset, int sampleCount) {
		int byteMax=sampleCount*2+byteOffset-1;
		int i=byteOffset;
		while (i<byteMax) {
			byte h=buffer[i];
			buffer[i]=buffer[++i];
			buffer[i++]=h;
		}
	}

	public static void swapOrder24(byte[] buffer, int byteOffset, int sampleCount) {
		int byteMax=sampleCount*3+byteOffset-2;
		int i=byteOffset;
		while (i<byteMax) {
			byte h=buffer[i];
			buffer[i]=buffer[++i+1];
			buffer[++i]=h;
			i++;
		}
	}

	public static void swapOrder32(byte[] buffer, int byteOffset, int sampleCount) {
		int byteMax=sampleCount*4+byteOffset-3;
		int i=byteOffset;
		while (i<byteMax) {
			byte h=buffer[i];
			buffer[i]=buffer[i+3];
			buffer[i+3]=h;
			i++;
			h=buffer[i];
			buffer[i]=buffer[++i];
			buffer[i++]=h;
			i++;
		}
	}

	public static void convertSign8(byte[] inBuffer, int inByteOffset,
	                                byte[] outBuffer, int outByteOffset, int sampleCount) {
		while (sampleCount>0) {
			outBuffer[outByteOffset++]=(byte)(inBuffer[inByteOffset++]+128);
			sampleCount--;
		}
	}

	public static void swapOrder16(byte[] inBuffer, int inByteOffset,
	                               byte[] outBuffer, int outByteOffset, int sampleCount) {
		while (sampleCount>0) {
			outBuffer[outByteOffset++]=inBuffer[inByteOffset+1];
			outBuffer[outByteOffset++]=inBuffer[inByteOffset++];
			inByteOffset++;
			sampleCount--;
		}
	}

	public static void swapOrder24(byte[] inBuffer, int inByteOffset,
	                               byte[] outBuffer, int outByteOffset, int sampleCount) {
		while (sampleCount>0) {
			outBuffer[outByteOffset++]=inBuffer[inByteOffset+2];
			outByteOffset++;
			outBuffer[outByteOffset++]=inBuffer[inByteOffset++];
			inByteOffset++;
			inByteOffset++;
			sampleCount--;
		}
	}

	public static void swapOrder32(byte[] inBuffer, int inByteOffset,
	                               byte[] outBuffer, int outByteOffset, int sampleCount) {
		while (sampleCount>0) {
			outBuffer[outByteOffset++]=inBuffer[inByteOffset+3];
			outBuffer[outByteOffset++]=inBuffer[inByteOffset+2];
			outBuffer[outByteOffset++]=inBuffer[inByteOffset+1];
			outBuffer[outByteOffset++]=inBuffer[inByteOffset++];
			inByteOffset++;
			inByteOffset++;
			inByteOffset++;
			sampleCount--;
		}
	}


	///////////////// conversion functions for byte arrays ////////////////////////////


	/**
	 * Converts 2 bytes to a signed sample of type <code>short</code>.
	 * <p> This is a reference function.
	 */
	public static short bytesToShort16(byte highByte, byte lowByte) {
		return (short) ((highByte<<8) | (lowByte & 0xFF));
	}

	/**
	 * Converts 2 successive bytes starting at <code>byteOffset</code> in 
	 * <code>buffer</code> to a signed sample of type <code>short</code>.
	 * <p>
	 * For little endian, buffer[byteOffset] is interpreted as low byte,
	 * whereas it is interpreted as high byte in big endian.
	 * <p> This is a reference function.
	 */
	public static short bytesToShort16(byte[] buffer, int byteOffset, boolean bigEndian) {
		return bigEndian?
		       ((short) ((buffer[byteOffset]<<8) | (buffer[byteOffset+1] & 0xFF))):
		       ((short) ((buffer[byteOffset+1]<<8) | (buffer[byteOffset] & 0xFF)));
	}

	/**
	 * Converts 2 bytes to a signed integer sample with 16bit range.
	 * <p> This is a reference function.
	 */
	public static int bytesToInt16(byte highByte, byte lowByte) {
		return (highByte<<8) | (lowByte & 0xFF);
	}

	/**
	 * Converts 2 successive bytes starting at <code>byteOffset</code> in 
	 * <code>buffer</code> to a signed integer sample with 16bit range.
	 * <p>
	 * For little endian, buffer[byteOffset] is interpreted as low byte,
	 * whereas it is interpreted as high byte in big endian.
	 * <p> This is a reference function.
	 */
	public static int bytesToInt16(byte[] buffer, int byteOffset, boolean bigEndian) {
		return bigEndian?
		       ((buffer[byteOffset]<<8) | (buffer[byteOffset+1] & 0xFF)):
		       ((buffer[byteOffset+1]<<8) | (buffer[byteOffset] & 0xFF));
	}

	/**
	 * Converts 3 successive bytes starting at <code>byteOffset</code> in 
	 * <code>buffer</code> to a signed integer sample with 24bit range.
	 * <p>
	 * For little endian, buffer[byteOffset] is interpreted as lowest byte,
	 * whereas it is interpreted as highest byte in big endian.
	 * <p> This is a reference function.
	 */
	public static int bytesToInt24(byte[] buffer, int byteOffset, boolean bigEndian) {
		return bigEndian?
		       ((buffer[byteOffset]<<16)             // let Java handle sign-bit
		        | ((buffer[byteOffset+1] & 0xFF)<<8) // inhibit sign-bit handling
		        | (buffer[byteOffset+2] & 0xFF)):
		       ((buffer[byteOffset+2]<<16)           // let Java handle sign-bit
		        | ((buffer[byteOffset+1] & 0xFF)<<8) // inhibit sign-bit handling
		        | (buffer[byteOffset] & 0xFF));
	}

	/**
	 * Converts a 4 successive bytes starting at <code>byteOffset</code> in 
	 * <code>buffer</code> to a signed 32bit integer sample.
	 * <p>
	 * For little endian, buffer[byteOffset] is interpreted as lowest byte,
	 * whereas it is interpreted as highest byte in big endian.
	 * <p> This is a reference function.
	 */
	public static int bytesToInt32(byte[] buffer, int byteOffset, boolean bigEndian) {
		return bigEndian?
		       ((buffer[byteOffset]<<24)              // let Java handle sign-bit
		        | ((buffer[byteOffset+1] & 0xFF)<<16) // inhibit sign-bit handling
		        | ((buffer[byteOffset+2] & 0xFF)<<8)  // inhibit sign-bit handling
		        | (buffer[byteOffset+3] & 0xFF)):
		       ((buffer[byteOffset+3]<<24)            // let Java handle sign-bit
		        | ((buffer[byteOffset+2] & 0xFF)<<16) // inhibit sign-bit handling
		        | ((buffer[byteOffset+1] & 0xFF)<<8)  // inhibit sign-bit handling
		        | (buffer[byteOffset] & 0xFF));
	}


	/**
	 * Converts a sample of type <code>short</code> to 2 bytes in an array.
	 * <code>sample</code> is interpreted as signed (as Java does).
	 * <p>
	 * For little endian, buffer[byteOffset] is filled with low byte of sample, 
	 * and buffer[byteOffset+1] is filled with high byte of sample.
	 * <p> For big endian, this is reversed.
	 * <p> This is a reference function.
	 */
	public static void shortToBytes16(short sample, byte[] buffer, int byteOffset, boolean bigEndian) {
		intToBytes16(sample, buffer, byteOffset, bigEndian);
	}

	/**
	 * Converts a 16 bit sample of type <code>int</code> to 2 bytes in an array.
	 * <code>sample</code> is interpreted as signed (as Java does).
	 * <p>
	 * For little endian, buffer[byteOffset] is filled with low byte of sample, 
	 * and buffer[byteOffset+1] is filled with high byte of sample + sign bit.
	 * <p> For big endian, this is reversed.
	 * <p> Before calling this function, it should be assured that <code>sample</code>
	 * is in the 16bit range - it will not be clipped.
	 * <p> This is a reference function.
	 */
	public static void intToBytes16(int sample, byte[] buffer, int byteOffset, boolean bigEndian) {
		if (bigEndian) {
			buffer[byteOffset++]=(byte) (sample >> 8);
			buffer[byteOffset]=(byte) (sample & 0xFF);
		} else {
			buffer[byteOffset++]=(byte) (sample & 0xFF);
			buffer[byteOffset]=(byte) (sample >> 8);
		}
	}

	/**
	 * Converts a 24 bit sample of type <code>int</code> to 3 bytes in an array.
	 * <code>sample</code> is interpreted as signed (as Java does).
	 * <p>
	 * For little endian, buffer[byteOffset] is filled with low byte of sample, 
	 * and buffer[byteOffset+2] is filled with the high byte of sample + sign bit.
	 * <p> For big endian, this is reversed.
	 * <p> Before calling this function, it should be assured that <code>sample</code>
	 * is in the 24bit range - it will not be clipped.
	 * <p> This is a reference function.
	 */
	public static void intToBytes24(int sample, byte[] buffer, int byteOffset, boolean bigEndian) {
		if (bigEndian) {
			buffer[byteOffset++]=(byte) (sample >> 16);
			buffer[byteOffset++]=(byte) ((sample >>> 8) & 0xFF);
			buffer[byteOffset]=(byte) (sample & 0xFF);
		} else {
			buffer[byteOffset++]=(byte) (sample & 0xFF);
			buffer[byteOffset++]=(byte) ((sample >>> 8) & 0xFF);
			buffer[byteOffset]=(byte) (sample >> 16);
		}
	}


	/**
	 * Converts a 32 bit sample of type <code>int</code> to 4 bytes in an array.
	 * <code>sample</code> is interpreted as signed (as Java does).
	 * <p>
	 * For little endian, buffer[byteOffset] is filled with lowest byte of sample, 
	 * and buffer[byteOffset+3] is filled with the high byte of sample + sign bit.
	 * <p> For big endian, this is reversed.
	 * <p> This is a reference function.
	 */
	public static void intToBytes32(int sample, byte[] buffer, int byteOffset, boolean bigEndian) {
		if (bigEndian) {
			buffer[byteOffset++]=(byte) (sample >> 24);
			buffer[byteOffset++]=(byte) ((sample >>> 16) & 0xFF);
			buffer[byteOffset++]=(byte) ((sample >>> 8) & 0xFF);
			buffer[byteOffset]=(byte) (sample & 0xFF);
		} else {
			buffer[byteOffset++]=(byte) (sample & 0xFF);
			buffer[byteOffset++]=(byte) ((sample >>> 8) & 0xFF);
			buffer[byteOffset++]=(byte) ((sample >>> 16) & 0xFF);
			buffer[byteOffset]=(byte) (sample >> 24);
		}
	}


	/////////////////////// ULAW ///////////////////////////////////////////

	private static final boolean ZEROTRAP=true;
	private static final short BIAS=0x84;
	private static final int CLIP=32635;
	private static final int exp_lut1[] ={
	    0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,
	    4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
	    5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
	    5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
	    6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
	    6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
	    6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
	    6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
	    7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	    7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	    7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	    7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	    7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	    7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	    7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	    7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7
	};


	/**
	 * Converts a linear signed 16bit sample to a uLaw byte.
	 * Ported to Java by fb.
	 * <BR>Originally by:<BR>
	 * Craig Reese: IDA/Supercomputing Research Center <BR>
	 * Joe Campbell: Department of Defense <BR>
	 * 29 September 1989 <BR>
	 */
	public static byte linear2ulaw(int sample) {
		int sign, exponent, mantissa, ulawbyte;

		if (sample>32767) sample=32767;
	else if (sample<-32768) sample=-32768;
		/* Get the sample into sign-magnitude. */
		sign = (sample >> 8) & 0x80;    /* set aside the sign */
		if (sign != 0) sample = -sample;    /* get magnitude */
		if (sample > CLIP) sample = CLIP;    /* clip the magnitude */

		/* Convert from 16 bit linear to ulaw. */
		sample = sample + BIAS;
		exponent = exp_lut1[(sample >> 7) & 0xFF];
		mantissa = (sample >> (exponent + 3)) & 0x0F;
		ulawbyte = ~(sign | (exponent << 4) | mantissa);
		if (ZEROTRAP)
			if (ulawbyte == 0) ulawbyte = 0x02;  /* optional CCITT trap */
		return((byte) ulawbyte);
	}

	/* u-law to linear conversion table */
	private static short[] u2l = {
	    -32124, -31100, -30076, -29052, -28028, -27004, -25980, -24956,
	    -23932, -22908, -21884, -20860, -19836, -18812, -17788, -16764,
	    -15996, -15484, -14972, -14460, -13948, -13436, -12924, -12412,
	    -11900, -11388, -10876, -10364, -9852, -9340, -8828, -8316,
	    -7932, -7676, -7420, -7164, -6908, -6652, -6396, -6140,
	    -5884, -5628, -5372, -5116, -4860, -4604, -4348, -4092,
	    -3900, -3772, -3644, -3516, -3388, -3260, -3132, -3004,
	    -2876, -2748, -2620, -2492, -2364, -2236, -2108, -1980,
	    -1884, -1820, -1756, -1692, -1628, -1564, -1500, -1436,
	    -1372, -1308, -1244, -1180, -1116, -1052, -988, -924,
	    -876, -844, -812, -780, -748, -716, -684, -652,
	    -620, -588, -556, -524, -492, -460, -428, -396,
	    -372, -356, -340, -324, -308, -292, -276, -260,
	    -244, -228, -212, -196, -180, -164, -148, -132,
	    -120, -112, -104, -96, -88, -80, -72, -64,
	    -56, -48, -40, -32, -24, -16, -8, 0,
	    32124, 31100, 30076, 29052, 28028, 27004, 25980, 24956,
	    23932, 22908, 21884, 20860, 19836, 18812, 17788, 16764,
	    15996, 15484, 14972, 14460, 13948, 13436, 12924, 12412,
	    11900, 11388, 10876, 10364, 9852, 9340, 8828, 8316,
	    7932, 7676, 7420, 7164, 6908, 6652, 6396, 6140,
	    5884, 5628, 5372, 5116, 4860, 4604, 4348, 4092,
	    3900, 3772, 3644, 3516, 3388, 3260, 3132, 3004,
	    2876, 2748, 2620, 2492, 2364, 2236, 2108, 1980,
	    1884, 1820, 1756, 1692, 1628, 1564, 1500, 1436,
	    1372, 1308, 1244, 1180, 1116, 1052, 988, 924,
	    876, 844, 812, 780, 748, 716, 684, 652,
	    620, 588, 556, 524, 492, 460, 428, 396,
	    372, 356, 340, 324, 308, 292, 276, 260,
	    244, 228, 212, 196, 180, 164, 148, 132,
	    120, 112, 104, 96, 88, 80, 72, 64,
	    56, 48, 40, 32, 24, 16, 8, 0
	};
	public static short ulaw2linear(byte ulawbyte) {
		return u2l[ulawbyte & 0xFF];
	}



	/**
	 * Converts a buffer of signed 16bit big endian samples to uLaw.
	 * The uLaw bytes overwrite the original 16 bit values.
	 * The first byte-offset of the uLaw bytes is byteOffset.
	 * It will be written sampleCount/2 bytes.
	 */
	public static void pcm162ulaw(byte[] buffer, int byteOffset, int sampleCount, boolean bigEndian) {
		int shortIndex=byteOffset;
		int ulawIndex=shortIndex;
		if (bigEndian) {
			while (sampleCount>0) {
				buffer[ulawIndex++]=linear2ulaw
				                    (bytesToInt16(buffer[shortIndex], buffer[shortIndex+1]));
				shortIndex++;
				shortIndex++;
				sampleCount--;
			}
		} else {
			while (sampleCount>0) {
				buffer[ulawIndex++]=linear2ulaw
				                    (bytesToInt16(buffer[shortIndex+1], buffer[shortIndex]));
				shortIndex++;
				shortIndex++;
				sampleCount--;
			}
		}
	}

	/**
	 * Fills outBuffer with ulaw samples.
	 * reading starts from inBuffer[inByteOffset].
	 * writing starts at outBuffer[outByteOffset].
	 * There will be sampleCount*2 bytes read from inBuffer;
	 * There will be sampleCount <B>bytes</B> written to outBuffer.
	 */
	public static void pcm162ulaw(byte[] inBuffer, int inByteOffset,
	                              byte[] outBuffer, int outByteOffset,
	                              int sampleCount, boolean bigEndian) {
		int shortIndex=inByteOffset;
		int ulawIndex=outByteOffset;
		if (bigEndian) {
			while (sampleCount>0) {
				outBuffer[ulawIndex++]=linear2ulaw
				                       (bytesToInt16(inBuffer[shortIndex], inBuffer[shortIndex+1]));
				shortIndex++;
				shortIndex++;
				sampleCount--;
			}
		} else {
			while (sampleCount>0) {
				outBuffer[ulawIndex++]=linear2ulaw
				                       (bytesToInt16(inBuffer[shortIndex+1], inBuffer[shortIndex]));
				shortIndex++;
				shortIndex++;
				sampleCount--;
			}
		}
	}

	// TODO: either direct 8bit pcm to ulaw, or better conversion from 8bit to 16bit
	/**
	 * Converts a buffer of 8bit samples to uLaw.
	 * The uLaw bytes overwrite the original 8 bit values.
	 * The first byte-offset of the uLaw bytes is byteOffset.
	 * It will be written sampleCount bytes.
	 */
	public static void pcm82ulaw(byte[] buffer, int byteOffset, int sampleCount, boolean signed) {
		sampleCount+=byteOffset;
		if (signed) {
			for (int i=byteOffset; i<sampleCount; i++) {
				buffer[i]=linear2ulaw(buffer[i] << 8);
			}
		} else {
			for (int i=byteOffset; i<sampleCount; i++) {
				buffer[i]=linear2ulaw(((byte) (buffer[i]+128)) << 8);
			}
		}
	}

	/**
	 * Fills outBuffer with ulaw samples.
	 * reading starts from inBuffer[inByteOffset].
	 * writing starts at outBuffer[outByteOffset].
	 * There will be sampleCount <B>bytes</B> written to outBuffer.
	 */
	public static void pcm82ulaw(byte[] inBuffer, int inByteOffset,
	                             byte[] outBuffer, int outByteOffset, int sampleCount, boolean signed) {
		int ulawIndex=outByteOffset;
		int pcmIndex=inByteOffset;
		if (signed) {
			while (sampleCount>0) {
				outBuffer[ulawIndex++]=linear2ulaw(inBuffer[pcmIndex++] << 8);
				sampleCount--;
			}
		} else {
			while (sampleCount>0) {
				outBuffer[ulawIndex++]=linear2ulaw(((byte) (inBuffer[pcmIndex++]+128)) << 8);
				sampleCount--;
			}
		}
	}

	/**
	* Fills outBuffer with pcm signed 16 bit samples.
	* reading starts from inBuffer[inByteOffset].
	* writing starts at outBuffer[outByteOffset].
	* There will be sampleCount bytes read from inBuffer;
	* There will be sampleCount*2 bytes written to outBuffer.
	*/
	public static void ulaw2pcm16(byte[] inBuffer, int inByteOffset,
	                              byte[] outBuffer, int outByteOffset,
	                              int sampleCount, boolean bigEndian) {
		int shortIndex=outByteOffset;
		int ulawIndex=inByteOffset;
		while (sampleCount>0) {
			intToBytes16
			(u2l[inBuffer[ulawIndex++] & 0xFF], outBuffer, shortIndex++, bigEndian);
			shortIndex++;
			sampleCount--;
		}
	}


	// TODO: either direct 8bit pcm to ulaw, or better conversion from 8bit to 16bit
	/**
	 * Inplace-conversion of a ulaw buffer to 8bit samples.
	 * The 8bit bytes overwrite the original ulaw values.
	 * The first byte-offset of the uLaw bytes is byteOffset.
	 * It will be written sampleCount bytes.
	 */
	public static void ulaw2pcm8(byte[] buffer, int byteOffset, int sampleCount, boolean signed) {
		sampleCount+=byteOffset;
		if (signed) {
			for (int i=byteOffset; i<sampleCount; i++) {
				buffer[i]=(byte) ((u2l[buffer[i] & 0xFF] >> 8) & 0xFF);
			}
		} else {
			for (int i=byteOffset; i<sampleCount; i++) {
				buffer[i]=(byte) ((u2l[buffer[i] & 0xFF]>>8)+128);
			}
		}
	}

	/**
	 * Fills outBuffer with ulaw samples.
	 * reading starts from inBuffer[inByteOffset].
	 * writing starts at outBuffer[outByteOffset].
	 * There will be sampleCount <B>bytes</B> written to outBuffer.
	 */
	public static void ulaw2pcm8(byte[] inBuffer, int inByteOffset,
	                             byte[] outBuffer, int outByteOffset, int sampleCount, boolean signed) {
		int ulawIndex=inByteOffset;
		int pcmIndex=outByteOffset;
		if (signed) {
			while (sampleCount>0) {
				outBuffer[pcmIndex++]=
				    (byte) ((u2l[inBuffer[ulawIndex++] & 0xFF] >> 8) & 0xFF);
				sampleCount--;
			}
		} else {
			while (sampleCount>0) {
				outBuffer[pcmIndex++]=
				    (byte) ((u2l[inBuffer[ulawIndex++] & 0xFF]>>8)+128);
				sampleCount--;
			}
		}
	}


	//////////////////// ALAW ////////////////////////////


	/*
	 * This source code is a product of Sun Microsystems, Inc. and is provided
	 * for unrestricted use.  Users may copy or modify this source code without
	 * charge.
	 *
	 * linear2alaw() - Convert a 16-bit linear PCM value to 8-bit A-law
	 *
	 * linear2alaw() accepts an 16-bit integer and encodes it as A-law data.
	 *
	 *		Linear Input Code	Compressed Code
	 *	------------------------	---------------
	 *	0000000wxyza			000wxyz
	 *	0000001wxyza			001wxyz
	 *	000001wxyzab			010wxyz
	 *	00001wxyzabc			011wxyz
	 *	0001wxyzabcd			100wxyz
	 *	001wxyzabcde			101wxyz
	 *	01wxyzabcdef			110wxyz
	 *	1wxyzabcdefg			111wxyz
	 *
	 * For further information see John C. Bellamy's Digital Telephony, 1982,
	 * John Wiley & Sons, pps 98-111 and 472-476.
	 */
	private static final byte QUANT_MASK = 0xf;		/* Quantization field mask. */
	private static final byte SEG_SHIFT = 4;		/* Left shift for segment number. */
	private static final short[] seg_end = {
	    0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF
	};

	public static byte linear2alaw(short pcm_val)	/* 2's complement (16-bit range) */
	{
		byte	mask;
		byte	seg=8;
		byte	aval;

		if (pcm_val >= 0) {
			mask = (byte) 0xD5;		/* sign (7th) bit = 1 */
		} else {
			mask = 0x55;		/* sign bit = 0 */
			pcm_val = (short) (-pcm_val - 8);
		}

		/* Convert the scaled magnitude to segment number. */
		for (int i = 0; i < 8; i++) {
			if (pcm_val <= seg_end[i]) {
				seg=(byte) i;
				break;
			}
		}

		/* Combine the sign, segment, and quantization bits. */
		if (seg >= 8)		/* out of range, return maximum value. */
			return (byte) ((0x7F ^ mask) & 0xFF);
		else {
			aval = (byte) (seg << SEG_SHIFT);
			if (seg < 2)
				aval |= (pcm_val >> 4) & QUANT_MASK;
			else
				aval |= (pcm_val >> (seg + 3)) & QUANT_MASK;
			return (byte) ((aval ^ mask) & 0xFF);
		}
	}

	private static short[] a2l = {
	    -5504, -5248, -6016, -5760, -4480, -4224, -4992, -4736,
	    -7552, -7296, -8064, -7808, -6528, -6272, -7040, -6784,
	    -2752, -2624, -3008, -2880, -2240, -2112, -2496, -2368,
	    -3776, -3648, -4032, -3904, -3264, -3136, -3520, -3392,
	    -22016, -20992, -24064, -23040, -17920, -16896, -19968, -18944,
	    -30208, -29184, -32256, -31232, -26112, -25088, -28160, -27136,
	    -11008, -10496, -12032, -11520, -8960, -8448, -9984, -9472,
	    -15104, -14592, -16128, -15616, -13056, -12544, -14080, -13568,
	    -344, -328, -376, -360, -280, -264, -312, -296,
	    -472, -456, -504, -488, -408, -392, -440, -424,
	    -88, -72, -120, -104, -24, -8, -56, -40,
	    -216, -200, -248, -232, -152, -136, -184, -168,
	    -1376, -1312, -1504, -1440, -1120, -1056, -1248, -1184,
	    -1888, -1824, -2016, -1952, -1632, -1568, -1760, -1696,
	    -688, -656, -752, -720, -560, -528, -624, -592,
	    -944, -912, -1008, -976, -816, -784, -880, -848,
	    5504, 5248, 6016, 5760, 4480, 4224, 4992, 4736,
	    7552, 7296, 8064, 7808, 6528, 6272, 7040, 6784,
	    2752, 2624, 3008, 2880, 2240, 2112, 2496, 2368,
	    3776, 3648, 4032, 3904, 3264, 3136, 3520, 3392,
	    22016, 20992, 24064, 23040, 17920, 16896, 19968, 18944,
	    30208, 29184, 32256, 31232, 26112, 25088, 28160, 27136,
	    11008, 10496, 12032, 11520, 8960, 8448, 9984, 9472,
	    15104, 14592, 16128, 15616, 13056, 12544, 14080, 13568,
	    344, 328, 376, 360, 280, 264, 312, 296,
	    472, 456, 504, 488, 408, 392, 440, 424,
	    88, 72, 120, 104, 24, 8, 56, 40,
	    216, 200, 248, 232, 152, 136, 184, 168,
	    1376, 1312, 1504, 1440, 1120, 1056, 1248, 1184,
	    1888, 1824, 2016, 1952, 1632, 1568, 1760, 1696,
	    688, 656, 752, 720, 560, 528, 624, 592,
	    944, 912, 1008, 976, 816, 784, 880, 848
	};

	public static short alaw2linear(byte ulawbyte) {
		return a2l[ulawbyte & 0xFF];
	}

	/**
	 * Converts a buffer of signed 16bit big endian samples to uLaw.
	 * The uLaw bytes overwrite the original 16 bit values.
	 * The first byte-offset of the uLaw bytes is byteOffset.
	 * It will be written sampleCount/2 bytes.
	 */
	public static void pcm162alaw(byte[] buffer, int byteOffset, int sampleCount, boolean bigEndian) {
		int shortIndex=byteOffset;
		int alawIndex=shortIndex;
		if (bigEndian) {
			while (sampleCount>0) {
				buffer[alawIndex++]=
				    linear2alaw(bytesToShort16
				                (buffer[shortIndex], buffer[shortIndex+1]));
				shortIndex++;
				shortIndex++;
				sampleCount--;
			}
		} else {
			while (sampleCount>0) {
				buffer[alawIndex++]=
				    linear2alaw(bytesToShort16
				                (buffer[shortIndex+1], buffer[shortIndex]));
				shortIndex++;
				shortIndex++;
				sampleCount--;
			}
		}
	}

	/**
	 * Fills outBuffer with alaw samples.
	 * reading starts from inBuffer[inByteOffset].
	 * writing starts at outBuffer[outByteOffset].
	 * There will be sampleCount*2 bytes read from inBuffer;
	 * There will be sampleCount <B>bytes</B> written to outBuffer.
	 */
	public static void pcm162alaw(byte[] inBuffer, int inByteOffset,
	                              byte[] outBuffer, int outByteOffset, int sampleCount, boolean bigEndian) {
		int shortIndex=inByteOffset;
		int alawIndex=outByteOffset;
		if (bigEndian) {
			while (sampleCount>0) {
				outBuffer[alawIndex++]=linear2alaw
				                       (bytesToShort16(inBuffer[shortIndex], inBuffer[shortIndex+1]));
				shortIndex++;
				shortIndex++;
				sampleCount--;
			}
		} else {
			while (sampleCount>0) {
				outBuffer[alawIndex++]=linear2alaw
				                       (bytesToShort16(inBuffer[shortIndex+1], inBuffer[shortIndex]));
				shortIndex++;
				shortIndex++;
				sampleCount--;
			}
		}
	}

	/**
	 * Converts a buffer of 8bit samples to alaw.
	 * The alaw bytes overwrite the original 8 bit values.
	 * The first byte-offset of the aLaw bytes is byteOffset.
	 * It will be written sampleCount bytes.
	 */
	public static void pcm82alaw(byte[] buffer, int byteOffset, int sampleCount, boolean signed) {
		sampleCount+=byteOffset;
		if (signed) {
			for (int i=byteOffset; i<sampleCount; i++) {
				buffer[i]=linear2alaw((short) (buffer[i] << 8));
			}
		} else {
			for (int i=byteOffset; i<sampleCount; i++) {
				buffer[i]=linear2alaw((short) (((byte) (buffer[i]+128)) << 8));
			}
		}
	}

	/**
	 * Fills outBuffer with alaw samples.
	 * reading starts from inBuffer[inByteOffset].
	 * writing starts at outBuffer[outByteOffset].
	 * There will be sampleCount <B>bytes</B> written to outBuffer.
	 */
	public static void pcm82alaw(byte[] inBuffer, int inByteOffset,
	                             byte[] outBuffer, int outByteOffset, int sampleCount, boolean signed) {
		int alawIndex=outByteOffset;
		int pcmIndex=inByteOffset;
		if (signed) {
			while (sampleCount>0) {
				outBuffer[alawIndex++]=
				    linear2alaw((short) (inBuffer[pcmIndex++] << 8));
				sampleCount--;
			}
		} else {
			while (sampleCount>0) {
				outBuffer[alawIndex++]=
				    linear2alaw((short) (((byte) (inBuffer[pcmIndex++]+128)) << 8));
				sampleCount--;
			}
		}
	}



	/**
	 * Converts an alaw buffer to 8bit pcm samples
	 * The 8bit bytes overwrite the original alaw values.
	 * The first byte-offset of the aLaw bytes is byteOffset.
	 * It will be written sampleCount bytes.
	 */
	public static void alaw2pcm8(byte[] buffer, int byteOffset, int sampleCount, boolean signed) {
		sampleCount+=byteOffset;
		if (signed) {
			for (int i=byteOffset; i<sampleCount; i++) {
				buffer[i]=(byte) ((a2l[buffer[i] & 0xFF] >> 8) & 0xFF);
			}
		} else {
			for (int i=byteOffset; i<sampleCount; i++) {
				buffer[i]=(byte) ((a2l[buffer[i] & 0xFF]>>8)+128);
			}
		}
	}

	/**
	 * Fills outBuffer with alaw samples.
	 * reading starts from inBuffer[inByteOffset].
	 * writing starts at outBuffer[outByteOffset].
	 * There will be sampleCount <B>bytes</B> written to outBuffer.
	 */
	public static void alaw2pcm8(byte[] inBuffer, int inByteOffset,
	                             byte[] outBuffer, int outByteOffset, int sampleCount, boolean signed) {
		int alawIndex=inByteOffset;
		int pcmIndex=outByteOffset;
		if (signed) {
			while (sampleCount>0) {
				outBuffer[pcmIndex++]=
				    (byte) ((a2l[inBuffer[alawIndex++] & 0xFF] >> 8) & 0xFF);
				sampleCount--;
			}
		} else {
			while (sampleCount>0) {
				outBuffer[pcmIndex++]=
				    (byte) ((a2l[inBuffer[alawIndex++] & 0xFF]>>8)+128);
				sampleCount--;
			}
		}
	}

	/**
	 * Fills outBuffer with pcm signed 16 bit samples.
	 * reading starts from inBuffer[inByteOffset].
	 * writing starts at outBuffer[outByteOffset].
	 * There will be sampleCount bytes read from inBuffer;
	 * There will be sampleCount*2 bytes written to outBuffer.
	 */
	public static void alaw2pcm16(byte[] inBuffer, int inByteOffset,
	                              byte[] outBuffer, int outByteOffset,
	                              int sampleCount, boolean bigEndian) {
		int shortIndex=outByteOffset;
		int alawIndex=inByteOffset;
		while (sampleCount>0) {
			intToBytes16
			(a2l[inBuffer[alawIndex++] & 0xFF], outBuffer, shortIndex++, bigEndian);
			shortIndex++;
			sampleCount--;
		}
	}

	//////////////////////// cross conversion alaw <-> ulaw ////////////////////////////////////////

	private static byte[] u2a = {
	    -86, -85, -88, -87, -82, -81, -84, -83, -94, -93, -96, -95, -90, -89, -92, -91,
	    -70, -69, -72, -71, -66, -65, -68, -67, -78, -77, -80, -79, -74, -73, -76, -75,
	    -118, -117, -120, -119, -114, -113, -116, -115, -126, -125, -128, -127, -122, -121, -124, -123,
	    -101, -104, -103, -98, -97, -100, -99, -110, -109, -112, -111, -106, -105, -108, -107, -22,
	    -24, -23, -18, -17, -20, -19, -30, -29, -32, -31, -26, -25, -28, -27, -6, -8,
	    -2, -1, -4, -3, -14, -13, -16, -15, -10, -9, -12, -11, -53, -55, -49, -51,
	    -62, -61, -64, -63, -58, -57, -60, -59, -38, -37, -40, -39, -34, -33, -36, -35,
	    -46, -46, -45, -45, -48, -48, -47, -47, -42, -42, -41, -41, -44, -44, -43, -43,
	    42, 43, 40, 41, 46, 47, 44, 45, 34, 35, 32, 33, 38, 39, 36, 37,
	    58, 59, 56, 57, 62, 63, 60, 61, 50, 51, 48, 49, 54, 55, 52, 53,
	    10, 11, 8, 9, 14, 15, 12, 13, 2, 3, 0, 1, 6, 7, 4, 5,
	    27, 24, 25, 30, 31, 28, 29, 18, 19, 16, 17, 22, 23, 20, 21, 106,
	    104, 105, 110, 111, 108, 109, 98, 99, 96, 97, 102, 103, 100, 101, 122, 120,
	    126, 127, 124, 125, 114, 115, 112, 113, 118, 119, 116, 117, 75, 73, 79, 77,
	    66, 67, 64, 65, 70, 71, 68, 69, 90, 91, 88, 89, 94, 95, 92, 93,
	    82, 82, 83, 83, 80, 80, 81, 81, 86, 86, 87, 87, 84, 84, 85, 85,
	};

	public static byte ulaw2alaw(byte sample) {
		return u2a[sample & 0xFF];
	}

	/**
	 * Converts a buffer of uLaw samples to aLaw.
	 */
	public static void ulaw2alaw(byte[] buffer, int byteOffset, int sampleCount) {
		sampleCount+=byteOffset;
		for (int i=byteOffset; i<sampleCount; i++) {
			buffer[i]=u2a[buffer[i] & 0xFF];
		}
	}

	/**
	 * Fills outBuffer with alaw samples.
	 */
	public static void ulaw2alaw(byte[] inBuffer, int inByteOffset,
	                             byte[] outBuffer, int outByteOffset, int sampleCount) {
		int ulawIndex=outByteOffset;
		int alawIndex=inByteOffset;
		while (sampleCount>0) {
			outBuffer[alawIndex++]=u2a[inBuffer[ulawIndex++] & 0xFF];
			sampleCount--;
		}
	}

	private static byte[] a2u = {
	    -86, -85, -88, -87, -82, -81, -84, -83, -94, -93, -96, -95, -90, -89, -92, -91,
	    -71, -70, -73, -72, -67, -66, -69, -68, -79, -78, -80, -80, -75, -74, -77, -76,
	    -118, -117, -120, -119, -114, -113, -116, -115, -126, -125, -128, -127, -122, -121, -124, -123,
	    -102, -101, -104, -103, -98, -97, -100, -99, -110, -109, -112, -111, -106, -105, -108, -107,
	    -30, -29, -32, -31, -26, -25, -28, -27, -35, -35, -36, -36, -33, -33, -34, -34,
	    -12, -10, -16, -14, -4, -2, -8, -6, -22, -21, -24, -23, -18, -17, -20, -19,
	    -56, -55, -58, -57, -52, -51, -54, -53, -64, -63, -65, -65, -60, -59, -62, -61,
	    -42, -41, -44, -43, -38, -37, -40, -39, -49, -49, -50, -50, -46, -45, -48, -47,
	    42, 43, 40, 41, 46, 47, 44, 45, 34, 35, 32, 33, 38, 39, 36, 37,
	    57, 58, 55, 56, 61, 62, 59, 60, 49, 50, 48, 48, 53, 54, 51, 52,
	    10, 11, 8, 9, 14, 15, 12, 13, 2, 3, 0, 1, 6, 7, 4, 5,
	    26, 27, 24, 25, 30, 31, 28, 29, 18, 19, 16, 17, 22, 23, 20, 21,
	    98, 99, 96, 97, 102, 103, 100, 101, 93, 93, 92, 92, 95, 95, 94, 94,
	    116, 118, 112, 114, 124, 126, 120, 122, 106, 107, 104, 105, 110, 111, 108, 109,
	    72, 73, 70, 71, 76, 77, 74, 75, 64, 65, 63, 63, 68, 69, 66, 67,
	    86, 87, 84, 85, 90, 91, 88, 89, 79, 79, 78, 78, 82, 83, 80, 81,
	};

	public static byte alaw2ulaw(byte sample) {
		return a2u[sample & 0xFF];
	}

	/**
	 * Converts a buffer of aLaw samples to uLaw.
	 * The uLaw bytes overwrite the original aLaw values.
	 * The first byte-offset of the uLaw bytes is byteOffset.
	 * It will be written sampleCount bytes.
	 */
	public static void alaw2ulaw(byte[] buffer, int byteOffset, int sampleCount) {
		sampleCount+=byteOffset;
		for (int i=byteOffset; i<sampleCount; i++) {
			buffer[i]=a2u[buffer[i] & 0xFF];
		}
	}

	/**
	 * Fills outBuffer with ulaw samples.
	 * reading starts from inBuffer[inByteOffset].
	 * writing starts at outBuffer[outByteOffset].
	 * There will be sampleCount <B>bytes</B> written to outBuffer.
	 */
	public static void alaw2ulaw(byte[] inBuffer, int inByteOffset,
	                             byte[] outBuffer, int outByteOffset, int sampleCount) {
		int ulawIndex=outByteOffset;
		int alawIndex=inByteOffset;
		while (sampleCount>0) {
			outBuffer[ulawIndex++]=a2u[inBuffer[alawIndex++] & 0xFF];
			sampleCount--;
		}
	}


	//////////////////////// high level methods /////////////////////////////////////////////////

	/*
	 *	!! Here, unlike other functions in this class, the length is
	 *	in bytes rather than samples !!
	 */
	public static void changeOrderOrSign(byte[] buffer, int nOffset,
	                                     int nByteLength, int nBytesPerSample) {
		switch (nBytesPerSample) {
		case 1:
			convertSign8(buffer, nOffset, nByteLength);
			break;

		case 2:
			swapOrder16(buffer, nOffset, nByteLength / 2);
			break;

		case 3:
			swapOrder24(buffer, nOffset, nByteLength / 3);
			break;

		case 4:
			swapOrder32(buffer, nOffset, nByteLength / 4);
			break;
		}
	}



	/*
	 *	!! Here, unlike other functions in this class, the length is
	 *	in bytes rather than samples !!
	 */
	public static void changeOrderOrSign(
	    byte[] inBuffer, int nInOffset,
	    byte[] outBuffer, int nOutOffset,
	    int nByteLength, int nBytesPerSample) {
		switch (nBytesPerSample) {
		case 1:
			convertSign8(
			    inBuffer, nInOffset,
			    outBuffer, nOutOffset,
			    nByteLength);
			break;

		case 2:
			swapOrder16(
			    inBuffer, nInOffset,
			    outBuffer, nOutOffset,
			    nByteLength / 2);
			break;

		case 3:
			swapOrder24(
			    inBuffer, nInOffset,
			    outBuffer, nOutOffset,
			    nByteLength / 3);
			break;

		case 4:
			swapOrder32(
			    inBuffer, nInOffset,
			    outBuffer, nOutOffset,
			    nByteLength / 4);
			break;
		}
	}
}



/*** MixingAudioInputStream.java ***/

