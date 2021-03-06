/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.xms.smoke.test.examples;

import com.dialogic.XMSClientLibrary.XMSCall;
import com.dialogic.XMSClientLibrary.XMSConnector;
import com.dialogic.XMSClientLibrary.XMSObjectFactory;
import com.dialogic.XMSClientLibrary.XMSReturnCode;
import com.dialogic.xms.smoke.gui.SimpleIVRTestForm;
import com.dialogic.xms.smoke.test.Audit;
import com.dialogic.xms.smoke.test.Checkpoint;
import com.dialogic.xms.smoke.test.Utility;
import com.dialogic.xms.smoke.test.stim.SimpleIVRTestStim;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ssatyana
 */
public class SimpleIVRTest extends Observable {

    static final Logger logger = Logger.getLogger(SimpleIVRTest.class.getName());
    String configName;
    static SimpleIVRTestForm form;
    List<Checkpoint> checkpoints = new ArrayList<>();
    boolean stim = Boolean.FALSE;

    public Audit start() {
        Audit ivrAudit = new Audit();
        if (stim) {
            SimpleIVRTestStim stim = new SimpleIVRTestStim();
        } else {
            try {
                if (this.getConfigFileName() != null) {
                    ivrAudit.setAuditTestName("SimpleIVRTest");
                    ivrAudit.setStartTime(Utility.timeStamp());
                    ivrAudit.setConfigFileName(this.getConfigFileName());
                    ivrAudit.setConfigContents(Utility.getConfigContents(new FileInputStream(this.getConfigFileName())));

                    XMSObjectFactory myFactory = new XMSObjectFactory();
                    XMSConnector myConnector = myFactory.CreateConnector(this.getConfigFileName());
                    XMSCall call = myFactory.CreateCall(myConnector);

                    Checkpoint waitCall = Utility.getCheckpoint("WaitCall", "Add to waitCall list");
                    setValue(waitCall.getShortDesc());
                    XMSReturnCode result = call.Waitcall();
                    waitCall = Utility.setResult(ivrAudit, waitCall, result, call);
                    this.checkpoints.add(waitCall);

                    while (true) {

                        call.CollectDigitsOptions.SetMaxDigits(1);
                        call.CollectDigitsOptions.SetTimeout(20);
                        Checkpoint collect = Utility.getCheckpoint("Collect", "Waiting for DTMF digits");
                        setValue(collect.getShortDesc());
                        XMSReturnCode collResult = call.PlayCollect("smoketest/mainIVR");
                        collect = Utility.setResult(ivrAudit, collect, collResult, call);
                        this.checkpoints.add(collect);

                        if (call.getLastEvent().getReason().contains("term-digit")) {
                            System.out.println("Term Digit of # detected.... hanging up");
                            setValue("Term Digit of # detected, hanging up");
                            break;
                        }

                        if (call.getLastEvent().getReason().contains("max-digits")) {
                            if (call.getLastEvent().getData().contentEquals("1")) {
                                Checkpoint sales = Utility.getCheckpoint("Play Prompt", "You have reached sales");
                                setValue(sales.getShortDesc());
                                XMSReturnCode playResult2 = call.Play("smoketest/sales");
                                sales = Utility.setResult(ivrAudit, sales, playResult2, call);
                                this.checkpoints.add(sales);
                                break;
                            } else if (call.getLastEvent().getData().contentEquals("2")) {
                                Checkpoint services = Utility.getCheckpoint("Play Prompt", "You have reached services");
                                setValue(services.getShortDesc());
                                XMSReturnCode playResult3 = call.Play("smoketest/services");
                                services = Utility.setResult(ivrAudit, services, playResult3, call);
                                this.checkpoints.add(services);
                                break;
                            } else if (call.getLastEvent().getData().contentEquals("3")) {
                                Checkpoint ceo = Utility.getCheckpoint("Play Prompt", "You have reached the CEO");
                                setValue(ceo.getShortDesc());
                                XMSReturnCode playResult4 = call.Play("smoketest/ceo");
                                ceo = Utility.setResult(ivrAudit, ceo, playResult4, call);
                                this.checkpoints.add(ceo);
                                break;
                            } else {
                                System.out.println("Digit " + call.getLastEvent().getData() + " was detected");
                                setValue("Digit " + call.getLastEvent().getData() + " was detected");
                                continue;
                            }
                        }
                    }

                    Utility.Sleep(10000);

                    Checkpoint dropCall = Utility.getCheckpoint("FinalDropCall", "Drop call");
                    setValue(dropCall.getShortDesc());
                    XMSReturnCode dropResult = call.Dropcall();
                    dropCall = Utility.setResult(ivrAudit, dropCall, dropResult, call);
                    this.checkpoints.add(dropCall);

                    ivrAudit.setCheckpointCount(this.checkpoints.size());
                    ivrAudit.setTestCheckpoints(this.checkpoints);
                    if (ivrAudit.getNumFail() > 0) {
                        ivrAudit.setTestStatus("FAILURE");
                        setValue("FAILURE");
                    } else {
                        ivrAudit.setTestStatus("SUCCESS");
                        setValue("SUCCESS");
                    }
                } else {
                    System.out.println("NO CONFIG FILE SPECIFIED -> " + configName);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return ivrAudit;

    }

    public void configure() {
        form = new SimpleIVRTestForm(this);
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

    public void isCancelled() {
        setValue("CANCELLED");
    }

    public void setValue(String value) {
        setChanged();
        notifyObservers(value);
    }
}
