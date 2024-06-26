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
package org.opennms.netmgt.reporting.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRCsvExporter;

import org.opennms.core.db.DataSourceFactory;
import org.opennms.netmgt.config.reportd.Parameter;
import org.opennms.netmgt.config.reportd.Report;
import org.opennms.netmgt.dao.api.ReportCatalogDao;
import org.opennms.netmgt.model.ReportCatalogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * <p>DefaultReportService class.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public class DefaultReportService implements ReportService,InitializingBean {
	
	
	private static final Logger LOG = LoggerFactory
			.getLogger(DefaultReportService.class);
    
    private enum Format { pdf,html,xml,xls,csv };

    private ReportCatalogDao m_reportCatalogDao;

    /** {@inheritDoc} 
     * @throws ReportRunException */
    @Override
    public synchronized String runReport(Report report,String reportDirectory) throws ReportRunException {

        String outputFile = null;
        try {
            outputFile = generateReportName(reportDirectory,report.getReportName(), report.getReportFormat());
            JasperPrint print = runAndRender(report);
            outputFile = saveReport(print,report,outputFile);    
            
        } catch (JRException e) {
            LOG.error("Error running report: {}", e.getMessage(), e);
            throw new ReportRunException("Caught JRException: " + e.getMessage());
        }  catch (Throwable e){
            LOG.error("Unexpected exception: {}", e.getMessage(), e);
            throw new ReportRunException("Caught unexpected " + e.getClass().getName() + ": " + e.getMessage());
        }        
 
        return outputFile;
    
    }
 
    /**
     * <p>getReportCatalogDao</p>
     *
     * @return a {@link org.opennms.netmgt.dao.api.ReportCatalogDao} object.
     */
    public ReportCatalogDao getReportCatalogDao() {
        return m_reportCatalogDao;
    }

    /**
     * <p>setReportCatalogDao</p>
     *
     * @param reportCatalogDao a {@link org.opennms.netmgt.dao.api.ReportCatalogDao} object.
     */
    public void setReportCatalogDao(ReportCatalogDao reportCatalogDao) {
        this.m_reportCatalogDao = reportCatalogDao;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(m_reportCatalogDao,"No Report Catalog DAO set");
    }

    private void createReportCatalogEntry(JasperPrint jasperPrint, Report report, String fileName) throws ReportRunException {
        ReportCatalogEntry catalogEntry = new ReportCatalogEntry();
        catalogEntry.setDate(new Date());
        catalogEntry.setReportId("reportd_" + report.getReportTemplate()); // FIXME Is this correct ?
        catalogEntry.setTitle(report.getReportName());
        catalogEntry.setLocation(fileName);
        try {
            m_reportCatalogDao.save(catalogEntry);
        } catch (Exception e) {
            throw new ReportRunException("Can't save a report catalog entry, " + e.getMessage());
        }
    }

    private String generateReportName(String reportDirectory, String reportName, String reportFormat){
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyyMMddHHmmss");
        return  reportDirectory + reportName + sdf.format(new Date())  + "." + reportFormat;
    }

    
    private String saveReport(JasperPrint jasperPrint, Report report, String destFileName) throws JRException, Exception{
        createReportCatalogEntry(jasperPrint, report, destFileName);
        String reportName=null;
        switch(Format.valueOf(report.getReportFormat())){    
        case pdf:
            JasperExportManager.exportReportToPdfFile(jasperPrint, destFileName);
            reportName = destFileName;
            break;
        case html:
            JasperExportManager.exportReportToHtmlFile(jasperPrint,destFileName);
            reportName = createZip(destFileName);
            break;
        case xml:
            JasperExportManager.exportReportToXmlFile(jasperPrint,destFileName,true);
            reportName = createZip(destFileName);
            break;
        case csv:
            JRCsvExporter exporter = new JRCsvExporter();
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFileName);
            exporter.exportReport();
            reportName = destFileName;
            break;
        default:
            LOG.error("Error Running Report: Unknown Format: {}", report.getReportFormat());
        }    
        
        return reportName;
        
    }
        
    
    private JasperPrint runAndRender(Report report) throws Exception, JRException {
        JasperPrint jasperPrint = new JasperPrint();
        
        JasperReport jasperReport = JasperCompileManager.compileReport(
                                                                       System.getProperty("opennms.home") + 
                                                                       File.separator + "etc" +
                                                                       File.separator + "report-templates" + 
                                                                       File.separator + report.getReportTemplate() );
        
        if(report.getReportEngine().equals("jdbc")){
            Connection connection = DataSourceFactory.getInstance().getConnection();
            jasperPrint = JasperFillManager.fillReport(jasperReport,
                                                       paramListToMap(report.getParameters()),
                                                       connection );
            connection.close();
        }
 

        else if(report.getReportEngine().equals("opennms")){
            LOG.error("Sorry the OpenNMS Data source engine is not yet available");
            jasperPrint = null;
        }
        else{
            LOG.error("Unknown report engine: {} ", report.getReportEngine());
            jasperPrint = null;
        }
        
        return jasperPrint;
        
    }

    
    private String createZip(String baseFileName) {
        File reportResourceDirectory = new File(baseFileName + "_files");
        String zipFile = baseFileName + ".zip";
        
        if (reportResourceDirectory.exists() && reportResourceDirectory.isDirectory()){
            ZipOutputStream reportArchive;
        
            try {
                reportArchive = new ZipOutputStream(new FileOutputStream(zipFile));
                addFileToArchive(reportArchive,baseFileName);

                reportArchive.putNextEntry(new ZipEntry(baseFileName));
                for(String file : Arrays.asList(reportResourceDirectory.list()) ){
                    addFileToArchive(reportArchive, file);
                }
                reportArchive.close();
            }
            catch (final Exception e) {
                LOG.warn("unable to create {}", zipFile, e);
            }

        }

        return zipFile;
    }

    private void addFileToArchive(ZipOutputStream reportArchive, String file)
    throws FileNotFoundException, IOException {
        FileInputStream asf = new FileInputStream(file);
        reportArchive.putNextEntry(new ZipEntry(file));
        byte[] buffer = new byte[18024]; 
        int len;
        while ((len = asf.read(buffer)) > 0){
            reportArchive.write(buffer, 0, len);
        }

        asf.close();
        reportArchive.closeEntry();
    }
    
    
    private Map<String, Object> paramListToMap(List<Parameter> parameters){
        Map<String,Object> parmMap = new HashMap<>();
        for(Parameter parm : parameters) {
            parmMap.put(parm.getName(), parm.getValue());
        }
        return parmMap;
    }

}
