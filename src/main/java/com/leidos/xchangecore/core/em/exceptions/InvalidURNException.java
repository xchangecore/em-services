package com.leidos.xchangecore.core.em.exceptions;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CLIENT)
public class InvalidURNException
    extends Exception {

    static final long serialVersionUID = 2;
    private String urn;

    public InvalidURNException(String urn) {

        super("Bad URN: [" + urn + "]+");
        this.urn = urn;
    }

    public String getURN() {

        return this.urn;
    }
}
