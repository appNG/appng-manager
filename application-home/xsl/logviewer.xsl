<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="xs">
	<xsl:output method="xhtml" />

	<xsl:template match="datasource[@id = 'logfile']" priority="10" mode="#all">
	<xsl:apply-templates select="./config/linkpanel" />
	<xsl:variable name="url" select="./config/linkpanel/link/@target" />
	
	<div>
		<div class="pre">
			<pre id="logfile"></pre>
			<script language="javascript" type="text/javascript">
				$(document).ready(function(){
					polling.init({url:'<xsl:value-of select="$url"/>', selector:'logfile', interval: 3000, polling: true});
				});
			</script>
		</div>
	</div>
	</xsl:template>

</xsl:stylesheet>