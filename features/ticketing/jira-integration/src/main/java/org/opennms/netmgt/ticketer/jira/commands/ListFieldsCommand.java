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
package org.opennms.netmgt.ticketer.jira.commands;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.netmgt.ticketer.jira.JiraClientUtils;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.CimFieldInfo;
import com.atlassian.jira.rest.client.api.domain.CimIssueType;
import com.atlassian.jira.rest.client.api.domain.CimProject;
import com.google.common.base.Strings;

/**
 * <p>This command implements the Apache Karaf 3 and Apache Karaf 4 shell APIs.
 * Once the Karaf 4 commands work, the deprecated Karaf 3 annotations should 
 * be removed:</p>
 * <ul>
 * <li>{@link org.apache.karaf.shell.commands.Command}</li>
 * <li>{@link org.apache.karaf.shell.console.OsgiCommandSupport}</li>
 * </ul>
 */
@Command(scope = "opennms", name = "jira-list-fields", description="Uses the JIRA ReST API to list all fields available")
@org.apache.karaf.shell.commands.Command(scope = "opennms", name = "jira-list-fields", description="Uses the JIRA ReST API to list all fields available")
@Service
public class ListFieldsCommand extends AbstractJiraCommand implements Action {

    @Option(name="-k", aliases="--project-key", description = "The project key to filter for.")
    String projectKey;

    @Option(name="-i", aliases="--issue-type-name", description = "The issue type name to filter for.")
    String issueTypeName;

    @Option(name="-s", aliases="--show-all", description = "Show all fields. By default only custom fields are shown.")
    boolean showAll = false;

    @Override
    protected void doExecute(JiraRestClient jiraRestClient) throws Exception {
        final StringBuilder info = new StringBuilder();
        info.append("Fetching ");
        info.append(showAll ? "custom" : "all");
        info.append(" fields for ");
        info.append(Strings.isNullOrEmpty(projectKey) ? "all projects" : "project with key '" + projectKey + "'");
        info.append(Strings.isNullOrEmpty(issueTypeName) ? " and all issue types" : " and issue type with name '" + issueTypeName + "'");
        info.append(".");

        System.out.println(info.toString());
        System.out.println();

        // Fetch data
        final Iterable<CimProject> cimProjects = JiraClientUtils.getIssueMetaData(jiraRestClient, "projects.issuetypes.fields", issueTypeName, projectKey);

        // If we have fields, show them, otherwise bail out
        final Iterator<CimProject> iterator = cimProjects.iterator();
        if (!iterator.hasNext()) {
            System.out.println("No fields found. The user making the ReST call may not have sufficient permissions.");
            return;
        }

        // We found fields, so show them
        final String ISSUE_ROW_FORMAT = "%s fields for issue type %s (id: %s)";
        final String FIELD_ROW_FORMAT = "%-30s %-20s %-10s %-10s %s";
        while (iterator.hasNext()) {
            CimProject project = iterator.next();
            System.out.println("Project " + project.getName() + " (" + project.getKey() + ")");
            System.out.println(LINE);

            // No types found
            Iterator<CimIssueType> issueIt = project.getIssueTypes().iterator();
            if (!issueIt.hasNext()) {
                System.out.println("No issue types found");
                continue;
            }

            // List each issue type
            while (issueIt.hasNext()) {
                final CimIssueType issue = issueIt.next();
                final Map<String, CimFieldInfo> fieldsMap = filter(issue.getFields(), showAll);

                System.out.println();
                System.out.println(String.format(ISSUE_ROW_FORMAT, showAll ? "All" : "Custom", issue.getName(), issue.getId()));
                System.out.println(LINE + LINE);
                if (fieldsMap.isEmpty()) {
                    System.out.println("No fields found");
                    continue;
                }

                // Sort by name
                final List<CimFieldInfo> fields = fieldsMap.values().stream().sorted(Comparator.comparing(CimFieldInfo::getName)).collect(Collectors.toList());
                System.out.println(String.format(FIELD_ROW_FORMAT, "Name", "Id", "Required", "Custom", "Type"));
                for (CimFieldInfo eachField : fields) {
                    System.out.println(
                            String.format(FIELD_ROW_FORMAT,
                                eachField.getName(),
                                eachField.getId(),
                                eachField.isRequired(),
                                isCustom(eachField),
                                eachField.getSchema().getType()));
                }
            }
        }
    }

    private static Map<String, CimFieldInfo> filter(Map<String, CimFieldInfo> fields, boolean showAll) {
        if (showAll) {
            return fields;
        }
        Map<String, CimFieldInfo> filteredFields = new HashMap<>();
        for (Map.Entry<String, CimFieldInfo> eachField : fields.entrySet()) {
            if (isCustom(eachField.getValue())) {
                filteredFields.put(eachField.getKey(), eachField.getValue());
            }
        }
        return filteredFields;
    }

    private static boolean isCustom(CimFieldInfo info) {
        return info != null && info.getSchema() != null && info.getSchema().getCustomId() != null;
    }
}
