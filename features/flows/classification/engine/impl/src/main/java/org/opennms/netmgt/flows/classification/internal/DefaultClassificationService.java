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
package org.opennms.netmgt.flows.classification.internal;

import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.netmgt.dao.api.SessionUtils;
import org.opennms.netmgt.flows.classification.ClassificationEngine;
import org.opennms.netmgt.flows.classification.ClassificationRequest;
import org.opennms.netmgt.flows.classification.ClassificationService;
import org.opennms.netmgt.flows.classification.FilterService;
import org.opennms.netmgt.flows.classification.csv.CsvImportResult;
import org.opennms.netmgt.flows.classification.csv.CsvService;
import org.opennms.netmgt.flows.classification.error.ErrorContext;
import org.opennms.netmgt.flows.classification.error.Errors;
import org.opennms.netmgt.flows.classification.exception.CSVImportException;
import org.opennms.netmgt.flows.classification.exception.ClassificationException;
import org.opennms.netmgt.flows.classification.exception.InvalidRuleException;
import org.opennms.netmgt.flows.classification.internal.csv.CsvServiceImpl;
import org.opennms.netmgt.flows.classification.internal.validation.GroupValidator;
import org.opennms.netmgt.flows.classification.internal.validation.RuleValidator;
import org.opennms.netmgt.flows.classification.persistence.api.ClassificationGroupDao;
import org.opennms.netmgt.flows.classification.persistence.api.ClassificationRuleDao;
import org.opennms.netmgt.flows.classification.persistence.api.Group;
import org.opennms.netmgt.flows.classification.persistence.api.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultClassificationService implements ClassificationService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultClassificationService.class);

    private final ClassificationRuleDao classificationRuleDao;

    private final ClassificationGroupDao classificationGroupDao;

    private final ClassificationEngine classificationEngine;

    private final RuleValidator ruleValidator;

    private final CsvService csvService;

    private final GroupValidator groupValidator;

    private final SessionUtils sessionUtils;

    public DefaultClassificationService(ClassificationRuleDao classificationRuleDao,
                                        ClassificationGroupDao classificationGroupDao,
                                        ClassificationEngine classificationEngine,
                                        FilterService filterService,
                                        SessionUtils sessionUtils) {
        this.classificationRuleDao = Objects.requireNonNull(classificationRuleDao);
        this.classificationGroupDao = Objects.requireNonNull(classificationGroupDao);
        this.classificationEngine = Objects.requireNonNull(classificationEngine);
        this.sessionUtils = Objects.requireNonNull(sessionUtils);
        this.ruleValidator = new RuleValidator(filterService);
        this.groupValidator = new GroupValidator(classificationRuleDao);
        this.csvService = new CsvServiceImpl(ruleValidator);
    }

    @Override
    public List<Rule> findMatchingRules(Criteria criteria) {
        return classificationRuleDao.findMatching(criteria);
    }

    @Override
    public int countMatchingRules(Criteria criteria) {
        return classificationRuleDao.countMatching(criteria);
    }

    @Override
    public Rule getRule(int ruleId) {
        Rule rule = classificationRuleDao.get(ruleId);
        if (rule == null) throw new NoSuchElementException();
        return rule;
    }

    @Override
    public Integer saveRule(Rule rule) throws InvalidRuleException {
        return runInTransactionAndThenReload(() -> {

            ruleValidator.validate(rule);

            final Group group = classificationGroupDao.get(rule.getGroup().getId());
            if(group == null) {
                throw new NoSuchElementException(String.format("Unknown group with id=%s", rule.getGroup().getId()));
            }
            assertRuleIsNotInReadOnlyGroup(group);
            groupValidator.validate(group, rule);

            // persist
            group.addRule(rule);
            final Integer ruleId = classificationRuleDao.save(rule);
            updateRulePositions(PositionUtil.sortRulePositions(rule));

            return ruleId;
        });
    }

    @Override
    public void importRules(int groupId, InputStream inputStream, boolean hasHeader, boolean deleteExistingRules) throws CSVImportException {
        runInTransactionAndThenReload(() -> {

            // Get and check group
            Group group = classificationGroupDao.get(groupId);
            if(group == null) {
                throw new ClassificationException(ErrorContext.Name, Errors.GROUP_NOT_FOUND, groupId);
            }
            if(group.isReadOnly()) {
                throw new ClassificationException(ErrorContext.Name, Errors.GROUP_READ_ONLY, groupId);
            }

            // Parse and validate the rules
            final CsvImportResult result = csvService.parseCSV(group, inputStream, hasHeader);
            if (!result.isSuccess()) {
                throw new CSVImportException(result);
            }

            // Remove existing rules and afterwards add new rules
            if (deleteExistingRules) {
                for (Rule eachRule : group.getRules()) {
                    classificationRuleDao.delete(eachRule);
                }
                group.getRules().clear();
            }

            // Add new rules
            final List<Rule> rules = result.getRules();
            for (int i=0; i<rules.size(); i++) {
                final Rule rule = rules.get(i);
                try {
                    groupValidator.validate(group, rule);
                    group.addRule(rule);
                } catch (ClassificationException ex) {
                    result.markError(i, ex.getError());
                }
            }

            // before continuing, verify everything is okay, otherwise bail
            if (!result.isSuccess()) {
                throw new CSVImportException(result);
            }

            updateRulePositions(PositionUtil.sortRulePositions(group.getRules()));
            classificationGroupDao.saveOrUpdate(group);

            return null;
        });
    }

    @Override
    public String exportRules(int groupId) {
        final Group group = classificationGroupDao.get(groupId);
        if (group == null) throw new NoSuchElementException();
        final String csvContent = csvService.createCSV(group.getRules());
        return csvContent;
    }

    @Override
    public void deleteRules(int groupId) {
        CriteriaBuilder criteriaBuilder = new CriteriaBuilder(Rule.class).alias("group", "group");
        criteriaBuilder.eq("group.id", groupId);
        runInTransactionAndThenReload(() -> {
            Group group = classificationGroupDao.get(groupId);
            if (group == null) throw new NoSuchElementException();
            if(group.isReadOnly()) {
                throw new ClassificationException(ErrorContext.Entity, Errors.GROUP_READ_ONLY);
            }
            final Criteria criteria = criteriaBuilder.toCriteria();
            classificationRuleDao.findMatching(criteria).forEach(r -> classificationRuleDao.delete(r));
            return null;
        });
    }

    @Override
    public void deleteRule(int ruleId) {
        runInTransaction(() -> {
            final Rule rule = classificationRuleDao.get(ruleId);
            if (rule == null) throw new NoSuchElementException();
            assertRuleIsNotInReadOnlyGroup(rule);
            return runInTransactionAndThenReload(() -> {
                // Remove from group, as it would be saved later otherwise
                final Group group = rule.getGroup();
                group.removeRule(rule);
                classificationRuleDao.delete(rule);
                updateRulePositions(PositionUtil.sortRulePositions(group.getRules()));
                return null;
            });
        });
    }

    @Override
    public void updateRule(final Rule rule) {
        if (rule.getId() == null) throw new NoSuchElementException();
        assertRuleIsNotInReadOnlyGroup(rule);

        // Persist
        runInTransactionAndThenReload(() -> {
            ruleValidator.validate(rule);
            groupValidator.validate(rule.getGroup(), rule);
            classificationRuleDao.saveOrUpdate(rule);
            // reload to get updated group since we might just have switched groups
            Rule reloaded = classificationRuleDao.get(rule.getId());
            updateRulePositions(PositionUtil.sortRulePositions(reloaded));
            return null;
        });
    }

    private void assertRuleIsNotInReadOnlyGroup(Rule rule) {
        assertRuleIsNotInReadOnlyGroup(rule.getGroup());
    }

    private void assertRuleIsNotInReadOnlyGroup(Group group) {
        if(group.isReadOnly()) {
            throw new ClassificationException(ErrorContext.Entity, Errors.GROUP_READ_ONLY);
        }
    }

    @Override
    public String classify(ClassificationRequest classificationRequest) {
        final String classification = classificationEngine.classify(classificationRequest);
        return classification;
    }

    @Override
    public List<Group> findMatchingGroups(Criteria criteria) {
        return classificationGroupDao.findMatching(criteria);
    }

    @Override
    public int countMatchingGroups(Criteria criteria) {
        return classificationGroupDao.countMatching(criteria);
    }

    @Override
    public Group getGroup(int groupId) {
        final Group group = classificationGroupDao.get(groupId);
        if (group == null) throw new NoSuchElementException();
        return group;
    }

    @Override
    public Integer saveGroup(Group group) {
        checkForDuplicateName(group);
        return runInTransactionAndThenReload(() -> {
            Integer groupId = classificationGroupDao.save(group);
            updateGroupPositions(PositionUtil.sortGroupPositions(group, classificationGroupDao.findAll()));
            return groupId;
        });
    }

    @Override
    public void deleteGroup(int groupId) {
        runInTransactionAndThenReload(() -> {
            final Group group = classificationGroupDao.get(groupId);
            if (group == null) {
                throw new NoSuchElementException();
            } else if(group.isReadOnly()) {
                throw new ClassificationException(ErrorContext.Entity, Errors.GROUP_READ_ONLY);
            }
            classificationGroupDao.delete(group);
            updateGroupPositions(PositionUtil.sortGroupPositions(classificationGroupDao.findAll()));
            return null;
        });
    }

    @Override
    public void updateGroup(Group group) {
        if (group.getId() == null) throw new NoSuchElementException();
        checkForDuplicateName(group);
        runInTransactionAndThenReload(() -> {
            classificationGroupDao.saveOrUpdate(group);
            updateGroupPositions(PositionUtil.sortGroupPositions(group, classificationGroupDao.findAll()));
            updateRulePositions(PositionUtil.sortRulePositions(group.getRules()));
            return null;
        });
    }

    @Override
    public List<Rule> getInvalidRules() {
        return classificationEngine.getInvalidRules();
    }

    @Override
    public void validateRule(final Rule validateMe) {
        Objects.requireNonNull(validateMe);
        ruleValidator.validate(validateMe);
    }

    private <T> T runInTransaction(Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        return sessionUtils.withTransaction(supplier);
    }

    private <T> T runInTransactionAndThenReload(Supplier<T> supplier) {
        T res = runInTransaction(supplier);
        try {
            classificationEngine.reload();
        } catch (InterruptedException e) {
            LOG.error("reload was interrupted", e);
        }
        return res;
    }

    private void updateRulePositions(final List<Rule> rules) {

        // Update position field
        for (int i=0; i<rules.size(); i++) {
            Rule rule = rules.get(i);
            rule.setPosition(i);
            classificationRuleDao.saveOrUpdate(rule);
        }

    }

    private void updateGroupPositions(final List<Group> groups) {

        // Update position field
        for (int i=0; i<groups.size(); i++) {
            Group group = groups.get(i);
            group.setPosition(i);
            classificationGroupDao.saveOrUpdate(group);
        }

    }

    private void checkForDuplicateName (Group group) {
        CriteriaBuilder builder = new CriteriaBuilder(Group.class)
                .eq("name", group.getName());
        if(group.getId() !=null) {
            builder.ne("id", group.getId());
        }
        if(countMatchingGroups(builder.toCriteria()) > 0) {
            throw new ClassificationException(ErrorContext.Entity, Errors.GROUP_NAME_NOT_UNIQUE, group.getName());
        }
    }
}
