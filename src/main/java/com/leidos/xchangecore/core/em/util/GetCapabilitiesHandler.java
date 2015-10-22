package com.leidos.xchangecore.core.em.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.usersmarts.cx.ows.OWS;
import com.usersmarts.cx.ows.api.Contact;
import com.usersmarts.cx.ows.api.GetCapabilitiesRequest;
import com.usersmarts.cx.ows.api.OWSException;
import com.usersmarts.cx.ows.api.Operation;
import com.usersmarts.cx.ows.api.OperationsMetadata;
import com.usersmarts.cx.ows.api.OwsRequest;
import com.usersmarts.cx.ows.api.Parameter;
import com.usersmarts.cx.ows.api.ResponsibleParty;
import com.usersmarts.cx.ows.api.ServiceIdentification;
import com.usersmarts.cx.ows.api.ServiceMetadata;
import com.usersmarts.cx.ows.api.ServiceProvider;
import com.usersmarts.cx.ows.spi.AbstractRequestHandler;
import com.usersmarts.cx.wms.WMS;
import com.usersmarts.cx.wms.api.Layer;
import com.usersmarts.cx.wms.api.Server;
import com.usersmarts.util.DOMUtils;
import com.vividsolutions.jts.geom.Envelope;

/**
 * GetCapabilitiesHandler
 *
 *
 * @author Santhosh Amanchi - Image Matters, LLC
 * @package com.leidos.xchangecore.core.em.util
 * @created Jun 28, 2011
 */
public class GetCapabilitiesHandler extends AbstractRequestHandler {

    public GetCapabilitiesHandler() {

    }

    public ServiceMetadata computeMetadata(Element viewContext, GetCapabilitiesRequest gcRequest,
            HttpServletRequest httpRequest) {

        String language = gcRequest.getLanguage();
        if (!("en-US".equals(language))) {
            throw new OWSException(OWSException.INVALIDVALUE, "language");
        }

        String host = httpRequest.getServerName();
        int sport = httpRequest.getServerPort();
        String path = httpRequest.getRequestURI();
        String uri = "http://" + host + ":" + sport + "/" + path;
        ServiceMetadata result = new ServiceMetadata();

        result.setVersion(gcRequest.getVersion());

        List<String> sections = gcRequest.getSections();
        if (sections.isEmpty() || sections.contains(OWS.ServiceIdentification.getLocalPart())) {
            Element general = DOMUtils.getChild(viewContext, "General");
            ServiceIdentification ident = new ServiceIdentification("UICDS Map Service",
                    gcRequest.getVersion());
            ident.setTitle(getElementValue(general, "Title"));
            ident.setAbstract(getElementValue(general, "Abstract"));
            ident.setKeywords(getKeywords(general));
            ident.setFees("None");
            ident.setAccessConstraints("None");
            result.setServiceIdentification(ident);
        }

        if (sections.isEmpty() || sections.contains(OWS.ServiceProvider.getLocalPart())) {
            ServiceProvider prov = new ServiceProvider();
            prov.setProviderName("");
            ResponsibleParty contact = new ResponsibleParty();
            Contact info = new Contact();
            contact.setContactInfo(info);
            prov.setServiceContact(contact);
            result.setServiceProvider(prov);
        }

        try {
            if (sections.isEmpty() || sections.contains(OWS.OPERATIONS_METADATA.getLocalPart())) {
                OperationsMetadata ops = new OperationsMetadata();
                {
                    Operation op = new Operation("GetCapabilities");
                    op.setGETUrl(uri);
                    Parameter versions = new Parameter("AcceptVersion");
                    versions.getValues().add(gcRequest.getVersion());
                    op.getOWSParameters().add(versions);
                    Parameter lang = new Parameter("language");
                    lang.getValues().add("en-US");
                    op.getOWSParameters().add(lang);
                    Parameter cformats = new Parameter("Format");
                    cformats.getValues().add("application/vnd.ogc.wms_xml");
                    op.getOWSParameters().add(cformats);
                    ops.addOperation(op);
                }
                {
                    Operation op = new Operation("GetMap");
                    op.setGETUrl(uri);
                    Parameter formats = new Parameter("Format");
                    formats.getValues().add("image/png");
                    formats.getValues().add("image/jpeg");
                    op.getOWSParameters().add(formats);
                    ops.addOperation(op);
                }
                result.setOperationsMetadata(ops);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        List<Layer> layers = getLayers(viewContext);
        if (sections.isEmpty() || sections.contains(WMS.LAYERS.getLocalPart())) {
            result.setContents(layers);
        }
        return result;
    }

    private String getElementValue(Element elem, String key) {

        Element valueElem = DOMUtils.getChild(elem, key);
        String value = DOMUtils.getContent(valueElem);
        return value;
    }

    private List<String> getKeywords(Element general) {

        List<String> keys = new ArrayList<String>();
        Element keywordsElem = DOMUtils.getChild(general, "KeywordList");
        if (keywordsElem == null)
            return keys;
        List<Element> keywords = DOMUtils.getChildren(keywordsElem, "Keyword");
        for (Element keyword : keywords) {
            String key = DOMUtils.getContent(keyword);
            keys.add(key);
        }
        return keys;
    }

    private List<Layer> getLayers(Element viewContext) {

        List<Layer> layers = new ArrayList<Layer>();
        Element layerList = DOMUtils.getChild(viewContext, "LayerList");
        List<Element> elements = DOMUtils.getChildren(layerList, "Layer");
        for (Element elem : elements) {
            Layer layer = new Layer();
            layer.setName(getElementValue(elem, "Name"));
            layer.setTitle(getElementValue(elem, "Title"));
            layer.setSrs(getElementValue(elem, "SRS"));
            layer.setBoundingBox(new Envelope(-180, 180, -90, 90));
            Server layerServer = getLayerServerInfo(elem, "Server");
            layer.setServer(layerServer);
            layers.add(layer);
        }
        return layers;
    }

    private Server getLayerServerInfo(Element elem, String key) {

        Node server = DOMUtils.getChild(elem, key);
        Element or = DOMUtils.getChild(server, "OnlineResource");
        String href = or.getAttribute("xlink:href");
        String ser = ((Element) server).getAttribute("service");
        String tit = ((Element) server).getAttribute("title");
        String ver = ((Element) server).getAttribute("version");
        URI uri = null;
        try {
            uri = new URI(href);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Server layerServer = new Server(uri, ser, ver, tit);
        return layerServer;
    }

    protected String getRequestParameter(String name, HttpServletRequest request, String fallback) {

        for (Object key : request.getParameterMap().keySet()) {
            if (((String) key).equalsIgnoreCase(name)) {
                return request.getParameter((String) key);
            }
        }
        return fallback;
    }

    @Override
    public ModelAndView handleRequest(OwsRequest owsRequest, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        ModelAndView result = null;

        return result;
    }

    @Override
    public OwsRequest parseQueryParameters(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String version = getRequestParameter("version", request, null);
        String language = getRequestParameter("language", request, "en-US");

        GetCapabilitiesRequest result = new GetCapabilitiesRequest();
        if (version != null) {
            result.setVersion(version);
            result.getVersions().add(version);
        }
        result.setLanguage(language);
        return result;
    }

}
