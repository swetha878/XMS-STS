/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.xms.smoke.test.examples;

import com.dialogic.xms.smoke.test.stim.ConnectTestStim;
import com.dialogic.XMSClientLibrary.*;
import com.dialogic.xms.smoke.gui.ConnectTestForm;
import com.dialogic.xms.smoke.test.Audit;
import com.dialogic.xms.smoke.test.Checkpoint;
import com.dialogic.xms.smoke.test.Utility;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dwolansk
 */
public class ConnectTest extends Observable {

    static ConnectTestForm form;
    String configName;
    String playFileName;
    List<Checkpoint> checkpoints = new ArrayList<>();
    boolean stim = Boolean.FALSE;
    private String callType;

    /**
     * @return
     */
    public Audit start() {
        Audit audit = null;
        if (stim) {
            ConnectTestStim stim = new ConnectTestStim();
        } else {
            if (this.getCallType().equalsIgnoreCase("Inbound")) {
                audit = inboundPlay();
            } else if (this.getCallType().equalsIgnoreCase("Outbound")) {
                audit = outboundPlay();
            }
        }
        return audit;

    }

    private Audit inboundPlay() {
        Audit inboundPlayAudit = new Audit();
        try {
            if (this.getConfigFileName() != null) {
                inboundPlayAudit.setAuditTestName("InboundPlayTest");
                inboundPlayAudit.setStartTime(Utility.timeStamp());
                inboundPlayAudit.setConfigFileName(this.getConfigFileName());
                inboundPlayAudit.setConfigContents(Utility.getConfigContents(new FileInputStream(this.getConfigFileName())));

                XMSObjectFactory myFactory = new XMSObjectFactory();
                XMSConnector myConnector = myFactory.CreateConnector(this.getConfigFileName());
                XMSCall myCall = myFactory.CreateCall(myConnector);
                boolean isVideo = inboundPlayAudit.getConfigContents().isVideo();

                if (isVideo) {
                    myCall.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
                }
                Checkpoint waitCall = Utility.getCheckpoint("WaitCall", "Adding to waitCall list");
                setValue(waitCall.getShortDesc());
                XMSReturnCode result = myCall.Waitcall();
                waitCall = Utility.setResult(inboundPlayAudit, waitCall, result, myCall);
                this.checkpoints.add(waitCall);

                if (myCall.getState() == XMSCallState.CONNECTED) {
                    setValue("Call Connected");
                    if (isVideo) {
                        myCall.PlayOptions.SetMediaType(XMSMediaType.VIDEO);
                    }
                    Checkpoint playStat = Utility.getCheckpoint("Play", "Playing");
                    setValue(playStat.getShortDesc());
                    XMSReturnCode playResult = myCall.Play(inboundPlayAudit.getConfigContents().getPlayFileName());
                    playStat = Utility.setResult(inboundPlayAudit, playStat, playResult, myCall);
                    this.checkpoints.add(playStat);
                }
                Utility.Sleep(5000);
                //Hangup the call
                Checkpoint dropStat = Utility.getCheckpoint("DropCall", "Drop call");
                setValue(dropStat.getShortDesc());
                XMSReturnCode dropResult = myCall.Dropcall();
                dropStat = Utility.setResult(inboundPlayAudit, dropStat, dropResult, myCall);
                this.checkpoints.add(dropStat);

                inboundPlayAudit.setCheckpointCount(this.checkpoints.size());
                inboundPlayAudit.setTestCheckpoints(this.checkpoints);
                if (inboundPlayAudit.getNumFail() > 0) {
                    inboundPlayAudit.setTestStatus("FAILURE");
                    setValue("FAILURE");
                } else {
                    inboundPlayAudit.setTestStatus("SUCCESS");
                    setValue("SUCCESS");
                }
            } else {
                System.out.println("NO CONFIG FILE SPECIFIED -> " + configName);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConnectTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return inboundPlayAudit;
    }

    private Audit outboundPlay() {
        Audit outboundPlayAudit = new Audit();
        try {
            outboundPlayAudit.setAuditTestName("OutboundPlayTest");
            outboundPlayAudit.setStartTime(Utility.timeStamp());
            outboundPlayAudit.setConfigFileName(this.getConfigFileName());
            outboundPlayAudit.setConfigContents(Utility.getConfigContents(new FileInputStream(this.getConfigFileName())));

            XMSObjectFactory myFactory = new XMSObjectFactory();
            XMSConnector myConnector = myFactory.CreateConnector(this.getConfigFileName());
            XMSCall myCall = myFactory.CreateCall(myConnector);

            String OutboundAddress = outboundPlayAudit.getConfigContents().getOutboundAddress();
            boolean isVideo = outboundPlayAudit.getConfigContents().isVideo();

            if (isVideo) {
                myCall.MakecallOptions.SetMediaType(XMSMediaType.VIDEO);
            }
            Checkpoint makeCall = Utility.getCheckpoint("MakeCall", "Making an outbound call");
            setValue(makeCall.getShortDesc());
            XMSReturnCode result = myCall.Makecall(OutboundAddress);
            makeCall = Utility.setResult(outboundPlayAudit, makeCall, result, myCall);
            this.checkpoints.add(makeCall);

            if (myCall.getState() == XMSCallState.CONNECTED) {
                if (isVideo) {
                    myCall.PlayOptions.SetMediaType(XMSMediaType.VIDEO);
                }
                Checkpoint playStat = Utility.getCheckpoint("Play", "Playing");
                setValue(playStat.getShortDesc());
                XMSReturnCode playResult = myCall.Play(outboundPlayAudit.getConfigContents().getPlayFileName());
                playStat = Utility.setResult(outboundPlayAudit, playStat, playResult, myCall);
                this.checkpoints.add(playStat);
            }
            Utility.Sleep(5000);
            //Hangup the call
            Checkpoint dropStat = Utility.getCheckpoint("DropCall", "Drop call");
            setValue(dropStat.getShortDesc());
            XMSReturnCode dropResult = myCall.Dropcall();
            dropStat = Utility.setResult(outboundPlayAudit, dropStat, dropResult, myCall);
            this.checkpoints.add(dropStat);

            outboundPlayAudit.setCheckpointCount(this.checkpoints.size());
            outboundPlayAudit.setTestCheckpoints(this.checkpoints);
            if (outboundPlayAudit.getNumFail() > 0) {
                outboundPlayAudit.setTestStatus("FAILURE");
                setValue("FAILURE");
            } else {
                outboundPlayAudit.setTestStatus("SUCCESS");
                setValue("SUCCESS");
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConnectTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return outboundPlayAudit;
    }

    public void configure() {
        form = new ConnectTestForm(this);
    }

    public void setConfigFileName(String file) {
        configName = file;
    }

    public String getConfigFileName() {
        System.out.println("filename -> " + configName);
        return configName;
    }

    public void setCount(String count) {
        setValue("Count:" + count);
    }

    public void setisStim() {
        stim = Boolean.TRUE;
    }

    /**
     * @return the callType
     */
    public String getCallType() {
        return callType;
    }

    /**
     * @param callType the callType to set
     */
    public void setCallType(String callType) {
        this.callType = callType;
    }

    public void isCancelled() {
        setValue("CANCELLED");
    }

    public void setValue(String value) {
        setChanged();
        notifyObservers(value);
    }
}
