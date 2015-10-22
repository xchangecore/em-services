package com.leidos.xchangecore.core.em.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.incidentCommandStructureService.CreateCommandStructureRequestDocument;
import org.uicds.incidentCommandStructureService.CreateCommandStructureResponseDocument;
import org.uicds.incidentCommandStructureService.GetCommandStructureByIncidentRequestDocument;
import org.uicds.incidentCommandStructureService.GetCommandStructureByIncidentResponseDocument;
import org.uicds.incidentCommandStructureService.GetCommandStructureRequestDocument;
import org.uicds.incidentCommandStructureService.GetCommandStructureResponseDocument;
import org.uicds.incidentCommandStructureService.UpdateCommandStructureRequestDocument;
import org.uicds.incidentCommandStructureService.UpdateCommandStructureResponseDocument;
import org.uicds.organizationElement.OrganizationElementDocument;

import com.leidos.xchangecore.core.em.service.IncidentCommandService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;

/**
 * The XchangeCore Incident Command Service allows clients to create and modify command structures for
 * incidents (including both ICS and MACS) and associate resources to organizational roles within
 * these structures.
 * <p>
 * The Organizational Structure is defined as the following data structure:<br/>
 * <img src="doc-files/OrganizationElement.png"/> <BR>
 * <p>
 * Organizational structures for an incident are represented by building up hierarchies of instances
 * of OrganizationalElements. Positions in the organizational structure are represented by an
 * OrganizationPositionType. Positions contain a reference to a XchangeCore Resource Profile (role) and a
 * XchangeCore Resource Instance (resource/person). The references should be the identifier of the
 * resource profile or resource instance. Resource profiles are managed by the Resource Profile
 * Service and resource instances are managed by the Resource Instance Service.
 * 
 * The generic nature of the OrganizationalElement structure was chosen to provide the maximum
 * flexibility for representing any type of organizational structure.
 * <p>
 * The Incident Command Service manages organizational structures for an incident as work products.
 * The work product types are "ICS" (Incident Command Structures) or "MACS" (Multi-Agency
 * Coordination Systems). An ICS work product type will be assigned if the role profile of the
 * person in charge of the top level OrganizationElement starts with "Incident Commander" otherwise
 * the work product type will be MACS.
 * <p>
 * 
 * @author Nathan Lewnes
 * @see <a href="../../wsdl/IncidentCommandService.wsdl">Appendix: IncidentCommandService.wsdl</a>
 * @see <a href="../../services/IncidentCommand/0.1/IncidentCommandStructure.xsd">Appendix:
 *      IncidentCommandStructure.xsd</a>
 * @see <a href="../../services/IncidentCommand/0.1/OrganizationElement.xsd">Appendix:
 *      OrganizationElement.xsd</a>
 * @idd
 */
@Endpoint
public class IncidentCommandServiceEndpoint
    implements ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(IncidentCommandServiceEndpoint.class);

    @Autowired
    private IncidentCommandService icService;

    @Autowired
    private WorkProductService productService;

    /**
     * Get the Incident Command Structure work product for a particular incident by supplying the
     * incident identifier (Interest Group identifier).
     * 
     * @param GetCommandStructureByIncidentRequestDocument
     * 
     * @return GetCommandStructureByIncidentResponseDocument
     * @see <a href="../../services/IncidentCommand/0.1/IncidentCommandStructure.xsd">Appendix:
     *      IncidentCommandStructure.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentCommandStructureService, localPart = "GetCommandStructureByIncidentRequest")
    public GetCommandStructureByIncidentResponseDocument getCommandStructureByIncident(GetCommandStructureByIncidentRequestDocument request) {

        GetCommandStructureByIncidentResponseDocument response = GetCommandStructureByIncidentResponseDocument.Factory.newInstance();
        response.addNewGetCommandStructureByIncidentResponse().setWorkProduct(WorkProductHelper.toWorkProduct(icService.getCommandStructureByIncident(request.getGetCommandStructureByIncidentRequest())));

        return response;
    }

    /**
     * Gets the Incident Command Structure work product that corresponds to the supplied
     * WorkProductIdentification in the request. If null is passed in it will return an empty
     * OrganizationDocument.
     * 
     * @param GetCommandStructureRequestDocument
     * 
     * @return GetCommandStructureResponseDocument
     * @see <a href="../../services/IncidentCommand/0.1/IncidentCommandStructure.xsd">Appendix:
     *      IncidentCommandStructure.xsd</a>
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentCommandStructureService, localPart = "GetCommandStructureRequest")
    public GetCommandStructureResponseDocument getCommandStructure(GetCommandStructureRequestDocument request) {

        GetCommandStructureResponseDocument response = GetCommandStructureResponseDocument.Factory.newInstance();
        response.addNewGetCommandStructureResponse().setWorkProduct(WorkProductHelper.toWorkProduct(productService.getProduct(request.getGetCommandStructureRequest().getWorkProductIdentification())));

        return response;
    }

    /**
     * Create an Incident Command Structure work product and optionally associate with an incident
     * by supplying the incident identifier (Interest Group identifier). ICS type structures are
     * created if the top level role is "Incident Commander". Otherwise the work product is of type
     * MACS.
     * 
     * @param CreateCommandStructureRequestDocument
     * 
     * @return CreateCommandStructureResponseDocument
     * @see <a href="../../services/IncidentCommand/0.1/IncidentCommandStructure.xsd">Appendix:
     *      IncidentCommandStructure.xsd</a>
     * @see <a href="../../services/IncidentCommand/0.1/OrganizationElement.xsd">Appendix:
     *      OrganizationElement.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentCommandStructureService, localPart = "CreateCommandStructureRequest")
    public CreateCommandStructureResponseDocument createCommandStructure(CreateCommandStructureRequestDocument request) {

        UpdateCommandStructureRequestDocument updateRequest = UpdateCommandStructureRequestDocument.Factory.newInstance();
        updateRequest.addNewUpdateCommandStructureRequest().addNewOrganizationElement().set(request.getCreateCommandStructureRequest().getOrganizationElement());

        UpdateCommandStructureResponseDocument updateResponse = updateCommandStructure(updateRequest);

        CreateCommandStructureResponseDocument response = CreateCommandStructureResponseDocument.Factory.newInstance();
        response.addNewCreateCommandStructureResponse().addNewWorkProductPublicationResponse().set(updateResponse.getUpdateCommandStructureResponse().getWorkProductPublicationResponse());
        return response;
    }

    /**
     * Update the Incident Command Structure work product and optionally associate it with incident
     * by supplying the incident identifier (Interest Group identifier).
     * 
     * @param UpdateCommandStructureRequestDocument
     * 
     * @return UpdateCommandStructureResponseDocument
     * @see <a href="../../services/IncidentCommand/0.1/IncidentCommandStructure.xsd">Appendix:
     *      IncidentCommandStructure.xsd</a>
     * @see <a href="../../services/IncidentCommand/0.1/OrganizationElement.xsd">Appendix:
     *      OrganizationElement.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentCommandStructureService, localPart = "UpdateCommandStructureRequest")
    public UpdateCommandStructureResponseDocument updateCommandStructure(UpdateCommandStructureRequestDocument request) {

        log.debug(request.toString());
        OrganizationElementDocument orgDoc = OrganizationElementDocument.Factory.newInstance();
        orgDoc.addNewOrganizationElement().set(request.getUpdateCommandStructureRequest().getOrganizationElement());

        UpdateCommandStructureResponseDocument response = UpdateCommandStructureResponseDocument.Factory.newInstance();
        response.addNewUpdateCommandStructureResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(icService.updateCommandStructure(request.getUpdateCommandStructureRequest().getWorkProductIdentification(),
            orgDoc,
            request.getUpdateCommandStructureRequest().getIncidentID())));
        return response;
    }
}
