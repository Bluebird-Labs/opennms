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
package org.opennms.netmgt.enlinkd.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;
import org.opennms.netmgt.model.FilterManager;
import org.opennms.netmgt.model.OnmsNode;

@Entity
@Table(name="ipNetToMedia")
@Filter(name=FilterManager.AUTH_FILTER_NAME, condition="exists (select distinct x.nodeid from node x join category_node cn on x.nodeid = cn.nodeid join category_group cg on cn.categoryId = cg.categoryId where x.nodeid = nodeid and cg.groupId in (:userGroups))")
public class IpNetToMedia implements Serializable {
/**
 * ipNetToMediaType OBJECT-TYPE
 *   SYNTAX     INTEGER {
 *               other(1),        -- none of the following
 *               invalid(2),      -- an invalidated mapping
 *               dynamic(3),
 *               static(4)
 *           }
 *  
 * @author antonio
 *
 */

	public enum IpNetToMediaType {
		IPNETTOMEDIA_TYPE_OTHER(1),
		IPNETTOMEDIA_TYPE_INVALID(2),
		IPNETTOMEDIA_TYPE_DYNAMIC(3),
		IPNETTOMEDIA_TYPE_STATIC(4);
		
        private final int m_value;

        IpNetToMediaType(int value) {
        	m_value=value;
        }

        static final Map<Integer, String> s_typeMap = new HashMap<>();

        static {
        	s_typeMap.put(1, "other" );
        	s_typeMap.put(2, "invalid" );
        	s_typeMap.put(3, "dynamic" );
        	s_typeMap.put(4, "static" );
        }

        public static String getTypeString(Integer code) {
            if (s_typeMap.containsKey(code))
                    return s_typeMap.get( code);
            return null;
        }
        
        public static IpNetToMediaType get(Integer code) {
            if (code == null) {
                return null;
            }
            switch (code) {
            case 1: 	return IPNETTOMEDIA_TYPE_OTHER;
            case 2: 	return IPNETTOMEDIA_TYPE_INVALID;
            case 3: 	return IPNETTOMEDIA_TYPE_DYNAMIC;
            case 4: 	return IPNETTOMEDIA_TYPE_STATIC;
            default:    return null;
            }
        }
        
        public Integer getValue() {
            return m_value;
        }

		
	}
	
    private static final long serialVersionUID = 7750043250236397014L;

    private Integer m_id;
    
    private OnmsNode m_node;
    private Integer m_ifIndex;
    private String  m_port;
    private InetAddress m_netAddress;
    private String m_physAddress;
    private IpNetToMediaType m_ipNetToMediaType;

    private OnmsNode m_sourceNode;
    private Integer m_sourceIfIndex;

    private Date m_createTime = new Date();
    private Date m_lastPollTime;

    
    /**
     * <p>Constructor for IpNetToMedia.</p>
     */
    public IpNetToMedia() {
    }

    /**
     * Unique identifier for ipInterface.
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Id
    @Column(nullable=false)
    @XmlTransient
    @SequenceGenerator(name="opennmsSequence", sequenceName="opennmsNxtId")
    @GeneratedValue(generator="opennmsSequence")
    public Integer getId() {
        return m_id;
    }

    /**
     * <p>setId</p>
     *
     * @param id a {@link java.lang.Integer} object.
     */
    public void setId(Integer id) {
        m_id = id;
    }
    
    @Column(name="netAddress", nullable=false)
    @Type(type="org.opennms.netmgt.model.InetAddressUserType")
    public InetAddress getNetAddress() {
        return m_netAddress;
    }

	public void setNetAddress(InetAddress netAddress) {
		m_netAddress = netAddress;
	}
    
    /**
     * <p>getPhysAddr</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name="physAddress", length=32, nullable=false)
    public String getPhysAddress() {
        return m_physAddress;
    }

    /**
     * <p>setPhysAddr</p>
     *
     * @param physAddr a {@link java.lang.String} object.
     */
    public void setPhysAddress(String physAddr) {
        m_physAddress = physAddr;
    }
    
	@Transient
    public IpNetToMediaType getIpNetToMediaType() {
		return m_ipNetToMediaType;
	}

	public void setIpNetToMediaType(IpNetToMediaType ipNetToMediaType) {
		m_ipNetToMediaType = ipNetToMediaType;
	}

    /**
     * <p>getSourceNode</p>
     *
     * @return a {@link org.opennms.netmgt.model.OnmsNode} object.
     */
    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="sourceNodeId", nullable=false)
    public OnmsNode getSourceNode() {
        return m_sourceNode;
    }

    public void setSourceNode(OnmsNode sourceNode) {
        m_sourceNode = sourceNode;
    }

    @Column(name="sourceIfIndex", nullable=false)
    public Integer getSourceIfIndex() {
        return m_sourceIfIndex;
    }
    
    public void setSourceIfIndex(Integer sourceIfIndex) {
        m_sourceIfIndex = sourceIfIndex;
    }

    @Transient
    public Integer getNodeId() {
        if (m_node != null) {
            return m_node.getId();
        }
        return null;
    }
    /**
     * <p>getNode</p>
     *
     * @return a {@link org.opennms.netmgt.model.OnmsNode} object.
     */
    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="nodeId")
    public OnmsNode getNode() {
        return m_node;
    }

    /**
     * <p>setNode</p>
     *
     * @param node a {@link org.opennms.netmgt.model.OnmsNode} object.
     */
    public void setNode(OnmsNode node) {
        m_node = node;
    }

    @Column(name="ifIndex")
    public Integer getIfIndex() {
        return m_ifIndex;
    }
    
    /**
     * <p>setIfIndex</p>
     *
     * @param ifIndex a {@link java.lang.Integer} object.
     */
    public void setIfIndex(Integer ifIndex) {
        m_ifIndex = ifIndex;
    }

    public String getPort() {
        return m_port;
    }

    public void setPort(String port) {
        m_port = port;
    }

   
    /**
     * <p>getCreateTime</p>
     *
     * @return a {@link java.util.Date} object.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="createTime", nullable=false)
    public Date getCreateTime() {
        return m_createTime;
    }

    /**
     * <p>setLastPoll</p>
     *
     * @param lastPoll a {@link java.util.Date} object.
     */
    public void setCreateTime(Date lastPoll) {
        m_createTime = lastPoll;
    }


    /**
     * <p>getLastPollTime</p>
     *
     * @return a {@link java.util.Date} object.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastPollTime", nullable=false)
    public Date getLastPollTime() {
        return m_lastPollTime;
    }

    /**
     * <p>setLastPoll</p>
     *
     * @param lastPoll a {@link java.util.Date} object.
     */
    public void setLastPollTime(Date lastPoll) {
        m_lastPollTime = lastPoll;
    }

	public void merge(IpNetToMedia element) {
	        setNode(element.getNode());
	        setIfIndex(element.getIfIndex());
	        setPort(element.getPort());    
		setSourceNode(element.getSourceNode());
		setSourceIfIndex(element.getSourceIfIndex());
		setLastPollTime(element.getCreateTime());
	}

    /**
     * <p>toString</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String toString() {
        StringBuilder strb = new StringBuilder();
        strb.append("ipnettomedia: ");
        strb.append("nodeid:["); 
        if (getNode() != null) {
            strb.append(getNode().getId());
        } else {
            strb.append("null");
        }
        strb.append("]. ifindex:[");
        strb.append(getIfIndex());
        strb.append("]. ipaddr:[");
        strb.append(getNetAddress());
        strb.append("]. physaddr:[");
        strb.append(getPhysAddress());
        strb.append(" source nodeid:["); 
        strb.append(getSourceNode().getId());
        strb.append("]. source ifindex:[");
        strb.append(getSourceIfIndex());
        strb.append("]");

        return strb.toString();
    }

}
