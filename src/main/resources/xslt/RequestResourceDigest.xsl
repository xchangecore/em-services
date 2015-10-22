<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns="http://ucore.gov/ucore/2.0" xmlns:ucoregml="http://www.opengis.net/gml/3.2" xmlns:rmsg="urn:oasis:names:tc:emergency:EDXL:RM:1.0:msg" xmlns:rm="urn:oasis:names:tc:emergency:EDXL:RM:1.0" xmlns:gml="http://www.opengis.net/gml" xmlns:ns1="http://metadata.dod.mil/mdr/ns/DDMS/2.0/" xmlns:xal="urn:oasis:names:tc:ciq:xal:3">
	<xsl:output version="1.0" method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>
	<xsl:param name="OrganizationID">Organization-<xsl:value-of select="generate-id()"/></xsl:param>
	<xsl:param name="EventID_Incident">
		<xsl:if test="count(rmsg:RequestResource/rmsg:IncidentInformation/rm:IncidentID) > 0">
			<xsl:value-of select="rmsg:RequestResource/rmsg:IncidentInformation/rm:IncidentID"/>
		</xsl:if>
		<xsl:if test="count(rmsg:RequestResource/rmsg:IncidentInformation/rm:IncidentID) = 0">uicds-incident-id</xsl:if>
	</xsl:param>
	<xsl:param name="EventID_Resource"><xsl:value-of select="rmsg:RequestResource/rmsg:MessageID"/>-<xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ResourceInfoElementID"/></xsl:param>
	<xsl:param name="LocationID_Schedule">Location-Schedule-<xsl:value-of select="generate-id()"/></xsl:param>
	<xsl:param name="LocationID_Contact">Location-Contact-<xsl:value-of select="generate-id()"/></xsl:param>
	<xsl:param name="LocatedAtID">LocatedAt-<xsl:value-of select="generate-id()"/></xsl:param>
	<xsl:param name="PolygonID">Polygon-<xsl:value-of select="generate-id()"/></xsl:param>
	<xsl:param name="OccursAtID">OccursAt-<xsl:value-of select="generate-id(rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location)"/></xsl:param>
	<xsl:param name="CauseOf">CauseOf-<xsl:value-of select="generate-id()"/></xsl:param>
	<xsl:param name="PointID_Schedule">point-schedule-<xsl:value-of select="generate-id()"/></xsl:param>
	<xsl:param name="PointID_Contact">point-contact-<xsl:value-of select="generate-id()"/></xsl:param>
	<xsl:param name="InvolvedInID">InvolvedInID-<xsl:value-of select="generate-id()"/></xsl:param>
	<xsl:template match="/">
	
	<ns:Digest>
	
		 <xsl:if test="count(rmsg:RequestResource/rmsg:IncidentInformation/rm:IncidentID) > 0">
			<ns:Event>
				<xsl:attribute name="id"><xsl:value-of select="$EventID_Incident"/></xsl:attribute>
				<ns:Identifier ns:label="label">Request Resource - <xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactDescription"/></ns:Identifier>
				<ns:SimpleProperty ns:codespace="http://uicds.gov/1.0/codespace/event"  ns:code="Incident" ns:label="Type"><xsl:value-of select="rmsg:RequestResource/rmsg:MessageContentType"/></ns:SimpleProperty>
				<ns:What ns:codespace="http://ucore.gov/ucore/2.0/codespace/" ns:code="Event"/>
			</ns:Event>
		</xsl:if>
		
		<ns:Event>
			<xsl:attribute name="id"><xsl:value-of select="$EventID_Resource"/></xsl:attribute>
			<ns:Descriptor>Resource Request</ns:Descriptor>
			<ns:SimpleProperty ns:codespace="http://uicds.gov/1.0/codespace/event/status/experimental" ns:code="Open" ns:label="Status"/>
			<ns:SimpleProperty ns:codespace="urn:oasis:names:tc:emergency:EDXL:RM:1.0" ns:code="AssignmentInformation.Quantity" ns:label="Quantity"><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:AssignmentInformation/rmsg:Quantity/rm:QuantityText"/></ns:SimpleProperty>
			<ns:SimpleProperty ns:codespace="http://nimsonline.org/2.0" ns:label="Category"><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:Resource/rmsg:TypeInfo/Category"/></ns:SimpleProperty>
			<ns:SimpleProperty ns:codespace="http://nimsonline.org/2.0" ns:label="Kind"><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:Resource/rmsg:TypeInfo/Kind"/></ns:SimpleProperty>
			<ns:SimpleProperty ns:codespace="http://nimsonline.org/2.0" ns:label="Resource"><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:Resource/rmsg:TypeInfo/Resource"/></ns:SimpleProperty>
			<ns:SimpleProperty ns:codespace="http://nimsonline.org/2.0" ns:label="MinimumCapabilities"><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:Resource/rmsg:TypeInfo/MinimumCapabilities"/></ns:SimpleProperty>
			<ns:What ns:codespace="http://ucore.gov/ucore/2.0/codespace/" ns:code="CommunicationEvent"/>
			<ns:What ns:codespace="http://uicds.gov/1.0/codespace" ns:code="RequestResource"/>
			<ns:What>
					<xsl:attribute name="ns:code"><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:Resource/rmsg:TypeInfo/Resource"/></xsl:attribute>
					<xsl:attribute name="ns:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
			</ns:What>
			
			<!--
			<ns:Who>
					<xsl:attribute name="ns:code"><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactDescription"/><xsl:text> </xsl:text><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactRole"/></xsl:attribute>
					<xsl:attribute name="ns:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
			</ns:Who>
			<ns:Where>
				<xsl:attribute name="ns:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
				<xsl:attribute name="ns:code"><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:LocationDescription"/></xsl:attribute>
			</ns:Where>
			<ns:When>
				<xsl:attribute name="ns:code"><xsl:value-of select="rmsg:RequestResource/rmsg:SentDateTime"/></xsl:attribute>
				<xsl:attribute name="ns:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
			</ns:When>
-->

		</ns:Event>
		
		<ns:Location>
			<xsl:attribute name="id"><xsl:value-of select="$LocationID_Schedule"/></xsl:attribute>
			<xsl:if test="count(rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:LocationDescription) > 0">
				<ns:Descriptor><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:LocationDescription"/></ns:Descriptor>
			</xsl:if>
			<xsl:if test="count(rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address) > 0">
				<ns:PhysicalAddress>
					<ns1:postalAddress>
						<ns1:street><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:Thoroughfare/xal:Number"/><xsl:text> </xsl:text><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:Thoroughfare/xal:NameElement"/>
						</ns1:street>
						<ns1:city><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:Locality/xal:NameElement"/></ns1:city>
						<ns1:state><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:AdministrativeArea/xal:NameElement/@xal:NameCode"/></ns1:state>
						<ns1:postalCode><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:PostCode/xal:Identifier"/></ns1:postalCode>
						<ns1:countryCode>
							<xsl:attribute name="ns1:value"><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:Address/xal:Country/xal:NameElement/@xal:NameCode"/></xsl:attribute>
							<xsl:attribute name="ns1:qualifier">ISO 3166-1</xsl:attribute>
						</ns1:countryCode>
					</ns1:postalAddress>
				</ns:PhysicalAddress>
			</xsl:if>
			<xsl:if test="count(rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:TargetArea) > 0">
				<ns:GeoLocation>
					<ns:Point>
						<ucoregml:Point>
							<xsl:attribute name="ucoregml:id"><xsl:value-of select="$PointID_Schedule"/></xsl:attribute>
							<ucoregml:pos srsName="EPSG:4326"><xsl:value-of select="rmsg:RequestResource/rmsg:ResourceInformation/rmsg:ScheduleInformation/rmsg:Location/rm:TargetArea/gml:Point/gml:pos"/></ucoregml:pos>
						</ucoregml:Point>
					</ns:Point>
				</ns:GeoLocation>
			</xsl:if>
		</ns:Location>
		
		<xsl:if test="count(rmsg:RequestResource/rmsg:IncidentInformation/rm:IncidentID) > 0">
			<ns:OccursAt>
			<xsl:attribute name="id"><xsl:value-of select="$OccursAtID"/></xsl:attribute>
			<ns:Time>
				<ns:TimeInstant>
					<ns:Value><xsl:value-of select="current-dateTime()"/></ns:Value>  
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
		
		<xsl:if test="count(rmsg:RequestResource/rmsg:IncidentInformation/rm:IncidentID) > 0">
			<ns:CauseOf>
			<xsl:attribute name="id"><xsl:value-of select="$CauseOf"/></xsl:attribute>
			<ns:Cause>
				<xsl:attribute name="ref"><xsl:value-of select="$EventID_Incident"/></xsl:attribute>
			</ns:Cause>
			<ns:Effect>
				<xsl:attribute name="ref"><xsl:value-of select="$EventID_Resource"/></xsl:attribute>
			</ns:Effect>
		</ns:CauseOf>
		</xsl:if>
		
		<ns:Location>
			<xsl:attribute name="id"><xsl:value-of select="$LocationID_Contact"/></xsl:attribute>
			<ns:Descriptor><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactLocation/rm:LocationDescription"/></ns:Descriptor>
			<xsl:if test="count(rmsg:RequestResource/rmsg:ContactInformation/rm:ContactLocation/rm:TargetArea) > 0">
				<ns:GeoLocation>
					<ns:Point>
						<ucoregml:Point>
							<xsl:attribute name="ucoregml:id"><xsl:value-of select="$PointID_Contact"/></xsl:attribute>
							<ucoregml:pos srsName="EPSG:4326"><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactLocation/rm:TargetArea/gml:Point/gml:pos"/></ucoregml:pos>
						</ucoregml:Point>
					</ns:Point>
				</ns:GeoLocation>
			</xsl:if>
			<xsl:if test="count(rmsg:RequestResource/rmsg:ContactInformation/rm:ContactLocation/rm:Address) > 0">
				<ns:PhysicalAddress>
					<ns1:postalAddress>
						<ns1:street><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactLocation/rm:Address/xal:Thoroughfare/xal:Number"/><xsl:text> </xsl:text><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactLocation/rm:Address/xal:Thoroughfare/xal:NameElement"/>
						</ns1:street>
						<ns1:city><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactLocation/rm:Address/xal:Locality/xal:NameElement"/></ns1:city>
						<ns1:state><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactLocation/rm:Address/xal:AdministrativeArea/xal:NameElement/@xal:NameCode"/></ns1:state>
						<ns1:postalCode><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactLocation/rm:Address/xal:PostCode/xal:Identifier"/></ns1:postalCode>
						<ns1:countryCode>
							<xsl:attribute name="ns1:value"><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactLocation/rm:Address/xal:Country/xal:NameElement/@xal:NameCode"/></xsl:attribute>
							<xsl:attribute name="ns1:qualifier">ISO 3166-1</xsl:attribute>
						</ns1:countryCode>
					</ns1:postalAddress>
				</ns:PhysicalAddress>
			</xsl:if>
		</ns:Location>

		<ns:Organization>
			<xsl:attribute name="id"><xsl:value-of select="$OrganizationID"/></xsl:attribute>
			<ns:Descriptor><xsl:value-of select="rmsg:RequestResource/rmsg:ContactInformation/rm:ContactDescription"/></ns:Descriptor>
			<ns:What ns:codespace="http://ucore.gov/ucore/2.0/codespace/" ns:code="Organization"/>
		</ns:Organization>
		
		<ns:LocatedAt>
			<xsl:attribute name="id"><xsl:value-of select="$LocatedAtID"/></xsl:attribute>
			<ns:Time>
				<ns:TimeInstant>
						<ns:Value><xsl:value-of select="current-dateTime()"/></ns:Value>  
				</ns:TimeInstant>
			</ns:Time>
			<ns:EntityRef>
				<xsl:attribute name="ref"><xsl:value-of select="$OrganizationID"/></xsl:attribute>
			</ns:EntityRef>
			<ns:LocationRef>
				<xsl:attribute name="ref"><xsl:value-of select="$LocationID_Contact"/></xsl:attribute>
			</ns:LocationRef>
		</ns:LocatedAt>
		
		<ns:InvolvedIn>
			<xsl:attribute name="id"><xsl:value-of select="$InvolvedInID"/></xsl:attribute>
			<ns:Time>
				<ns:TimeInstant>
					<ns:Value><xsl:value-of select="current-dateTime()"/></ns:Value>  
				</ns:TimeInstant>
			</ns:Time>
			<ns:AgentRef>
				<xsl:attribute name="ref"><xsl:value-of select="$OrganizationID"/></xsl:attribute>
			</ns:AgentRef>
			<ns:EventRef>
				<xsl:attribute name="ref"><xsl:value-of select="$EventID_Resource"/></xsl:attribute>
			</ns:EventRef>
		</ns:InvolvedIn>
		
	</ns:Digest>
	</xsl:template>
</xsl:stylesheet>