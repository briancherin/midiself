/*

	Multiple notes at same time: Need to make it so that C3 differentiates from C4 (different octave)
		Make each note C4 or C3 or B4 but when finding the file do str.substring(0, length-1)


*/



import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collections;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;


public class MergeTest {

	public static final int NOTE_ON = 0x90;
	public static final int NOTE_OFF = 0x80;
	public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

	public static List<String> getNotes(String midiFileName) throws Exception {
		
		List<String> notesInSong = new ArrayList<String>();
	
		Sequence sequence = MidiSystem.getSequence(new File(midiFileName));
		
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
					//	System.out.println("Note on, " + noteName + octave + " key = " + key + " velocity: " + velocity);
					//	notesInSong.add(noteName + ".wav");			///ADD TO LIST OF NOTES
						notesInSong.add(noteName);
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
	
		return notesInSong;
	}


	public static void main (String[] args) throws Exception {
		/*
				G#(on) F(on) F(off) G#(off)
		*/
		
		String[] randomNotes = {"G#", 
							"F", 
								"E",
									"A",
										"B",
											"C",
											"C",
										"B",
										"D",
											"C#",
											"C#",
										"D",
									"A",
								"E",
							"F", 
						"G#", 
						"C#", 
							"F", 
							"F", 
						"C#"};

	//	String[] noteArray = {"E", "E", "G", "G", "E", "E", "C", "C", "D", "D", "G", "G"};
		
		List<String> notes = new ArrayList<String>();
		
		notes = getNotes("songs/Sm1cave.mid");
	
	//	notes.clear();
//		Collections.addAll(notes, randomNotes); //add all elements of array to the arraylist
		
		System.out.println(notes);
						
		List<String> tempNotes = new ArrayList<String>();
		
		List<String> finalAudioList = new ArrayList<String>();
		
		int numbSets = 0;
						
		for (int i = 0; i < notes.size(); i++){
			if ((i+1 < notes.size()) && notes.get(i) != notes.get(i+1))	{	//more than one note at the same time
			System.out.println("Inside if: more than one note at same time.");
			//	System.out.print(notes.get(i));
				tempNotes.add(notes.get(i));	//add original note to list
				for (int j = i+1; j < notes.size(); j++){
					if (notes.get(j) != notes.get(i)){		//if original note NOT seen
						tempNotes.add(notes.get(j));	//add note to list
					}
					else{					//if original note seen
						j = notes.size();	//get out of loop
						i += tempNotes.size();

						numbSets++;
					}
				}
			System.out.println("Outside for loop: tempNotes = " + tempNotes);
				
				AudioMixer mixer = new AudioMixer();
				
				tempNotes = new ArrayList<String>(new LinkedHashSet<String>(tempNotes));	//remove duplicate notes 
				System.out.println("After removal of duplicates: tempNotes = " + tempNotes);		//MERGE AUDIOS HERE (below actually)
				
				Collection<File> audioFileList = new ArrayList<File>();
		
				for (int k = 0; k < tempNotes.size(); k++){
					File file = new File("/notes" + tempNotes.get(k) + ".wav");
					audioFileList.add(file);
				}				
					
					
				String filename = "finalAudio" + numbSets + ".wav";
				File finalFile = new File(filename);

				if (finalFile.createNewFile()){
					System.out.println("file " + finalFile + " created.");
				}
	
				mixer.mixAudioFiles(audioFileList, finalFile);
				
				finalAudioList.add(filename);
				
				
				tempNotes.clear();
				audioFileList.clear();
			}
			
			if ((i+1 < notes.size()) && notes.get(i) == notes.get(i+1)) {	//Just one note. nothing at the same time.	//CHANGED <= TO <
				tempNotes.add(notes.get(i));	//add that note to note list
				i++;						//skip the off signal note
				
				
				
				tempNotes = new ArrayList<String>(new LinkedHashSet<String>(tempNotes));	//remove duplicate notes 
				System.out.println(tempNotes);		//MERGE AUDIOS HERE (below actually)
				
				Collection<File> audioFileList = new ArrayList<File>();
		
				for (int k = 0; k < tempNotes.size(); k++){
					File file = new File("notes/" + tempNotes.get(k) + ".wav");
					audioFileList.add(file);
				}			
				
				String filename = "tempAudio/finalAudio" + numbSets + ".wav";
					
				File finalFile = new File(filename);

				if (finalFile.createNewFile()){
					System.out.println("file " + finalFile + " created.");
				}
				else{
					System.out.println("file " + finalFile + " already exists!");
				}
		
				AudioMixer mixer = new AudioMixer();
		
				mixer.mixAudioFiles(audioFileList, finalFile);
				
				finalAudioList.add(filename);
				
				
				tempNotes.clear();
				audioFileList.clear();
				
				numbSets++;
				
			
			}
		}
		
		midi catDoer = new midi();
		
	//	System.out.println(finalAudioList);
		
		System.out.println(catDoer.concatenateFiles(finalAudioList, "finallyFinalAudio.wav"));	//combine all audio clips
		
		
	}
}