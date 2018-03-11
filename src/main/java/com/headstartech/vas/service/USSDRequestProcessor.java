package com.headstartech.vas.service;

import com.headstartech.vas.web.DialogController;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.MAPMessageType;
import org.mobicents.protocols.ss7.map.api.datacoding.CBSDataCodingScheme;
import org.mobicents.protocols.ss7.map.api.primitives.USSDString;
import org.mobicents.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSResponse;
import org.mobicents.protocols.ss7.map.datacoding.CBSDataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.primitives.USSDStringImpl;
import org.mobicents.protocols.ss7.map.service.supplementary.ProcessUnstructuredSSResponseImpl;
import org.mobicents.protocols.ss7.map.service.supplementary.UnstructuredSSRequestImpl;
import org.mobicents.protocols.ss7.map.service.supplementary.UnstructuredSSResponseImpl;
import org.mobicents.protocols.ss7.tcap.api.MessageType;
import org.mobicents.ussdgateway.XmlMAPDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * @author Per Johansson
 */
@Service
public class USSDRequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DialogController.class);

    public XmlMAPDialog process(XmlMAPDialog mapDialogMessage, HttpSession session) throws MAPException {

        logger.debug("Processing dialog message: session= {}, message = {}", session.getId(), mapDialogMessage);

        if(mapDialogMessage.getMAPMessages().isEmpty()) {
            throw new RuntimeException("no map message!");
        }
        MAPMessage mapMessage = mapDialogMessage.getMAPMessages().getFirst();
        MessageType tcapMessagetype = mapDialogMessage.getTCAPMessageType();
        if(MessageType.Begin.equals(tcapMessagetype)) {
            if(MAPMessageType.processUnstructuredSSRequest_Request.equals(mapMessage.getMessageType())) {
                ProcessUnstructuredSSRequest processUnstructuredSSRequest = (ProcessUnstructuredSSRequest) mapMessage;
                process(processUnstructuredSSRequest, mapDialogMessage, session);
                return mapDialogMessage;
            }
        } else if (MessageType.Continue.equals(tcapMessagetype)) {
            UnstructuredSSResponse unstructuredSSResponse = (UnstructuredSSResponseImpl) mapMessage;
            process(unstructuredSSResponse, mapDialogMessage, session);
            return mapDialogMessage;
        }

        // TODO: handle this scenario!
        throw new RuntimeException("invalid dialog message!");
    }

    private void process(ProcessUnstructuredSSRequest processUnstructuredSSRequest, XmlMAPDialog mapDialogMessage, HttpSession session) throws MAPException {
        logger.debug("Received ProcessUnstructuredSSRequest: message = {}",processUnstructuredSSRequest);

        CBSDataCodingScheme cbsDataCodingScheme = processUnstructuredSSRequest.getDataCodingScheme();
        session.setAttribute("ProcessUnstructuredSSRequest_InvokeId", processUnstructuredSSRequest.getInvokeId());

        USSDString ussdStr = new USSDStringImpl("USSD String : Hello World\n 1. Balance\n 2. Texts Remaining", cbsDataCodingScheme, null);
        UnstructuredSSRequest unstructuredSSRequestIndication = new UnstructuredSSRequestImpl(cbsDataCodingScheme, ussdStr, null, null);

        mapDialogMessage.reset();
        mapDialogMessage.setUserObject("Session Id : " + session.getId());
        mapDialogMessage.setCustomInvokeTimeOut(25000);
        mapDialogMessage.setTCAPMessageType(MessageType.Continue);
        mapDialogMessage.addMAPMessage(unstructuredSSRequestIndication);
    }

    private void process(UnstructuredSSResponse unstructuredSSResponse, XmlMAPDialog mapDialogMessage, HttpSession session) throws MAPException {
        logger.debug("Received UnstructuredSSResponse: message = {}", unstructuredSSResponse);

        long invokeId = (Long) session.getAttribute("ProcessUnstructuredSSRequest_InvokeId");

        CBSDataCodingScheme cbsDataCodingScheme = unstructuredSSResponse.getDataCodingScheme();

        USSDString ussdStringObj = unstructuredSSResponse.getUSSDString();
        String ussdString = null;
        if (ussdStringObj != null) {
            ussdString = ussdStringObj.getString(null);
        }

        cbsDataCodingScheme = new CBSDataCodingSchemeImpl(0x0f);
        USSDString ussdStr = new USSDStringImpl("Thank You!", null, null);
        ProcessUnstructuredSSResponse processUnstructuredSSResponse = new ProcessUnstructuredSSResponseImpl(
                cbsDataCodingScheme, ussdStr);
        processUnstructuredSSResponse.setInvokeId(invokeId);

        mapDialogMessage.reset();
        mapDialogMessage.setTCAPMessageType(MessageType.End);
        mapDialogMessage.addMAPMessage(processUnstructuredSSResponse);
        mapDialogMessage.close(false);
    }
}
