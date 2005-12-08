<%--

//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2005 Sep 30: Hacked up to use CSS for layout. -- DJ Gregor
//
// Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//

--%>

<%-- 
  This page is included by other JSPs to create a box containing an
  abbreviated list of outages.  All current outages and any outages resolved
  within the last 24 hours are shown.
  
  It expects that a <base> tag has been set in the including page
  that directs all URLs to be relative to the servlet context.
--%>

<%@page language="java" contentType="text/html" session="true" import="org.opennms.web.outage.*,java.util.*" %>

<%! 
    OutageModel model = new OutageModel();
%>

<%
    //required parameter node
    String nodeIdString = request.getParameter("node");
    
    if( nodeIdString == null ) {
        throw new org.opennms.web.MissingParameterException("node");
    }
        
    int nodeId = Integer.parseInt(nodeIdString);
    
    //determine yesterday's respresentation
    Calendar cal = new GregorianCalendar();
    cal.add( Calendar.DATE, -1 );
    Date yesterday = cal.getTime();

    //gets all current outages and outages that have been resolved within the
    //the last 24 hours
    Outage[] outages = this.model.getOutagesForNode(nodeId, yesterday);
%>

<table id="nodeOutages" cellpadding="2" width="100%">
<tbody>


<% if(outages.length == 0) { %>
  <tr>
    <!-- XXX should this be header, instead? -->
    <td class="headerplain" colspan="5">There have been no outages on this node in the last 24 hours.</td>
  </tr>
<% } else { %>
  <tr> 
    <!-- XXX should this be header, instead? -->
    <td class="headerplain" colspan="5">Recent Outages</td>
  </tr>

  <tr>
    <!-- XXX should be able to get rid of the bold -->
    <td><b>Interface</b></td>
    <td><b>Service</b></td>
    <td><b>Lost</b></td>
    <td><b>Regained</b></td>
    <td><b>Outage ID</b></td>
  </tr>

  <% for( int i=0; i < outages.length; i++ ) { %>
     <tr>
      <td><a href="element/interface.jsp?node=<%=nodeId%>&intf=<%=outages[i].getIpAddress()%>"><%=outages[i].getIpAddress()%></a></td>
      <td><a href="element/service.jsp?node=<%=nodeId%>&intf=<%=outages[i].getIpAddress()%>&service=<%=outages[i].getServiceId()%>"><%=outages[i].getServiceName()%></a></td>
      <td><%=org.opennms.netmgt.EventConstants.formatToUIString(outages[i].getLostServiceTime())%></td>
      
      <% if( outages[i].getRegainedServiceTime() == null ) { %>
        <td bgcolor="red"><b>DOWN</b></td>
      <% } else { %>
        <td><%=org.opennms.netmgt.EventConstants.formatToUIString(outages[i].getRegainedServiceTime())%></td>      
      <% } %>
      <td><a href="outage/detail.jsp?id=<%=outages[i].getId()%>"><%=outages[i].getId()%></a></td>       
    </tr>
  <% } %>
<% } %>

</tbody>
</table>      
