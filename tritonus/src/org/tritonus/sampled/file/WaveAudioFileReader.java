/*
 *	WaveAudioFileReader.java
 */

/*
 *  Copyright (c) 1999,2000 by Florian Bomers <florian@bome.com>
 *  Copyright (c) 1999 by Matthias Pfisterer <Matthias.Pfisterer@gmx.de>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */


package	org.tritonus.sampled.file;


import	java.io.DataInputStream;
import	java.io.File;
import	java.io.InputStream;
import	java.io.IOException;

import	javax.sound.sampled.AudioSystem;
import	javax.sound.sampled.AudioFormat;
import	javax.sound.sampled.AudioFileFormat;
import	javax.sound.sampled.AudioInputStream;
import	javax.sound.sampled.UnsupportedAudioFileException;
import	javax.sound.sampled.spi.AudioFileReader;

import	org.tritonus.TDebug;


/**
 * Class for reading wave files.
 *
 * @author Florian Bomers
 * @author Matthias Pfisterer
 */

public class WaveAudioFileReader extends TAudioFileReader {

	protected void advanceChunk(DataInputStream dis, long prevLength, long prevRead)
	throws IOException {
		if (prevLength>0) {
			dis.skip(((prevLength+1) & 0xFFFFFFFE)-prevRead);
		}
	}


	protected long findChunk(DataInputStream dis, int key)
	throws UnsupportedAudioFileException, IOException {
		// $$fb 1999-12-18: we should take care that we don't exceed
		// the mark of this stream. When we exceeded the mark and
		// we notice that we don't support this wave file,
		// other potential wave file readers have no chance.
		int thisKey;
		long chunkLength=0;
		do {
			advanceChunk(dis, chunkLength, 0);
			try {
				thisKey = dis.readInt();
			} catch (IOException e) {
				// $$fb: when we come here, we skipped past the end of the wave file
				// without finding the chunk.
				// IMHO, this is not an IOException, as there are incarnations
				// of WAVE files which store data in different chunks.
				// maybe we can find a nice description of the "required chunk" ?
				throw new UnsupportedAudioFileException("unsupported WAVE file: required chunk not found.");
			}
			chunkLength = readLittleEndianInt(dis) & 0xFFFFFFFF; // unsigned
		}
		while (thisKey != key);
		return chunkLength;
	}

	protected AudioFormat readFormatChunk(DataInputStream dis,
	                                      long chunkLength) throws UnsupportedAudioFileException, IOException {
		String debugAdd="";

		int read=WaveTool.MIN_FMT_CHUNK_LENGTH;

		if (chunkLength<WaveTool.MIN_FMT_CHUNK_LENGTH) {
			throw new UnsupportedAudioFileException(
			    "corrupt WAVE file: format chunk is too small");
		}

		short formatCode=readLittleEndianShort(dis);
		short channelCount = readLittleEndianShort(dis);
		if (channelCount <= 0) {
			throw new UnsupportedAudioFileException(
			    "corrupt WAVE file: number of channels must be positive");
		}

		int sampleRate = readLittleEndianInt(dis);
		if (sampleRate <= 0) {
			throw new UnsupportedAudioFileException(
			    "corrupt WAVE file: sample rate must be positive");
		}

		int avgBytesPerSecond=readLittleEndianInt(dis);
		int blockAlign=readLittleEndianShort(dis);

		AudioFormat.Encoding encoding;
		int sampleSizeInBits;
		int frameSize=0;
		float frameRate=(float) sampleRate;

		switch (formatCode) {
		case WaveTool.WAVE_FORMAT_PCM:
			if (chunkLength<WaveTool.MIN_FMT_CHUNK_LENGTH+2) {
				throw new UnsupportedAudioFileException(
				    "corrupt WAVE file: format chunk is too small");
			}
			sampleSizeInBits = readLittleEndianShort(dis);
			if (sampleSizeInBits <= 0) {
				throw new UnsupportedAudioFileException(
				    "corrupt WAVE file: sample size must be positive");
			}
			encoding = (sampleSizeInBits <= 8) ?
			           WaveTool.PCM_UNSIGNED : WaveTool.PCM_SIGNED;
			if (TDebug.TraceAudioFileReader) {
				debugAdd+=", wBitsPerSample="+sampleSizeInBits;
			}
			read+=2;
			break;
		case WaveTool.WAVE_FORMAT_ALAW:
			sampleSizeInBits = 8;
			encoding = WaveTool.ALAW;
			break;
		case WaveTool.WAVE_FORMAT_ULAW:
			sampleSizeInBits = 8;
			encoding = WaveTool.ULAW;
			break;
		case WaveTool.WAVE_FORMAT_GSM610:
			if (chunkLength<WaveTool.MIN_FMT_CHUNK_LENGTH+6) {
				throw new UnsupportedAudioFileException(
				    "corrupt WAVE file: extra GSM bytes are missing");
			}
			sampleSizeInBits = readLittleEndianShort(dis); // sample Size (is 0 for GSM)
			int cbSize=readLittleEndianShort(dis);
			if (cbSize < 2) {
				throw new UnsupportedAudioFileException(
				    "corrupt WAVE file: extra GSM bytes are corrupt");
			}
			int decodedSamplesPerBlock=readLittleEndianShort(dis) & 0xFFFF; // unsigned
			if (TDebug.TraceAudioFileReader) {
				debugAdd+=", wBitsPerSample="+sampleSizeInBits
				          +", cbSize="+cbSize
				          +", wSamplesPerBlock="+decodedSamplesPerBlock;
			}
			sampleSizeInBits = AudioSystem.NOT_SPECIFIED;
			encoding = WaveTool.GSM0610;
			frameSize=blockAlign;
			frameRate=((float) sampleRate)/((float) decodedSamplesPerBlock);
			read+=6;
			break;
		default:
			throw new UnsupportedAudioFileException(
			    "unsupported WAVE file: unknown format code "+formatCode);
		}
		// if frameSize isn't set, calculate it (the default)
		if (frameSize==0) {
			frameSize=(sampleSizeInBits * channelCount) / 8;
		}

		if (TDebug.TraceAudioFileReader) {
			TDebug.out("WaveAudioFileReader.readFormatChunk():");
			TDebug.out("  read values: wFormatTag="+formatCode
			           +", nChannels="+channelCount
			           +", nSamplesPerSec="+sampleRate
			           +", nAvgBytesPerSec="+avgBytesPerSecond
			           +", nBlockAlign=="+blockAlign
			           +debugAdd);
			TDebug.out("  constructed values: "
			           +"encoding="+encoding
			           +", sampleRate="+((float) sampleRate)
			           +", sampleSizeInBits="+sampleSizeInBits
			           +", channels="+channelCount
			           +", frameSize="+frameSize
			           +", frameRate="+frameRate);
		}

		// go to next chunk
		advanceChunk(dis, chunkLength, read);
		return new AudioFormat(
		           encoding,
		           (float) sampleRate,
		           sampleSizeInBits,
		           channelCount,
		           frameSize,
		           frameRate,
		           false);
	}

	public AudioFileFormat getAudioFileFormat(InputStream inputStream)
	throws	UnsupportedAudioFileException, IOException {
		DataInputStream	dataInputStream = new DataInputStream(inputStream);
		int magic = dataInputStream.readInt();
		if (magic != WaveTool.WAVE_RIFF_MAGIC) {
			throw new UnsupportedAudioFileException(
			    "not a WAVE file: wrong header magic");
		}
		long totalLength = readLittleEndianInt(dataInputStream) & 0xFFFFFFFF; // unsigned
		magic = dataInputStream.readInt();
		if (magic != WaveTool.WAVE_WAVE_MAGIC) {
			throw new UnsupportedAudioFileException("not a WAVE file: wrong header magic");
		}
		// search for "fmt " chunk
		long chunkLength = findChunk(dataInputStream, WaveTool.WAVE_FMT_MAGIC);
		AudioFormat format = readFormatChunk(dataInputStream, chunkLength);

		// search for "data" chunk
		long dataChunkLength = findChunk(dataInputStream, WaveTool.WAVE_DATA_MAGIC);

		long frameLength = dataChunkLength / format.getFrameSize();
		if (format.getEncoding().equals(WaveTool.GSM0610)) {
			// TODO: should not be necessary
			frameLength = dataChunkLength;
		}

		if (TDebug.TraceAudioFileReader) {
			TDebug.out("WaveAudioFileReader.getAudioFileFormat(): total length: "
			           +totalLength+", frame length = "+frameLength);
		}
		return new TAudioFileFormat(WaveTool.WAVE,
		                            format,
		                            (int) frameLength,
		                            (int) (totalLength + WaveTool.CHUNK_HEADER_SIZE));
	}
}

/*** WaveAudioFileReader.java ***/

