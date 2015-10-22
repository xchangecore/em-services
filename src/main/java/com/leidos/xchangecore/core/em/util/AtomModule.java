package com.leidos.xchangecore.core.em.util;

import javax.xml.stream.XMLStreamException;

import com.usersmarts.pub.atom.APP;
import com.usersmarts.pub.atom.ATOM;
import com.usersmarts.pub.atom.Categories;
import com.usersmarts.pub.atom.Category;
import com.usersmarts.pub.atom.Collection;
import com.usersmarts.pub.atom.Content;
import com.usersmarts.pub.atom.Entry;
import com.usersmarts.pub.atom.Feed;
import com.usersmarts.pub.atom.Link;
import com.usersmarts.pub.atom.Service;
import com.usersmarts.pub.atom.Workspace;
import com.usersmarts.pub.atom.xml.AtomFormatter;
import com.usersmarts.pub.atom.xml.ContentFormatter;
import com.usersmarts.pub.georss.xml.GeoRSSFormatter;
import com.usersmarts.util.Coerce;
import com.usersmarts.util.StringUtils;
import com.usersmarts.util.stax.ExtendedXMLStreamReader;
import com.usersmarts.util.stax.ExtendedXMLStreamWriter;
import com.usersmarts.util.stax.StaxUtils;
import com.usersmarts.xmf2.Configuration;
import com.usersmarts.xmf2.MarshalContext;
import com.usersmarts.xmf2.MarshalException;
import com.usersmarts.xmf2.stax.StaxAdaptersModule;
import com.usersmarts.xmf2.stax.StaxFormatter;
import com.usersmarts.xmf2.stax.StaxFormatterFinder;
import com.usersmarts.xmf2.stax.StaxParser;
import com.usersmarts.xmf2.stax.StaxParserFinder;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class AtomModule
    extends com.usersmarts.pub.atom.xml.AtomModule {

    @Override
    public void configure(Configuration ctx) {

        super.configure(ctx);
        ctx.require(StaxAdaptersModule.class);
        ctx.require(GMLStaxModule.class);
        // ctx.require(GeoRSSModule.class);

        addFinder(new StaxParserFinder(getFeedParser()).localPart(ATOM.FEED));
        addFinder(new StaxParserFinder(getEntryParser()).localPart(ATOM.ENTRY));
        addFinder(new StaxParserFinder(new ServiceParser()).localPart(APP.SERVICE));
        addFinder(new StaxParserFinder(new WorkspaceParser()).localPart(APP.WORKSPACE));
        addFinder(new StaxParserFinder(new CollectionParser()).localPart(APP.COLLECTION));
        addFinder(new StaxParserFinder(new CategoriesParser()).localPart(APP.CATEGORIES));
        addFinder(new StaxFormatterFinder(new AtomFormatter(), Feed.class, Entry.class, Link.class));
        addFinder(new StaxFormatterFinder(new ContentFormatter(), Content.class));
        addFinder(new StaxFormatterFinder(new ServiceFormatter(), Service.class));
        // addFinder(new StaxParserFinder(new GeoRSSParser(), GEORSS.BOX, GEORSS.POINT, GEORSS.LINE,
        // GEORSS.POLYGON, GEORSS.WHERE));
        addFinder(new StaxFormatterFinder(new GeoRSSFormatter(), Geometry.class, Envelope.class));
    }

    static class ServiceParser
        extends StaxParser {

        @Override
        public Object parse(MarshalContext ctx, ExtendedXMLStreamReader in, Object out)
            throws MarshalException, XMLStreamException {

            Service service = new Service();
            in.next();
            for (int depth = in.getDepth(); depth <= in.getDepth();) {
                parseOrConsume(ctx, in, service);
            }
            return service;
        }
    }

    static class WorkspaceParser
        extends StaxParser {

        @Override
        public Object parse(MarshalContext ctx, ExtendedXMLStreamReader in, Object out)
            throws MarshalException, XMLStreamException {

            Workspace workspace = new Workspace();
            in.next();
            for (int depth = in.getDepth(); depth <= in.getDepth();) {
                if (StaxUtils.match(in, ATOM.TITLE)) {
                    workspace.setTitle(StaxUtils.getText(in));
                } else {
                    parseOrConsume(ctx, in, workspace);
                }
            }
            if (out instanceof Service) {
                ((Service) out).getWorkspaces().add(workspace);
            }
            return workspace;
        }
    }

    static class CollectionParser
        extends StaxParser {

        @Override
        public Object parse(MarshalContext ctx, ExtendedXMLStreamReader in, Object out)
            throws MarshalException, XMLStreamException {

            Collection result = new Collection(in.getAttributeValue(null, "href"));
            in.next();

            for (int depth = in.getDepth(); depth <= in.getDepth();) {
                if (StaxUtils.match(in, ATOM.TITLE)) {
                    result.setTitle(StaxUtils.getText(in));
                } else if (StaxUtils.match(in, APP.ACCEPT)) {
                    String accept = StaxUtils.getText(in);
                    result.getAccepts().add(accept);
                } else {
                    parseOrConsume(ctx, in, result);
                }
            }
            if (out instanceof Workspace) {
                ((Workspace) out).append(APP.COLLECTION, result);
            }
            return result;
        }
    }

    static class CategoriesParser
        extends StaxParser {

        @Override
        public Object parse(MarshalContext ctx, ExtendedXMLStreamReader in, Object out)
            throws MarshalException, XMLStreamException {

            Categories categories = null;
            String href = in.getAttributeValue(null, "href");

            if (StringUtils.isNotEmpty(href)) {
                categories = new Categories(href);
                // no contents are allowed if href is provided.
                in.skipElement(true);
            } else {
                boolean fixed = "yes".equals(in.getAttributeValue(null, "fixed"));
                String scheme = in.getAttributeValue(null, "scheme");
                categories = new Categories(scheme, fixed);
                in.next();

                for (int depth = in.getDepth(); depth <= in.getDepth();) {
                    if (StaxUtils.match(in, ATOM.CATEGORY)) {
                        String term = in.getAttributeValue(null, "term");
                        String s = in.getAttributeValue(null, "scheme");
                        String label = in.getAttributeValue(null, "label");
                        Category category = new Category(term, s, label);
                        in.skipElement(true);
                        categories.getCategories().add(category);
                    } else {
                        parseOrConsume(ctx, in, categories);
                    }
                }
            }
            if (out instanceof Collection) {
                ((Collection) out).setCategories(categories);
            }
            return categories;
        }
    }

    static class ServiceFormatter
        extends StaxFormatter {

        @Override
        public Object format(MarshalContext ctx, Object object, ExtendedXMLStreamWriter writer)
            throws MarshalException, XMLStreamException {

            Service service = (Service) object;

            writer.setDefaultNamespace(APP.NAMESPACE);
            writer.writeStartElement(APP.SERVICE);
            writer.writeNamespace("atom", ATOM.NAMESPACE);

            for (Workspace workspace : service.getWorkspaces()) {
                writer.writeStartElement(APP.WORKSPACE);
                StaxUtils.appendElementWithText(writer, ATOM.TITLE, workspace.getTitle());

                //extension to AtomPub to allow links directly to the workspace
                for (Object l : workspace.getList(ATOM.LINK)) {
                    if (l instanceof Link && StringUtils.isNotEmpty(((Link) l).getHref())) {
                        Link link = (Link) l;
                        writer.element(ATOM.LINK);
                        writer.attribute("rel", Coerce.toString(link.getRel()));
                        writer.attribute("title", Coerce.toString(link.getTitle()));
                        writer.attribute("type", Coerce.toString(link.getType(),
                            "application/atom+xml"));
                        writer.attribute("href", Coerce.toString(link.getHref()));
                        writer.end();
                    }
                }

                for (Collection collection : workspace.getCollections()) {
                    writer.writeStartElement(APP.COLLECTION);
                    // need to apply a template to the href!!! Perhaps not
                    // here???
                    writer.writeNonEmptyAttribute("href", collection.getHref());
                    StaxUtils.appendElementWithText(writer, ATOM.TITLE, collection.getTitle());

                    for (String accept : collection.getAccepts()) {
                        StaxUtils.appendElementWithText(writer, APP.ACCEPT, accept);
                    }
                    Categories categories = collection.getCategories();
                    if (categories != null) {
                        writer.writeStartElement(APP.CATEGORIES);
                        writer.writeNonEmptyAttribute("src", categories.getHref());
                        writer.writeNonEmptyAttribute("scheme", categories.getScheme());
                        writer.writeAttribute("fixed", categories.isFixed() ? "yes" : "no");

                        for (Category category : categories.getCategories()) {
                            writer.writeStartElement(ATOM.CATEGORY);
                            writer.writeNonEmptyAttribute("term", category.getTerm());
                            writer.writeNonEmptyAttribute("scheme", category.getScheme());
                            writer.writeNonEmptyAttribute("label", category.getLabel());
                            writer.writeEndElement(); // CATEGORY
                        }
                        writer.writeEndElement(); // CATEGORIES
                    }
                    writer.writeEndElement(); // COLLECTION
                }
                writer.writeEndElement(); // WORKSPACE
            }
            writer.writeEndElement(); // SERVICE
            return writer;
        }
    }
}
