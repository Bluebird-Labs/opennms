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
package org.opennms.reporting.availability;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.opennms.core.logging.Logging;
import org.opennms.reporting.datablock.IfService;
import org.opennms.reporting.datablock.Interface;
import org.opennms.reporting.datablock.Node;
import org.opennms.reporting.datablock.OutageSince;
import org.opennms.reporting.datablock.OutageSvcTimesList;
import org.opennms.reporting.datablock.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AvailCalculations does all computations for all reports for a category. The
 * types include Last 30 days daily availability Last 30 days total availability
 * Last 30 days daily service availability Last Months Top 20 offenders Last
 * Months Top 20 Service outages Last N Months Availability Last Months Daily
 * Availability Last Months Total Availability Last Months Daily Service
 * Availability Month To Date Daily Availability Month To Date Total
 * Availability
 *
 * @author <A HREF="mailto:jacinta@oculan.com">Jacinta Remedios </A>
 */
public class AvailCalculations extends Object {
    private static final Logger LOG = LoggerFactory.getLogger(AvailCalculations.class);
    /**
     * The log4j category used to log debug messsages and statements.
     */
    private static final String LOG4J_CATEGORY = "reports";

    /**
     * Object that holds all the information required for the generating
     * xml to be translated to the pdf.
     */
    private static Report m_report = null;

    /**
     * Services map
     */
    private Map<String, Map<IfService, OutageSvcTimesList>> m_services = null;

    /**
     * End time of the last month.
     */
    private long m_endLastMonthTime;

    /**
     * Number of days in the last month.
     */
    private int m_daysInLastMonth;

    /**
     * The time in milliseconds per day.
     */
    private static final long ROLLING_WINDOW = 86400000L;

    /**
     * Constant
     */
    private static final int THIRTY = 30;

    /**
     * Constant (Number of months)
     */
    private static final int NMONTHS = 12;

    /**
     * Nodes that match this category.
     */
    private List<Node> m_nodes;

    /**
     * Monitored Services for the category
     */
    private List<String> m_monitoredServices;

    /**
     * This is used for the PDF Report generation
     */
    private int m_sectionIndex;

    /**
     * Constructor
     *
     * @param nodes
     *            List of nodes
     * @param endTime1
     *            End time ( end of yesterday in milliseconds)
     * @param lastMonthEndTime
     *            Last months end time (end of the last day of last month in
     *            milliseconds)
     * @param monitoredServices
     *            Monitored services belonging to the category.
     * @param report
     *            Report class.
     * @param offenders
     *            Map of all offenders -- percent/(list of node) pairs
     * @param format
     *            Value can be "SVG / all"
     * @param warning a double.
     * @param normal a double.
     * @param comments a {@link java.lang.String} object.
     * @param name a {@link java.lang.String} object.
     * @param monthFormat a {@link java.lang.String} object.
     * @param catIndex a int.
     * @param sectionIndex a int.
     */
    public AvailCalculations(final List<Node> nodes, final long endTime, final long lastMonthEndTime, final List<String> monitoredServices, final Report report, final Map<Double, List<String>> offenders, final double warning, final double normal, final String comments, final String name, final String format, final String monthFormat, final int catIndex, final int sectionIndex) {
        Logging.withPrefix(LOG4J_CATEGORY, new Runnable() {
            @Override public void run() {
                m_sectionIndex = sectionIndex;
                org.opennms.reporting.availability.Category category = new org.opennms.reporting.availability.Category();
                category.setWarning(warning);
                category.setNormal(normal);
                category.setCatComments(comments);
                category.setCatName(name);
                category.setCatIndex(catIndex);
                category.setNodeCount(nodes.size());
                int ipaddrCount = 0;
                int serviceCount = 0;

                for(Node tmpNode : nodes) {
                    if (tmpNode != null) {
                        ipaddrCount += tmpNode.getInterfaceCount();
                        serviceCount += tmpNode.getServiceCount();
                    }
                }
                category.setIpaddrCount(ipaddrCount);
                category.setServiceCount(serviceCount);

                org.opennms.reporting.availability.Categories categories = report.getCategories();

                LOG.debug("Inside AvailCalculations using endTime {}", endTime);

                m_monitoredServices = monitoredServices;
                m_endLastMonthTime = lastMonthEndTime;
                m_daysInLastMonth = getDaysForMonth(m_endLastMonthTime);
                m_report = report;

                m_nodes = nodes;

                String label;
                String descr;


                // Please node the following 4 formats are displayed on the graphical
                // report.
                // (i) last12MoAvail
                // (ii) LastMonthsDailyAvailability
                // (iii) MonthToDateDailyAvailability
                // (iv) lastMoTop20offenders


                LOG.debug("Now computing last 12 months daily availability ");
                //
                // N Months Availability
                //
                label = AvailabilityConstants.NMONTH_TOTAL_LABEL;
                descr = AvailabilityConstants.NMONTH_TOTAL_DESCR;
                if (label == null || label.length() == 0)
                    label = "The last 12 Months Availability";
                if (descr == null || descr.length() == 0)
                    descr = "The last 12 Months Availability";
                CatSections catSections = new CatSections();
                lastNMonthsAvailability(NMONTHS, m_endLastMonthTime, catSections, label, descr);

                LOG.debug("Computed lastNMonthsAvailability");

                //
                // Last Months Daily Availability
                //

                LOG.debug("Now computing last months daily availability ");
                label = AvailabilityConstants.LAST_MONTH_DAILY_LABEL;
                descr = AvailabilityConstants.LAST_MONTH_DAILY_DESCR;
                if (label == null || label.length() == 0)
                    label = "The last Months Daily Availability";
                if (descr == null || descr.length() == 0)
                    descr = "Daily Average of svcs monitored and availability of svcs divided by the total svc minutes (last month)";
                if (monthFormat.equalsIgnoreCase("calendar")){
                    lastCalMoDailyAvailability(m_daysInLastMonth, m_endLastMonthTime, catSections, label, descr, "LastMonthsDailyAvailability");
                }else {
                    lastMoDailyAvailability(m_daysInLastMonth, m_endLastMonthTime, catSections, label, descr, "LastMonthsDailyAvailability");
                }

                LOG.debug("Computed lastNDaysDailyAvailability");

                //
                // Month To Date Daily Availability
                //

                LOG.debug("Now computing  month to date daily availability ");
                label = AvailabilityConstants.LAST_MTD_DAILY_LABEL;
                descr = AvailabilityConstants.LAST_MTD_DAILY_DESCR;
                if (label == null || label.length() == 0)
                    label = "Month To Date Daily Availability";
                if (descr == null || descr.length() == 0)
                    descr = "Daily Average of svc monitored and availability of svcs div by total svc minutes of month frm 1st till date";
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(new Date(endTime));
                int numDaysInMonth = calendar.get(Calendar.DAY_OF_MONTH);
                if (monthFormat.equalsIgnoreCase("calendar")){
                    lastCalMTDDailyAvailability(numDaysInMonth, endTime, catSections, label, descr, "MonthToDateDailyAvailability");
                }else {
                    lastMTDDailyAvailability(numDaysInMonth, endTime, catSections, label, descr, "MonthToDateDailyAvailability");
                }


                LOG.debug("Computed lastNDaysDailyAvailability");

                //
                // Last Months Top Offenders
                //

                LOG.debug("Now computing Last Months Top Offenders ");
                label = AvailabilityConstants.NOFFENDERS_LABEL;
                descr = AvailabilityConstants.NOFFENDERS_DESCR;
                if (label == null || label.length() == 0)
                    label = "Last Months Top Offenders";
                if (descr == null || descr.length() == 0)
                    descr = "This is the list of the worst available devices in the category for the last month";
                lastMoTopNOffenders(offenders, catSections, label, descr);

                LOG.debug("Computed lastMoTopNOffenders ");

                //
                // Last N days Daily Availability
                //
                if (!format.equals("SVG")) {

                    LOG.debug("Now computing LAST_30_DAYS_DAILY_LABEL ");
                    label = AvailabilityConstants.LAST_30_DAYS_DAILY_LABEL;
                    descr = AvailabilityConstants.LAST_30_DAYS_DAILY_DESCR;
                    if (label == null || label.length() == 0)
                        label = "The last 30 Days Daily Availability";
                    if (descr == null || descr.length() == 0)
                        descr = "Daily average of svcs and dvcs monitored and their availability divided by total mins for 30days";
                    lastNDaysDailyAvailability(THIRTY, endTime, catSections, label, descr, "Last30DaysDailyAvailability");

                    LOG.debug("Computed lastNDaysDailyAvailability");
                }

                //
                // N days total availability
                //
                if (!format.equals("SVG")) {

                    LOG.debug("Now computing LAST_30_DAYS_TOTAL_LABEL ");
                    label = AvailabilityConstants.LAST_30_DAYS_TOTAL_LABEL;
                    descr = AvailabilityConstants.LAST_30_DAYS_TOTAL_DESCR;
                    if (label == null || label.length() == 0)
                        label = "The last 30 Days Total Availability";
                    if (descr == null || descr.length() == 0)
                        descr = "Average of svcs monitored and availability of svcs divided by total svc minutes of the last 30 days";
                    lastNDaysTotalAvailability(THIRTY, endTime, catSections, label, descr);

                    LOG.debug("Computed lastNDaysTotalAvailability");
                }

                //
                // Last Months Total Availability
                //
                if (!format.equals("SVG")) {

                    LOG.debug("Now computing LAST_MONTH_TOTAL_LABEL ");
                    label = AvailabilityConstants.LAST_MONTH_TOTAL_LABEL;
                    descr = AvailabilityConstants.LAST_MONTH_TOTAL_DESCR;
                    if (label == null || label.length() == 0)
                        label = "The last Months Total Availability";
                    if (descr == null || descr.length() == 0)
                        descr = "Average of svcs monitored and availability of svcs divided by the total svc minutes of the month";
                    lastMoTotalAvailability(m_daysInLastMonth, m_endLastMonthTime, catSections, label, descr);

                    LOG.debug("Computed lastNDaysDailyAvailability");
                }

                //
                // Month To Date Total Availability
                //
                if (!format.equals("SVG")) {

                    LOG.debug("Now computing LAST_MTD_TOTAL_LABEL ");
                    label = AvailabilityConstants.LAST_MTD_TOTAL_LABEL;
                    descr = AvailabilityConstants.LAST_MTD_TOTAL_DESCR;
                    if (label == null || label.length() == 0)
                        label = "Month To Date Total Availability";
                    if (descr == null || descr.length() == 0)
                        descr = "Average of svc monitored and availability of svcs dividedby total svc minutes of month frm 1st till date";
                    lastMoTotalAvailability(numDaysInMonth, endTime, catSections, label, descr);

                    LOG.debug("Computed MTDTotalAvailability");
                }

                m_services = new HashMap<String, Map<IfService, OutageSvcTimesList>>();

                for(Node node : nodes) {
                    if (node != null) {
                        for(Interface intf : node.getInterfaces()) {
                            if (intf != null) {
                                for(Service svc : intf.getServices()) {
                                    if (svc != null) {
                                        OutageSvcTimesList outages = svc.getOutages();
                                        if (outages != null) {
                                            IfService ifservice = new IfService(node.getNodeID(), intf.getName(), -1, node.getName(), svc.getName());
                                            Map<IfService, OutageSvcTimesList> svcOutages = m_services.get(svc.getName());
                                            if (svcOutages == null)
                                                svcOutages = new HashMap<IfService, OutageSvcTimesList>();
                                            svcOutages.put(ifservice, outages);
                                            m_services.put(svc.getName(), svcOutages);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                LOG.debug("Services {}", m_services);
                m_nodes = null;
                //
                // N Days Daily Service Availability
                //
                if (!format.equals("SVG")) {

                    LOG.debug("Now computing LAST_30_DAYS_SVC_AVAIL_LABEL ");
                    label = AvailabilityConstants.LAST_30_DAYS_SVC_AVAIL_LABEL;
                    descr = AvailabilityConstants.LAST_30_DAYS_SVC_AVAIL_DESCR;
                    if (label == null || label.length() == 0)
                        label = "The last 30 days Daily Service Availability";
                    if (descr == null || descr.length() == 0)
                        descr = "The last 30 days Daily Service Availability is the daily average of services";
                    lastNDaysDailyServiceAvailability(THIRTY, endTime, catSections, label, descr);

                    LOG.debug("Computed lastNDaysDailyServiceAvailability");
                }

                //
                // Last Months Daily Service Availability
                //
                if (!format.equals("SVG")) {

                    LOG.debug("Now computing LAST_MONTH_SVC_AVAIL_LABE");
                    label = AvailabilityConstants.LAST_MONTH_SVC_AVAIL_LABEL;
                    descr = AvailabilityConstants.LAST_MONTH_SVC_AVAIL_DESCR;
                    if (label == null || label.length() == 0)
                        label = "The last Months Daily Service Availability";
                    if (descr == null || descr.length() == 0)
                        descr = "The last Months Daily Service Availability is the daily average of services and devices";
                    lastNDaysDailyServiceAvailability(m_daysInLastMonth, m_endLastMonthTime, catSections, label, descr);

                    LOG.debug("Computed lastNDaysDailyServiceAvailability");
                }

                //
                // Top N Service Outages
                //
                if (!format.equals("SVG")) {

                    LOG.debug("Now computing TOP20_SVC_OUTAGES_LABEL");
                    label = AvailabilityConstants.TOP20_SVC_OUTAGES_LABEL;
                    descr = AvailabilityConstants.TOP20_SVC_OUTAGES_DESCR;
                    if (label == null || label.length() == 0)
                        label = "Last Month Top Service Outages for ";
                    if (descr == null || descr.length() == 0)
                        descr = "Last Month Top Service Outages for ";
                    lastMonTopNServiceOutages(catSections, label, descr);

                    LOG.debug("Computed lastMonTopNServiceOutages");
                }

                m_services = null;
                category.addCatSections(catSections);
                categories.addCategory(category);

                m_report.setCategories(categories);

                LOG.debug("Leaving AvailCalculations");
            }
        });
    }

    /**
     * <p>getSectionIndex</p>
     *
     * @return a int.
     */
    public int getSectionIndex() {
        return m_sectionIndex;
    }

    /**
     * Last Months Top N Service Outages.
     * 
     * @param catSections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     */
    private void lastMonTopNServiceOutages(CatSections catSections, String label, String descr) {
        // Result is a map of outage / lost time
        //

        // For each monitored service, get all individual outages.
        //
        TreeMap<Long, List<OutageSince>> treeMap = null;

        for(Entry<String, Map<IfService, OutageSvcTimesList>> serviceEntry : m_services.entrySet()) {
            treeMap = new TreeMap<Long, List<OutageSince>>();
            Map<IfService, OutageSvcTimesList> ifSvcOutageList = serviceEntry.getValue();

            for(Entry<IfService, OutageSvcTimesList> ifserviceEntry : ifSvcOutageList.entrySet()) {
                if (ifserviceEntry.getKey() != null) {
                    OutageSvcTimesList outageSvcList = ifserviceEntry.getValue();
                    if (outageSvcList != null) {
                        long rollingWindow = m_daysInLastMonth * ROLLING_WINDOW;
                        List<OutageSince> svcOutages = outageSvcList.getServiceOutages(ifserviceEntry.getKey().getNodeName(), m_endLastMonthTime, rollingWindow);
                        for(OutageSince outageSince : svcOutages) {
                            if (outageSince != null) {
                                long outage = outageSince.getOutage() / 1000;
                                List<OutageSince> tmpList = treeMap.get(new Long(outage));
                                if (tmpList == null)
                                    tmpList = new ArrayList<OutageSince>();
                                tmpList.add(outageSince);
                                treeMap.put(new Long(-1 * outage), tmpList);
                            }
                        }
                    }
                }
            }
            LOG.debug("Top 20 service outages from the list {}", treeMap);


            int top20Count = 0;
            Rows rows = new Rows();

            final SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

            loop : for(List<OutageSince> list : treeMap.values()) {
                for(OutageSince outageSince : list) {
                    top20Count++;
                    String nodeName = outageSince.getNodeName();

                    Value nodeValue = new Value();
                    nodeValue.setContent(nodeName);
                    nodeValue.setType("title");

                    Value value = new Value();
                    long outtime = outageSince.getOutage() / 1000;
                    int hrs = (new Long(outtime / (60 * 60))).intValue();
                    int remain = (new Long(outtime % (60 * 60))).intValue();
                    int mins = remain / (60);
                    remain = remain % (60);
                    int secs = remain;
                    LOG.debug("Outage : {} in mins {} hrs {} mins {} secs ", outtime, hrs, mins, secs);
                    value.setContent(hrs + " hrs " + mins + " mins " + secs + " secs ");
                    value.setType("data");

                    Value datevalue = new Value();
                    datevalue.setContent(fmt.format(new Date(outageSince.getOutTime())));
                    datevalue.setType("other");

                    Row row = new Row();
                    row.addValue(nodeValue);
                    row.addValue(value);
                    row.addValue(datevalue);
                    rows.addRow(row);

                    if (top20Count >= 20) {
                        break loop;
                    }
                }
            }
            Col col = new Col();
            col.addColTitle(0, "Node Name");
            col.addColTitle(1, "Duration Of Outage");
            col.addColTitle(2, "Service Lost Time");
            ClassicTable table = new ClassicTable();
            table.setCol(col);
            table.setRows(rows);
            Section section = new Section();
            section.setClassicTable(table);
            section.setSectionName(label + " " + serviceEntry.getKey());
            section.setSectionTitle(label + " " + serviceEntry.getKey());
            section.setSectionDescr(descr + " " + serviceEntry.getKey());
            section.setSectionIndex(m_sectionIndex);
            m_sectionIndex++;
            catSections.addSection(section);
        }
    }

    /**
     * Last Month To Date Daily Availability
     * 
     * @param days
     *            Number of days for which the availability computations are
     *            made.
     * @param endTime
     *            End time
     * @param sections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     * @param sectionName
     *            Section name.
     */
    private void lastMTDDailyAvailability(int days, long endTime, CatSections sections, String label, String descr, String sectionName) {
        lastNDaysDailyAvailability(days, endTime, sections, label, descr, sectionName);
    }

    /**
     * Last Month To Date Daily Availability
     * 
     * @param days
     *            Number of days for which the availability computations are
     *            made.
     * @param endTime
     *            End time
     * @param sections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     * @param sectionName
     *            Section name.
     */
    private void lastCalMTDDailyAvailability(int days, long endTime, CatSections sections, String label, String descr, String sectionName) {
        lastNDaysCalDailyAvailability(days, endTime, sections, label, descr, sectionName);
    }
    /**
     * Last N Days Total Availability.
     * 
     * @param days
     *            Number of days for which the availability computations are
     *            made.
     * @param endTime
     *            End time
     * @param catSections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     */
    private void lastMoTotalAvailability(int days, long endTime, CatSections catSections, String label, String descr) {
        lastNDaysTotalAvailability(days, endTime, catSections, label, descr);
    }


    /**
     * 
     * Last Months Top N offenders.
     * 
     * @param offenders
     *            Top Offenders
     * @param catSections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     * 
     */
    private void lastMoTopNOffenders(Map<Double, List<String>> offenders, CatSections catSections, String label, String descr) {
        // copy this method from the outage data code.
        //
        LOG.debug("Offenders {}", offenders);
        LOG.debug("Inside lastMoTopNOffenders");
        Set<Entry<Double, List<String>>> percentEntries = offenders.entrySet();
        Iterator<Entry<Double, List<String>>> iter = percentEntries.iterator();

        Rows rows = new Rows();
        int top20Count = 0;
        loop: while (iter.hasNext()) {
            Entry<Double, List<String>> percentEntry = iter.next();
            Double percent = percentEntry.getKey();
            if (percent.doubleValue() < 100.0) {
                List<String> nodeNames = percentEntry.getValue();
                if (nodeNames != null) {
                    ListIterator<String> lstIter = nodeNames.listIterator();
                    while (lstIter.hasNext()) {
                        top20Count++;
                        String nodeName = lstIter.next();

                        Value dateValue = new Value();
                        dateValue.setContent(nodeName);
                        dateValue.setType("title");

                        Value value = new Value();
                        value.setContent(formatNumber("" + percent.doubleValue()));
                        value.setType("data");

                        Row row = new Row();
                        row.addValue(dateValue);
                        row.addValue(value);
                        rows.addRow(row);

                        if (top20Count >= 20) {
                            break loop;
                        }
                    }
                }
            }
        }
        Col col = new Col();
        col.addColTitle(0, "Node Name");
        col.addColTitle(1, "Percentage Availability");
        ClassicTable table = new ClassicTable();
        table.setCol(col);
        table.setRows(rows);
        Section section = new Section();
        section.setClassicTable(table);
        section.setSectionName("lastMoTop20offenders");
        section.setSectionTitle(label);
        section.setSectionDescr(descr);
        section.setSectionIndex(m_sectionIndex);
        m_sectionIndex++;
        catSections.addSection(section);

        LOG.debug("Leaving lastMoTopNOffenders");
    }

    /**
     * Last Months Daily availability
     * 
     * @param days
     *            Number of days for which the availability computations are
     *            made
     * @param endTime
     *            End time
     * @param sections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     * @param sectionName
     *            Section name
     */
    private void lastMoDailyAvailability(int days, long endTime, CatSections sections, String label, String descr, String sectionName) {
        lastNDaysDailyAvailability(days, endTime, sections, label, descr, sectionName);
    }
    /**
     * Last Months Daily availability
     * 
     * @param days
     *            Number of days for which the availability computations are
     *            made
     * @param endTime
     *            End time
     * @param sections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     * @param sectionName
     *            Section name
     */
    private void lastCalMoDailyAvailability(int days, long endTime, CatSections sections, String label, String descr, String sectionName) {
        lastNDaysCalDailyAvailability(days, endTime, sections, label, descr, sectionName);
    }

    /**
     * Last N days daily availability.
     * 
     * @param days
     *            Number of days for which the availability computations are
     *            made.
     * @param endTime
     *            End time
     * @param catSections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     * @param sectionName
     *            Section name
     */
    private void lastNDaysCalDailyAvailability(int days, long endTime, CatSections catSections, String label, String descr, String sectionName) {

        LOG.debug("Inside lastNDaysDailyAvailability");
        int numdays = 0;
        CalendarTableBuilder calBuilder = new CalendarTableBuilder(endTime);
        TreeMap<Date, Double> treeMap = new TreeMap<Date, Double>();
        final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM, yyyy");
        String periodEnd = fmt.format(new java.util.Date(endTime));
        String periodFrom = "";
        while (numdays++ < days) {

            LOG.debug("Computing for {}", new Date(endTime));
            int serviceCount = 0;
            long outage = 0;
            //
            // get the outage and service count.
            //
            for(Node node : m_nodes) {
                outage += node.getOutage(endTime, ROLLING_WINDOW);
                serviceCount += node.getServiceCount();
            }
            double percentAvail;
            if (serviceCount > 0){

                LOG.debug("LOOK: calculating percentAvail using outage {} service count {} ROLLING_WINODW {} endTime {}", endTime, outage, serviceCount, ROLLING_WINDOW);
                percentAvail = 100.0 * (1 - (outage * 1.0) / (1.0 * serviceCount * ROLLING_WINDOW));
            }
            else
                percentAvail = 100.0;

            //need a double object in here

            treeMap.put(new Date(endTime), new Double (percentAvail));
            Date nicedate = new Date(endTime);
            LOG.debug("Inserting {} into {}", nicedate, percentAvail);

            periodFrom = fmt.format(new java.util.Date(endTime));
            endTime -= ROLLING_WINDOW;
        }

        int dateSlot = 0;
        for (final Map.Entry<Date,Double> entry : treeMap.entrySet()) {
            final Double percent = entry.getValue();
            dateSlot++;
            LOG.debug("Inserting value {} into date slot {}", dateSlot, percent);
            calBuilder.setPctValue(dateSlot, percent);
        }

        Section section = new Section();
        section.setCalendarTable(calBuilder.getTable());
        section.setSectionName(sectionName); // "LastMonthsDailyAvailability");
        section.setSectionTitle(label);
        section.setSectionDescr(descr);
        section.setPeriod(periodFrom + " to " + periodEnd);
        section.setSectionIndex(m_sectionIndex);
        m_sectionIndex++;
        catSections.addSection(section);
        LOG.debug("Leaving lastNDaysCalDailyAvailability");

    }


    /**
     * Last N days daily availability.
     * 
     * @param days
     *            Number of days for which the availability computations are
     *            made.
     * @param endTime
     *            End time
     * @param catSections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     * @param sectionName
     *            Section name
     */
    private void lastNDaysDailyAvailability(int days, long endTime, CatSections catSections, String label, String descr, String sectionName) {

        LOG.debug("Inside lastNDaysDailyAvailability");
        int numdays = 0;
        Rows rows = new Rows();
        TreeMap<Date, String> treeMap = new TreeMap<Date, String>();
        final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM, yyyy");
        String periodEnd = fmt.format(new java.util.Date(endTime));
        String periodFrom = "";
        while (numdays++ < days) {

            LOG.debug("Computing for {}", new Date(endTime));
            int serviceCount = 0;
            long outage = 0;
            //
            // get the outage and service count.
            //
            for(Node node : m_nodes) {
                outage += node.getOutage(endTime, ROLLING_WINDOW);
                serviceCount += node.getServiceCount();
            }
            double percentAvail;
            if (serviceCount > 0)
                percentAvail = 100.0 * (1 - (outage * 1.0) / (1.0 * serviceCount * ROLLING_WINDOW));
            else
                percentAvail = 100.0;

            treeMap.put(new Date(endTime), formatNumber(percentAvail + ""));

            periodFrom = fmt.format(new java.util.Date(endTime));
            endTime -= ROLLING_WINDOW;
        }

        final SimpleDateFormat fmtmp = new SimpleDateFormat("dd");
        for (final Map.Entry<Date,String> entry : treeMap.entrySet()) {
            final Date key = entry.getKey();
            final String percent = entry.getValue();

            final Value dateValue = new Value();
            dateValue.setContent(fmtmp.format(key));
            dateValue.setType("title");

            final Value value = new Value();
            value.setContent(percent);
            value.setType("data");

            final Row row = new Row();
            row.addValue(dateValue);
            row.addValue(value);
            rows.addRow(row);
        }

        Col col = new Col();
        col.addColTitle(0, "Date");
        col.addColTitle(1, "Percentage Availability");
        ClassicTable table = new ClassicTable();
        table.setCol(col);
        table.setRows(rows);
        Section section = new Section();
        section.setClassicTable(table);
        section.setSectionName(sectionName); // "LastMonthsDailyAvailability");
        section.setSectionTitle(label);
        section.setSectionDescr(descr);
        section.setPeriod(periodFrom + " to " + periodEnd);
        section.setSectionIndex(m_sectionIndex);
        m_sectionIndex++;
        catSections.addSection(section);
        LOG.debug("Leaving lastNDaysDailyAvailability");
    }

    /**
     * Last N Days Total Availability.
     * 
     * @param days
     *            Number of days for which the availability computations are
     *            made.
     * @param endTime
     *            End time
     * @param catSections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     */
    private void lastNDaysTotalAvailability(int days, long endTime, CatSections catSections, String label, String descr) {
        LOG.debug("Inside lastNDaysTotalAvailability");
        Rows rows = new Rows();
        int serviceCount = 0;
        long outage = 0;
        int numdays = 0;
        final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM, yyyy");
        String periodEnd = fmt.format(new java.util.Date(endTime));
        String periodFrom = "";
        while (numdays++ < days) {
            //
            // get the outage and service count.
            //
            for(Node node : m_nodes) {
                serviceCount += node.getServiceCount();
                outage += node.getOutage(endTime, ROLLING_WINDOW);
            }
            periodFrom = fmt.format(new java.util.Date(endTime)) + " to " + periodEnd;
            endTime -= ROLLING_WINDOW;
        }
        double percentAvail;
        if (serviceCount > 0)
            percentAvail = 100.0 * (1 - (outage * 1.0) / (1.0 * serviceCount * ROLLING_WINDOW));
        else
            percentAvail = 100.0;
        Value dateValue = new Value();
        dateValue.setContent(periodFrom);
        dateValue.setType("title");

        Value value = new Value();
        value.setContent(formatNumber(percentAvail + ""));
        value.setType("data");

        Row row = new Row();
        row.addValue(dateValue);
        row.addValue(value);
        rows.addRow(row);
        Col col = new Col();
        col.addColTitle(0, "Date");
        col.addColTitle(1, "Percentage Availability");
        ClassicTable table = new ClassicTable();
        table.setCol(col);
        table.setRows(rows);
        Section section = new Section();
        section.setClassicTable(table);
        section.setSectionName("Last" + days + "TotalAvailability");
        section.setSectionTitle(label);
        section.setSectionDescr(descr);
        section.setPeriod(periodFrom);
        section.setSectionIndex(m_sectionIndex);
        m_sectionIndex++;
        catSections.addSection(section);
        LOG.debug("Leaving lastNDaysTotalAvailability");
    }

    /**
     * Last N Months Availability
     * 
     * @param nMonths
     *            Number of months for which the availability computations are
     *            made.
     * @param endTime
     *            End time
     * @param catSections
     *            sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     */
    private void lastNMonthsAvailability(int nMonths, long endTime, CatSections catSections, String label, String descr) {
        LOG.debug("Inside lastNMonthsAvailability");
        Rows rows = new Rows();
        int numMonths = 0;

        int numDays = getDaysForMonth(endTime);
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(endTime));
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.set(year, month, numDays, 23, 59, 59);
        endTime = calendar.getTime().getTime();
        final SimpleDateFormat fmt = new SimpleDateFormat("MMM, yyyy");
        String periodEnd = fmt.format(new java.util.Date(endTime));
        TreeMap<Date, String> treeMap = new TreeMap<Date, String>(); // Holds all the month/percent
        // values to be displayed in order
        // on pdf.
        String periodFrom = null;

        while (numMonths++ < nMonths) {
            int serviceCount = 0;
            long outage = 0;
            LOG.debug("Number of days {} in month of {}", numDays, new Date(endTime));
            long rollingWindow = numDays * ROLLING_WINDOW * 1L;
            //
            // get the outage and service count.
            //
            for(Node node : m_nodes) {
                serviceCount += node.getServiceCount();
                outage += node.getOutage(endTime, rollingWindow);
            }
            double percentAvail;
            if (serviceCount > 0)
                percentAvail = 100.0 * (1 - (outage * 1.0) / (1.0 * serviceCount * rollingWindow));
            else
                percentAvail = 100.0;

            treeMap.put(new java.util.Date(endTime), formatNumber(percentAvail + ""));

            periodFrom = fmt.format(new java.util.Date(endTime));
            calendar = new GregorianCalendar();
            calendar.setTime(new Date(endTime));
            month = calendar.get(Calendar.MONTH);
            year = calendar.get(Calendar.YEAR);

            calendar.set(year, month - 1, 1, 0, 0, 0);
            endTime = calendar.getTime().getTime();
            month = calendar.get(Calendar.MONTH);
            year = calendar.get(Calendar.YEAR);

            numDays = getDaysForMonth(endTime);
            calendar.set(year, month, numDays, 23, 59, 59);
            endTime = calendar.getTime().getTime();
        }

        final SimpleDateFormat fmtmp = new SimpleDateFormat("MMM");
        for (final Map.Entry<Date,String> entry : treeMap.entrySet()) {
            final Date key = entry.getKey();
            final String percent = entry.getValue();

            final Value dateValue = new Value();
            dateValue.setContent(fmtmp.format(key) + "");
            dateValue.setType("title");

            final Value value = new Value();
            value.setContent(percent);
            value.setType("data");

            final Row row = new Row();
            row.addValue(dateValue);
            row.addValue(value);
            rows.addRow(row);
        }

        Col col = new Col();
        col.addColTitle(0, "Date");
        col.addColTitle(1, "Percentage Availability");
        ClassicTable table = new ClassicTable();
        table.setCol(col);
        table.setRows(rows);
        Section section = new Section();
        section.setClassicTable(table);
        section.setSectionName("last12MoAvail");
        section.setSectionTitle(label);
        section.setSectionDescr(descr);
        section.setPeriod(periodFrom + " to " + periodEnd);
        section.setSectionIndex(m_sectionIndex);
        m_sectionIndex++;
        catSections.addSection(section);
        LOG.debug("Leaving lastNMonthsAvailability");
    }

    /**
     * Returns the number of days in the month, also considers checks for leap
     * year.
     * 
     * @param isLeap
     *            the leap year flag.
     * @param month
     *            The month whose days count is reqd
     */
    private static synchronized int getDays(boolean isLeap, int month) {
        switch (month) {
        case 0:
        case 2:
        case 4:
        case 6:
        case 7:
        case 9:
        case 11:
            return 31;

        case 3:
        case 5:
        case 8:
        case 10:
            return 30;

        case 1:
            if (isLeap)
                return 29;
            else
                return 28;
        }
        return -1;
    }

    /**
     * Returns the number of Days in the month
     * 
     * @param endTime
     *            The end of the month (time in milliseconds)
     */
    private int getDaysForMonth(long endTime) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new java.util.Date(endTime));
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        return (getDays(calendar.isLeapYear(year), month));
    }

    /**
     * Compute N days daily service availability.
     * 
     * @param endTime
     *            End time
     * @param catSections
     *            Sections
     * @param label
     *            Section name in the xml
     * @param descr
     *            Section descr.
     */
    private void lastNDaysDailyServiceAvailability(int days, long endTime, CatSections catSections, String label, String descr) {
        LOG.debug("Inside lastNDaysDailyServiceAvailability {}", days);

        final SimpleDateFormat fmtmp = new SimpleDateFormat("dd MMM, yyyy");

        long outage;
        String periodFrom = "";
        String periodTo = "";
        periodTo = fmtmp.format(new java.util.Date(endTime));
        for(final String service : m_monitoredServices) {
            final TreeMap<Date, Double> treeMap = new TreeMap<Date, Double>();
            final Rows rows = new Rows();
            LOG.debug("SERvice {}", service);

            long curTime = endTime;
            Map<IfService, OutageSvcTimesList> svcOutages = null;
            if (m_services != null) svcOutages = m_services.get(service);
            if (svcOutages == null || svcOutages.size() <= 0) {
                int daysCnt = 0;
                while (daysCnt++ < days) {
                    LOG.debug("DAy 100 % : {}", daysCnt);
                    periodFrom = fmtmp.format(new java.util.Date(curTime));

                    treeMap.put(new java.util.Date(curTime), new Double(100.0));

                    curTime -= ROLLING_WINDOW;
                }

                final SimpleDateFormat fmt = new SimpleDateFormat("dd");

                for (final Date tmp : treeMap.keySet()) {
                    final Value dateValue = new Value();
                    dateValue.setContent(fmt.format(tmp) + "");
                    dateValue.setType("title");

                    final Value value = new Value();
                    value.setContent("100.0");
                    value.setType("data");

                    final Row row = new Row();
                    row.addValue(dateValue);
                    row.addValue(value);
                    rows.addRow(row);
                }

                final Col col = new Col();
                col.addColTitle(0, "Date");
                col.addColTitle(1, "Percentage Availability");

                final ClassicTable table = new ClassicTable();
                table.setCol(col);
                table.setRows(rows);

                final Section section = new Section();
                section.setClassicTable(table);
                section.setPeriod(periodFrom + " to " + periodTo);
                section.setSectionName(label + " " + service);
                section.setSectionTitle(label + " " + service);
                section.setSectionDescr(descr + " " + service);
                section.setSectionIndex(m_sectionIndex);
                m_sectionIndex++;
                catSections.addSection(section);
            } else {
                int daysCnt = 0;
                while (daysCnt++ < days) {
                    LOG.debug("DAy : {} end time {} ROLLING_WINDOW {}", daysCnt, new Date(curTime), ROLLING_WINDOW);
                    int serviceCnt = 0;
                    long outageTime = 0;
                    // For each node in the service table.
                    //
                    // Iterate each svc node for getting the ifservice
                    for (final Map.Entry<IfService, OutageSvcTimesList> entry : svcOutages.entrySet()) {
                        final IfService ifservice = entry.getKey();
                        final OutageSvcTimesList outageList = entry.getValue();

                        LOG.debug(ifservice.toString());

                        if (outageList != null) {
                            outage = outageList.getDownTime(curTime, ROLLING_WINDOW);
                            // Keep track of the number of services being
                            // monitored.
                            //
                            outageTime += outage;
                        }
                        serviceCnt++;
                    }
                    LOG.debug("Outage Time {}", outageTime);
                    long den = (ROLLING_WINDOW * serviceCnt);
                    double outag = 1.0 * outageTime;
                    double denom = 1.0 * den;
                    double cal = 0;
                    if (den > 0)
                        cal = 100.0 * (1 - (outag / denom));

                    treeMap.put(new java.util.Date(curTime), new Double(cal));

                    periodFrom = fmtmp.format(new java.util.Date(curTime));
                    LOG.debug("Added to svc list {}", new java.util.Date(curTime));
                    curTime -= ROLLING_WINDOW;
                }

                final SimpleDateFormat fmt = new SimpleDateFormat("dd");

                for (final Map.Entry<Date,Double> entry : treeMap.entrySet()) {
                    final Date tmp = entry.getKey();
                    final Double val = entry.getValue();

                    final Value dateValue = new Value();
                    dateValue.setContent(fmt.format(tmp) + "");
                    dateValue.setType("title");

                    final Value value = new Value();
                    value.setContent("" + val);
                    value.setType("data");

                    final Row row = new Row();
                    row.addValue(dateValue);
                    row.addValue(value);
                    rows.addRow(row);
                }

                Col col = new Col();
                col.addColTitle(0, "Date");
                col.addColTitle(1, "Percentage Availability");
                ClassicTable table = new ClassicTable();
                table.setCol(col);
                table.setRows(rows);
                Section section = new Section();
                section.setClassicTable(table);
                section.setPeriod(periodFrom + " to " + periodTo);
                section.setSectionName(label + " " + service);
                section.setSectionTitle(label + " " + service);
                section.setSectionDescr(descr + " " + service);
                section.setSectionIndex(m_sectionIndex);
                m_sectionIndex++;
                catSections.addSection(section);
            }
        }
        LOG.debug("Leaving lastNDaysDailyServiceAvailability");
    }

    /**
     * Format the number (String) and return 6 digits of the number
     */
    private String formatNumber(String num) {
        if (num.indexOf('.') == 0) {
            num = "0" + num;
        }
        if (num.indexOf('.') == -1) {
            num = num + ".0";
        }
        num = num + "000000";
        return (num.substring(0, num.indexOf('.') + 6));
    }

}
