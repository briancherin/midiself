public class Note {
	
	int key;
	int velocity;
	int octave;
	int note;
	String noteName;
	boolean noteOn;

	public static final int ON = 0x90;
	public static final int OFF = 0x80;
	public static final String[] NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

	public Note(int key, int velocity, int noteOn){
		this.key = key;
		this.noteName = NAMES[note];
		this.velocity = velocity;

		this.octave = (key / 12)-1;
		this.note = key % 12;

		this.noteOn = noteOn == ON;

	}

	public String toString(){
		return noteName + octave + (noteOn ? " On" : " Off");
	}
}