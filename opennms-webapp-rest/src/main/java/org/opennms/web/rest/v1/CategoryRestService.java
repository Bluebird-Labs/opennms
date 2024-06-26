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
package org.opennms.web.rest.v1;

import java.util.ArrayList;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.opennms.netmgt.dao.api.CategoryDao;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsCategoryCollection;
import org.opennms.web.rest.support.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>CategoryRestService class.</p>
 *
 * @author ranger
 * @version $Id: $
 * @since 1.8.1
 */
@Component("categoryRestService")
@Path("categories")
@Tag(name = "Categories", description = "Categories API")
@Transactional
public class CategoryRestService extends OnmsRestService {
	
	private static final Logger LOG = LoggerFactory.getLogger(CategoryRestService.class);

    @Autowired
    private CategoryDao m_categoryDao;

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Path("/nodes/{nodeCriteria}")
    public OnmsCategoryCollection getCategoriesForNode(@Context final ResourceContext context, @PathParam("nodeCriteria") String nodeCriteria) {
        return context.getResource(NodeRestService.class).getCategoriesForNode(nodeCriteria);
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Path("{categoryName}/nodes/{nodeCriteria}")
    public OnmsCategory getCategoryForNode(@Context final ResourceContext context, @PathParam("nodeCriteria") String nodeCriteria, @PathParam("categoryName") final String categoryName) {
        return context.getResource(NodeRestService.class).getCategoryForNode(nodeCriteria, categoryName);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_XML)
    @Path("{categoryName}/nodes/{nodeCriteria}/")
    public Response addCategoryToNode(@Context final ResourceContext context, @Context final UriInfo uriInfo, @PathParam("nodeCriteria") final String nodeCriteria, @PathParam("categoryName") final String categoryName) {
        return context.getResource(NodeRestService.class).addCategoryToNode(uriInfo, nodeCriteria, categoryName);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/{categoryName}")
    public Response updateCategory(@PathParam("categoryName") final String categoryName, final MultivaluedMapImpl params) {
        writeLock();
        try {
            OnmsCategory category = m_categoryDao.findByName(categoryName);
            if (category == null) {
                throw getException(Status.BAD_REQUEST, "Category with name '{}' was not found.", categoryName);
            }
            LOG.debug("updateCategory: updating category {}", category);
            BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(category);
            boolean modified = false;
            for(String key : params.keySet()) {
                if (wrapper.isWritableProperty(key)) {
                    String stringValue = params.getFirst(key);
                    Object value = wrapper.convertIfNecessary(stringValue, (Class<?>)wrapper.getPropertyType(key));
                    wrapper.setPropertyValue(key, value);
                    modified = true;
                }
            }
            LOG.debug("updateCategory: category {} updated", category);
            if (modified) {
                m_categoryDao.saveOrUpdate(category);
                return Response.noContent().build();
            }
            return Response.notModified().build();
        } finally {
            writeUnlock();
        }
    }

    @DELETE
    @Path("/{categoryName}/nodes/{nodeCriteria}/")
    public Response removeCategoryFromNode(@Context final ResourceContext context, @PathParam("nodeCriteria") String nodeCriteria, @PathParam("categoryName") String categoryName) {
        return context.getResource(NodeRestService.class).removeCategoryFromNode(nodeCriteria, categoryName);
    }

    @GET
    @Path("/{categoryName}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public OnmsCategory getCategory(@PathParam("categoryName") final String categoryName) {
        OnmsCategory category = m_categoryDao.findByName(categoryName);
        if (category == null) throw getException(Response.Status.NOT_FOUND, "Category with name '{}' was not found.", categoryName);
        return category;
    }
    
    @POST
    @Path("/")
    public Response createCategory(@Context final UriInfo uriInfo, final OnmsCategory category) {
        if (category == null) throw getException(Response.Status.BAD_REQUEST, "Category must not be null.");
        boolean exists = m_categoryDao.findByName(category.getName()) != null;
        if (!exists) {
            m_categoryDao.save(category);
            return Response.created(getRedirectUri(uriInfo, category.getName())).build();
        }
        throw getException(Response.Status.BAD_REQUEST, "A category with name '{}' already exists.", category.getName());
    }

    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public OnmsCategoryCollection listCategories() {
        return new OnmsCategoryCollection(new ArrayList<OnmsCategory>(m_categoryDao.findAll()));
    }

    @DELETE
    @Path("/{categoryName}")
    public Response deleteCategory(@PathParam("categoryName") final String categoryName) {
        OnmsCategory category = m_categoryDao.findByName(categoryName);
        if (category != null) {
            m_categoryDao.delete(category);
            return Response.noContent().build();
        }
        throw getException(Response.Status.BAD_REQUEST, "A category with name '{}' does not exist.", categoryName);
    }


    @PUT
    @Path("/{categoryName}/groups/{groupName}")
    public Response addCategoryToGroup(@Context final ResourceContext context, @PathParam("groupName") final String groupName, @PathParam("categoryName") final String categoryName) {
        return context.getResource(GroupRestService.class).addCategory(groupName, categoryName);
    }

    @DELETE
    @Path("/{categoryName}/groups/{groupName}")
    public Response removeCategoryFromGroup(@Context final ResourceContext context, @PathParam("groupName") final String groupName, @PathParam("categoryName") final String categoryName) {
        return context.getResource(GroupRestService.class).removeCategory(groupName, categoryName);
    }

    @GET
    @Path("/groups/{groupName}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public OnmsCategoryCollection listCategoriesForGroup(@Context final ResourceContext context, @PathParam("groupName") final String groupName) {
        return context.getResource(GroupRestService.class).listCategories(groupName);
    }
}
