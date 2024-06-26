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
package org.opennms.features.config.exception;

import com.atlassian.oai.validator.report.ValidationReport;

import java.util.stream.Collectors;

/**
 * This exception is about config validation.
 **/
public class ValidationException extends ConfigRuntimeException {
    private ValidationReport report;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(ValidationReport report) {
        this.report = report;
    }

    public ValidationReport getReport() {
        return report;
    }

    @Override
    public String getMessage() {
        if (report != null) {
            return report.getMessages().stream().map(ValidationReport.Message::getMessage)
                    .collect(Collectors.joining("\n"));
        } else {
            return super.getMessage();
        }
    }
}