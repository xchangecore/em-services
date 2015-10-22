package com.leidos.xchangecore.core.em.util;

import javax.xml.stream.XMLStreamException;

import com.usersmarts.pub.georss.GEORSS;
import com.usersmarts.util.stax.ExtendedXMLStreamWriter;
import com.usersmarts.xmf2.MarshalContext;
import com.usersmarts.xmf2.MarshalException;

public class GMLStaxFormatter
    extends com.usersmarts.geo.gml.GMLStaxFormatter {

    @Override
    public Object format(MarshalContext context, Object geometry, ExtendedXMLStreamWriter writer)
        throws MarshalException, XMLStreamException {

        writer.element("georss", "where", GEORSS.GEORSS_NS);
        formatGeometry(context, geometry, writer);
        writer.end();

        return writer;
    }

}
