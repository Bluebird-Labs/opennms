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
package org.opennms.web.rest.v2;

import java.util.Collection;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.opennms.core.config.api.JaxbListWrapper;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.netmgt.dao.api.CategoryDao;
import org.opennms.netmgt.dao.support.CreateIfNecessaryTemplate;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsCategoryCollection;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.events.EventUtils;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.web.api.RestUtils;
import org.opennms.web.rest.support.MultivaluedMapImpl;
import org.opennms.web.rest.support.RedirectHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * Basic Web Service using REST for {@link OnmsCategory} entity.
 *
 * @author <a href="agalue@opennms.org">Alejandro Galue</a>
 */
@Component
@Transactional
public class NodeCategoriesRestService extends AbstractNodeDependentRestService<OnmsCategory,OnmsCategory,Integer,String> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeCategoriesRestService.class);

    @Autowired
    private CategoryDao m_dao;

    @Autowired
    private PlatformTransactionManager m_transactionManager;

    @Override
    protected CategoryDao getDao() {
        return m_dao;
    }

    @Override
    protected Class<OnmsCategory> getDaoClass() {
        return OnmsCategory.class;
    }

    @Override
    protected Class<OnmsCategory> getQueryBeanClass() {
        return OnmsCategory.class;
    }

    @Override
    protected CriteriaBuilder getCriteriaBuilder(final UriInfo uriInfo) {
        return new CriteriaBuilder(getDaoClass()).distinct();
    }

    @Override
    protected JaxbListWrapper<OnmsCategory> createListWrapper(Collection<OnmsCategory> list) {
        return new OnmsCategoryCollection(list);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML})
    public Response get(@Context final UriInfo uriInfo, @Context final SearchContext searchContext) {
        final OnmsNode node = getNode(uriInfo);
        if (node == null) return Response.status(Status.NOT_FOUND).build();
        return Response.ok(new OnmsCategoryCollection(node.getCategories())).build();
    }

    @GET
    @Path("count")
    @Produces({MediaType.TEXT_PLAIN})
    public Response getCount(@Context final UriInfo uriInfo, @Context final SearchContext searchContext) {
        final OnmsNode node = getNode(uriInfo);
        if (node == null) return Response.status(Status.NOT_FOUND).build();
        return Response.ok(node.getCategories().size()).build();
    }

    @Override
    protected Response doCreate(SecurityContext securityContext, UriInfo uriInfo, OnmsCategory source) {
        OnmsNode node = getNode(uriInfo);
        if (node == null) {
            throw getException(Status.BAD_REQUEST, "Node was not found.");
        } else if (source == null) {
            throw getException(Status.BAD_REQUEST, "Category object cannot be null");
        } else if (source.getName() == null) {
            throw getException(Status.BAD_REQUEST, "Category's name cannot be null");
        }
        final OnmsCategory category = getCategory(source.getName());
        node.addCategory(category);
        m_nodeDao.saveOrUpdate(node);

        final Event event = EventUtils.createNodeCategoryMembershipChangedEvent("ReST", node.getId(), node.getLabel(), new String[] { category.getName() }, null);
        sendEvent(event);

        return Response.created(RedirectHelper.getRedirectUri(uriInfo, category.getName())).build();
    }

    @Override
    protected Response doUpdateProperties(SecurityContext securityContext, UriInfo uriInfo, OnmsCategory targetObject, MultivaluedMapImpl params) {
        if (params.getFirst("name") != null) {
            throw getException(Status.BAD_REQUEST, "Cannot rename category.");
        }
        RestUtils.setBeanProperties(targetObject, params);
        getDao().update(targetObject);
        return Response.noContent().build();
    }

    @Override
    protected void doDelete(SecurityContext securityContext, UriInfo uriInfo, OnmsCategory category) {
        getNode(uriInfo).removeCategory(category);
        getDao().delete(category);

        final OnmsNode node = getNode(uriInfo);
        final Event event = EventUtils.createNodeCategoryMembershipChangedEvent("ReST", node.getId(), node.getLabel(), null, new String[] { category.getName() });
        sendEvent(event);
    }

    @Override
    protected OnmsCategory doGet(UriInfo uriInfo, String categoryName) {
        final OnmsNode node = getNode(uriInfo);
        if (node == null) return null;
        Optional<OnmsCategory> optional = node.getCategories().stream().filter(c -> c.getName().equals(categoryName)).findFirst();
        return optional.isPresent() ? optional.get() : null;
    }

    private OnmsCategory getCategory(final String categoryName) {
        final OnmsCategory category = new CreateIfNecessaryTemplate<OnmsCategory, CategoryDao>(m_transactionManager, m_dao) {
            @Override
            protected OnmsCategory query() {
                return m_dao.findByName(categoryName);
            }
            @Override
            protected OnmsCategory doInsert() {
                LOG.info("getCategory: creating category {}", categoryName);
                final OnmsCategory c = new OnmsCategory(categoryName);
                m_dao.saveOrUpdate(c);
                return c;
            }
        }.execute();
        return category;
    }

}
