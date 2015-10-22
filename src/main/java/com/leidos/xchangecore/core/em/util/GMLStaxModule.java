package com.leidos.xchangecore.core.em.util;

import com.usersmarts.geo.gml.GML3;
import com.usersmarts.geo.gml.GMLStaxParser;
import com.usersmarts.xmf2.Configuration;
import com.usersmarts.xmf2.stax.StaxFormatterFinder;
import com.usersmarts.xmf2.stax.StaxParserFinder;
import com.usersmarts.xmf2.util.XmlParserFinder;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class GMLStaxModule
    extends com.usersmarts.geo.gml.GMLStaxModule {

    @Override
    public void configure(Configuration cfg) {

        XmlParserFinder spf = new StaxParserFinder(new GMLStaxParser()).localPart(GMLStaxParser.getSupportedLocalParts()).namespace(GML3.NAMESPACE);
        cfg.registerFinder(spf);
        StaxFormatterFinder sff = new StaxFormatterFinder(new GMLStaxFormatter(),
                                                          Geometry.class,
                                                          Envelope.class);
        cfg.registerFinder(sff);
    }
}
