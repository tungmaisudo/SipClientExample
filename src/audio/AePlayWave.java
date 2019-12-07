package audio;

import jlibrtp.RTPSession;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AePlayWave extends Thread {

    protected static final boolean DEBUG = false;

    protected File file;
    protected RTPSession rtpSession;

    public AePlayWave(File file, RTPSession rtpSession) {
        this.file = file;
        this.rtpSession = rtpSession;
        if (DEBUG) System.out.println("AePlayWave constructor");
    }

    @Override
    public void run() {
        if (DEBUG) System.out.println("AePlayWave running");

        AudioInputStream audioInputStream = verifyInputStream();
        if (audioInputStream == null) {
            return;
        }

        AudioFormat format = audioInputStream.getFormat();
        SourceDataLine audioLine = openInputStream(format);

        if (DEBUG) System.out.println(audioLine.getLineInfo());

        if (audioLine != null) {
            audioLine.start();
            playInputStream(audioInputStream, audioLine);
        }
    }

    protected AudioInputStream verifyInputStream() {
        if (DEBUG) System.out.println("AePlayWave verifyInputStream");
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return audioInputStream;
    }

    protected SourceDataLine openInputStream(AudioFormat format) {
        if (DEBUG) System.out.println("AePlayWave openInputStream");
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine audioLine = null;
        if (DEBUG) System.out.println("AePlayWave openInputStream try");
        try {
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            if (DEBUG) System.out.println("AePlayWave openInputStream getLine");
            audioLine.open(format);
            if (DEBUG) System.out.println("AePlayWave openInputStream open");
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return audioLine;
    }

    protected void playInputStream(AudioInputStream audioInputStream,
                                   SourceDataLine audioLine) {
        if (DEBUG) System.out.println("AePlayWave playInputStream");
        int externalBufferSize = (int) audioInputStream.getFrameLength() * 4;
        if (DEBUG) System.out.println("AePlayWave playInputStream externalBufferSize: "
                + externalBufferSize);
        int nBytesRead = 0;
        byte[] abData = new byte[externalBufferSize];

        try {
            while (nBytesRead != -1) {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
                if (nBytesRead >= 0) {
                    audioLine.write(abData, 0, nBytesRead);
                    long[] result = rtpSession.sendData(abData);
                    System.out.println("send: " + (result.length > 0 ? result[0] : null));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            audioLine.drain();
            audioLine.close();
        }
    }
}