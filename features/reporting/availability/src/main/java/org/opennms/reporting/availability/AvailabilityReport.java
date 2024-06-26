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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.opennms.core.logging.Logging;
import org.opennms.core.utils.ConfigFileConstants;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.reporting.availability.render.HTMLReportRenderer;
import org.opennms.reporting.availability.render.PDFReportRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * AvailabilityReport generates the Availability report in PDF format
 *
 * @author <A HREF="mailto:jacinta@oculan.com">Jacinta Remedios </A>
 */
public class AvailabilityReport extends Object {
    private static final Logger LOG = LoggerFactory.getLogger(AvailabilityReport.class);
    /**
     * The log4j category used to log debug messsages and statements.
     */
    private static final String LOG4J_CATEGORY = "reports";

    /*
     * classic month format
     */

    private static final String MONTH_FORMAT_CLASSIC = "classic";

    /**
     * Object that holds all the information required for the
     * generating xml to be translated to the pdf.
     */
    private Report m_report = null;

    /**
     * String of Months
     */

    public static String[] months = new String[] { "January", "February",
        "March", "April", "May", "June", "July", "August", "September",
        "October", "November", "December" };

    /**
     * Default constructor
     *
     * @param author a {@link java.lang.String} object.
     * @param startMonth a {@link java.lang.String} object.
     * @param startDate a {@link java.lang.String} object.
     * @param startYear a {@link java.lang.String} object.
     */
    public AvailabilityReport(final String author, final String startMonth,
            final String startDate, final String startYear) {
        Logging.withPrefix(LOG4J_CATEGORY, new Runnable() {

            @Override
            public void run() {
                LOG.debug("Inside AvailabilityReport");

                Calendar today = new GregorianCalendar();
                int day = Integer.parseInt(startDate);
                int year = Integer.parseInt(startYear);
                // int month = Integer.parseInt(startMonth);
                // int day = today.get(Calendar.DAY_OF_MONTH);
                // int year = today.get(Calendar.YEAR);
                // SimpleDateFormat smpMonth = new SimpleDateFormat("MMMMMMMMMMM");
                // String month = smpMonth.format(new
                // java.util.Date(today.getTime().getTime()));
                // int month = today.get(Calendar.MONTH) + 1;
                String month = months[Integer.parseInt(startMonth)];
                int hour = today.get(Calendar.HOUR);
                int minute = today.get(Calendar.MINUTE);
                int second = today.get(Calendar.SECOND);
                Created created = new Created();
                created.setDay(day);
                created.setHour(hour);
                created.setMin(minute);
                created.setMonth(month);
                created.setSec(second);
                created.setYear(year);
                created.setContent(new BigDecimal(today.getTime().getTime()));

                m_report = new Report();
                m_report.setCreated(created);
                m_report.setAuthor(author);

                LOG.debug("Leaving AvailabilityReport");
            }

        });
    }

    /**
     * This when invoked generates the data into report classes.
     *
     * @param logourl
     *            location of the logo to be displayed on the report
     * @param categoryName
     *            of the logo to be displayed on the report
     * @param reportFormat
     *            Report Format ("SVG" / all)
     * @param monthFormat
     *            Format for month data ("classic"/"calendar")
     * @param startMonth a {@link java.lang.String} object.
     * @param startDate a {@link java.lang.String} object.
     * @param startYear a {@link java.lang.String} object.
     */
    public void getReportData(String logourl, String categoryName,
            String reportFormat, String monthFormat, String startMonth,
            String startDate, String startYear) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("inside getReportData");
            LOG.debug("Category name {}", categoryName);
            LOG.debug("Report format {}", reportFormat);
            LOG.debug("logo {}", logourl);
            LOG.debug("monthFormat {}", monthFormat);
        }
        populateReport(logourl, categoryName, reportFormat, monthFormat,
                       startMonth, startDate, startYear);
        try {
            marshalReport();
        } catch (Throwable e) {
            LOG.error("Exception", e);
        }
    }

    /**
     * This when invoked populates the classes.
     *
     * @param logourl
     *            location of the logo to be displayed on the report
     * @param categoryName
     *            of the logo to be displayed on the report
     * @param reportFormat
     *            Report Format ("SVG" / all)
     * @param monthFormat
     *            Format for month data ("classic"/"calendar")
     * @param startMonth a {@link java.lang.String} object.
     * @param startDate a {@link java.lang.String} object.
     * @param startYear a {@link java.lang.String} object.
     */
    public void populateReport(String logourl, String categoryName,
            String reportFormat, String monthFormat, String startMonth,
            String startDate, String startYear) {
        m_report.setLogo(logourl);
        ViewInfo viewInfo = new ViewInfo();
        m_report.setViewInfo(viewInfo);
        org.opennms.reporting.availability.Categories categories = new org.opennms.reporting.availability.Categories();
        m_report.setCategories(categories);
        try {
            AvailabilityData reportSource = new AvailabilityData();

            reportSource.fillReport(categoryName, m_report, reportFormat,
                                    monthFormat, startMonth,
                                    startDate, startYear);
        } catch (Throwable e) {
            LOG.error("Exception", e);
        }
    }

    /**
     * This when invoked marshals the report XML from the classes.
     *
     * @throws java.io.IOException if any.
     * @throws java.lang.Exception if any.
     */
    public void marshalReport() throws IOException, Exception {

        File file = new File(ConfigFileConstants.getHome()
                             + "/share/reports/AvailReport.xml");
        try {
            Writer fileWriter = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            JaxbUtils.marshal(m_report, fileWriter);
            LOG.debug("The xml marshalled from the JAXB classes is saved in {}/share/reports/AvailReport.xml", ConfigFileConstants.getHome());
            fileWriter.close();
        } catch (Throwable e) {
            LOG.error("Exception", e);
        }
    }

    /**
     * Generate PDF from classes.
     *
     * @param xsltFileName a {@link java.lang.String} object.
     * @param out a {@link java.io.OutputStream} object.
     * @param format a {@link java.lang.String} object.
     * @throws java.lang.Exception if any.
     */
    public void generatePDF(final String xsltFileName, final OutputStream out,
            final String format) throws Exception {
        Logging.withPrefix(LOG4J_CATEGORY, new Runnable() {

            @Override
            public void run() {
                LOG.debug("inside generatePDF");
                File file = new File(ConfigFileConstants.getHome()
                                     + "/share/reports/AvailReport.xml");
                try {
                    LOG.debug("The xml marshalled from the JAXB classes is saved in {}/share/reports/AvailReport.xml", ConfigFileConstants.getHome());
                    Reader fileReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                    if (!format.equals("HTML")) {
                        new PDFReportRenderer().render(fileReader, out, new InputStreamReader(new FileInputStream(xsltFileName), StandardCharsets.UTF_8));
                    } else {
                        new HTMLReportRenderer().render(fileReader, out, new InputStreamReader(new FileInputStream(xsltFileName), StandardCharsets.UTF_8));
                    }
                } catch (Throwable e) {
                    LOG.error("Exception", e);
                }
                LOG.info("leaving generatePDF");
            }

        });

    }

    /**
     * Main method
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {
        Logging.withPrefix(LOG4J_CATEGORY, new Runnable() {
            @Override public void run() {
                LOG.debug("main() called with args: {}", StringUtils.arrayToDelimitedString(args, ", "));

                System.setProperty("java.awt.headless", "true");

                String logourl = System.getProperty("image");
                String categoryName = System.getProperty("catName");
                if (categoryName == null || categoryName.equals("")) {
                    categoryName = "all";
                }
                String format = System.getProperty("format");
                if (format == null || format.equals("")) {
                    format = "SVG";
                }
                String monthFormat = System.getProperty("MonthFormat");
                if (monthFormat == null || format.equals("")) {
                    monthFormat = MONTH_FORMAT_CLASSIC;
                }
                String startMonth = System.getProperty("startMonth");
                String startDate = System.getProperty("startDate");
                String startYear = System.getProperty("startYear");

                if (startMonth == null || startDate == null || startYear == null) {
                    throw new NumberFormatException("missing date properties");
                }

                try {
                    generateReport(logourl, categoryName, format, monthFormat, startMonth, startDate, startYear);
                } catch (final Exception e) {
                    LOG.warn("Error while generating report.", e);
                }
            }
        });
    }


    /**
     * <p>generateReport</p>
     *
     * @param logourl a {@link java.lang.String} object.
     * @param categoryName a {@link java.lang.String} object.
     * @param format a {@link java.lang.String} object.
     * @param monthFormat a {@link java.lang.String} object.
     * @param startMonth a {@link java.lang.String} object.
     * @param startDate a {@link java.lang.String} object.
     * @param startYear a {@link java.lang.String} object.
     * @throws java.lang.Exception if any.
     */
    public static void generateReport(final String logourl, final String categoryName, final String format, final String monthFormat, final String startMonth, final String startDate, final String startYear) throws Exception {

        // This report will be invoked by the mailer script.
        // Only SVG formatted reports are needed.
        // 
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        String catFileName = categoryName.replace(' ', '-');

        String pdfFileName;
        String xslFileName;
        if (format.equals("SVG")) {
            pdfFileName = ConfigFileConstants.getHome()
                    + "/share/reports/AVAIL-SVG-" + catFileName
                    + fmt.format(new java.util.Date()) + ".pdf";
            xslFileName = ConfigFileConstants.getFilePathString()
                    + ConfigFileConstants.getFileName(ConfigFileConstants.REPORT_SVG_XSL);
        } else if (format.equals("PDF")) {
            pdfFileName = ConfigFileConstants.getHome()
                    + "/share/reports/AVAIL-PDF-" + catFileName
                    + fmt.format(new java.util.Date()) + ".pdf";
            xslFileName = ConfigFileConstants.getFilePathString()
                    + ConfigFileConstants.getFileName(ConfigFileConstants.REPORT_PDF_XSL);
        } else if (format.equals("HTML")) {
            pdfFileName = ConfigFileConstants.getHome()
                    + "/share/reports/AVAIL-HTML-" + catFileName
                    + fmt.format(new java.util.Date()) + ".html";
            xslFileName = ConfigFileConstants.getFilePathString()
                    + ConfigFileConstants.getFileName(ConfigFileConstants.REPORT_HTML_XSL);
        } else {
            LOG.error("Format '{}' is unsupported.  Must be one of: SVG, PDF, or HTML.", format);
            return;
        }

        try {
            AvailabilityReport report = new AvailabilityReport("Unknown",
                                                               startMonth,
                                                               startDate,
                                                               startYear);
            report.getReportData(logourl, categoryName, format, monthFormat,
                                 startMonth, startDate, startYear);
            LOG.info("Generated Report Data... ");
            File file = new File(pdfFileName);
            FileOutputStream pdfFileWriter = new FileOutputStream(file);
            report.generatePDF(xslFileName, pdfFileWriter, format);
            LOG.debug("xsl -> {} pdfFileName -> {} format -> {}", xslFileName, pdfFileName, format);
            LOG.info("Generated Report ... and saved as {}", pdfFileName);
        } catch (Throwable e) {
            LOG.error("Exception", e);
        }
    }
}
