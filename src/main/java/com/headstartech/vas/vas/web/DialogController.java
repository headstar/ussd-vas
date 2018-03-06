package com.headstartech.vas.vas.web;

import javolution.util.FastList;
import javolution.xml.stream.XMLStreamException;
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
import org.mobicents.ussdgateway.EventsSerializeFactory;
import org.mobicents.ussdgateway.XmlMAPDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.PostConstruct;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.mobicents.protocols.ss7.map.api.MAPMessageType.processUnstructuredSSRequest_Request;

/**
 * @author Per Johansson
 */
@Controller
public class DialogController {

    private static final Logger logger = LoggerFactory.getLogger(DialogController.class);

    private EventsSerializeFactory factory = null;

    @PostConstruct
    public void init() {
        factory = new EventsSerializeFactory();
    }

    @PostMapping(value = "/dialog", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public void dialogRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, MAPException {
        ServletInputStream is = request.getInputStream();

        try {
            XmlMAPDialog original = factory.deserialize(is);
            HttpSession session = request.getSession(true);
            logger.info("HttpSession=" + session.getId() + " Dialog = " + original);

            USSDString ussdStr = null;
            byte[] data = null;
            final FastList<MAPMessage> capMessages = original.getMAPMessages();

            MessageType messageType = original.getTCAPMessageType();

            // This is initial request, if its not NTFY, we need session
            for (FastList.Node<MAPMessage> n = capMessages.head(), end = capMessages.tail(); (n = n.getNext()) != end; ) {
                final MAPMessage mapMessage = n.getValue();
                final MAPMessageType type = mapMessage.getMessageType();

                switch (messageType) {

                    case Begin:
                        switch (mapMessage.getMessageType()) {
                            case processUnstructuredSSRequest_Request:
                                ProcessUnstructuredSSRequest processUnstructuredSSRequest = (ProcessUnstructuredSSRequest) mapMessage;
                                CBSDataCodingScheme cbsDataCodingScheme = processUnstructuredSSRequest.getDataCodingScheme();
                                if (logger.isInfoEnabled()) {
                                    logger.info("Received ProcessUnstructuredSSRequestIndication USSD String="
                                            + processUnstructuredSSRequest.getUSSDString());
                                }

                                session.setAttribute(
                                        "ProcessUnstructuredSSRequest_InvokeId",
                                        processUnstructuredSSRequest.getInvokeId());

                                //You business logic here and finally send back response

                                //Urdu
                                //CBSDataCodingScheme cbsDataCodingSchemeUrdu = new  CBSDataCodingSchemeImpl(72);
                                //ussdStr = new USSDStringImpl("\u062C\u0645\u064A\u0639 \u0627\u0644\u0645\u0633\u062A\u0639\u0645\u0644\u064A\u0646 \u0627\u0644\u0622\u062E\u0631\u064A\u0646 \u062A\u0645 \u0625\u0636\u0627\u0641\u062A\u0647\u0645",
                                //		cbsDataCodingSchemeUrdu, null);
                                //UnstructuredSSRequest unstructuredSSRequestIndication = new UnstructuredSSRequestImpl(
                                //		cbsDataCodingSchemeUrdu, ussdStr, null, null);

                                //English

                                ussdStr = new USSDStringImpl("USSD String : Hello World\n 1. Balance\n 2. Texts Remaining", cbsDataCodingScheme, null);
                                UnstructuredSSRequest unstructuredSSRequestIndication = new UnstructuredSSRequestImpl(cbsDataCodingScheme, ussdStr, null, null);

                                original.reset();
                                original.setTCAPMessageType(MessageType.Continue);
                                original.addMAPMessage(unstructuredSSRequestIndication);

                                data = factory.serialize(original);

                                response.getOutputStream().write(data);
                                response.flushBuffer();

                                break;
                            default:
                                // This is error. If its begin it should be only Process
                                // Unstructured SS Request
                                logger.error("Received Dialog BEGIN but message is not ProcessUnstructuredSSRequestIndication. Message="
                                        + mapMessage);
                                break;
                        }

                        break;
                    case Continue:
                        switch (type) {
                            case unstructuredSSRequest_Response:
                                UnstructuredSSResponse unstructuredSSResponse = (UnstructuredSSResponseImpl) mapMessage;

                                CBSDataCodingScheme cbsDataCodingScheme = unstructuredSSResponse.getDataCodingScheme();

                                long invokeId = (Long) session.getAttribute("ProcessUnstructuredSSRequest_InvokeId");

                                USSDString ussdStringObj = unstructuredSSResponse
                                        .getUSSDString();
                                String ussdString = null;
                                if (ussdStringObj != null) {
                                    ussdString = ussdStringObj.getString(null);
                                }

                                logger.info("Received UnstructuredSSResponse USSD String="
                                        + ussdString
                                        + " HttpSession="
                                        + session.getId() + " invokeId=" + invokeId);

                                cbsDataCodingScheme = new CBSDataCodingSchemeImpl(0x0f);
                                ussdStr = new USSDStringImpl("Thank You!", null, null);
                                ProcessUnstructuredSSResponse processUnstructuredSSResponse = new ProcessUnstructuredSSResponseImpl(
                                        cbsDataCodingScheme, ussdStr);
                                processUnstructuredSSResponse.setInvokeId(invokeId);

                                original.reset();
                                original.setTCAPMessageType(MessageType.End);
                                original.addMAPMessage(processUnstructuredSSResponse);
                                original.close(false);

                                data = factory.serialize(original);

                                response.getOutputStream().write(data);
                                response.flushBuffer();

                                try {
                                    session.invalidate();
                                } catch (Exception e) {
                                    session.invalidate();
                                    logger.error("Error while invalidating HttpSession=" + session.getId());
                                }
                                break;
                            default:
                                // This is error. If its begin it should be only Process
                                // Unstructured SS Request
                                logger.error("Received Dialog CONTINUE but message is not UnstructuredSSResponseIndication. Message=" + mapMessage);
                                break;
                        }

                        break;

                /*    case ABORT:
                        // The Dialog is aborted, lets do cleaning here

                        try {
                            session.invalidate();
                        } catch (Exception e) {
                            session.invalidate();
                            logger.error("Error while invalidating HttpSession=" + session.getId());
                        }
                        break;*/
                }
            }

        } catch (XMLStreamException e) {
            logger.error("Error while processing received XML", e);
        }

    }
}
