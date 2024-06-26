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
package org.opennms.features.vaadin.datacollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opennms.features.vaadin.api.OnmsBeanContainer;
import org.opennms.netmgt.config.api.DataCollectionConfigDao;
import org.opennms.netmgt.config.datacollection.IncludeCollection;
import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.CustomField;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.v7.ui.Table;
import com.vaadin.v7.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

/**
 * The Include Collection Field.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a> 
 */
@SuppressWarnings("serial")
public class IncludeCollectionField extends CustomField<List<IncludeCollection>> {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 3677540981240383672L;

    /** The Container. */
    private final OnmsBeanContainer<IncludeCollectionWrapper> container = new OnmsBeanContainer<IncludeCollectionWrapper>(IncludeCollectionWrapper.class);

    /** The Include Field Table. */
    private final Table table = new Table("Includes List", container);

    /** The Toolbar. */
    private final HorizontalLayout toolbar = new HorizontalLayout();

    /**
     * Instantiates a new include collection field.
     * 
     * @param dataCollectionConfigDao the data collection configuration DAO
     */
    public IncludeCollectionField(final DataCollectionConfigDao dataCollectionConfigDao) {
        setCaption("Include Collections");

        table.addStyleName("light");
        table.setVisibleColumns(new Object[]{"type", "value"});
        table.setColumnHeaders(new String[]{"Type", "Value"});
        table.setEditable(!isReadOnly());
        table.setSelectable(true);
        table.setImmediate(true);
        table.setSizeFull();
        final Button add = new Button("Add", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                final IncludeCollectionWrapper obj = new IncludeCollectionWrapper();
                IncludeCollectionWindow w = new IncludeCollectionWindow(dataCollectionConfigDao, container, obj) {
                    @Override
                    public void fieldChanged() {
                        container.addBean(obj);
                        table.select(obj);
                    }
                };
                getUI().addWindow(w);
            }
        });
        final Button edit = new Button("Edit", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                final Object value = table.getValue();
                if (value == null) {
                    Notification.show("Please select a IncludeCollection from the table.");
                    return;
                }
                IncludeCollectionWindow w = new IncludeCollectionWindow(dataCollectionConfigDao, container, container.getOnmsBean(value)) {
                    @Override
                    public void fieldChanged() {}
                };
                getUI().addWindow(w);
            }
        });
        final Button delete = new Button("Delete", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                deleteHandler();
            }
        });

        toolbar.addComponent(add);
        toolbar.addComponent(edit);
        toolbar.addComponent(delete);
        toolbar.setVisible(table.isEditable());
    }

    /* (non-Javadoc)
     * @see com.vaadin.ui.CustomField#initContent()
     */
    @Override
    public Component initContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.addComponent(table);
        layout.addComponent(toolbar);
        layout.setComponentAlignment(toolbar, Alignment.MIDDLE_RIGHT);
        return layout;
    }

    /* (non-Javadoc)
     * @see com.vaadin.ui.AbstractField#getType()
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends List<IncludeCollection>> getType() {
        // Check org.opennms.netmgt.config.datacollection.SnmpCollection.getIncludeCollections() 
        return (Class<? extends List<IncludeCollection>>) Collections.unmodifiableList(new ArrayList<IncludeCollection>()).getClass();
    }

    /* (non-Javadoc)
     * @see com.vaadin.ui.AbstractField#setInternalValue(java.lang.Object)
     */
    @Override
    protected void setInternalValue(List<IncludeCollection> includeCollections) {
        container.removeAllItems();
        for (IncludeCollection ic : includeCollections) {
            container.addBean(new IncludeCollectionWrapper(ic));
        }
    }

    /* (non-Javadoc)
     * @see com.vaadin.ui.AbstractField#getInternalValue()
     */
    @Override
    protected List<IncludeCollection> getInternalValue() {
        final List<IncludeCollection> beans = new ArrayList<>();
        for (IncludeCollectionWrapper wrapper : container.getOnmsBeans()) {
            beans.add(wrapper.createIncludeCollection());
        }
        return beans;
    }

    /* (non-Javadoc)
     * @see com.vaadin.ui.AbstractComponent#setReadOnly(boolean)
     */
    @Override
    public void setReadOnly(boolean readOnly) {
        table.setEditable(!readOnly);
        toolbar.setVisible(!readOnly);
        super.setReadOnly(readOnly);
    }

    /**
     * Delete handler.
     */
    private void deleteHandler() {
        final Object itemId = table.getValue();
        if (itemId == null) {
            Notification.show("Please select a IncludeCollection from the table.");
            return;
        }
        ConfirmDialog.show(getUI(),
                           "Are you sure?",
                           "Do you really want to remove the selected Include Collection field?\nThis action cannot be undone.",
                           "Yes",
                           "No",
                           new ConfirmDialog.Listener() {
            public void onClose(ConfirmDialog dialog) {
                if (dialog.isConfirmed()) {
                    table.removeItem(itemId);
                }
            }
        });
    }

}
