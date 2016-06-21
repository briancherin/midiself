import java.util.List;
import java.util.ArrayList;
import java.io.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.util.Collections;
import javax.sound.sampled.AudioFileFormat.Type;

public class combineSounds {
	public static Boolean concatenateFiles(List<String> sourceFilesList, String destinationFileName) throws Exception {
			Boolean result = false;

			AudioInputStream audioInputStream = null;
			List<AudioInputStream> audioInputStreamList = null;
			AudioFormat audioFormat = null;
			Long frameLength = null;

			try {
				// loop through our files first and load them up
				for (String sourceFile : sourceFilesList) {
					audioInputStream = AudioSystem.getAudioInputStream(new File(sourceFile));

					// get the format of first file
					if (audioFormat == null) {
						audioFormat = audioInputStream.getFormat();
					}

					// add it to our stream list
					if (audioInputStreamList == null) {
						audioInputStreamList = new ArrayList<AudioInputStream>();
					}
					audioInputStreamList.add(audioInputStream);

					// keep calculating frame length
					if(frameLength == null) {
						frameLength = audioInputStream.getFrameLength();
					}
					else {
						frameLength += audioInputStream.getFrameLength();
					}
				}

				// now write our concatenated file
				AudioSystem.write(new AudioInputStream(new SequenceInputStream(Collections.enumeration(audioInputStreamList)), audioFormat, frameLength), Type.WAVE, new File(destinationFileName));

				// if all is good, return true
				result = true;
			}
			catch (Exception e) {
				throw e;
			}
			finally {
				if (audioInputStream != null) {
					audioInputStream.close();
				}
				if (audioInputStreamList != null) {
					audioInputStreamList = null;
				}
			}

			return result;
		}
		
		public static void main (String[]args) throws Exception {
			List<String> sourceFilesList = new ArrayList<String>();
			
			sourceFilesList.add("sound1.wav");
			sourceFilesList.add("sound2.wav");
			sourceFilesList.add("sound3.wav");
			
			System.out.println(sourceFilesList);
			
			
			System.out.println(concatenateFiles(sourceFilesList, "mergedSounds.mp3"));
		}
}