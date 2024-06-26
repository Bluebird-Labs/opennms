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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.opennms.core.utils.ConfigFileConstants;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.config.viewsdisplay.View;
import org.opennms.netmgt.config.viewsdisplay.Viewinfo;

/**
 * <p>ViewsDisplayFactory class.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public class ViewsDisplayFactory {
    /** The singleton instance. */
    private static ViewsDisplayFactory m_instance;

    /** File path of groups.xml. */
    protected File m_viewsDisplayFile;

    /** Boolean indicating if the init() method has been called. */
    protected boolean initialized = false;

    /** Timestamp of the viewDisplay file, used to know when to reload from disk. */
    protected long m_lastModified;

    /** Map of view objects by name. */
    protected Map<String,View> m_viewsMap;

    private Viewinfo m_viewInfo;

    /**
     * Empty private constructor so this class cannot be instantiated outside
     * itself.
     * @throws IOException 
     * @throws FileNotFoundException
     */
    private ViewsDisplayFactory() throws FileNotFoundException, IOException {
        reload();
    }

    /**
     * <p>Constructor for ViewsDisplayFactory.</p>
     *
     * @param file a {@link java.lang.String} object.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     */
    public ViewsDisplayFactory(String file) throws FileNotFoundException, IOException {
        setViewsDisplayFile(new File(file));
        reload();
    }

    /**
     * Be sure to call this method before calling getInstance().
     *
     * @throws java.io.IOException if any.
     * @throws java.io.FileNotFoundException if any.
     */
    public static synchronized void init() throws IOException, FileNotFoundException {
        if (m_instance == null) {
            setInstance(new ViewsDisplayFactory());
        }
    }

    /**
     * Singleton static call to get the only instance that should exist for the
     * ViewsDisplayFactory
     *
     * @return the single views display factory instance
     * @throws java.lang.IllegalStateException
     *             if init has not been called
     */
    public static synchronized ViewsDisplayFactory getInstance() {
        if (m_instance == null) {
            throw new IllegalStateException("You must call ViewDisplay.init() before calling getInstance().");
        }

        return m_instance;
    }

    /**
     * Parses the viewsdisplay.xml
     *
     * @throws java.io.IOException if any.
     * @throws java.io.FileNotFoundException if any.
     */
    public synchronized void reload() throws IOException, FileNotFoundException {
        InputStream stream = null;
        try {
            stream = getStream();
            unmarshal(stream);
        } finally {
            if (stream != null) {
                IOUtils.closeQuietly(stream);
            }
        }
    }

    private void unmarshal(InputStream stream) throws IOException {
        try(final Reader reader = new InputStreamReader(stream)) {
            m_viewInfo = JaxbUtils.unmarshal(Viewinfo.class, reader);
        }
        updateViewsMap();
    }

    private void updateViewsMap() {
        Map<String, View> viewsMap = new HashMap<String,View>();

        for (View view : m_viewInfo.getViews()) {
            viewsMap.put(view.getViewName(), view);
        }
        
        m_viewsMap = viewsMap;
    }
    
    private InputStream getStream() throws IOException {
        File viewsDisplayFile = getViewsDisplayFile();
        m_lastModified = viewsDisplayFile.lastModified();
        return new FileInputStream(viewsDisplayFile);
    }

    /**
     * <p>setViewsDisplayFile</p>
     *
     * @param viewsDisplayFile a {@link java.io.File} object.
     */
    public void setViewsDisplayFile(File viewsDisplayFile) {
        m_viewsDisplayFile = viewsDisplayFile;
    }

    /**
     * <p>getViewsDisplayFile</p>
     *
     * @return a {@link java.io.File} object.
     * @throws java.io.IOException if any.
     */
    public File getViewsDisplayFile() throws IOException {
        if (m_viewsDisplayFile == null) {
            m_viewsDisplayFile = ConfigFileConstants.getFile(ConfigFileConstants.VIEWS_DISPLAY_CONF_FILE_NAME);
        }
        return m_viewsDisplayFile;
    }

    /**
     * Can be null
     *
     * @param viewName a {@link java.lang.String} object.
     * @return a {@link org.opennms.netmgt.config.viewsdisplay.View} object.
     * @throws java.io.IOException if any.
     */
    public View getView(String viewName) throws IOException {
        if (viewName == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }

        updateFromFile();

        View view = m_viewsMap.get(viewName);

        return view;
    }
    
    /**
     * <p>getDefaultView</p>
     *
     * @return a {@link org.opennms.netmgt.config.viewsdisplay.View} object.
     */
    public View getDefaultView() {
        return m_viewsMap.get(m_viewInfo.getDefaultView());
    }

    /**
     * Reload the viewsdisplay.xml file if it has been changed since we last
     * read it.
     *
     * @throws java.io.IOException if any.
     */
    protected void updateFromFile() throws IOException {
        if (m_lastModified != m_viewsDisplayFile.lastModified()) {
            reload();
        }
    }

    /**
     * <p>setInstance</p>
     *
     * @param instance a {@link org.opennms.netmgt.config.ViewsDisplayFactory} object.
     */
    public static void setInstance(ViewsDisplayFactory instance) {
        m_instance = instance;
        m_instance.initialized = true;
    }

    /**
     * <p>getDisconnectTimeout</p>
     *
     * @return a int.
     */
    public int getDisconnectTimeout() {
        return m_viewInfo.getDisconnectTimeout();
    }
}
