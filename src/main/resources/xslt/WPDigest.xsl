<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"  xmlns:map="http://uicds.org/MapService" xmlns:context="http://www.opengis.net/context" xmlns:ucore="http://ucore.gov/ucore/2.0" xmlns:ddms="http://metadata.dod.mil/mdr/ns/DDMS/2.0/" xmlns:base="http://www.saic.com/precis/2009/06/base" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:ucoregml="http://www.opengis.net/gml/3.2">
<xsl:output method="xml" indent="yes"/>
    
    <xsl:template match="/">
        <xsl:apply-templates select="//context:ViewContext"/> 
    </xsl:template>
    
    <!-- These items apply to Web Map Context Documents (mapview and layers) -->
    <xsl:template match="context:ViewContext">
        <Digest xmlns="http://ucore.gov/ucore/2.0" xmlns:ns="http://www.opengis.net/gml/3.2" xmlns:ddms="http://metadata.dod.mil/mdr/ns/DDMS/2.0/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <xsl:apply-templates select="context:General" />
        </Digest>
    </xsl:template>
        
    <xsl:template match="context:General">
        <xsl:apply-templates select="context:ContactInformation/context:ContactOrganization"/>
        <ucore:Event>
            <xsl:attribute name="id">event-<xsl:value-of select="generate-id()"/></xsl:attribute>
            <!-- generate default ucore what -->
            <ucore:What ucore:code="Event" ucore:codespace="http://ucore.gov/ucore/2.0/codespace/"></ucore:What>
            <ucore:What ucore:code="Map Data" ucore:codespace="http://ucore.gov/ucore/2.0/codespace/"></ucore:What>
            <!-- title as a what is incorrect -->
            <!-- <xsl:apply-templates select="context:Title"/> -->
            <!-- there is no incident name to use as identifier, so use the IG ID -->
            <xsl:apply-templates select="//base:AssociatedGroups[1]/base:Identifier"/>
            <xsl:apply-templates select="context:Abstract"/>
        </ucore:Event>
        <xsl:apply-templates select="context:BoundingBox"/>
        <xsl:apply-templates select="../context:LayerList"/>
    </xsl:template>
    
    <xsl:template match="context:BoundingBox">
        <ucore:Location>
            <xsl:attribute name="id">loc-<xsl:value-of select="generate-id()"/></xsl:attribute>
            <ucore:Descriptor>Overlay Bounding Box</ucore:Descriptor>
            <ucore:GeoLocation>
                <ucore:Envelope>
                    <ucoregml:Envelope>
                        <ucoregml:lowerCorner><xsl:value-of select="./@minx"/><xsl:text> </xsl:text><xsl:value-of select="./@miny"/></ucoregml:lowerCorner>
                        <ucoregml:upperCorner><xsl:value-of select="./@maxx"/><xsl:text> </xsl:text><xsl:value-of select="./@maxy"/></ucoregml:upperCorner>
                    </ucoregml:Envelope>
                </ucore:Envelope>
            </ucore:GeoLocation>
        </ucore:Location>
    </xsl:template>
    
    <xsl:template match="base:Identifier">
        <ucore:Identifier ucore:codespace="http://uicds.us/identifier" ucore:code="interestGroup" ucore:label="Interest Group"><xsl:value-of select="."/></ucore:Identifier>
    </xsl:template>
    
    <xsl:template match="context:Abstract">
        <ucore:Descriptor>
             <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                <div><xsl:value-of select="."/></div>
            <xsl:for-each select="../../context:LayerList/context:Layer[not(context:Title='Base Map' or context:Title='Incident Features')]">
                     <b>Title: </b><xsl:value-of select="context:Title"/><br />
                     <blockquote>
                         <b>Name: </b><xsl:value-of select="context:Name"/><br />
                         
                         <b>Abstract: </b><xsl:value-of select="context:Abstract"/><br />
                         <b>Attachment: </b><xsl:value-of select="context:Server/context:OnlineResource/@xlink:href"/><br />
                     </blockquote>
                </xsl:for-each>
             <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text> 
         </ucore:Descriptor>

    </xsl:template>
    
    <xsl:template match="context:LayerList">
        <xsl:for-each select="context:Layer[not(context:Title='Base Map' or context:Title='Incident Features')]">
            <ucore:Location>
                <xsl:attribute name="id">loc-<xsl:value-of select="generate-id()"/></xsl:attribute>
                <ucore:Descriptor>
                    <xsl:value-of select="context:Title"/>
                </ucore:Descriptor>
                <ucore:CyberAddress>
                <ddms:virtualCoverage>
                    <xsl:attribute name="address">
                        <xsl:value-of select="context:Server/context:OnlineResource/@xlink:href"/>
                    </xsl:attribute>
                </ddms:virtualCoverage>
            </ucore:CyberAddress>
            </ucore:Location>
        </xsl:for-each>

    </xsl:template>
    
    <xsl:template match="context:Title">
                 <ucore:What>
                     <xsl:attribute name="ucore:code"><xsl:value-of select="."/></xsl:attribute>
                     <xsl:attribute name="ucore:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
                 </ucore:What>
    </xsl:template>
    
    <xsl:template match="context:ContactOrganization">
        <ucore:Organization>
            <xsl:attribute name="id">org-<xsl:value-of select="generate-id()"/></xsl:attribute>
            <ucore:What>
                <xsl:attribute name="ucore:code">Organization</xsl:attribute>
                <xsl:attribute name="ucore:codespace">http://ucore.gov/ucore/2.0/codespace/</xsl:attribute>
            </ucore:What>
            <ucore:Name>
                <ucore:Value><xsl:value-of select="."/></ucore:Value>
            </ucore:Name>
        </ucore:Organization>
    </xsl:template>
    
</xsl:stylesheet>