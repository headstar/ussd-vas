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
    private static String INVOKE_SESSION_ATTRIBUTE_KEY = "ProcessUnstructuredSSRequest_InvokeId";

    public XmlMAPDialog process(XmlMAPDialog mapDialogMessage, HttpSession session) throws MAPException {

        String sessionId = session.getId();
        logger.debug("Received dialog message: sessionId = {}, sessionNew = {}, message = {}", sessionId, session.isNew(), mapDialogMessage);

        MessageType tcapMessageType = mapDialogMessage.getTCAPMessageType();
        if(MessageType.Begin.equals(tcapMessageType)) {
            MAPMessage mapMessage = getMapMessageOrThrow(mapDialogMessage);
            if (MAPMessageType.processUnstructuredSSRequest_Request.equals(mapMessage.getMessageType())) {
                ProcessUnstructuredSSRequest processUnstructuredSSRequest = (ProcessUnstructuredSSRequest) mapMessage;
                process(processUnstructuredSSRequest, mapDialogMessage, session);
                logger.debug("Response dialog message: sessionId = {}, message = {}", sessionId, mapDialogMessage);
                return mapDialogMessage;
            } else {
                throw new UnexpectedMessage(String.format("Not expecting MAP message of type %s", mapMessage.getMessageType().name()));
            }
        } else if (MessageType.Continue.equals(tcapMessageType)) {
            MAPMessage mapMessage = getMapMessageOrThrow(mapDialogMessage);
            UnstructuredSSResponse unstructuredSSResponse = (UnstructuredSSResponseImpl) mapMessage;
            process(unstructuredSSResponse, mapDialogMessage, session);
        } else if (MessageType.Abort.equals(tcapMessageType)) {
            logger.debug("Session aborted: sessionId = {}", sessionId);
            session.invalidate();
            return null;
        }
        throw new UnexpectedMessage(String.format("Not expecting diaolg message with tcap type %s", tcapMessageType.name()));
    }

    private MAPMessage getMapMessageOrThrow(XmlMAPDialog xmlMAPDialog) {
        if(xmlMAPDialog.getMAPMessages().isEmpty()) {
            throw new UnexpectedMessage("no MAP message in dialog message");
        }
        return xmlMAPDialog.getMAPMessages().getFirst();
    }

    private void process(ProcessUnstructuredSSRequest processUnstructuredSSRequest, XmlMAPDialog mapDialogMessage, HttpSession session) throws MAPException {
        CBSDataCodingScheme cbsDataCodingScheme = processUnstructuredSSRequest.getDataCodingScheme();
        session.setAttribute(INVOKE_SESSION_ATTRIBUTE_KEY, processUnstructuredSSRequest.getInvokeId());

        USSDString ussdStr = new USSDStringImpl("USSD String : Hello World\n 1. Balance\n 2. Texts Remaining", cbsDataCodingScheme, null);
        UnstructuredSSRequest unstructuredSSRequest = new UnstructuredSSRequestImpl(cbsDataCodingScheme, ussdStr, null, null);

        mapDialogMessage.reset();
        mapDialogMessage.setCustomInvokeTimeOut(25000);
        mapDialogMessage.setTCAPMessageType(MessageType.Continue);
        mapDialogMessage.addMAPMessage(unstructuredSSRequest);
    }

    private void process(UnstructuredSSResponse unstructuredSSResponse, XmlMAPDialog mapDialogMessage, HttpSession session) throws MAPException {
        long invokeId = (Long) session.getAttribute(INVOKE_SESSION_ATTRIBUTE_KEY);

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

        session.invalidate();
    }
}
