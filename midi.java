/******

	* Tell user to record a sound. Figure out what note the sound is. 
	* If that particular note hasn't already been recorded, save the sound as the note.
	* If that note has already been recorded, ignore it and tell you user to make another noise.
		

******/

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import java.io.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.util.Collections;
import javax.sound.sampled.AudioFileFormat.Type;



public class midi {

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


	public static final int NOTE_ON = 0x90;
	public static final int NOTE_OFF = 0x80;
	public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
	
	public static void main (String[] args) throws Exception {
	
	
	
		//ArrayList notesInSong = new ArrayList();
		List<String> notesInSong = new ArrayList<String>();
	
		Sequence sequence = MidiSystem.getSequence(new File("up.mid"));
		
		int trackNumber = 0;
		for (Track track : sequence.getTracks()) {
			trackNumber++;
			System.out.println("Track" + trackNumber + ": size = " + track.size());
			System.out.println();
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
//				System.out.println("@" + event.getTick() + " ");
				MidiMessage message = event.getMessage();
				if (message instanceof ShortMessage){
					ShortMessage sm = (ShortMessage) message;
//					System.out.println("Channel: " + sm.getChannel() + " ");
					if (sm.getCommand() == NOTE_ON) {
						int key = sm.getData1();
						int octave = (key / 12)-1;
						int note = key % 12;
						String noteName = NOTE_NAMES[note];
						int velocity = sm.getData2();
	//					System.out.println("Note on, " + noteName + octave + " key = " + key + " velocity: " + velocity);
						notesInSong.add(noteName + ".wav");			///ADD TO LIST OF NOTES
					//	System.out.println(notesInSong);
					} else if (sm.getCommand() == NOTE_OFF) {
						int key = sm.getData1();
						int octave = (key / 12) -1;
						int note = key % 12;
						String noteName = NOTE_NAMES[note];
						int velocity = sm.getData2();
	//					System.out.println("Note off, " + noteName + octave + " key = " + key + " velocity: " + velocity);
					} else {
	//					System.out.println("Command: " + sm.getCommand());
					}
				} else {
	//				System.out.println("Other message: " + message.getClass());
				}
			}
	//		System.out.println();
		}
		
	//	System.out.println("Notes in song: " + notesInSong);
		
		
		
		String[] testList = {"C", "C", "G", "G", "A", "A", "G", "F", "F", "E", "E", "D", "D", "C", "G", "G", "F", "F", "E", "E", "D", "G", "G", "F", "F", "E", "E", "D", "C", "C", "G", "G", "A", "A", "G", "F", "F", "E", "E", "D", "D", "C"};
		
		List<String> sourceFilesList = new ArrayList<String>();
		
		for (int i = 0; i < testList.length; i++){
			sourceFilesList.add(testList[i] + ".wav");
		}
		
		System.out.println(notesInSong);
		
		System.out.println(concatenateFiles(notesInSong, "mergedSounds.mp3"));
		
	}
}