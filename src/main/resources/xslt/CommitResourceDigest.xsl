<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns="http://ucore.gov/ucore/2.0" xmlns:ucoregml="http://www.opengis.net/gml/3.2" xmlns:rmsg="urn:oasis:names:tc:emergency:EDXL:RM:1.0:msg" xmlns:rm="urn:oasis:names:tc:emergency:EDXL:RM:1.0" xmlns:gml="http://www.opengis.net/gml" xmlns:ns1="http://metadata.dod.mil/mdr/ns/DDMS/2.0/" xmlns:xal="urn:oasis:names:tc:ciq:xal:3">
	<xsl:output version="1.0" method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes" cdata-section-elements="ns:Descriptor"/>
	<xsl:param name="OrganizationID">Organization-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="EventID_Incident">
		<xsl:if test="count(rmsg:CommitResource/rmsg:IncidentInformation/rm:IncidentID) > 0">
			<xsl:value-of select="rmsg:CommitResource/rmsg:IncidentInformation/rm:IncidentID"/>
		</xsl:if>
	</xsl:param>
	<xsl:param name="EventID_Resource_Prefix"><xsl:value-of select="rmsg:CommitResource/rmsg:OriginatingMessageID"/>-</xsl:param>
	<xsl:param name="LocationID_Schedule">Location-Schedule-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="LocationID_Contact">Location-Contact-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="LocatedAtID">LocatedAt-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="PolygonID">Polygon-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="OccursAtID">OccursAt-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="HasDestinationOfID">HasDestinationOf-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="CauseOf">CauseOf-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="PointID_Schedule">point-schedule-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="PointID_Contact">point-contact-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="InvolvedInID">InvolvedInID-<xsl:value-of select="generate-id(.)"/></xsl:param>
	<xsl:param name="UCoreWho"><xsl:value-of select="rmsg:CommitResource/rmsg:ContactInformation/rm:ContactDescription"/><xsl:text> </xsl:text><xsl:value-of select="rmsg:CommitResource/rmsg:ContactInformation/rm:ContactRole"/></xsl:param>
	<xsl:param name="UCoreWhen"><xsl:value-of select="rmsg:CommitResource/rmsg:SentDateTime"/></xsl:param>
	<xsl:template match="/">
	
	<ns:Digest>
	
		<xsl:choose> 
			<xsl:when test="string-length($EventID_Incident) > 0"> 
				<ns:Event>
					<xsl:attribute name="id"><xsl:value-of select="$EventID_Incident"/></xsl:attribute>
					<ns:Identifier ns:label="label">Resource - <xsl:value-of select="concat(rmsg:CommitResource/rmsg:ContactInformation/rm:ContactDescription, ' - ', 
					    rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:LocationDescription)"/>
					</ns:Identifier>
					<ns:Descriptor>	
					   <xsl:text >&#10;</xsl:text>			    
					   <xsl:for-each select="rmsg:CommitResource/rmsg:ResourceInformation">
					     <xsl:value-of select="concat(rmsg:Resource/rmsg:Name,':',rmsg:AssignmentInformation/rmsg:Quantity/rm:QuantityText, '&lt;br/>')"/>
					  </xsl:for-each>
					  </ns:Descriptor>
					<ns:SimpleProperty ns:codespace="http://uicds.gov/1.0/codespace/event"  ns:code="Incident" ns:label="Type"><xsl:value-of select="rmsg:CommitResource/rmsg:MessageContentType"/></ns:SimpleProperty>
					<ns:What ns:codespace="http://ucore.gov/ucore/2.0/codespace/" ns:code="Event"/>
				</ns:Event>
				<xsl:if test="count(rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation) > 0">
				<ns:Location>
					<xsl:attribute name="id"><xsl:value-of select="$LocationID_Schedule"/></xsl:attribute>
					<xsl:if test="count(rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:LocationDescription) > 0">
						<ns:Descriptor><xsl:value-of select="rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:LocationDescription"/></ns:Descriptor>
					</xsl:if>
					<xsl:if test="count(rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address) > 0">
						<ns:PhysicalAddress>
							<ns1:postalAddress>
								<ns1:street><xsl:value-of select="rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:FreeTextAddress/xal:AddressLine[1]"/></ns1:street>
								<ns1:city><xsl:value-of select="rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:FreeTextAddress/xal:AddressLine[2]"/></ns1:city>
								<ns1:state><xsl:value-of select="rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:AdministrativeArea/xal:NameElement/@xal:NameCode"/></ns1:state>
								<ns1:postalCode><xsl:value-of select="rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:PostCode/xal:Identifier"/></ns1:postalCode>
								<ns1:countryCode>
									<xsl:attribute name="ns1:value"><xsl:value-of select="rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:Country/xal:NameElement/@xal:NameCode"/></xsl:attribute>
									<xsl:attribute name="ns1:qualifier">ISO 3166-1</xsl:attribute>
								</ns1:countryCode>
							</ns1:postalAddress>
						</ns:PhysicalAddress>
					</xsl:if>
					<xsl:if test="count(rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:TargetArea) > 0">
						<ns:GeoLocation>
						<!--  
						<ns:CircleByCenterPoint>
							<ucoregml:CircleByCenterPoint>
								<xsl:attribute name="numArc">1</xsl:attribute>
								<ucoregml:pos>
									<xsl:attribute name="srsName">EPSG:4326</xsl:attribute>
                                    <xsl:value-of select="rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:TargetArea/gml:Point/gml:pos"/>
								</ucoregml:pos>
								<ucoregml:radius>
									<xsl:attribute name="uom">SMI</xsl:attribute>
									<xsl:value-of select="4.5"/>
								</ucoregml:radius>
							</ucoregml:CircleByCenterPoint>
						</ns:CircleByCenterPoint>	
						-->
						 
							<ns:Point>
								<ucoregml:Point>
									<xsl:attribute name="ucoregml:id">point-schedule-<xsl:value-of select="generate-id(rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:TargetArea)"/></xsl:attribute>
									<ucoregml:pos srsName="EPSG:4326"><xsl:value-of select="rmsg:CommitResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:TargetArea/gml:Point/gml:pos"/></ucoregml:pos>
									<ucoregml:radius uom="SMI">4.5</ucoregml:radius>
								</ucoregml:Point>
							</ns:Point>
							
						</ns:GeoLocation>
					</xsl:if>
				</ns:Location>
				<ns:OccursAt>
				<xsl:attribute name="id"><xsl:value-of select="$OccursAtID"/></xsl:attribute>
				  <ns:Time>
					<ns:TimeInstant>
						<Value><xsl:value-of select="current-dateTime()"/></Value> 
					</ns:TimeInstant>
				  </ns:Time>
				  <ns:EventRef>
					<xsl:attribute name="ref"><xsl:value-of select="$EventID_Incident"/></xsl:attribute>
				  </ns:EventRef>
				  <ns:LocationRef>
					<xsl:attribute name="ref"><xsl:value-of select="$LocationID_Schedule"/></xsl:attribute>
				  </ns:LocationRef>
			    </ns:OccursAt>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise> 
				<!-- Not valid --> 
			</xsl:otherwise> 
		</xsl:choose> 
		
		<xsl:for-each select="rmsg:CommitResource/rmsg:ResourceInformation">
			<ns:Entity>
				<xsl:attribute name="id"><xsl:value-of select="$EventID_Resource_Prefix"/><xsl:value-of select="rmsg:ResourceInfoElementID"/>-<xsl:value-of select="rmsg:Resource/rmsg:ResourceID"/></xsl:attribute>
				<ns:Descriptor><xsl:value-of select="rmsg:Resource/rmsg:Name"/></ns:Descriptor>
				<ns:SimpleProperty ns:codespace="http://uicds.gov/1.0/codespace/event/status/experimental" ns:label="Status">
					<xsl:choose> 
						<xsl:when test="normalize-space(rmsg:ResponseInformation/rm:ResponseType)='Accept'">
							<xsl:attribute name="ns:code">Comitted</xsl:attribute>
						</xsl:when>
						<xsl:when test="normalize-space(rmsg:ResponseInformation/rm:ResponseType)='Decline'">
							<xsl:attribute name="ns:code">Declined</xsl:attribute>
						</xsl:when>
						<xsl:otherwise> 
							<!-- No volid --> 
						</xsl:otherwise> 
					</xsl:choose>
				</ns:SimpleProperty>
				<ns:SimpleProperty ns:codespace="http://nimsonline.org/2.0" ns:label="Category"><xsl:value-of select="rmsg:Resource/rmsg:TypeInfo/Category"/></ns:SimpleProperty>
				<ns:SimpleProperty ns:codespace="http://nimsonline.org/2.0" ns:label="Kind"><xsl:value-of select="rmsg:Resource/rmsg:TypeInfo/Kind"/></ns:SimpleProperty>
				<ns:SimpleProperty ns:codespace="http://nimsonline.org/2.0" ns:label="Resource"><xsl:value-of select="rmsg:Resource/rmsg:TypeInfo/Resource"/></ns:SimpleProperty>
				<ns:SimpleProperty ns:codespace="http://nimsonline.org/2.0" ns:label="MinimumCapabilities"><xsl:value-of select="rmsg:Resource/rmsg:TypeInfo/MinimumCapabilities"/></ns:SimpleProperty>
				<ns:What ns:codespace="http://ucore.gov/ucore/2.0/codespace/">
					<xsl:if test="count(rmsg:Resource/rmsg:TypeInfo/Kind) > 0">
						<xsl:attribute name="ns:code"><xsl:value-of select="rmsg:Resource/rmsg:TypeInfo/Kind"/></xsl:attribute>
					</xsl:if>
					<xsl:if test="count(rmsg:Resource/rmsg:TypeInfo/Kind) = 0">
						<xsl:attribute name="ns:code">Equipment</xsl:attribute>
					</xsl:if>
				</ns:What>
				
				<!--
				<ns:Who>
					<xsl:attribute name="ns:code"><xsl:value-of select="$UCoreWho"/></xsl:attribute>
					<xsl:attribute name="ns:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
				</ns:Who>
				<ns:Where>
					<xsl:attribute name="ns:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
					<xsl:attribute name="ns:code"><xsl:value-of select="rmsg:ScheduleInformation/rmsg:Location/rm:LocationDescription"/></xsl:attribute>
				</ns:Where>
				<ns:When>
					<xsl:attribute name="ns:code"><xsl:value-of select="$UCoreWhen"/></xsl:attribute>
					<xsl:attribute name="ns:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
				</ns:When>				
				-->
			</ns:Entity>

			<xsl:choose> 
				<xsl:when test="string-length($EventID_Incident) > 0"> 
					<ns:InvolvedIn>
						<xsl:attribute name="id">InvolvedInID-<xsl:value-of select="generate-id(.)"/></xsl:attribute>
						<ns:Time>
							<ns:TimeInstant>
								<ns:Value><xsl:value-of select="current-dateTime()"/></ns:Value>  
							</ns:TimeInstant>
						</ns:Time>
						<ns:AgentRef>
							<xsl:attribute name="ref"><xsl:value-of select="$EventID_Resource_Prefix"/><xsl:value-of select="rmsg:ResourceInfoElementID"/>-<xsl:value-of select="rmsg:Resource/rmsg:ResourceID"/></xsl:attribute>
						</ns:AgentRef>
						<ns:EventRef>
							<xsl:attribute name="ref"><xsl:value-of select="$EventID_Incident"/></xsl:attribute>
						</ns:EventRef>
					</ns:InvolvedIn>
				</xsl:when>
				<xsl:otherwise> 
					<!-- No volid --> 
				</xsl:otherwise> 
			</xsl:choose> 
			

			<xsl:if test="count(rmsg:ScheduleInformation) > 0">
			<!--  Take Location out of the Event loop, since KML renderer grabs all the location in the digest, it will have 
multiple push pins of the same location for each shipment.
				<ns:Location>
					<xsl:attribute name="id">Location-Schedule-<xsl:value-of select="generate-id(rmsg:ScheduleInformation)"/></xsl:attribute>
					<xsl:if test="count(rmsg:ScheduleInformation/rmsg:Location/rm:LocationDescription) > 0">
						<ns:Descriptor><xsl:value-of select="rmsg:ScheduleInformation/rmsg:Location/rm:LocationDescription"/></ns:Descriptor>
					</xsl:if>
					<xsl:if test="count(rmsg:ScheduleInformation/rmsg:Location/rm:Address) > 0">
						<ns:PhysicalAddress>
							<ns1:postalAddress>
								<ns1:street><xsl:value-of select="rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:FreeTextAddress/xal:AddressLine[1]"/></ns1:street>
								<ns1:city><xsl:value-of select="rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:FreeTextAddress/xal:AddressLine[2]"/></ns1:city>
								<ns1:state><xsl:value-of select="rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:AdministrativeArea/xal:NameElement/@xal:NameCode"/></ns1:state>
								<ns1:postalCode><xsl:value-of select="rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:PostCode/xal:Identifier"/></ns1:postalCode>
								<ns1:countryCode>
									<xsl:attribute name="ns1:value"><xsl:value-of select="rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:Country/xal:NameElement/@xal:NameCode"/></xsl:attribute>
									<xsl:attribute name="ns1:qualifier">ISO 3166-1</xsl:attribute>
								</ns1:countryCode>
							</ns1:postalAddress>
						</ns:PhysicalAddress>
					</xsl:if>
					<xsl:if test="count(rmsg:ScheduleInformation/rmsg:Location/rm:TargetArea) > 0">
						<ns:GeoLocation>
							<ns:Point>
								<ucoregml:Point>
									<xsl:attribute name="ucoregml:id">point-schedule-<xsl:value-of select="generate-id(rmsg:ScheduleInformation/rmsg:Location/rm:TargetArea)"/></xsl:attribute>
									<ucoregml:pos srsName="EPSG:4326"><xsl:value-of select="rmsg:ScheduleInformation/rmsg:Location/rm:TargetArea/gml:Point/gml:pos"/></ucoregml:pos>
								</ucoregml:Point>
							</ns:Point>
						</ns:GeoLocation>
					</xsl:if>
				</ns:Location>
				-->
				<ns:HasDestinationOf>
					<xsl:attribute name="id">HasDestinationOf-<xsl:value-of select="generate-id(.)"/></xsl:attribute>
					<ns:Time>
						<ns:TimeInstant>
							<ns:Value><xsl:value-of select="current-dateTime()"/></ns:Value>  
						</ns:TimeInstant>
					</ns:Time>
					<ns:EntityRef>
						<xsl:attribute name="ref"><xsl:value-of select="$EventID_Resource_Prefix"/><xsl:value-of select="rmsg:ResourceInfoElementID"/>-<xsl:value-of select="rmsg:Resource/rmsg:ResourceID"/></xsl:attribute>
					</ns:EntityRef>
					<ns:LocationRef>
						<xsl:attribute name="ref">Location-Schedule-<xsl:value-of select="generate-id(rmsg:ScheduleInformation)"/></xsl:attribute>
					</ns:LocationRef>
				</ns:HasDestinationOf>
			</xsl:if>

		</xsl:for-each>
		
	</ns:Digest>
	</xsl:template>
</xsl:stylesheet>