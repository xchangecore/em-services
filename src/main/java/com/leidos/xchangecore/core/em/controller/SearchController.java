package com.leidos.xchangecore.core.em.controller;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import com.leidos.xchangecore.core.infrastructure.controller.WorkProductQueryBuilder;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.ConfigurationService;
import com.usersmarts.cx.Entity;
import com.usersmarts.cx.Workspace;
import com.usersmarts.cx.hbm.search.HibernateSearchQueryProcessor;
import com.usersmarts.cx.hbm.search.lucene.TypeSystem;
import com.usersmarts.cx.hbm.search.lucene.TypeSystemUtils;
import com.usersmarts.cx.search.Query;
import com.usersmarts.cx.search.QueryBuilder;
import com.usersmarts.cx.search.QueryProcessor;
import com.usersmarts.cx.search.Results;
import com.usersmarts.cx.web.AbstractConnectorController;
import com.usersmarts.cx.web.XmfView;
import com.usersmarts.pub.atom.ATOM;
import com.usersmarts.xmf2.MarshalContext;

/**
 * The Search Service provides XchangeCore clients with services to discover and access work products
 * using OpenSearch enabled feeds.
 *
 * The Search Service accepts Google-style query strings via the q parameter. For instance:
 *
 * <pre>
 *     ?q=productType=incident&updatedBy=bob
 * </pre>
 *
 * The table follow describes the supported query terms.
 * <table>
 * <thead>
 * <tr>
 * <td width="20%"><b>Term</b></td>
 * <td><b>Description</b></td></thead> <tbody>
 * <tr>
 * <td width="20%">productID</td>
 * <td>The identifier of a Work Product</td>
 * </tr>
 * <tr>
 * <td width="20%">productType</td>
 * <td>The type of a Work Product</td>
 * </tr>
 * <tr>
 * <td width="20%">productVersion</td>
 * <td>The version of a Work Product</td>
 * </tr>
 * <tr>
 * <td width="20%">createdDate</td>
 * <td>The date and time the Work Product was created</td>
 * </tr>
 * <tr>
 * <td width="20%">createdBy</td>
 * <td>The name of the user that created the Work Product</td>
 * </tr>
 * <tr>
 * <td width="20%">updatedDate</td>
 * <td>The date and time the Work Product was last updated</td>
 * </tr>
 * <tr>
 * <td width="20%">updatedBy</td>
 * <td>The name of the user that last updated the Work Product</td>
 * </tr>
 * <tr>
 * <td width="20%">active</td>
 * <td></td>Boolean (true or false) indicator of the Work Product status
 * </tr>
 * <tr>
 * <td width="20%">cyber.protocol</td>
 * <td>Any internet protocol (HTTP, FTP, etc) contained in the Work Product metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">cyber.address</td>
 * <td>Any internet address contained in the Work Product metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">digest.event.descriptor</td>
 * <td>The textual description of an event contained in the Work Product metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">where.minx</td>
 * <td>The West boundary of bounding box of all location information contained within the Work
 * Product Metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">where.maxx</td>
 * <td>The East boundary of bounding box of all location information contained within the Work
 * Product Metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">where.miny</td>
 * <td>The South boundary of bounding box of all location information contained within the Work
 * Product Metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">where.maxy</td>
 * <td>The North boundary of bounding box of all location information contained within the Work
 * Product Metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">where.city</td>
 * <td>Any city name referenced within the Work Product metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">where.postalCode</td>
 * <td>Any postal code referenced within the Work Product metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">where.province</td>
 * <td>Any province name referenced within the Work Product metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">where.state</td>
 * <td>Any state name referenced within the Work Product metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">where.street</td>
 * <td>Any street address referenced within the Work Product metadata, including the street number</td>
 * </tr>
 * <tr>
 * <td width="20%">where.countryCode</td>
 * <td>Any count code referenced within the Work Product metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">digest.event.what</td>
 * <td>The type of any "Thing" referenced within the Work Product metadata</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * The format of the results is controlled by the format parameter. For instance:
 *
 * <pre>
 *     ?q=productType=incident&updatedBy=bob&format=atom
 * </pre>
 *
 * Currently supported formats:
 * <table>
 * <thead>
 * <tr>
 * <td width="20%"><b>Format</b></td>
 * <td><b>Description</b></td>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td width="20%">atom</td>
 * <td>Atom Syndication Format with GeoRSS GML extensions</td>
 * <tr>
 * <td width="20%">rss</td>
 * <td>RSS 2.0 with W3C Basic Geo extensions</td> </tbody>
 * </table>
 *
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/1.1">OpenSearch 1.1
 *      Specification</a>
 * @see <a href="http://tools.ietf.org/html/rfc4287">The Atom Syndication Format</a>
 * @see <a href="http://cyber.law.harvard.edu/rss/rss.html">RSS 2.0 Specification</a>
 * @see <a href="http://www.georss.org/">GeoRSS Specification</a>
 * @see <a href="http://www.georss.org/W3C_Basic">W3C Basic Geo Vocabulary</a>
 * @author Christopher Lakey
 * idd -- remove it from IDD by DDH
 *
 */
public class SearchController
    extends AbstractConnectorController {

    private ConfigurationService configurationService;

    @PersistenceContext
    EntityManager entityManager;

    private QueryProcessor processor;

    private final QueryBuilder queryBuilder = new WorkProductQueryBuilder();

    public SearchController() {

        String[] supportedMethods = {
            "GET"
        };
        setSupportedMethods(supportedMethods);
    }

    protected QueryProcessor getProcessor() {

        if (processor == null) {
            // processor = new CriteriaQueryProcessor();
            TypeSystem ts = TypeSystemUtils.instance().getTypeSystem();
            processor = new HibernateSearchQueryProcessor(ts);
        }
        return processor;
    }

    public QueryBuilder getQueryBuilder() {

        return queryBuilder;
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
                                                 HttpServletResponse response) throws Exception {

        ModelAndView result = new ModelAndView();

        // build query from request querystring
        Query query = getQueryBuilder().buildQuery(request); // , condition);
        query.addCollections(new QName(WorkProduct.class.getName()));
        query.getSort().setSort(ATOM.UPDATED);

        // query for results
        Results<?> searchResults = search(request, query);
        logger.debug("Found " + searchResults.getResultSize() + " results");
        result.addObject("output", searchResults);

        // write out results
        XmfView xmfView = new XmfView();
        MarshalContext marshalContext = new MarshalContext(SearchResultsModule.class);
        String format = request.getParameter("format");
        if (StringUtils.isEmpty(format)) {
            format = "atom";
        }
        marshalContext.setProperty("format", format);
        marshalContext.setProperty("baseURL", configurationService.getRestBaseURL());
        xmfView.setMarshalContext(marshalContext);
        result.setView(xmfView);
        return result;
    }

    /**
     * @param request
     * @param query
     * @return Results<Entity>
     * @throws IOException
     * @throws ServletException
     */
    protected Results<Entity> search(HttpServletRequest request, Query query) throws IOException,
        ServletException {

        Workspace workspace = (Workspace) getConnector(request);
        QueryProcessor processor = getProcessor();
        Results<Entity> results = processor.search(workspace, query);
        return results;
    }

    public void setConfigurationService(ConfigurationService configurationService) {

        configurationService = configurationService;
    }

    public void setEntityManager(EntityManager entityManager) {

        entityManager = entityManager;
    }

    public void setQueryBuilder(QueryBuilder queryBuilder) {

        queryBuilder = queryBuilder;
    }

}
