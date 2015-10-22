<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xnl="urn:oasis:names:tc:ciq:xnl:3" xmlns:geo-oasis="urn:oasis:names:tc:emergency:EDXL:HAVE:1.0:geo-oasis" xmlns:have="urn:oasis:names:tc:emergency:EDXL:HAVE:1.0" xmlns:ucore="http://ucore.gov/ucore/2.0" xmlns:gml="http://www.opengis.net/gml" xmlns:ucoregml="http://www.opengis.net/gml/3.2" xmlns:ns1="http://niem.gov/niem/niem-core/2.0" xmlns:inc="http://uicds.org/incident">
	<xsl:output version="1.0" method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>
<!--	<xsl:param name="OrganizationID">Organization-<xsl:value-of select="generate-id(inc:Incident/ns1:ActivityDescriptionText)"/></xsl:param>  -->
<!--	<xsl:param name="EventID">Event-<xsl:value-of select="generate-id(inc:Incident/ns1:ActivityDescriptionText)"/></xsl:param> -->
	<xsl:param name="OrganizationID">Organization-<xsl:value-of select="generate-id(inc:Incident/ns1:ActivityIdentification/ns1:IdentificationID)"/></xsl:param>
	<xsl:param name="EventID">Event-<xsl:value-of select="generate-id(inc:Incident/ns1:ActivityIdentification/ns1:IdentificationID)"/></xsl:param>
	<xsl:param name="LocationID">Location-<xsl:value-of select="generate-id(inc:Incident/ns1:ActivityIdentification/ns1:IdentificationID)"/></xsl:param>
	<xsl:param name="PolygonID">Polygon-<xsl:value-of select="generate-id(inc:Incident/ns1:ActivityIdentification/ns1:IdentificationID)"/></xsl:param>
	<xsl:param name="OccursAtID">OccursAt-<xsl:value-of select="generate-id(inc:Incident/ns1:ActivityIdentification/ns1:IdentificationID)"/></xsl:param>
	<xsl:template match="/">
		<Digest xmlns="http://ucore.gov/ucore/2.0" xmlns:ns="http://www.opengis.net/gml/3.2" xmlns:ddms="http://metadata.dod.mil/mdr/ns/DDMS/2.0/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
			<Organization>
				<xsl:attribute name="id"><xsl:value-of select="$OrganizationID"/></xsl:attribute>
				<What>
					<xsl:attribute name="ucore:code">Organization</xsl:attribute>
					<xsl:attribute name="ucore:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
				</What>
				<Name>
					<Value><xsl:value-of select="inc:Incident/ns1:IncidentJurisdictionalOrganization/ns1:OrganizationName"/></Value>
				</Name>
			</Organization>
			<Event>
				<xsl:attribute name="id"><xsl:value-of select="$EventID"/></xsl:attribute>
				<Descriptor><xsl:value-of select="inc:Incident/ns1:ActivityDescriptionText"/></Descriptor>
				<Identifier ucore:codespace="http://niem.gov/niem/niem-core/2.0" ucore:code="ActivityName" ucore:label="ID" ><xsl:value-of select="inc:Incident/ns1:ActivityName"/></Identifier>
				<Identifier ucore:codespace="http://uicds.us/identifier" ucore:code="label" ucore:label="Label" ><xsl:value-of select="substring(inc:Incident/ns1:ActivityName,1,15)"/>...</Identifier>
				<SimpleProperty ucore:label="Icon" ucore:code="icon" ucore:codespace="http://uicds.us/style"><xsl:text> </xsl:text></SimpleProperty>
				<SimpleProperty ucore:label="Color" ucore:code="color" ucore:codespace="http://uicds.us/style"><xsl:text> </xsl:text></SimpleProperty>
				<SimpleProperty ucore:label="Scalar" ucore:code="scalar" ucore:codespace="http://uicds.us/style"><xsl:text> </xsl:text></SimpleProperty>
				<What>
					<xsl:attribute name="ucore:code"><xsl:value-of select="inc:Incident/ns1:ActivityCategoryText"/></xsl:attribute>
					<xsl:attribute name="ucore:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
				</What>
		<!--	<Who>
					<xsl:attribute name="ucore:code"><xsl:value-of select="inc:Incident/ns1:ActivityName"/></xsl:attribute>
					<xsl:attribute name="ucore:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
				</Who>
				<Where>
					<xsl:attribute name="ucore:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
					<xsl:if test="count(inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationStreet/ns1:StreetNumberText) > 0">
						<xsl:attribute name="ucore:code"><xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationStreet/ns1:StreetNumberText"/><xsl:text> </xsl:text><xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationStreet/ns1:StreetName"/><xsl:text> </xsl:text><xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationCityName"/><xsl:text> </xsl:text><xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationStateUSPostalServiceCode"/><xsl:text> </xsl:text><xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationPostalCode"/></xsl:attribute>
					</xsl:if>
					<xsl:if test="count(inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationStreet/ns1:StreetNumberText) = 0">
						<xsl:attribute name="ucore:code"><xsl:value-of select="inc:Incident/ns1:IncidentJurisdictionalOrganization"/></xsl:attribute>
					</xsl:if>
				</Where>
				<When>
					<xsl:attribute name="ucore:code"><xsl:value-of select="inc:Incident/ns1:ActivityDate/ns1:DateTime"/></xsl:attribute>
					<xsl:attribute name="ucore:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
				</When>
-->
			</Event>
			<Location>
				<xsl:attribute name="id"><xsl:value-of select="$LocationID"/></xsl:attribute>
				<xsl:for-each select="inc:Incident/ns1:IncidentLocation/ns1:LocationArea/ns1:AreaCircularRegion">
          			<xsl:variable name="latdecimal">
           				<xsl:choose>
           					<xsl:when test="contains(string(ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLatitude/ns1:LatitudeDegreeValue), '-')">
           					  <xsl:value-of select="concat('-',format-number(abs(ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLatitude/ns1:LatitudeDegreeValue) + (ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLatitude/ns1:LatitudeMinuteValue div 60) + (ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLatitude/ns1:LatitudeSecondValue div 3600),'###.00000'))"/>
           					</xsl:when>
           				<xsl:otherwise>
           					<xsl:value-of select="format-number(ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLatitude/ns1:LatitudeDegreeValue + (ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLatitude/ns1:LatitudeMinuteValue div 60) + (ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLatitude/ns1:LatitudeSecondValue div 3600),'###.00000')"/>
           				</xsl:otherwise>
           				</xsl:choose>	
      				</xsl:variable>  
           			<xsl:variable name="longdecimal">
           			<xsl:choose>
           				<xsl:when test="contains(string(ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLongitude/ns1:LongitudeDegreeValue), '-')">
           					<xsl:value-of select="concat('-',format-number(abs(ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLongitude/ns1:LongitudeDegreeValue) + (ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLongitude/ns1:LongitudeMinuteValue div 60) + (ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLongitude/ns1:LongitudeSecondValue div 3600), '###.00000'))"/>
                        </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="format-number(ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLongitude/ns1:LongitudeDegreeValue + (ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLongitude/ns1:LongitudeMinuteValue div 60) + (ns1:CircularRegionCenterCoordinate/ns1:GeographicCoordinateLongitude/ns1:LongitudeSecondValue div 3600), '###.00000')"/>
                        </xsl:otherwise>
                    </xsl:choose>
           			</xsl:variable>
					 <GeoLocation>
						<CircleByCenterPoint>
							<ucoregml:CircleByCenterPoint>
								<xsl:attribute name="numArc">1</xsl:attribute>
								<ucoregml:pos>
									<xsl:attribute name="srsName">EPSG:4326</xsl:attribute>
                                    <xsl:value-of select="$latdecimal"/><xsl:text> </xsl:text><xsl:value-of select="$longdecimal"/>
								</ucoregml:pos>
								<ucoregml:radius>
									<xsl:attribute name="uom">SMI</xsl:attribute>
									<xsl:value-of select="ns1:CircularRegionRadiusLengthMeasure/ns1:MeasurePointValue"/>
								</ucoregml:radius>
							</ucoregml:CircleByCenterPoint>
						</CircleByCenterPoint>	
					</GeoLocation>
				</xsl:for-each>
				<xsl:if test="count(inc:Incident/ns1:IncidentLocation/ns1:LocationArea/ns1:AreaPolygonGeographicCoordinate) > 1">
					<GeoLocation>
						<Polygon>
							<ucoregml:Polygon srsName="EPSG:4326">
								<xsl:attribute name="ucoregml:id"><xsl:value-of select="$PolygonID"/></xsl:attribute>
								<ucoregml:exterior>
									<ucoregml:LinearRing>
										<xsl:for-each select="inc:Incident/ns1:IncidentLocation/ns1:LocationArea/ns1:AreaPolygonGeographicCoordinate">
											<ucoregml:pos srsName="EPSG:4326"><xsl:value-of select="format-number(ns1:GeographicCoordinateLatitude/ns1:LatitudeDegreeValue + (ns1:GeographicCoordinateLatitude/ns1:LatitudeMinuteValue div 60) + (ns1:GeographicCoordinateLatitude/ns1:LatitudeSecondValue div 3600),'###.00000')"/>, <xsl:value-of select="format-number(ns1:GeographicCoordinateLongitude/ns1:LongitudeDegreeValue - (ns1:GeographicCoordinateLongitude/ns1:LongitudeMinuteValue div 60) - (ns1:GeographicCoordinateLongitude/ns1:LongitudeSecondValue div 3600),'###.00000')"/> </ucoregml:pos>
										</xsl:for-each>
									</ucoregml:LinearRing>
								</ucoregml:exterior>
							</ucoregml:Polygon>
						</Polygon>
					</GeoLocation>
				</xsl:if>
				<PhysicalAddress>
					<ddms:postalAddress>
					   	<ddms:street><xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationStreet/ns1:StreetNumberText"/><xsl:text> </xsl:text>
<xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationStreet/ns1:StreetName"/></ddms:street>
						<ddms:city><xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationCityName"></xsl:value-of></ddms:city>
						<ddms:state><xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationStateUSPostalServiceCode"></xsl:value-of></ddms:state>
						<ddms:postalCode><xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationPostalCode"></xsl:value-of></ddms:postalCode>
						<ddms:countryCode>
							<xsl:attribute name="ddms:value"><xsl:value-of select="inc:Incident/ns1:IncidentLocation/ns1:LocationAddress/ns1:StructuredAddress/ns1:LocationCountryISO3166Alpha2Code"></xsl:value-of></xsl:attribute>
							<xsl:attribute name="ddms:qualifier">ISO 3166-1</xsl:attribute>
						</ddms:countryCode>
					</ddms:postalAddress>
				</PhysicalAddress>
			</Location>
			<OccursAt>
				<xsl:attribute name="id"><xsl:value-of select="$OccursAtID"/></xsl:attribute>
				<Time>
					<TimeInstant>
						<Value><xsl:value-of select="current-dateTime()"/></Value> 
					</TimeInstant>
				</Time>
				<EventRef>
					<xsl:attribute name="ref"><xsl:value-of select="$EventID"/></xsl:attribute>
				</EventRef>
				<LocationRef>
					<xsl:attribute name="ref"><xsl:value-of select="$LocationID"/></xsl:attribute>
				</LocationRef>
			</OccursAt>
		</Digest>
	</xsl:template>
</xsl:stylesheet>