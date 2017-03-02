<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text"/>
    <xsl:template match="/mdc">
        <xsl:variable name="quoteString">"</xsl:variable>
        <xsl:variable name="delimiter"> </xsl:variable>
        <xsl:variable name="KV">=</xsl:variable>
        
        <xsl:variable name="startTime"><xsl:value-of select="mfh/cbt"/></xsl:variable>
        <xsl:variable name="endTime"><xsl:value-of select="mff/ts"/></xsl:variable>
        
        <xsl:for-each select="md">
            <xsl:for-each select="./mi/mv/moid">
        
        		<xsl:text>Vendor=NokiaXML</xsl:text>
                <xsl:value-of select="$delimiter"/>
                <xsl:text> </xsl:text>
                
                <xsl:text>StartDateTime=</xsl:text>
                <xsl:value-of select="$startTime"/>
                <xsl:value-of select="$delimiter"/>
                <xsl:text> </xsl:text>
                
                <xsl:text>EndDateTime=</xsl:text>
                <xsl:value-of select="$endTime"/>
                <xsl:value-of select="$delimiter"/>
                <xsl:text> </xsl:text>
                
                <xsl:text>SwVersion=</xsl:text>
                <xsl:value-of select="../../../neid/nesw"/>
                <xsl:value-of select="$delimiter"/>
                <xsl:text> </xsl:text>
                                
                <xsl:text>Node=</xsl:text>
                <xsl:value-of select="../../../neid/neun"/>
                <xsl:value-of select="$delimiter"/>
                
                <xsl:variable name="mv_index" select="position()" />
                
                <xsl:text> </xsl:text>
                <xsl:value-of select="translate(.,',',' ')" />
                
                <xsl:text> GP=</xsl:text>
                <xsl:value-of select="../../../mi/gp"/>
                
                <xsl:for-each select="../../mt">
                    <xsl:variable name="index" select="position()" />
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="."/>
                    <xsl:value-of select="$KV"/>
                    <xsl:value-of select="parent::mi/mv[$mv_index]/r[$index]" />
                </xsl:for-each>
                <xsl:text>&#xa;</xsl:text>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>