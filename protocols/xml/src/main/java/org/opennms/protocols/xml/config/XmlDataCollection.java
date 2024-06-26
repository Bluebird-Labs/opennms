/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.protocols.xml.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * The Class XmlDataCollection.
 * 
 * <pre>
 * &lt;xml-datacollection-config rrdRepository="/opt/opennms/share/rrd/snmp/"
 *    xmlns="http://xmlns.opennms.org/xsd/config/xml-datacollection"&gt;
 *    &lt;xml-collection name="3GPP"&gt;
 *         &lt;rrd step="300">
 *              &lt;rra&gt;RRA:AVERAGE:0.5:1:8928&lt;/rra&gt;
 *              &lt;rra&gt;RRA:AVERAGE:0.5:12:8784&lt;/rra&gt;
 *              &lt;rra&gt;RRA:MIN:0.5:12:8784&lt;/rra&gt;
 *              &lt;rra&gt;RRA:MAX:0.5:12:8784&lt;/rra&gt;
 *         &lt;/rrd&gt;
 *         &lt;xml-source url="sftp://{ipaddr}/opt/hitachi/cnp/data/pm/reports/3gpp/5/A{3gpp-range}_MME00001.xml"
 *              user-name="opennms" password="Op3nNmS!"&gt;
 *             &lt;xml-group name="platform-system-resource" resource-type="platformSystemResource"
 *                  key-xpath="@measObjLdn"
 *                  resource-xpath="/measCollecFile/measData/measInfo[@measInfoId='platform-system|resource']/measValue"&gt;
 *                  &lt;xml-object name="cpuUtilization" type="GAUGE" xpath="r[@p=1]" /&gt;
 *                  &lt;xml-object name="memUtilization" type="GAUGE" xpath="r[@p=2]" /&gt;
 *                  &lt;xml-object name="suspect" type="STRING" xpath="suspect" /&gt;
 *             &lt;/xml-group&gt;
 *         &lt;/xml-source&gt;
 *    &lt;/xml-collection&gt;
 * &lt;/xml-datacollection-config&gt;
 * </pre>
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
@XmlRootElement(name="xml-collection")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlDataCollection implements Serializable, Comparable<XmlDataCollection>, Cloneable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 6448438583337362122L;

    /** The Constant OF_XML_SOURCES. */
    @XmlTransient
    private static final XmlSource[] OF_XML_SOURCES = new XmlSource[0];

    /** The data collection name. */
    @XmlAttribute(name="name")
    private String m_name;

    /** The RRD configuration object. */
    @XmlElement(name="rrd")
    private XmlRrd m_xmlRrd;

    /** The XML Sources list. */
    @XmlElement(name="xml-source")
    private List<XmlSource> m_xmlSources = new ArrayList<>();

    /**
     * Instantiates a new XML data collection.
     */
    public XmlDataCollection() {
    }

    public XmlDataCollection(XmlDataCollection copy) {
        m_name = copy.m_name;
        m_xmlRrd = copy.m_xmlRrd.clone();
        copy.getXmlSources().stream().forEach(s -> m_xmlSources.add(s.clone()));
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        m_name = name;
    }

    /**
     * Gets the XML RRD.
     *
     * @return the XML RRD
     */
    public XmlRrd getXmlRrd() {
        return m_xmlRrd;
    }

    /**
     * Sets the XML RRD.
     *
     * @param xmlRrd the new XML RRD
     */
    public void setXmlRrd(XmlRrd xmlRrd) {
        m_xmlRrd = xmlRrd;
    }

    /**
     * Gets the XML sources.
     *
     * @return the XML sources
     */
    public List<XmlSource> getXmlSources() {
        return m_xmlSources;
    }

    /**
     * Sets the XML sources.
     *
     * @param xmlSources the new XML sources
     */
    public void setXmlSources(List<XmlSource> xmlSources) {
        m_xmlSources = xmlSources;
    }

    /**
     * Adds a new XML source.
     *
     * @param source the source
     */
    public void addXmlSource(XmlSource source) {
        m_xmlSources.add(source);
    }

    /**
     * Removes a XML source.
     *
     * @param source the source
     */
    public void removeXmlSource(XmlSource source) {
        m_xmlSources.remove(source);
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(XmlDataCollection obj) {
        return new CompareToBuilder()
        .append(getName(), obj.getName())
        .append(getXmlRrd(), obj.getXmlRrd())
        .append(getXmlSources().toArray(OF_XML_SOURCES), obj.getXmlSources().toArray(OF_XML_SOURCES))
        .toComparison();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof XmlDataCollection) {
            XmlDataCollection other = (XmlDataCollection) obj;
            return new EqualsBuilder()
            .append(getName(), other.getName())
            .append(getXmlRrd(), other.getXmlRrd())
            .append(getXmlSources().toArray(OF_XML_SOURCES), other.getXmlSources().toArray(OF_XML_SOURCES))
            .isEquals();
        }
        return false;
    }

    @Override
    public XmlDataCollection clone() {
        return new XmlDataCollection(this);
    }
}
