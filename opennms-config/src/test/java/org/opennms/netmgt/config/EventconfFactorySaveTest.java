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
package org.opennms.netmgt.config;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.opennms.netmgt.xml.eventconf.Event;
import org.opennms.netmgt.xml.eventconf.LogDestType;
import org.opennms.netmgt.xml.eventconf.Logmsg;
import org.opennms.test.FileAnticipator;
import org.springframework.core.io.FileSystemResource;

import junit.framework.TestCase;

/**
 * 
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 * @author <a href="mailto:cmiskell@opennms.org">Craig Miskell</a>
 */
public class EventconfFactorySaveTest extends TestCase {
    private static final String knownUEI1="uei.opennms.org/opennmsConfig/eventconf";
    private static final String knownSubfileUEI1="uei.opennms.org/IETF/Bridge/traps/newRoot";
    
    private static final String newUEI="uei.opennms.org/custom/addedUEI";
    private static final String newEventLabel="A New Event which is added to the eventconf";
    private static final String newDescr="A slightly longer descriptive bit of text";
    private static final LogDestType newDest=LogDestType.LOGNDISPLAY;
    private static final String newContent="Test message";
    private static final String newSeverity="Warning";
    
    private FileAnticipator m_fa;
    private DefaultEventConfDao m_eventConfDao;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        m_fa = new FileAnticipator();
        
        //Create a temporary directory
        File origHome = new File("src/test/resources");
        File origEtc = new File(origHome, "etc");
        File origEvents = new File(origEtc, "events");
        
        File tempHome = m_fa.getTempDir();
        File tempEtc = m_fa.expecting(tempHome, "etc");
        File tempEvents = m_fa.expecting(tempEtc, "events");
        
        File eventConf = createTempCopy(m_fa, origEtc, tempEtc, "eventconf.xml");
        createTempCopy(m_fa, origEvents, tempEvents, "Standard.events.xml");
        createTempCopy(m_fa, origEvents, tempEvents, "Syslog.test.events.xml");
        createTempCopy(m_fa, origEvents, tempEvents, "Syslog.LoadTest.events.xml");
        
        m_eventConfDao = new DefaultEventConfDao();
        m_eventConfDao.setConfigResource(new FileSystemResource(eventConf));
        m_eventConfDao.afterPropertiesSet();
    }

    @Override
    protected void tearDown() throws Exception {
        m_fa.deleteExpected();
        m_fa.tearDown();
        super.tearDown();
    }
    
    /**
     * Copys sourceDir/relativeFilePath to destDir/relativeFilePath
     * 
     * @param sourceDir
     * @param destDir
     * @param relativeFilePath
     */
    private static File createTempCopy(FileAnticipator fa, File sourceDir, File destDir, String file) throws Exception {
        FileUtils.copyFile(new File(sourceDir, file), new File(destDir, file));
        return fa.expecting(destDir, file);
    }
    
    public void testSave() throws Exception {
        String newUEI1="uei.opennms.org/custom/newTestUEI1";
        String newUEI2="uei.opennms.org/custom/newTestUEI2";
        
        //Now do the test
        { 
            m_eventConfDao.reload();
            List<Event> events=m_eventConfDao.getEvents(knownUEI1);
            Event event=events.get(0);
            event.setUei(newUEI1);
        }
        
        m_eventConfDao.saveCurrent();
        
        m_eventConfDao.reload(); //The reload might happen as part of the saveCurrent, but is not assured.  We do so here to be certain 
        { 
            List<Event> events=m_eventConfDao.getEvents(knownUEI1);
            assertNull("Shouldn't be any events by that uei", events);
            
            events=m_eventConfDao.getEvents(newUEI1);
            assertNotNull("Should be at least one event", events);
            assertEquals("Should be only one event", 1, events.size());
            Event event=events.get(0);
            assertEquals("Should be the new UEI", newUEI1, event.getUei());
        }
       
        //Check that we can change and save a UEI in a sub file
        { 
            List<Event> events=m_eventConfDao.getEvents(knownSubfileUEI1);
            Event event=events.get(0);
            event.setUei(newUEI2);
        }
        
        m_eventConfDao.saveCurrent();
        
        m_eventConfDao.reload(); //The reload might happen as part of the saveCurrent, but is not assured.  We do so here to be certain 
        { 
            List<Event> events=m_eventConfDao.getEvents(knownSubfileUEI1);
            assertNull("Shouldn't be any events by that uei", events);
            
            events=m_eventConfDao.getEvents(newUEI2);
            assertNotNull("Should be at least one event", events);
            assertEquals("Should be only one event", 1, events.size());
            Event event=events.get(0);
            assertEquals("Should be the new UEI", newUEI2, event.getUei());
        }

    }

    private static Event getAddableEvent() {
        Event event=new Event();
        event.setUei(newUEI);
        event.setEventLabel(newEventLabel);
        event.setDescr(newDescr);
        Logmsg logmsg=new Logmsg();
        logmsg.setDest(newDest);
        logmsg.setContent(newContent);
        event.setLogmsg(logmsg);
        event.setSeverity(newSeverity);
        return event;
    }
    
    private static void checkAddableEvent(Event event) {
        assertEquals("Should be the new UEI", newUEI, event.getUei());
        assertEquals(newEventLabel, event.getEventLabel());
        assertEquals(newDescr, event.getDescr());
        assertEquals(newDest, event.getLogmsg().getDest());
        assertEquals(newContent, event.getLogmsg().getContent());
        assertEquals(newSeverity, event.getSeverity()); 
    }
    
    public void testAddEvent() {
        Event event=getAddableEvent();
        
        //The tested event
        m_eventConfDao.addEvent(event);
        
        {
            List<Event> events=m_eventConfDao.getEvents(newUEI);
            assertNotNull("Should be at least one event", events);
            assertEquals("Should be only one event", 1, events.size());
            Event fetchedEvent=events.get(0);
            checkAddableEvent(fetchedEvent);
        }
        
        m_eventConfDao.saveCurrent();
        m_eventConfDao.reload();
        
        {
            //Check that the new Event is still there
            List<Event> events=m_eventConfDao.getEvents(newUEI);
            assertNotNull("Should be at least one event", events);
            assertEquals("Should be only one event", 1, events.size());
            Event fetchedEvent=events.get(0);
            checkAddableEvent(fetchedEvent);
        }
    }
    
    /**
     * Test adding and event to a specific file
     *
     */
    public void testAddEventToProgrammaticStore() {
        Event event=getAddableEvent();
        
        m_eventConfDao.addEventToProgrammaticStore(event);
        
        //Check that the new Event is still there
        {
            List<Event> events=m_eventConfDao.getEvents(newUEI);
  
            assertNotNull("Should be at least one event", events);
            assertEquals("Should be only one event", 1, events.size());
            Event fetchedEvent=events.get(0);
            checkAddableEvent(fetchedEvent);
        }
        
        m_eventConfDao.saveCurrent();
        m_eventConfDao.reload();
        
        //We are expecting this new file to be there - if it's not, that's an issue
        m_fa.expecting(new File(m_fa.getTempDir().getAbsolutePath()+File.separator+"etc"+File.separator+"events"),"programmatic.events.xml");
        //Check again after the reload
        {
            List<Event> events=m_eventConfDao.getEvents(newUEI);
  
            assertNotNull("Should be at least one event", events);
            assertEquals("Should be only one event", 1, events.size());
            Event fetchedEvent=events.get(0);
            checkAddableEvent(fetchedEvent);
        }
    }
    
    public void testRemoveEventToProgrammaticStore() {
        Event event=getAddableEvent();
        
        m_eventConfDao.addEventToProgrammaticStore(event);
        {
            //Check that the new Event is still there
            List<Event> events=m_eventConfDao.getEvents(newUEI);
            assertNotNull("Should be at least one event", events);
            assertEquals("Should be only one event", 1, events.size());
            Event fetchedEvent=events.get(0);
            checkAddableEvent(fetchedEvent);     
        }
        
        //Check before the save/reload
        assertTrue("remove should have returned true", m_eventConfDao.removeEventFromProgrammaticStore(event));
        {
            List<Event> events=m_eventConfDao.getEvents(newUEI);
            assertNull(events);
        }

        m_eventConfDao.saveCurrent();
        m_eventConfDao.reload();

        //Should get a "false" when the event is already missing
        assertFalse("remove should have returned false",m_eventConfDao.removeEventFromProgrammaticStore(event));
        //Check again after save/reload

        {
            List<Event> events=m_eventConfDao.getEvents(newUEI);
            assertNull(events);
        }

    }   
}
