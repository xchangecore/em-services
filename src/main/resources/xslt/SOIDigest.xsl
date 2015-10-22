<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xnl="urn:oasis:names:tc:ciq:xnl:3" xmlns:geo-oasis="urn:oasis:names:tc:emergency:EDXL:HAVE:1.0:geo-oasis" xmlns:have="urn:oasis:names:tc:emergency:EDXL:HAVE:1.0"  xmlns:ns="http://ucore.gov/ucore/2.0"  xmlns:gml="http://www.opengis.net/gml" xmlns:ucoregml="http://www.opengis.net/gml/3.2" xmlns:ns1="http://niem.gov/niem/niem-core/2.0" xmlns:tns="http://uicds.org/SensorService" xmlns:ddms="http://metadata.dod.mil/mdr/ns/DDMS/2.0/">
	<xsl:output version="1.0" method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>
	<xsl:param name="ObservationEventID">ObservationEvent-<xsl:value-of select="generate-id(tns:SensorObservationInfo)"/></xsl:param>
	<xsl:param name="LocationID">Location-<xsl:value-of select="generate-id(tns:SensorObservationInfo)"/></xsl:param>
	<xsl:param name="PointID">Point-<xsl:value-of select="generate-id(tns:SensorObservationInfo/tns:SensorInfo)"/></xsl:param>
	<xsl:param name="LocatedAtID">LocatedAt-<xsl:value-of select="generate-id(tns:SensorObservationInfo/tns:SensorInfo)"/></xsl:param>
    <xsl:param name="OccursAtID">OccursAt-<xsl:value-of select="generate-id(tns:SensorObservationInfo/tns:SensorInfo)"/></xsl:param>
	<xsl:template match="/">
	
	<ns:Digest>
		<ns:Event>
			<xsl:attribute name="id"><xsl:value-of select="$ObservationEventID"/></xsl:attribute>
			<ns:Descriptor><xsl:value-of select="tns:SensorObservationInfo/tns:SensorInfo/tns:description"/></ns:Descriptor>
			<ns:Identifier ns:code="" ns:codespace="" ns:label="Observation"><xsl:value-of select="tns:SensorObservationInfo/tns:SensorInfo/tns:name"/></ns:Identifier>
			<ns:What>
					<xsl:attribute name="ns:code"><xsl:value-of select="tns:SensorObservationInfo/tns:SensorInfo/tns:name"/></xsl:attribute>
					<xsl:attribute name="ns:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
			</ns:What>
		</ns:Event>
        <ns:Location>
			<xsl:attribute name="id"><xsl:value-of select="$LocationID"/></xsl:attribute>
			<ns:CyberAddress>
				<ddms:virtualCoverage ddms:protocol="IP">
					<xsl:attribute name="ddms:address"><xsl:value-of select="tns:SensorObservationInfo/tns:sosURN"/></xsl:attribute>
				</ddms:virtualCoverage>
			</ns:CyberAddress>
			<ns:GeoLocation>
				<ns:Point>
					<ucoregml:Point>
						<xsl:attribute name="ucoregml:id"><xsl:value-of select="$PointID"/></xsl:attribute>
						<ucoregml:pos srsDimension="2"><xsl:value-of select="tns:SensorObservationInfo/tns:SensorInfo/tns:latitude"/><xsl:text> </xsl:text><xsl:value-of select="tns:SensorObservationInfo/tns:SensorInfo/tns:longitude"/></ucoregml:pos>
					</ucoregml:Point>
				</ns:Point>
			</ns:GeoLocation>
		</ns:Location>
		<ns:LocatedAt>
			 <xsl:attribute name="id"><xsl:value-of select="$LocatedAtID"/></xsl:attribute>
			<ns:EntityRef>
				<xsl:attribute name="ref"><xsl:value-of select="$ObservationEventID"/></xsl:attribute>
			</ns:EntityRef>
			<ns:LocationRef>
				<xsl:attribute name="ref"><xsl:value-of select="$LocationID"/></xsl:attribute>
			</ns:LocationRef>
		</ns:LocatedAt>
		<ns:OccursAt>
				<xsl:attribute name="id"><xsl:value-of select="$OccursAtID"/></xsl:attribute>
				<ns:EventRef>
					<xsl:attribute name="ref"><xsl:value-of select="$ObservationEventID"/></xsl:attribute>
				</ns:EventRef>
				<ns:LocationRef>
					<xsl:attribute name="ref"><xsl:value-of select="$LocationID"/></xsl:attribute>
				</ns:LocationRef>
		</ns:OccursAt>
	</ns:Digest>
	
	</xsl:template>
</xsl:stylesheet>