package com.headstartech.vas.web;

import com.headstartech.vas.service.USSDRequestProcessor;
import javolution.xml.stream.XMLStreamException;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.ussdgateway.EventsSerializeFactory;
import org.mobicents.ussdgateway.XmlMAPDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Per Johansson
 */
@Controller
public class DialogController {

    private static final Logger logger = LoggerFactory.getLogger(DialogController.class);

    @Autowired
    private EventsSerializeFactory factory;

    @Autowired
    private USSDRequestProcessor ussdRequestProcessor;

    @PostMapping(value = "/dialog", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public void dialogRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, MAPException, XMLStreamException {
        ServletInputStream is = request.getInputStream();
        XmlMAPDialog xmlMAPDialogRequest = factory.deserialize(is);
        XmlMAPDialog xmlMAPDialogResponse = ussdRequestProcessor.process(xmlMAPDialogRequest, request.getSession(true));
        response.getOutputStream().write(factory.serialize(xmlMAPDialogResponse));
        response.flushBuffer();
    }
}
