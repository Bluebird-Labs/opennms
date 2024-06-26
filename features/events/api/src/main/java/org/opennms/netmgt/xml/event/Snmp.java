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
package org.opennms.netmgt.xml.event;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import org.opennms.netmgt.events.api.model.ISnmp;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * The snmp information from the trap
 * 
 * @version $Revision$ $Date$
 */

@XmlRootElement(name="snmp")
@XmlAccessorType(XmlAccessType.FIELD)
//@ValidateUsing("event.xsd")
public class Snmp implements Serializable {
	private static final long serialVersionUID = -3623082421217325379L;

	//--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * The snmp enterprise id
     */
	@XmlElement(name="id", required=true)
	@NotNull
    private java.lang.String _id;

	@XmlElement(name="trapOID")
	private String _trapOID;

    /**
     * The snmp enterprise id text
     */
	@XmlElement(name="idtext")
    private java.lang.String _idtext;

    /**
     * The snmp version
     */
	@XmlElement(name="version", required=true)
	@NotNull
    private java.lang.String _version;

    /**
     * The specific trap number
     */
	@XmlElement(name="specific")
    private Integer _specific;

    /**
     * The generic trap number
     */
	@XmlElement(name="generic")
    private Integer _generic;

    /**
     * The community name
     */
	@XmlElement(name="community")
    private java.lang.String _community;

    /**
     * The time stamp
     */
	@XmlElement(name="time-stamp")
    private Long _timeStamp;


      //----------------/
     //- Constructors -/
    //----------------/

    public Snmp() {
        super();
    }

    public static Snmp copyFrom(ISnmp source) {
        if (source == null) {
            return null;
        }

        Snmp snmp = new Snmp();
        snmp.setId(source.getId());
        snmp.setTrapOID(source.getTrapOID());
        snmp.setIdtext(source.getIdtext());
        snmp.setVersion(source.getVersion());
        snmp.setSpecific(source.hasSpecific() ? source.getSpecific() : null);
        snmp.setGeneric(source.hasGeneric() ? source.getGeneric() : null);
        snmp.setCommunity(source.getCommunity());
        snmp.setTimeStamp(source.hasTimeStamp() ? source.getTimeStamp() : null);
        return snmp;
    }

      //-----------/
     //- Methods -/
    //-----------/

    /**
     */
    public void deleteGeneric(
    ) {
    	this._generic = null;
    }

    /**
     */
    public void deleteSpecific(
    ) {
    	this._specific = null;
    }

    /**
     */
    public void deleteTimeStamp(
    ) {
    	this._timeStamp = null;
    }

    /**
     * Returns the value of field 'community'. The field
     * 'community' has the following description: The community
     * name
     * 
     * @return the value of field 'Community'.
     */
    public java.lang.String getCommunity(
    ) {
        return this._community;
    }

    /**
     * Returns the value of field 'generic'. The field 'generic'
     * has the following description: The generic trap number
     * 
     * @return the value of field 'Generic'.
     */
    public Integer getGeneric(
    ) {
        return this._generic == null? 0 : this._generic;
    }

    /**
     * Returns the value of field 'id'. The field 'id' has the
     * following description: The snmp enterprise id
     * 
     * @return the value of field 'Id'.
     */
    public java.lang.String getId(
    ) {
        return this._id;
    }

    /**
     * Returns the value of field 'idtext'. The field 'idtext' has
     * the following description: The snmp enterprise id text
     * 
     * @return the value of field 'Idtext'.
     */
    public java.lang.String getIdtext(
    ) {
        return this._idtext;
    }

    /**
     * Returns the value of field 'specific'. The field 'specific'
     * has the following description: The specific trap number
     * 
     * @return the value of field 'Specific'.
     */
    public Integer getSpecific(
    ) {
        return this._specific == null? 0 : this._specific;
    }

    /**
     * Returns the value of field 'timeStamp'. The field
     * 'timeStamp' has the following description: The time stamp
     * 
     * @return the value of field 'TimeStamp'.
     */
    public Long getTimeStamp(
    ) {
        return this._timeStamp == null? 0 : this._timeStamp;
    }

    /**
     * Returns the value of field 'version'. The field 'version'
     * has the following description: The snmp version
     * 
     * @return the value of field 'Version'.
     */
    public java.lang.String getVersion(
    ) {
        return this._version;
    }

    /**
     * Method hasGeneric.
     * 
     * @return true if at least one Generic has been added
     */
    public boolean hasGeneric(
    ) {
        return this._generic != null;
    }

    /**
     * Method hasSpecific.
     * 
     * @return true if at least one Specific has been added
     */
    public boolean hasSpecific(
    ) {
    	return this._specific != null;
    }

    public boolean hasTrapOID() {
        return this._trapOID != null;
    }

    /**
     * Method hasTimeStamp.
     * 
     * @return true if at least one TimeStamp has been added
     */
    public boolean hasTimeStamp(
    ) {
        return this._timeStamp != null;
    }

    /**
     * Sets the value of field 'community'. The field 'community'
     * has the following description: The community name
     * 
     * @param community the value of field 'community'.
     */
    public void setCommunity(
            final java.lang.String community) {
        this._community = community;
    }

    /**
     * Sets the value of field 'generic'. The field 'generic' has
     * the following description: The generic trap number
     * 
     * @param generic the value of field 'generic'.
     */
    public void setGeneric(
            final Integer generic) {
        this._generic = generic;
    }

    /**
     * Sets the value of field 'id'. The field 'id' has the
     * following description: The snmp enterprise id
     * 
     * @param id the value of field 'id'.
     */
    public void setId(
            final java.lang.String id) {
        this._id = id;
    }

    public String getTrapOID() {
        return _trapOID;
    }

    public void setTrapOID(String _trapOID) {
        this._trapOID = _trapOID;
    }

    /**
     * Sets the value of field 'idtext'. The field 'idtext' has the
     * following description: The snmp enterprise id text
     * 
     * @param idtext the value of field 'idtext'.
     */
    public void setIdtext(
            final java.lang.String idtext) {
        this._idtext = idtext;
    }

    /**
     * Sets the value of field 'specific'. The field 'specific' has
     * the following description: The specific trap number
     * 
     * @param specific the value of field 'specific'.
     */
    public void setSpecific(
            final Integer specific) {
        this._specific = specific;
    }

    /**
     * Sets the value of field 'timeStamp'. The field 'timeStamp'
     * has the following description: The time stamp
     * 
     * @param timeStamp the value of field 'timeStamp'.
     */
    public void setTimeStamp(
            final Long timeStamp) {
        this._timeStamp = timeStamp;
    }

    /**
     * Sets the value of field 'version'. The field 'version' has
     * the following description: The snmp version
     * 
     * @param version the value of field 'version'.
     */
    public void setVersion(
            final java.lang.String version) {
        this._version = version;
    }

        @Override
    public String toString() {
    	return new OnmsStringBuilder(this).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Snmp snmp = (Snmp) o;
        return Objects.equals(_id, snmp._id) && Objects.equals(_idtext, snmp._idtext) && Objects.equals(_version, snmp._version) && Objects.equals(_specific, snmp._specific) && Objects.equals(_generic, snmp._generic) && Objects.equals(_community, snmp._community) && Objects.equals(_timeStamp, snmp._timeStamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, _idtext, _version, _specific, _generic, _community, _timeStamp);
    }
}
