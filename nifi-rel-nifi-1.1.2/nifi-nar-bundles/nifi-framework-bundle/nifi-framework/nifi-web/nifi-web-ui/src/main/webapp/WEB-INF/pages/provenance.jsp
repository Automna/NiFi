<%--
 Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<html>
    <head>
        <title>NiFi Data Provenance</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <link rel="shortcut icon" href="images/nifi16.ico"/>
        <link rel="stylesheet" href="css/reset.css" type="text/css" />
        ${nf.provenance.style.tags}
        <link rel="stylesheet" href="js/jquery/tabbs/jquery.tabbs.css?${project.version}" type="text/css" />
        <link rel="stylesheet" href="js/jquery/combo/jquery.combo.css?${project.version}" type="text/css" />
        <link rel="stylesheet" href="js/jquery/modal/jquery.modal.css?${project.version}" type="text/css" />
        <link rel="stylesheet" href="js/jquery/qtip2/jquery.qtip.min.css?" type="text/css" />
        <link rel="stylesheet" href="js/jquery/ui-smoothness/jquery-ui-1.10.4.min.css" type="text/css" />
        <link rel="stylesheet" href="js/jquery/slickgrid/css/slick.grid.css" type="text/css" />
        <link rel="stylesheet" href="js/jquery/slickgrid/css/slick-default-theme.css" type="text/css" />
        <link rel="stylesheet" href="fonts/flowfont/flowfont.css" type="text/css" />
        <link rel="stylesheet" href="assets/angular-material/angular-material.css" type="text/css" />
        <link rel="stylesheet" href="assets/font-awesome/css/font-awesome.min.css" type="text/css" />
        <script type="text/javascript" src="js/d3/d3.min.js"></script>
        <script type="text/javascript" src="js/jquery/jquery-2.1.1.min.js"></script>
        <script type="text/javascript" src="js/jquery/jquery.base64.js"></script>
        <script type="text/javascript" src="js/jquery/jquery.center.js"></script>
        <script type="text/javascript" src="js/jquery/modal/jquery.modal.js?${project.version}"></script>
        <script type="text/javascript" src="js/jquery/tabbs/jquery.tabbs.js?${project.version}"></script>
        <script type="text/javascript" src="js/jquery/combo/jquery.combo.js?${project.version}"></script>
        <script type="text/javascript" src="js/jquery/jquery.ellipsis.js"></script>
        <script type="text/javascript" src="js/jquery/jquery.each.js"></script>
        <script type="text/javascript" src="js/jquery/ui-smoothness/jquery-ui-1.10.4.min.js"></script>
        <script type="text/javascript" src="js/jquery/qtip2/jquery.qtip.min.js"></script>
        <script type="text/javascript" src="js/jquery/jquery.event.drag-2.2.min.js"></script>
        <script type="text/javascript" src="js/jquery/slickgrid/plugins/slick.cellrangeselector.js"></script>
        <script type="text/javascript" src="js/jquery/slickgrid/plugins/slick.cellselectionmodel.js"></script>
        <script type="text/javascript" src="js/jquery/slickgrid/plugins/slick.rowselectionmodel.js"></script>
        <script type="text/javascript" src="js/jquery/slickgrid/plugins/slick.autotooltips.js"></script>
        <script type="text/javascript" src="js/jquery/slickgrid/slick.formatters.js"></script>
        <script type="text/javascript" src="js/jquery/slickgrid/slick.editors.js"></script>
        <script type="text/javascript" src="js/jquery/slickgrid/slick.dataview.js"></script>
        <script type="text/javascript" src="js/jquery/slickgrid/slick.core.js"></script>
        <script type="text/javascript" src="js/jquery/slickgrid/slick.grid.js"></script>
        <script type="text/javascript" src="assets/angular/angular.min.js"></script>
        <script type="text/javascript" src="assets/angular-messages/angular-messages.min.js"></script>
        <script type="text/javascript" src="assets/angular-resource/angular-resource.min.js"></script>
        <script type="text/javascript" src="assets/angular-route/angular-route.min.js"></script>
        <script type="text/javascript" src="assets/angular-aria/angular-aria.min.js"></script>
        <script type="text/javascript" src="assets/angular-animate/angular-animate.min.js"></script>
        <script type="text/javascript" src="assets/angular-material/angular-material.min.js"></script>
        <script type="text/javascript" src="js/nf/nf-namespace.js?${project.version}"></script>
        <script type="text/javascript" src="js/nf/nf-ng-namespace.js?${project.version}"></script>
        ${nf.provenance.script.tags}
    </head>
    <body ng-controller="ngProvenanceAppCtrl">
        <jsp:include page="/WEB-INF/partials/message-pane.jsp"/>
        <jsp:include page="/WEB-INF/partials/banners-utility.jsp"/>
        <jsp:include page="/WEB-INF/partials/ok-dialog.jsp"/>
        <jsp:include page="/WEB-INF/partials/yes-no-dialog.jsp"/>
        <jsp:include page="/WEB-INF/partials/provenance/provenance-content.jsp"/>
        <jsp:include page="/WEB-INF/partials/provenance/provenance-event-details-dialog.jsp"/>
        <jsp:include page="/WEB-INF/partials/provenance/provenance-search-dialog.jsp"/>
        <jsp:include page="/WEB-INF/partials/provenance/provenance-query-dialog.jsp"/>
        <jsp:include page="/WEB-INF/partials/provenance/lineage-query-dialog.jsp"/>
    </body>
</html>