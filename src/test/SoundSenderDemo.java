/* This file is based on 
 * http://www.anyexample.com/programming/java/java_play_wav_sound_file.xml
 * Please see the site for license information.
 */
	 
package test;


import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;
import java.lang.String;
import java.net.DatagramSocket;
import java.util.Enumeration;
import jlibrtp.*;
import server.SoundSender;

/**
 * @author Arne Kepp
 */
public class SoundSenderDemo implements RTPAppIntf  {
	public RTPSession rtpSession = null;
	static int pktCount = 0;
	static int dataCount = 0;
	public String filename = "D:\\New folder\\file_example_WAV_1MG.wav";
	private final int EXTERNAL_BUFFER_SIZE = 1024;
	SourceDataLine auline;
	private Position curPosition;
	boolean local;
	 enum Position {
		LEFT, RIGHT, NORMAL
	};
	
	public SoundSenderDemo(boolean isLocal, int rtpPort)  {
		DatagramSocket rtpSocket = null;
		DatagramSocket rtcpSocket = null;
		
		try {
			rtpSocket = new DatagramSocket(rtpPort);
			rtcpSocket = new DatagramSocket(rtpPort + 1);
		} catch (Exception e) {
			System.out.println("RTPSession failed to obtain port");
		}
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);
		rtpSession.RTPSessionRegister(this,null, null);
		System.out.println("CNAME: " + rtpSession.CNAME());
		this.local = isLocal;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for(int i=0;i<args.length;i++) {
			System.out.println("args["+i+"]" + args[i]);
		}

		if(args.length == 0) {
			args = new String[4];
			args[1] = "127.0.0.1";
			args[0] = "D:\\New folder\\file_example_WAV_1MG.wav";
			args[2] = "16384";
			args[3] = "16385";
		}

		SoundSenderDemo aDemo = new SoundSenderDemo(false, 16380);
		Participant p = new Participant("sip.linphone.org",Integer.parseInt(args[2]),Integer.parseInt(args[2]) + 1);
		aDemo.rtpSession.addParticipant(p);
		aDemo.filename = args[0];
		aDemo.run();
		System.out.println("pktCount: " + pktCount);

	}

	public void run() {

		long start = System.currentTimeMillis();

		runAudio();

		System.out.println("Time: " + (System.currentTimeMillis() - start) / 1000 + " s");
		try {
			Thread.sleep(200);
		} catch (Exception e) {
		}

		this.rtpSession.endSession();
	}
	
	public void receiveData(DataFrame dummy1, Participant dummy2) {
		// We don't expect any data.
	}
	
	public void userEvent(int type, Participant[] participant) {
		//Do nothing
	}
	
	public int frameSize(int payloadType) {
		return 1;
	}

	public void runAudio() {


		File soundFile = new File(filename);
        if (!soundFile.exists()) {
            System.err.println("Wave file not found: " + filename);
            return;
        }

        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        } catch (UnsupportedAudioFileException e1) {
            e1.printStackTrace();
            return;
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }

		int nBytesRead = 0;
		byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];

		try {
			while (nBytesRead != -1) {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
				if (nBytesRead >= 0) {
					long[] result = rtpSession.sendData(abData);

					System.out.println("send: " + (result.length > 0 ? result[0] : null));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {

		}
	}



}
