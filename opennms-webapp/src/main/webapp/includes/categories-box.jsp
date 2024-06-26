<%--

    Licensed to The OpenNMS Group, Inc (TOG) under one or more
    contributor license agreements.  See the LICENSE.md file
    distributed with this work for additional information
    regarding copyright ownership.

    TOG licenses this file to You under the GNU Affero General
    Public License Version 3 (the "License") or (at your option)
    any later version.  You may not use this file except in
    compliance with the License.  You may obtain a copy of the
    License at:

         https://www.gnu.org/licenses/agpl-3.0.txt

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied.  See the License for the specific
    language governing permissions and limitations under the
    License.

--%>
<%-- 
  This page is included by other JSPs to create a box containing a
  table of categories and their outage and availability status.
  
  It expects that a <base> tag has been set in the including page
  that directs all URLs to be relative to the servlet context.
--%>

<%@page language="java" contentType="text/html" session="true"
	import="org.opennms.web.category.Category,
		org.opennms.web.category.CategoryList,
		org.opennms.web.api.Util,
		java.util.Date,
		java.util.Iterator,
		java.util.List,
		java.util.Map" %>

<%!

    CategoryList m_category_list;

    public void init() throws ServletException {
	m_category_list = new CategoryList();
    }

	// Creates a link to the rtc/category.jsp according to the selected outagesType.
	public String createCategoriesOutageLink(HttpServletResponse response, Category category, String outagesType, String linkTitle, String linkText) {
		if (category.getLastUpdated() != null) {
			if (linkTitle == null) {
				return String.format("<a href=\"%s\">%s</a>",
						response.encodeURL("/opennms/rtc/category.jsp?showoutages=" + outagesType + "&category=" + Util.encode(category.getName())),
						linkText);
			}
			return String.format("<a href=\"%s\" title=\"%s\">%s</a>",
					response.encodeURL("/opennms/rtc/category.jsp?showoutages=" + outagesType + "&category=" + Util.encode(category.getName())),
					linkTitle,
					linkText);
		}
		return linkText;
	}
%>

<%
	Map<String, List<Category>> categoryData = m_category_list.getCategoryData();

	long earliestUpdate = m_category_list.getEarliestUpdate(categoryData);
	boolean opennmsDisconnect = m_category_list.isDisconnected(earliestUpdate);

	String titleName = "Availability Over the Past 24 Hours";
	if (opennmsDisconnect) {
		titleName = "Waiting for availability data. ";
		if (earliestUpdate > 0) {
			titleName += new Date(earliestUpdate).toString();
		} else {
			titleName += "One or more categories have never been updated.";
		}
	}
%>

<div class="card fix-subpixel">
  <div class="card-header">
    <span><%= titleName %></span>
  </div>

<table class="table table-sm severity">
<%
	for (Iterator<String> i = categoryData.keySet().iterator(); i.hasNext(); ) {
	    String sectionName = i.next();
%>
	<thead class="dark">
		<tr>
			<th><%= sectionName %></th>
			<th align="right">Outages</th>
			<th align="right">Availability</th>
		</tr>
	</thead>
<%
 	    List<Category> categories = categoryData.get(sectionName);

	    for (Iterator<Category> j = categories.iterator(); j.hasNext(); ) {
		Category category = j.next();
%>
	<tr>
		<td>
			<%=createCategoriesOutageLink(response, category, "all", category.getTitle(), category.getName())%>
		</td>
		<td class="severity-<%= (opennmsDisconnect ? "indeterminate" : category.getOutageClass().toLowerCase()) %> bright divider"
	        align="right"
		    title="Updated: <%= category.getLastUpdated() %>">
			<%=createCategoriesOutageLink(response, category, "outages", null, category.getOutageText())%>
		</td>
		<td class="severity-<%= (opennmsDisconnect ? "indeterminate" : category.getAvailClass().toLowerCase()) %> bright divider"
		    align="right" 
		    title="Updated: <%= category.getLastUpdated() %>">
			<%=createCategoriesOutageLink(response, category, "avail", null, category.getAvailText())%>
		</td>
	</tr>
	
<%
	    }
	}
%>
</table>
<!-- </div> -->
</div>
