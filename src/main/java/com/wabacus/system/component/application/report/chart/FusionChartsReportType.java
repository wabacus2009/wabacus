/* 
 * Copyright (C) 2010---2014 星星(wuweixing)<349446658@qq.com>
 * 
 * This file is part of Wabacus 
 * 
 * Wabacus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wabacus.system.component.application.report.chart;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Workbook;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsChartReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsChartReportBean;
import com.wabacus.system.component.application.report.chart.configbean.FunsionChartsReportBean;
import com.wabacus.system.component.application.report.chart.fusioncharts.AbsDatasetType;
import com.wabacus.system.component.application.report.chart.fusioncharts.HorizontalDatasetType;
import com.wabacus.system.component.application.report.chart.fusioncharts.VerticalDatasetType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.intercept.ReportDataBean;
import com.wabacus.system.tags.component.AbsComponentTag;
import com.wabacus.system.task.DeleteTempFileTask;
import com.wabacus.system.task.TimingThread;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class FusionChartsReportType extends AbsChartReportType
{
    public final static String KEY=FusionChartsReportType.class.getName();

    public static String chartXmlFileTempPath;
    
    private FunsionChartsReportBean fcrbean;
    
    private Map<String,String> mLinkedDataString;//本报表各列要通过link链接的所有<linkeddata/>数据，以linkid为key，以被链接的图表报表数据<chart/>为value
    
    public FusionChartsReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            fcrbean=(FunsionChartsReportBean)((ReportBean)comCfgBean).getExtendConfigDataForReportType(KEY);
        }
    }

    public FunsionChartsReportBean getFcrbean()
    {
        return fcrbean;
    }

    public void displayOnPage(AbsComponentTag displayTag)
    {
        if(!fcrbean.isLinkChart())
        {
            super.displayOnPage(displayTag);
        }
    }

    public String showTitle()
    {
        return getTitleDisplayValue(null,showButtonsOnTitleBar());
    }

    public void showReportData(StringBuilder resultBuf)
    {
        ReportDataBean reportDataObjFromInterceptor=null;
        if(rbean.getInterceptor()!=null)
        {
            reportDataObjFromInterceptor=new ReportDataBean(this,rbean.getDbean().getLstCols());
            rbean.getInterceptor().beforeDisplayReportData(rrequest,rbean,reportDataObjFromInterceptor);
        }
        if(reportDataObjFromInterceptor!=null&&reportDataObjFromInterceptor.getBeforeDisplayString()!=null)
        {
            resultBuf.append(reportDataObjFromInterceptor.getBeforeDisplayString());
        }
        if(reportDataObjFromInterceptor==null||reportDataObjFromInterceptor.isShouldDisplayReportData())
        {//拦截器没有阻止框架的自动显示数据部分
            resultBuf.append("<div id=\""+rbean.getGuid()+"_data\" "+rbean.getDatastyleproperty(rrequest,false)+"></div>");
        }
        if(reportDataObjFromInterceptor!=null&&reportDataObjFromInterceptor.getAfterDisplayString()!=null)
        {
            resultBuf.append(reportDataObjFromInterceptor.getAfterDisplayString());
        }
        if(reportDataObjFromInterceptor!=null&&!reportDataObjFromInterceptor.isShouldDisplayReportData()) return;
        String chartDataStr;
        if(reportDataObjFromInterceptor==null||reportDataObjFromInterceptor.getChartDataString()==null)
        {
            chartDataStr=loadStringChartData(false);
        }else
        {
            chartDataStr=reportDataObjFromInterceptor.getChartDataString();
        }
        if(reportDataObjFromInterceptor==null||!reportDataObjFromInterceptor.isStopAutoDisplayChart())
        {
            StringBuilder paramsBuf=new StringBuilder();
            paramsBuf.append("{pageid:\"").append(rbean.getPageBean().getId()).append("\"");
            paramsBuf.append(",reportid:\"").append(rbean.getId()).append("\"");
            paramsBuf.append(",swfileurl:\"").append(Config.webroot+"webresources/component/FusionCharts/swf/"+acrbean.getChartype()).append("\"");
            paramsBuf.append(",width:\"").append(fcrbean.getChartwidth()).append("\"");
            paramsBuf.append(",height:\"").append(fcrbean.getChartheight()).append("\"");
            paramsBuf.append(",debugMode:\"").append(fcrbean.isDebugmode()?"1":"0").append("\"");
            paramsBuf.append(",registerWithJS:\"").append(fcrbean.isRegisterwithjs()?"1":"0").append("\"");
            paramsBuf.append(",datatype:\"").append(acrbean.getDatatype()).append("\"");
            if(AbsChartReportBean.DATATYPE_XML.equals(acrbean.getDatatype()))
            {
                paramsBuf.append(",data:\"").append(Tools.jsParamEncode(chartDataStr)).append("\"");
            }else
            {
                String filename=Tools.getRandomString(6);
                if(rrequest.getRequest()!=null) filename+=getPureNumber(rrequest.getRequest().getRemoteAddr());
                filename+=System.currentTimeMillis()+".xml";
                String filepath=chartXmlFileTempPath+File.separator+filename;
                FilePathAssistant.getInstance().writeFileContentToDisk(filepath,chartDataStr,false);
                paramsBuf.append(",data:\"").append(filename).append("\"");
            }
            List<String[]> lstChartOnloadMethods=rrequest.getWResponse().getLstChartOnloadMethods(rbean.getId());
            if(lstChartOnloadMethods!=null&&lstChartOnloadMethods.size()>0)
            {//配置了显示图表后的回调函数
                paramsBuf.append(",chartOnloadMethods:[");
                for(String[] methodTmp:lstChartOnloadMethods)
                {
                    if(methodTmp==null||methodTmp.length!=2) continue;
                    paramsBuf.append("{method:").append(methodTmp[0]).append(",methodparams:"+Tools.jsParamEncode(methodTmp[1])+"},");
                }
                if(paramsBuf.charAt(paramsBuf.length()-1)==',') paramsBuf.deleteCharAt(paramsBuf.length()-1);
                paramsBuf.append("]");
            }
            paramsBuf.append("}");
            rrequest.getWResponse().addOnloadMethod("displayFusionChartsData",paramsBuf.toString(),true);
        }
    }

    private String getPureNumber(String ip)
    {
        StringBuilder ipBuf=new StringBuilder();
        if(ip!=null)
        {
            for(int i=0;i<ip.length();i++)
            {
                if(ip.charAt(i)<'0'||ip.charAt(i)>'9') continue;
                ipBuf.append(ip.charAt(i));
            }
        }
        return ipBuf.toString();
    }
    
    public String loadStringChartData(boolean invokeInterceptor)
    {
        if(invokeInterceptor&&rbean.getInterceptor()!=null)
        {
            ReportDataBean reportDataObjFromInterceptor=new ReportDataBean(this,rbean.getDbean().getLstCols());
            rbean.getInterceptor().beforeDisplayReportData(rrequest,rbean,reportDataObjFromInterceptor);
            if(reportDataObjFromInterceptor.getChartDataString()!=null) return reportDataObjFromInterceptor.getChartDataString();
        }
        StringBuilder resultBuf=new StringBuilder();
        String title=rbean.getTitle(rrequest);
        resultBuf.append("<chart ");
        if(title!=null&&!title.equals("")) resultBuf.append(" caption='").append(title).append("'");
        String subtitle=rbean.getSubtitle(rrequest);
        if(subtitle!=null&&!subtitle.equals("")) resultBuf.append(" subcaption='").append(subtitle).append("'");
        resultBuf.append("  ").append(acrbean.getChartstyleproperty(rrequest,false));
        resultBuf.append(">");
        resultBuf.append(getDataPartDisplayValue());
        resultBuf.append(getLinkedDataDisplayValue());
        resultBuf.append(fcrbean.getSubdisplayvalue(rrequest));
        resultBuf.append("</chart>");
        return resultBuf.toString();
    }

    public String getDataPartDisplayValue()
    {
        StringBuffer resultBuf=new StringBuffer();
        AbsDatasetType datasetTypeObj=getDataSetTypeObj();
        if(datasetTypeObj.getLstDisplayedColBeans()!=null&&datasetTypeObj.getLstDisplayedColBeans().size()>0)
        {
            if(fcrbean.isXyPlotChart()||!fcrbean.isSingleSeriesChart())
            {
                datasetTypeObj.displayCategoriesPart(resultBuf);
            }
            if(this.lstReportData!=null&&this.lstReportData.size()>0)
            {
                if(fcrbean.isXyPlotChart())
                {
                    ((VerticalDatasetType)datasetTypeObj).displayXyPlotChartDataPart(resultBuf);
                }else if(fcrbean.isSingleSeriesChart())
                {
                    datasetTypeObj.displaySingleSeriesDataPart(resultBuf);
                }else if(fcrbean.isDualLayerDatasetTag())
                {//两层<dataset/>的图表数据
                    datasetTypeObj.displayDualLayerDatasetDataPart(resultBuf);
                }else
                {
                    datasetTypeObj.displaySingleLayerDatasetDataPart(resultBuf);
                }
            }
        }
        return resultBuf.toString();
    }
    
    public String getLinkedDataDisplayValue()
    {
        StringBuffer resultBuf=new StringBuffer();
        if(this.mLinkedDataString!=null)
        {//本报表列数据需要显示<linkeddata/>关联到其它报表进行显示
            String linkedidTmp,linkedDataTmp;
            for(Entry<String,String> entryLinkedDataTmp:this.mLinkedDataString.entrySet())
            {
                linkedidTmp=entryLinkedDataTmp.getKey();
                if(linkedidTmp==null||linkedidTmp.trim().equals("")) continue;
                linkedDataTmp=entryLinkedDataTmp.getValue();
                if(linkedDataTmp==null) linkedDataTmp="";
                resultBuf.append("<linkeddata id='"+linkedidTmp+"'>");
                resultBuf.append(linkedDataTmp);
                resultBuf.append("</linkeddata>");
            }
        }
        return resultBuf.toString();
    }

    public void setLinkedChartData(String linkid,String linkedChartData)
    {
        if(mLinkedDataString==null) mLinkedDataString=new HashMap<String,String>();
        mLinkedDataString.put(linkid,linkedChartData);
    }
    
    private AbsDatasetType getDataSetTypeObj()
    {
        AbsDatasetType datasetTypeObj=null;
        if(rbean.getSbean().isHorizontalDataset()&&!fcrbean.isXyPlotChart())
        {//横向数据集
            datasetTypeObj=new HorizontalDatasetType(this);
        }else
        {
            datasetTypeObj=new VerticalDatasetType(this);
        }
        return datasetTypeObj;
    }
    
    public String getColSelectedMetadata()
    {
        return "";
    }

    protected void showReportOnPdfWithoutTpl()
    {}

    public void showReportOnPlainExcel(Workbook workbook)
    {}
    
    public boolean addNonFromDbColForChart(String beforelabelvalue,String displayvalue)
    {
        List<ColBean> lstColBeans=rrequest.getCdb(rbean.getId()).getLstDynOrderColBeans();
        if(lstColBeans==null) lstColBeans=rbean.getDbean().getLstCols();
        if(lstColBeans==null) return false;
        int addindex=-1;
        if(beforelabelvalue==null)
        {
            addindex=0;
        }else
        {
            int i=0;
            beforelabelvalue=beforelabelvalue.trim();
            for(ColBean cbTmp:lstColBeans)
            {
                if(beforelabelvalue.equals(cbTmp.isNonFromDbCol()?cbTmp.getTagcontent(rrequest).trim():cbTmp.getLabel(rrequest).trim()))
                {
                    addindex=i+1;
                    break;
                }
                i++;
            }
        }
        if(addindex==-1) return false;
        return addNonFromDbColForChart(addindex,displayvalue);
    }
    
    public boolean addNonFromDbColForChart(String displayvalue)
    {
        List<ColBean> lstColBeans=rrequest.getCdb(rbean.getId()).getLstDynOrderColBeans();
        if(lstColBeans==null) lstColBeans=rbean.getDbean().getLstCols();
        if(lstColBeans==null) return false;
        lstColBeans=(List<ColBean>)((ArrayList<ColBean>)lstColBeans).clone();
        lstColBeans.add(createNonFromDBCol(displayvalue));
        rrequest.getCdb(rbean.getId()).setLstDynOrderColBeans(lstColBeans);
        return true;
    }
    
    public boolean addNonFromDbColForChart(int index,String displayvalue)
    {
        if(index<0) return false;
        List<ColBean> lstColBeans=rrequest.getCdb(rbean.getId()).getLstDynOrderColBeans();
        if(lstColBeans==null) lstColBeans=rbean.getDbean().getLstCols();
        if(lstColBeans==null) return false;
        lstColBeans=(List<ColBean>)((ArrayList<ColBean>)lstColBeans).clone();
        if(index>=lstColBeans.size())
        {
            lstColBeans.add(createNonFromDBCol(displayvalue));
        }else
        {
            lstColBeans.add(index,createNonFromDBCol(displayvalue));
        }
        rrequest.getCdb(rbean.getId()).setLstDynOrderColBeans(lstColBeans);
        return true;
    }
    
    private ColBean createNonFromDBCol(String displayvalue)
    {
        ColBean cbeanNonFromDb=new ColBean(rbean.getDbean());
        cbeanNonFromDb.setColumn(Consts_Private.NON_FROMDB);
        cbeanNonFromDb.setProperty(cbeanNonFromDb.getColid());
        cbeanNonFromDb.setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});
        cbeanNonFromDb.setTagcontent(displayvalue);
        return cbeanNonFromDb;
    }
    
    public boolean deleteNonFromDbColForChart(String displayvalue)
    {
        List<ColBean> lstColBeans=rrequest.getCdb(rbean.getId()).getLstDynOrderColBeans();
        if(lstColBeans==null) lstColBeans=rbean.getDbean().getLstCols();
        if(lstColBeans==null) return false;
        lstColBeans=(List<ColBean>)((ArrayList<ColBean>)lstColBeans).clone();
        boolean result=false;
        for(int i=lstColBeans.size()-1;i>=0;i--)
        {
            if(lstColBeans.get(i).isNonFromDbCol())
            {
                if(displayvalue==null||displayvalue.equals(lstColBeans.get(i).getTagcontent(rrequest).trim()))
                {
                    lstColBeans.remove(i);
                    result=true;
                }
            }
        }
        if(result) rrequest.getCdb(rbean.getId()).setLstDynOrderColBeans(lstColBeans);
        return result;
    }
    
    public boolean deleteNonFromDbColForChart(int index)
    {
        List<ColBean> lstColBeans=rrequest.getCdb(rbean.getId()).getLstDynOrderColBeans();
        if(lstColBeans==null) lstColBeans=rbean.getDbean().getLstCols();
        if(lstColBeans==null||lstColBeans.size()<=index) return false;
        if(lstColBeans.get(index)!=null&&lstColBeans.get(index).isNonFromDbCol())
        {
            lstColBeans=(List<ColBean>)((ArrayList<ColBean>)lstColBeans).clone();
            lstColBeans.remove(index);
            rrequest.getCdb(rbean.getId()).setLstDynOrderColBeans(lstColBeans);
            return true;
        }
        return false;
    }
    
    public int afterReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        super.afterReportLoading(reportbean,lstEleReportBeans);
        XmlElementBean eleReportBean=lstEleReportBeans.get(0);
        FunsionChartsReportBean fcrbean=(FunsionChartsReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(fcrbean==null)
        {
            fcrbean=new FunsionChartsReportBean(reportbean);
            reportbean.setExtendConfigDataForReportType(KEY,fcrbean);
        }
        AbsChartReportBean acrbean=(AbsChartReportBean)reportbean.getExtendConfigDataForReportType(AbsChartReportType.KEY);
        String charttype=acrbean.getChartype().toLowerCase();
        if(charttype.equals("scatter.swf")||charttype.equals("bubble.swf"))
        {
            fcrbean.setXyPlotChart(true);
            fcrbean.setSingleSeriesChart(false);
        }else
        {
            fcrbean.setXyPlotChart(false);
            fcrbean.setSingleSeriesChart(lstSingleSeriesChartypes.contains(acrbean.getChartype().toLowerCase()));
        }
        String chartwidth=eleReportBean.attributeValue("chartwidth");
        if(chartwidth!=null) fcrbean.setChartwidth(chartwidth.trim());
        if(fcrbean.getChartwidth()==null||fcrbean.getChartwidth().trim().equals(""))
        {
            fcrbean.setChartwidth("100%");
        }
        String chartheight=eleReportBean.attributeValue("chartheight");
        if(chartheight!=null) fcrbean.setChartheight(chartheight.trim());
        if(fcrbean.getChartheight()==null||fcrbean.getChartheight().trim().equals(""))
        {
            fcrbean.setChartheight("100%");
        }
        String linkchart=eleReportBean.attributeValue("linkchart");
        if(linkchart!=null)
        {
            fcrbean.setLinkChart(linkchart.toLowerCase().trim().equals("true"));
        }
        String debugmode=eleReportBean.attributeValue("debugmode");
        if(debugmode!=null) fcrbean.setDebugmode(debugmode.trim().toLowerCase().equals("true"));//默认是false
        String registerwithjs=eleReportBean.attributeValue("registerwithjs");
        if(registerwithjs!=null) fcrbean.setRegisterwithjs(!registerwithjs.trim().toLowerCase().equals("false"));
        XmlElementBean eleSubdisplayBean=eleReportBean.getChildElementByName("subdisplay");
        if(eleSubdisplayBean!=null)
        {
            fcrbean.setSubdisplayvalue(Config.getInstance().getResourceString(null,reportbean.getPageBean(),eleSubdisplayBean.getContent(),true));
        }
        return 1;
    }

    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        String chartjsfile="/webresources/component/FusionCharts/js/FusionCharts.js";
        chartjsfile=Tools.replaceAll(Config.webroot+"/"+chartjsfile,"//","/");
        reportbean.getPageBean().addMyJavascriptFile(chartjsfile,0);
        AbsChartReportBean acrbean=(AbsChartReportBean)reportbean.getExtendConfigDataForReportType(AbsChartReportType.KEY);
        if(acrbean.getDatatype().equals(AbsChartReportBean.DATATYPE_JSONURL)||acrbean.getDatatype().equals(AbsChartReportBean.DATATYPE_XMLURL))
        {
            if(chartXmlFileTempPath==null)
            {
                String xmlTmpPath=Config.webroot_abspath;
                if(!xmlTmpPath.endsWith(File.separator)) xmlTmpPath=xmlTmpPath+File.separator;
                xmlTmpPath+="wxtmpfiles"+File.separator+"chartdata"+File.separator;
                FilePathAssistant.getInstance().checkAndCreateDirIfNotExist(xmlTmpPath);
                chartXmlFileTempPath=xmlTmpPath;
            }
            DeleteTempFileTask taskObj=new DeleteTempFileTask();
            taskObj.parseInterval(Config.getInstance().getSystemConfigValue("fusioncharts-autodelete-interval",""));
            taskObj.setId("fusioncharts_files");
            taskObj.setFilepath(chartXmlFileTempPath);
            TimingThread.getInstance().addTask(taskObj);
        }
        FunsionChartsReportBean fcrbean=(FunsionChartsReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(fcrbean.isXyPlotChart())
        {//如果是散列或泡沫图表
            fcrbean.setDualLayerDatasetTag(false);
            Map<String,ColBean> mXyzColBeans=new HashMap<String,ColBean>();
            for(ColBean cbTmp:reportbean.getDbean().getLstCols())
            {
                if("x".equals(cbTmp.getProperty())||"y".equals(cbTmp.getProperty()))
                {
                    mXyzColBeans.put(cbTmp.getProperty(),cbTmp);
                }else if("bubble.swf".equals(acrbean.getChartype())&&"z".equals(cbTmp.getProperty()))
                {
                    mXyzColBeans.put(cbTmp.getProperty(),cbTmp);
                }
            }
            if(!mXyzColBeans.containsKey("x")||!mXyzColBeans.containsKey("y"))
            {
                throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"是"+acrbean.getChartype()+"图表类型，必须配置property为x、y的<col/>");
            }
            if("bubble.swf".equals(acrbean.getChartype())&&!mXyzColBeans.containsKey("z"))
            {
                throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"是"+acrbean.getChartype()+"图表类型，必须配置property为x、y、z的<col/>");
            }
            ColBean cbTmp;
            String labelTmp;
            for(Entry<String,ColBean> entryTmp:mXyzColBeans.entrySet())
            {
                cbTmp=entryTmp.getValue();
                if(cbTmp.isControlCol()||cbTmp.isNonFromDbCol()||cbTmp.isNonValueCol()||Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbTmp.getDisplaytype(true)))
                {
                    throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"是"+acrbean.getChartype()
                            +"图表类型，property为x、y、z的<col/>必须是显示的且有效的数据列");
                }
                labelTmp=cbTmp.getLabel(null);
                if(labelTmp==null||labelTmp.equals(""))
                {//没有配置label，则不显示对应的<category/>
                    cbTmp.setLabel(ColBean.NON_LABEL);
                }
            }
        }else
        {
            fcrbean.setDualLayerDatasetTag(isDualLayerDatasetTag(reportbean));
        }
        return 1;
    }
    
    private boolean isDualLayerDatasetTag(ReportBean reportbean)
    {
        SqlBean sbean=reportbean.getSbean();
        FunsionChartsReportBean fcrbean=(FunsionChartsReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(fcrbean.isSingleSeriesChart()||!sbean.isMultiDatasetGroups(true)) return false;//是单序列数据或者只有一个groupid的<dataset/>配置
        if(!sbean.isHorizontalDataset()) return true;//垂直数据集的话，配置了多个独立的<dataset/>就要在图表中显示两层<dataset/>
        List<String> lstDatasetGroupidForData=new ArrayList<String>();
        for(ColBean cbTmp:reportbean.getDbean().getLstCols())
        {
            if(cbTmp.isControlCol()||Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbTmp.getDisplaytype(true))) continue;
            if(sbean.getHdsTitleLabelCbean().getColumn().trim().equals(cbTmp.getColumn())
                    ||sbean.getHdsTitleValueCbean().getColumn().equals(cbTmp.getColumn())) continue;
            String matchedDatasetGroupid=null;//存放本<col/>的数据来自的<dataset/>的groupid
            for(ReportDataSetBean dsbeanTmp:sbean.getLstDatasetBeans())
            {
                if(dsbeanTmp.getDatasetValueBeanOfCbean(cbTmp)==null) continue;
                if(!lstDatasetGroupidForData.contains(dsbeanTmp.getGroupid()))
                {
                    if(lstDatasetGroupidForData.size()>0)
                    {//说明有多个数据列<col/>分别来自不同groupid的<dataset/>，则要显示两层图表<dataset/>
                        return true;
                    }else
                    {
                        lstDatasetGroupidForData.add(dsbeanTmp.getGroupid());
                    }
                }else if(matchedDatasetGroupid==null)
                {
                    matchedDatasetGroupid=dsbeanTmp.getGroupid();
                }else if(!dsbeanTmp.getGroupid().equals(matchedDatasetGroupid))
                {//此<col/>的数据来自多个不同groupid的<dataset/>，也需要显示两层图表<dataset/>
                    return true;
                }
            }
        }
        return false;
    }

    private final static List<String> lstSingleSeriesChartypes=new ArrayList<String>();
    static
    {
        lstSingleSeriesChartypes.add("column2d.swf");
        lstSingleSeriesChartypes.add("column3d.swf");
        lstSingleSeriesChartypes.add("pie3d.swf");
        lstSingleSeriesChartypes.add("pie2d.swf");
        lstSingleSeriesChartypes.add("line.swf");
        lstSingleSeriesChartypes.add("bar2d.swf");
        lstSingleSeriesChartypes.add("area2d.swf");
        lstSingleSeriesChartypes.add("doughnut2d.swf");
        lstSingleSeriesChartypes.add("doughnut3d.swf");
        lstSingleSeriesChartypes.add("pareto2d.swf");
        lstSingleSeriesChartypes.add("pareto3d.swf");
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_FUSIONCHARTS;
    }
}
