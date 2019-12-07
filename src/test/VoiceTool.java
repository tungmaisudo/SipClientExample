package test;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.sun.media.rtp.RTPSessionMgr;

import javax.activation.FileDataSource;
import javax.media.*;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import java.io.File;
import java.net.FileNameMap;
import java.net.InetAddress;

public class VoiceTool implements ReceiveStreamListener {

    private Processor processor;
    private DataSource outDataSource;

    private RTPSessionMgr voiceSession;
    private SendStream sendStream;

    private ReceiveStream receiveStream;
    private Player player;

    private SdpInfo senderInfo;
    private SdpInfo receiverInfo;

    public void senderInfo(SdpInfo senderInfo) {
        this.senderInfo = senderInfo;
    }

    public void receiverInfo(SdpInfo receiverInfo) {
        this.receiverInfo = receiverInfo;
    }

    public void init() {
        try {
            System.out.println("init audio");
            voiceSession = new RTPSessionMgr();
            voiceSession.addReceiveStreamListener(this);

            SessionAddress localSessionAddress = new SessionAddress(
                    InetAddress.getByName(senderInfo.getIpSender()), senderInfo.getVoicePort());

            SessionAddress revSessionAddress = new SessionAddress(
                    InetAddress.getByName(receiverInfo.getIpSender()), receiverInfo.getVoicePort());

            voiceSession.initSession(new SessionAddress(), null, 0.25, 0.5);
            voiceSession.startSession(localSessionAddress, localSessionAddress, revSessionAddress, null);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void startMedia() {
        try {
            System.out.println("startMedia");
            File file = new File("D:\\New folder\\file_example_WAV_1MG.wav");
//            // bắt media stream từ soundcard
            MediaLocator locator = new MediaLocator(file.toURL());
//            MediaLocator locator = new MediaLocator("javasound://44100");

            DataSource dataSource = Manager.createDataSource(locator);

            // media stream được truyền qua mạng và loại media là RAW_RTP
            ContentDescriptor outputFile = new ContentDescriptor(ContentDescriptor.RAW_RTP);

            // kiểu định dạng audio
            AudioFormat[] aFormat = new AudioFormat[1];
            aFormat[0] = new AudioFormat(AudioFormat.LINEAR);

            // tạo đối tượng Processor
            ProcessorModel processorModel = new ProcessorModel(dataSource, aFormat, outputFile);
            processor = Manager.createRealizedProcessor(processorModel);

            // tạo DataSource đầu ra
            outDataSource = processor.getDataOutput();

        } catch (Exception e) {
            System.out.println("error : " + e.getMessage());
        }
    }

    public void send() {
        try {
            System.out.println("send audio");
            sendStream = voiceSession.createSendStream(outDataSource, 0);
            sendStream.start();
            processor.start();

        } catch (Exception e) {
            System.out.println("error : " + e.getMessage());
        }
    }

    public void stopMedia() {
        try {

            player.stop();
            player.deallocate();
            player.close();

            sendStream.stop();

            processor.stop();
            processor.deallocate();
            processor.close();

            voiceSession.closeSession();
            voiceSession.dispose();
        } catch (Exception e) {
            System.out.println("StopMedia : " + e.getMessage());
        }
    }

    @Override
    public void update(ReceiveStreamEvent rse) {
        try {
            if (rse instanceof NewReceiveStreamEvent) {
                receiveStream = rse.getReceiveStream();
                DataSource myDs = receiveStream.getDataSource();

                player = Manager.createRealizedPlayer(myDs);
                player.start();

            }
        } catch (Exception e) {
            System.out.println("Update : " + e.getMessage());
        }
    }

}