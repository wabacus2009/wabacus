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
package com.wabacus.config.component;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.component.application.IApplicationConfigBean;
import com.wabacus.config.component.application.jsphtml.HtmlComponentBean;
import com.wabacus.config.component.application.jsphtml.JspComponentBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.FormatBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.condition.ConditionExpressionBean;
import com.wabacus.config.component.application.report.condition.ConditionSelectItemBean;
import com.wabacus.config.component.application.report.condition.ConditionSelectorBean;
import com.wabacus.config.component.application.report.condition.ConditionValueSelectItemBean;
import com.wabacus.config.component.application.report.extendconfig.LoadExtendConfigManager;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.dataexport.DataExportsConfigBean;
import com.wabacus.config.dataexport.PDFExportBean;
import com.wabacus.config.other.ButtonsBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.print.DefaultPrintProviderConfigBean;
import com.wabacus.config.print.LodopPrintProviderConfigBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.config.template.TemplateParser;
import com.wabacus.config.template.tags.AbsTagInTemplate;
import com.wabacus.config.xml.XmlAssistant;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.AddButton;
import com.wabacus.system.buttons.CancelButton;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.buttons.ResetButton;
import com.wabacus.system.buttons.SaveButton;
import com.wabacus.system.buttons.UpdateButton;
import com.wabacus.system.buttons.WabacusButton;
import com.wabacus.system.commoninterface.IPagePersonalizePersistence;
import com.wabacus.system.commoninterface.IReportPersonalizePersistence;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportDeleteDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportExternalValueBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportInsertDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportUpdateDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.IEditableReportEditGroupOwnerBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.dataset.update.AbsUpdateActionProvider;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.BlobType;
import com.wabacus.system.datatype.ClobType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.intercept.AbsPageInterceptor;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class ComponentConfigLoadManager
{
    private static Log log=LogFactory.getLog(ComponentConfigLoadManager.class);

    private static List<Integer> lstDefaultPageSize;

    public static void loadApplicationsConfigFiles(BufferedInputStream bisReportFile,String jsFileUrl,String jsFilePath,
            Map<String,Map> mLocalResourcesTemp) throws Exception
    {
        Document doc=XmlAssistant.getInstance().loadXmlDocument(bisReportFile);
        Element root=doc.getRootElement();
        if(root==null)
        {
            log.warn("报表配置文件"+jsFilePath+"内容为空!!!");
            return;
        }
        Element eleLocalResources=XmlAssistant.getInstance().getSingleElementByName(root,"local-resources");
        if(eleLocalResources!=null)
        {
            Map mLocalDefineResources=new HashMap();
            Map mLocalResources=new HashMap();
            List lstLocalResources=eleLocalResources.elements("resource-file");
            List<String> lstLocalResourceFiles=ConfigLoadManager.getListConfigFilePaths(lstLocalResources);
            if(lstLocalResourceFiles!=null&&lstLocalResourceFiles.size()>0)
            {
                Map mResultsTmp;
                for(String file:lstLocalResourceFiles)
                {
                    if(file==null||file.trim().equals("")) continue;
                    String tempKey=file.trim().toLowerCase();
                    mResultsTmp=mLocalResourcesTemp.get(tempKey);
                    if(mResultsTmp==null)
                    {
                        mResultsTmp=ConfigLoadManager.loadResourceFile(file);
                        if(mResultsTmp==null) continue;
                        mLocalResourcesTemp.put(tempKey,mResultsTmp);
                    }
                    String key=Tools.copyMapData(mResultsTmp,mLocalResources,true);
                    if(key!=null)
                    {
                        throw new WabacusConfigLoadingException("在报表配置文件的local_resource文件中，name属性为"+key+"的资源存在重复，加载配置文件失败");
                    }
                }
                if(mLocalResources.size()>0)
                {
                    mLocalResources=Config.getInstance().getResources().replace(mLocalResources);
                    Config.getInstance().getResources().getMLocalResources().put(jsFileUrl,mLocalResources);
                }
            }
            Element eleLocalDefineResources=eleLocalResources.element("resources");
            if(eleLocalDefineResources!=null)
            {
                mLocalDefineResources=ConfigLoadManager.loadXmlResources(eleLocalDefineResources);
                if(mLocalDefineResources!=null&&mLocalDefineResources.size()>0)
                {
                    mLocalDefineResources=Config.getInstance().getResources().replace(mLocalDefineResources);
                    Config.getInstance().getResources().getMLocalDefineResources().put(jsFileUrl,mLocalDefineResources);
                }
            }
        }
        Config.getInstance().getMLocalCss().put(jsFileUrl,ConfigLoadManager.loadCssfiles(root.element("local-cssfiles")));
        Config.getInstance().getMLocalJavascriptFiles().put(jsFileUrl,ConfigLoadManager.loadJsfiles(root.element("local-jsfiles")));

        List lstPagesElement=XmlAssistant.getInstance().getElementsByName(root,"page");
        if(lstPagesElement==null||lstPagesElement.size()==0)
        {
            log.warn("报表配置文件没有配置报表!!!");
            return;
        }
        for(int i=0;i<lstPagesElement.size();i++)
        {
            Element elePage=(Element)lstPagesElement.get(i);
            if(elePage!=null)
            {
                XmlElementBean elePageBean=XmlAssistant.getInstance().parseXmlValueToXmlBean(elePage);
                loadPageConfig(jsFileUrl,jsFilePath,elePageBean);
            }
        }
    }

    private static void loadPageConfig(String jsFileUrl,String jsFilePath,XmlElementBean elePageBean)
    {
        PageBean pbean=new PageBean(null,"page");
        pbean.setJsFilePath(jsFilePath);
        pbean.setJsFileUrl(jsFileUrl);
        pbean.setReportfile_key(jsFileUrl);
        loadComponentCommonConfig(elePageBean,pbean);
        ConfigLoadManager.mAllPagesConfig.put(pbean.getId(),pbean);
        try
        {
            loadContainerCommonConfig(elePageBean,pbean);
            String css=elePageBean.attributeValue("css");
            if(css!=null)
            {
                List<String> lstCssesTmp=Tools.parseStringToList(css,",",false);
                String[] cssArray=lstCssesTmp.toArray(new String[lstCssesTmp.size()]);
                if(cssArray.length>0)
                {
                    for(int k=0;k<cssArray.length;k++)
                    {
                        if(cssArray[k]==null||cssArray[k].trim().equals("")) continue;
                        if(!cssArray[k].toLowerCase().trim().startsWith("http://"))
                        {
                            cssArray[k]=Config.webroot+cssArray[k];
                            cssArray[k]=Tools.replaceAll(cssArray[k],"//","/");
                        }
                        pbean.addMyCss(cssArray[k]);
                    }
                }
            }
            String js=elePageBean.attributeValue("js");
            if(js!=null)
            {
                List<String> lstJsTmp=Tools.parseStringToList(js,",",false);
                if(lstJsTmp.size()>0)
                {
                    for(String jsTmp:lstJsTmp)
                    {
                        if(jsTmp==null||jsTmp.trim().equals("")) continue;
                        int loadorder=0;
                        if(jsTmp.trim().startsWith("[")&&jsTmp.indexOf("]")>0)
                        {
                            loadorder=Integer.parseInt(jsTmp.substring(1,jsTmp.indexOf("]")).trim());
                            jsTmp=jsTmp.substring(jsTmp.indexOf("]")+1).trim();
                        }
                        if(!jsTmp.trim().startsWith(Config.webroot)&&!jsTmp.trim().toLowerCase().startsWith("http://"))
                        {
                            jsTmp=Tools.replaceAll(Config.webroot+"/"+jsTmp,"//","/");
                        }
                        pbean.addMyJavascriptFile(jsTmp,loadorder);
                    }
                }
            }
            String personalizeclass=elePageBean.attributeValue("personalizeclass");
            if(personalizeclass!=null&&!personalizeclass.trim().equals(""))
            {
                Object obj=null;
                try
                {
                    obj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(personalizeclass).newInstance();
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("页面"+pbean.getId()+"配置的personalizeclass："+personalizeclass+"类对象实例化失败",e);
                }
                if(!(obj instanceof IPagePersonalizePersistence))
                {
                    throw new WabacusConfigLoadingException("页面"+pbean.getId()+"配置的personalizeclass："+personalizeclass+"没有实现"
                            +IPagePersonalizePersistence.class.getName()+"接口");
                }
                pbean.setPersonalizeObj((IPagePersonalizePersistence)obj);
            }

            String checkpermission=elePageBean.attributeValue("checkpermission");
            if(checkpermission==null||(!checkpermission.trim().toLowerCase().equals("true")&&!checkpermission.trim().toLowerCase().equals("false")))
            {//如果没有配置checkpermission属性，或者配置的不合法，则用默认全局配置值
                pbean.setCheckPermission(Config.getInstance().getSystemConfigValue("default-checkpermission",true));
            }else
            {
                pbean.setCheckPermission(Boolean.parseBoolean(checkpermission.toLowerCase().trim()));
            }
            loadPageInterceptor(elePageBean,pbean);
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("加载页面"+pbean.getId()+"失败",e);
        }
    }

    private static void loadPageInterceptor(XmlElementBean elePageBean,PageBean pbean)
    {
        String interceptor=elePageBean.attributeValue("interceptor");
        if(interceptor!=null&&!interceptor.trim().equals(""))
        {
            List<String> lstInterceptors=Tools.parseStringToList(interceptor,";",false);
            Class clsTmp;
            Object objTmp;
            for(String interceptorTmp:lstInterceptors)
            {
                if(interceptorTmp.equals("")) continue;
                clsTmp=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(interceptorTmp);
                try
                {
                    objTmp=clsTmp.newInstance();
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("实例化页面"+pbean.getId()+"的拦截器"+interceptorTmp+"失败",e);
                }
                if(!(objTmp instanceof AbsPageInterceptor))
                {
                    throw new WabacusConfigLoadingException("页面"+pbean.getId()+"的拦截器"+interceptorTmp+"没有继承"+AbsPageInterceptor.class.getName()+"类");
                }
                pbean.addInterceptor((AbsPageInterceptor)objTmp);
            }
        }
        XmlElementBean eleInterceptorBean=elePageBean.getChildElementByName("interceptor");
        if(eleInterceptorBean!=null)
        {
            List<String> lstImportPackages=ConfigLoadAssistant.getInstance().loadImportsConfig(eleInterceptorBean);
            XmlElementBean elePreAction=eleInterceptorBean.getChildElementByName("preaction");
            String preaction=null;
            if(elePreAction!=null)
            {
                preaction=elePreAction.getContent();
            }
            preaction=preaction==null?"":preaction.trim();

            XmlElementBean eleBeforesaveAction=eleInterceptorBean.getChildElementByName("beforesave");
            String beforesaveaction=null;
            if(eleBeforesaveAction!=null) beforesaveaction=eleBeforesaveAction.getContent();
            beforesaveaction=beforesaveaction==null?"":beforesaveaction.trim();

            XmlElementBean eleAftersaveAction=eleInterceptorBean.getChildElementByName("aftersave");
            String aftersaveaction=null;
            if(eleAftersaveAction!=null) aftersaveaction=eleAftersaveAction.getContent();
            aftersaveaction=aftersaveaction==null?"":aftersaveaction.trim();

            XmlElementBean elePostAction=eleInterceptorBean.getChildElementByName("postaction");
            String postaction=null;
            if(elePostAction!=null)
            {
                postaction=elePostAction.getContent();
            }
            postaction=postaction==null?"":postaction.trim();
            if(!preaction.equals("")||!postaction.equals("")||!beforesaveaction.equals("")||!aftersaveaction.equals(""))
            {
                Class c=ComponentAssistant.getInstance().buildPageInterceptorClass(pbean,lstImportPackages,preaction,beforesaveaction,
                        aftersaveaction,postaction);
                try
                {
                    pbean.addInterceptor((AbsPageInterceptor)c.newInstance());
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("为页面"+pbean.getId()+"生成拦截器类失败",e);
                }
            }

        }
    }

    public static void loadComponentCommonConfig(XmlElementBean eleComponent,IComponentConfigBean acbean)
    {
        String id=eleComponent.attributeValue("id");
        String title=eleComponent.attributeValue("title");
        String subtitle=eleComponent.attributeValue("subtitle");
        String titlealign=eleComponent.attributeValue("titlealign");
        String parenttitle=eleComponent.attributeValue("parenttitle");
        String parentsubtitle=eleComponent.attributeValue("parentsubtitle");
        String width=eleComponent.attributeValue("width");
        String height=eleComponent.attributeValue("height");
        String align=eleComponent.attributeValue("align");
        String valign=eleComponent.attributeValue("valign");
        String top=eleComponent.attributeValue("top");
        String bottom=eleComponent.attributeValue("bottom");
        String left=eleComponent.attributeValue("left");
        String right=eleComponent.attributeValue("right");
        String scrollstyle=eleComponent.attributeValue("scrollstyle");
        String dataexport=eleComponent.attributeValue("dataexport");
        String contextmenu=eleComponent.attributeValue("contextmenu");
        String onload=eleComponent.attributeValue("onload");
        if(id==null||id.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("没有为页面"+acbean.getPageBean().getId()+"中子组件配置id属性");
        }
        if(ConfigLoadAssistant.lstInvalidIdCharacters.contains(id))
        {
            throw new WabacusConfigLoadingException("页面"+acbean.getPageBean().getId()+"中子元素的id属性"+id+"不合法，不能出现如下字符："
                    +ConfigLoadAssistant.lstInvalidIdCharacters);
        }
        id=id.trim();
        if(acbean.getParentContainer()==null)
        {//如果当前是加载顶层<page/>配置信息
            if(ConfigLoadManager.mAllPagesConfig.containsKey(id))
            {
                throw new WabacusConfigLoadingException("配置的<page/>的id属性："+id+"存在重复");
            }
        }else
        {
            if(id.equals(acbean.getPageBean().getId()))
            {
                throw new WabacusConfigLoadingException("id为"+acbean.getPageBean().getId()+"的页面中，存在本同ID的子组件");
            }
            List<String> lstAllChildIds=ConfigLoadManager.mAllPageChildIds.get(acbean.getPageBean().getId());
            if(lstAllChildIds==null)
            {
                lstAllChildIds=new ArrayList<String>();
                ConfigLoadManager.mAllPageChildIds.put(acbean.getPageBean().getId(),lstAllChildIds);
            }else if(lstAllChildIds.contains(id))
            {
                throw new WabacusConfigLoadingException("id为"+acbean.getPageBean().getId()+"的页面中，子组件ID:"+id+"存在重复");
            }
            lstAllChildIds.add(id);
        }
        acbean.setId(id.trim());
        if(title!=null)
        {
            acbean.setTitle(Config.getInstance().getResourceString(null,acbean.getPageBean(),title,true));
        }
        if(subtitle!=null)
        {
            acbean.setSubtitle(Config.getInstance().getResourceString(null,acbean.getPageBean(),subtitle,true));
        }
        if(titlealign!=null)
        {
            titlealign=titlealign.toLowerCase().trim();
            acbean.setTitlealign(titlealign.trim());
        }
        if(parenttitle!=null)
        {
            acbean.setParenttitle(Config.getInstance().getResourceString(null,acbean.getPageBean(),parenttitle,true));
        }
        if(parentsubtitle!=null)
        {
            acbean.setParentSubtitle(parentsubtitle.trim());
        }
        if(top!=null)
        {
            acbean.setTop(getRealHtmlSizeValueByConfig(top.trim()));
        }
        if(bottom!=null)
        {
            acbean.setBottom(getRealHtmlSizeValueByConfig(bottom.trim()));
        }
        if(left!=null)
        {
            acbean.setLeft(getRealHtmlSizeValueByConfig(left.trim()));
        }
        if(right!=null)
        {
            acbean.setRight(getRealHtmlSizeValueByConfig(right.trim()));
        }
        if(width!=null)
        {
            acbean.setWidth(getRealHtmlSizeValueByConfig(width.trim()));
        }
        if(height!=null)
        {
            acbean.setHeight(getRealHtmlSizeValueByConfig(height.trim()));
        }
        if(align!=null)
        {
            acbean.setAlign(align.trim());
        }
        if(valign!=null)
        {
            acbean.setValign(valign.trim());
        }
        DataExportsConfigBean debean=acbean.getDataExportsBean();
        if(debean==null) debean=new DataExportsConfigBean(acbean);
        if(dataexport!=null)
        {
            acbean.setDataExportsBean(debean);
            dataexport=dataexport.trim();
            if(dataexport.equals(""))
            {
                debean.setLstAutoDataExportTypes(null);
            }else
            {
                debean.setLstAutoDataExportTypes(Tools.parseStringToList(dataexport,"|",false));
            }
        }
        XmlElementBean eleDataExportsBean=eleComponent.getChildElementByName("dataexports");
        if(eleDataExportsBean!=null)
        {
            acbean.setDataExportsBean(debean);
            debean.loadConfig(eleDataExportsBean);
        }
        if(scrollstyle!=null)
        {
            scrollstyle=scrollstyle.toLowerCase().trim();
            if(scrollstyle.equals(""))
            {
                acbean.setScrollstyle(null);
            }else if(!Consts_Private.lstAllScrollStyles.contains(scrollstyle))
            {
                throw new WabacusConfigLoadingException("为组件"+acbean.getPath()+"配置的scrollstyle属性值："+scrollstyle+"不支持");
            }else
            {
                acbean.setScrollstyle(scrollstyle);
            }
        }

        if(contextmenu!=null)
        {
            contextmenu=contextmenu.toLowerCase().trim();
            if(contextmenu.equals("false"))
            {
                acbean.setShowContextMenu(false);
            }else
            {
                acbean.setShowContextMenu(true);
            }
        }
        XmlElementBean elePrintBean=eleComponent.getChildElementByName("print");
        if(elePrintBean!=null)
        {
            String type=elePrintBean.attributeValue("type");
            type=type==null?"":type.toLowerCase().trim();
            if(type.equals("")||type.equals("default")||type.equals("lodop"))
            {
                AbsPrintProviderConfigBean printConfigBean=null;
                if(type.equals("lodop"))
                {
                    printConfigBean=new LodopPrintProviderConfigBean(acbean);
                }else
                {
                    printConfigBean=new DefaultPrintProviderConfigBean(acbean);
                }
                printConfigBean.loadConfig(elePrintBean);
                acbean.setPrintBean(printConfigBean);
                acbean.setPdfPrintBean(null);
            }else if(type.equals("pdf"))
            {
                acbean.setPrintBean(null);//将可能从父报表继承过来的其它类型的打印清空
                PDFExportBean pdfprintbean=new PDFExportBean(acbean,Consts.DATAEXPORT_PDF);
                pdfprintbean.setPrint(true);
                pdfprintbean.loadConfig(elePrintBean);
                acbean.setPdfPrintBean(pdfprintbean);
            }else if(type.equals("none"))
            {
                acbean.setPrintBean(null);
                acbean.setPdfPrintBean(null);
            }else
            {
                throw new WabacusConfigLoadingException("加载组件"+acbean.getPath()+"的打印功能失败，为其<print/>配置的type属性"+type+"不支持");
            }
        }
        XmlElementBean eleButtonBean=eleComponent.getChildElementByName("buttons");
        if(eleButtonBean!=null)
        {
            loadButtonsInfo(acbean,eleButtonBean);
        }
        //加载组件的header/footer
        loadHeaderFooterConfig(acbean,eleComponent,"outerheader");
        loadHeaderFooterConfig(acbean,eleComponent,"header");
        loadHeaderFooterConfig(acbean,eleComponent,"footer");
        loadHeaderFooterConfig(acbean,eleComponent,"outerfooter");
        if(onload!=null)
        {
            onload=onload.trim();
            if(onload.equals(""))
            {
                acbean.removeOnloadMethodByType(Consts_Private.ONLOAD_CONFIG);
            }else
            {
                List<String> lstOnloadMethods=Tools.parseStringToList(onload,";",false);
                for(String onloadTmp:lstOnloadMethods)
                {
                    if(onloadTmp.trim().equals("")) continue;
                    acbean.addOnloadMethod(new OnloadMethodBean(Consts_Private.ONLOAD_CONFIG,onloadTmp));
                }
            }
        }
    }

    private static String getRealHtmlSizeValueByConfig(String htmlsize)
    {
        if(htmlsize==null||htmlsize.trim().equals("")) return "";
        htmlsize=htmlsize.trim();
        String[] htmlsizeArr=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(htmlsize);
        if(htmlsizeArr==null) return "";
        if(htmlsizeArr[0].equals("0")) return "";//如果配置为0，则相当于没有配置
        return htmlsizeArr[0]+htmlsizeArr[1];
    }

    public static void loadApplicationCommonConfig(XmlElementBean eleApplication,IApplicationConfigBean acbean)
    {
        String refreshid=eleApplication.attributeValue("refreshid");
        if(refreshid!=null)
        {
            acbean.setRefreshid(refreshid.trim());
        }
        String printwidth=eleApplication.attributeValue("printwidth");
        if(printwidth!=null)
        {
            acbean.setPrintwidth(printwidth.trim());
        }
    }

    private final static List<String> LstNonChildComponentNames=new ArrayList<String>();
    static
    {
        LstNonChildComponentNames.add("dataexports");
        LstNonChildComponentNames.add("print");
        LstNonChildComponentNames.add("buttons");
        LstNonChildComponentNames.add("interceptor");
        LstNonChildComponentNames.add("outerheader");
        LstNonChildComponentNames.add("header");
        LstNonChildComponentNames.add("footer");
        LstNonChildComponentNames.add("outerfooter");
    }

    public static void loadContainerCommonConfig(XmlElementBean eleContainer,AbsContainerConfigBean ccbean)
    {
        String border=eleContainer.attributeValue("border");
        String bordercolor=eleContainer.attributeValue("bordercolor");
        String margin=eleContainer.attributeValue("margin");
        String margin_left=eleContainer.attributeValue("margin_left");
        String margin_right=eleContainer.attributeValue("margin_right");
        String margin_top=eleContainer.attributeValue("margin_top");
        String margin_bottom=eleContainer.attributeValue("margin_bottom");
        String titleposition=eleContainer.attributeValue("titleposition");
        String scrollX=eleContainer.attributeValue("scrollX");
        String scrollY=eleContainer.attributeValue("scrollY");

        if(scrollX!=null&&scrollX.trim().equalsIgnoreCase("true"))
        {
            ccbean.setScrollX(true);
            if(ccbean.getWidth()==null||ccbean.getWidth().trim().equals("")||ccbean.getWidth().indexOf("%")>=0)
            {
                throw new WabacusConfigLoadingException("容器"+ccbean.getPath()+"配置了横向滚动条，所以必须为其配置width属性，且不能配置为百分比");
            }
        }
        if(scrollY!=null&&scrollY.trim().equalsIgnoreCase("true"))
        {
            ccbean.setScrollY(true);
            if(ccbean.getHeight()==null||ccbean.getHeight().trim().equals("")||ccbean.getHeight().indexOf("%")>=0)
            {
                throw new WabacusConfigLoadingException("容器"+ccbean.getPath()+"配置了垂直滚动条，所以必须为其配置height属性，且不能配置为百分比");
            }
        }

        ComponentAssistant.getInstance().doPostLoadForComponentScroll(ccbean,ccbean.isScrollX(),ccbean.isScrollY(),ccbean.getWidth(),
                ccbean.getHeight(),ccbean.getScrollstyle());
        if(border!=null)
        {
            try
            {
                ccbean.setBorder(Integer.parseInt(border.trim()));
            }catch(NumberFormatException e)
            {
                log.warn("页面"+ccbean.getPageBean().getId()+"中id为"+ccbean.getId()+"的子元素border属性不是合法数字",e);
            }
        }
        if(bordercolor!=null)
        {
            ccbean.setBordercolor(bordercolor.trim());
        }
        if(margin!=null)
        {//这个配置对left/right/top/bottom都有效
            ccbean.setMargin_left(margin.trim());
            ccbean.setMargin_right(margin.trim());
            ccbean.setMargin_top(margin.trim());
            ccbean.setMargin_bottom(margin.trim());
        }
        if(margin_left!=null)
        {
            ccbean.setMargin_left(margin_left.trim());
        }
        if(margin_right!=null)
        {
            ccbean.setMargin_right(margin_right.trim());
        }
        if(margin_top!=null)
        {
            ccbean.setMargin_top(margin_top.trim());
        }
        if(margin_bottom!=null)
        {
            ccbean.setMargin_bottom(margin_bottom.trim());
        }
        if(titleposition!=null) ccbean.setTitleposition(titleposition.trim());
        if(ccbean.getMargin_left()!=null&&!ccbean.getMargin_left().trim().equals("")&&ccbean.getMargin_right()!=null
                &&!ccbean.getMargin_right().trim().equals(""))
        {
            ccbean.setColspan_total(3);
        }else if((ccbean.getMargin_left()!=null&&!ccbean.getMargin_left().trim().equals(""))
                ||(ccbean.getMargin_right()!=null&&!ccbean.getMargin_right().trim().equals("")))
        {
            ccbean.setColspan_total(2);
        }else
        {
            ccbean.setColspan_total(1);
        }

        Map<String,IComponentConfigBean> mChildren=new HashMap<String,IComponentConfigBean>();
        List<String> lstChildrenIDs=new ArrayList<String>();
        ccbean.setMChildren(mChildren);
        ccbean.setLstChildrenIDs(lstChildrenIDs);
        List<XmlElementBean> lstChildElements=eleContainer.getLstChildElements();
        if(lstChildElements==null||lstChildElements.size()==0)
        {
            throw new WabacusConfigLoadingException("加载页面/容器"+ccbean.getPath()+"失败，内容为空");
        }
        for(XmlElementBean eleChildTmp:lstChildElements)
        {
            if(eleChildTmp==null) continue;
            if(LstNonChildComponentNames.contains(eleChildTmp.getName())) continue;
            String childid=eleChildTmp.attributeValue("id");
            if(childid==null||childid.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("容器"+ccbean.getPath()+"中子组件存在没有配置id属性的子组件");
            }
            if(ConfigLoadAssistant.lstInvalidIdCharacters.contains(childid))
            {
                throw new WabacusConfigLoadingException("容器"+ccbean.getPath()+"中子组件的id属性"+childid+"不合法，不能出现如下字符："
                        +ConfigLoadAssistant.lstInvalidIdCharacters);
            }
            lstChildrenIDs.add(childid);
            String tagname=eleChildTmp.getName();
            tagname=tagname==null?"":tagname.trim();
            if(tagname.equals("report"))
            {
                ComponentConfigLoadManager.loadReportConfig(eleChildTmp,ccbean);
            }else if(tagname.equals("html"))
            {
                HtmlComponentBean hcbean=new HtmlComponentBean(ccbean);
                loadComponentCommonConfig(eleChildTmp,hcbean);
                loadApplicationCommonConfig(eleChildTmp,hcbean);
                ccbean.getMChildren().put(hcbean.getId(),hcbean);
                hcbean.loadExtendConfig(eleChildTmp,ccbean);
            }else if(tagname.equals("jsp"))
            {
                JspComponentBean jspcbean=new JspComponentBean(ccbean);
                loadComponentCommonConfig(eleChildTmp,jspcbean);
                loadApplicationCommonConfig(eleChildTmp,jspcbean);
                ccbean.getMChildren().put(jspcbean.getId(),jspcbean);
                jspcbean.loadExtendConfig(eleChildTmp,ccbean);
            }else
            {
                AbsContainerType childContainer=Config.getInstance().getContainerType(tagname);
                if(childContainer==null)
                {
                    throw new WabacusConfigLoadingException("容器"+ccbean.getPath()+"配置的id属性："+childid+"的子元素对应的容器"+tagname+"不存在");
                }
                ccbean.getMChildren().put(childid,childContainer.loadConfig(eleChildTmp,ccbean,tagname));
            }
        }
    }

    public static void loadReportConfig(XmlElementBean eleReportBean,AbsContainerConfigBean parentContainerBean)
    {
        String reportid=eleReportBean.attributeValue("id");
        reportid=reportid.trim();
        String reportextends=eleReportBean.attributeValue("extends");
        ReportBean rbean=null;
        ReportBean rbeanParent=null;
        try
        {
            if(reportextends!=null&&!reportextends.trim().equals(""))
            {
                rbeanParent=getReportBeanByPath(reportextends);
                if(rbeanParent==null||rbeanParent.getEleReportBean()!=null)
                {
                    rbean=new ReportBean(parentContainerBean);
                    rbean.setId(reportid);
                    rbean.setEleReportBean(eleReportBean);
                    parentContainerBean.getMChildren().put(reportid,rbean);
                    ConfigLoadManager.lstExtendReports.add(rbean);
                    return;
                }else
                {
                    rbean=(ReportBean)rbeanParent.clone(reportid,parentContainerBean);
                    //                    rbean.setId(reportid);
                }
            }else
            {
                rbean=new ReportBean(parentContainerBean);
                rbean.setId(reportid);
            }
            parentContainerBean.getMChildren().put(reportid,rbean);
            loadReportInfo(rbean,eleReportBean,rbeanParent);
        }catch(Exception e)
        {
            String reportid2="";
            if(rbean!=null)
            {
                reportid2=rbean.getId();
            }
            throw new WabacusConfigLoadingException("加载报表"+parentContainerBean.getPath()+Consts_Private.PATH_SEPERATOR+reportid2+"时出错",e);
        }
    }

    public static ReportBean getReportBeanByPath(String path)
    {
        if(path==null||path.trim().equals("")||path.trim().indexOf(Consts_Private.PATH_SEPERATOR)<=0) return null;
        int idx=path.lastIndexOf(Consts_Private.PATH_SEPERATOR);
        String pageid=path.substring(0,idx).trim();
        String reportid=path.substring(idx+1).trim();
        PageBean pbean=ConfigLoadManager.mAllPagesConfig.get(pageid);
        if(pbean==null) return null;
        return pbean.getReportChild(reportid,true);
    }

    public static void loadReportInfo(ReportBean rb,XmlElementBean eleReportBean,ReportBean rbParent) throws Exception
    {
        try
        {
            rb.setElementBean(eleReportBean);
            loadComponentCommonConfig(eleReportBean,rb);
            loadApplicationCommonConfig(eleReportBean,rb);
            List<XmlElementBean> lstEleReportBeans=new ArrayList<XmlElementBean>();
            lstEleReportBeans.add(eleReportBean);
            String type=eleReportBean.attributeValue("type");
            if(type!=null) rb.setType(type.trim());
            LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(rb,lstEleReportBeans);
            String pojoclass=eleReportBean.attributeValue("pojoclass");
            String formatclass=eleReportBean.attributeValue("formatclass");
            String border=eleReportBean.attributeValue("border");
            String bordercolor=eleReportBean.attributeValue("bordercolor");
            String datastyleproperty=eleReportBean.attributeValue("datastyleproperty");
            String template=eleReportBean.attributeValue("template");
            String cellresize=eleReportBean.attributeValue("cellresize");
            String celldrag=eleReportBean.attributeValue("celldrag");
            String depends=eleReportBean.attributeValue("depends");
            //        String refreshparentondelete=eleReportBean.attributeValue("refreshparentondelete");
            String dependstype=eleReportBean.attributeValue("dependstype");
            String dependsParams=eleReportBean.attributeValue("dependsparams");
            String scrollheight=eleReportBean.attributeValue("scrollheight");
            String scrollwidth=eleReportBean.attributeValue("scrollwidth");
            String pagesize=eleReportBean.attributeValue("pagesize");
            String pagelazyloadata=eleReportBean.attributeValue("pagelazyloadata");
            String dataexportlazyloadata=eleReportBean.attributeValue("dataexportlazyloadata");
            String navigate_reportid=eleReportBean.attributeValue("navigate_reportid");
            String navigate=eleReportBean.attributeValue("navigate");
            String personalizeclass=eleReportBean.attributeValue("personalizeclass");
            String servervalidateclass=eleReportBean.attributeValue("servervalidateclass");
            loadInterceptorInfo(eleReportBean,rb);

            if(pagesize!=null)
            {
                pagesize=pagesize.trim();
                if(pagesize.equals(""))
                {
                    rb.setLstPagesize(null);
                }else
                {
                    rb.setLstPagesize(parsePagesize(rb,pagesize));
                }
            }
            if(rb.getLstPagesize()==null||rb.getLstPagesize().size()==0)
            {
                if(rb.isDetailReportType())
                {
                    List<Integer> lstPageSize=new ArrayList<Integer>();
                    lstPageSize.add(0);
                    rb.setLstPagesize(lstPageSize);
                }else if(rb.isChartReportType())
                {
                    List<Integer> lstPagesize=new ArrayList<Integer>();
                    lstPagesize.add(-1);
                    rb.setLstPagesize(lstPagesize);
                }else
                {
                    if(lstDefaultPageSize==null)
                    {//还没加载全局默认配置
                        lstDefaultPageSize=parsePagesize(null,Config.getInstance().getSystemConfigValue("default-pagesize","10"));
                        if(lstDefaultPageSize==null||lstDefaultPageSize.size()==0)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，没有为其配置pagesize值，且没有在wabacus.cfg.xml中指定全局默认页大小");
                        }
                    }
                    rb.setLstPagesize(lstDefaultPageSize);
                }
            }
            if(pagelazyloadata!=null||rbParent==null)
            {
                if((pagelazyloadata==null&&rbParent==null)||(pagelazyloadata!=null&&pagelazyloadata.trim().equals("")))
                {
                    pagelazyloadata=Config.getInstance().getSystemConfigValue("default-pagelazyloadata","-1");
                }
                rb.setPageLazyloadataCount(Integer.parseInt(pagelazyloadata));
            }
            
            if(dataexportlazyloadata!=null||rbParent==null)
            {
                if((dataexportlazyloadata==null&&rbParent==null)||(dataexportlazyloadata!=null&&dataexportlazyloadata.trim().equals("")))
                {
                    dataexportlazyloadata=Config.getInstance().getSystemConfigValue("default-dataexportlazyloadata","-1");
                }
                rb.setDataexportLazyloadataCount(Integer.parseInt(dataexportlazyloadata));
            }
            
            if(navigate_reportid!=null) rb.setNavigate_reportid(navigate_reportid.trim());
            if(navigate!=null)
            {
                navigate=navigate.trim();
                if(navigate.equals(""))
                {
                    rb.setNavigateObj(null);
                }else
                {
                    Object obj=navigate;
                    if(ComponentConfigLoadAssistant.getInstance().isStaticTemplateResource(navigate))
                    {//如果是静态模板
                        if(Tools.isDefineKey("$",navigate))
                        {
                            obj=Config.getInstance().getResourceObject(null,rb.getPageBean(),navigate,true);
                        }else
                        {
                            obj=TemplateParser.parseTemplateByPath(navigate);
                        }
                    }
                    rb.setNavigateObj(obj);
                }
            }
            if(personalizeclass!=null)
            {
                personalizeclass=personalizeclass.trim();
                if(personalizeclass.equals(""))
                {
                    rb.setPersonalizeObj(null);
                }else if(personalizeclass.toLowerCase().equals("default"))
                {
                    rb.setPersonalizeObj(Config.default_reportpersonalize_object);
                }else
                {
                    Object obj=null;
                    try
                    {
                        obj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(personalizeclass).newInstance();
                    }catch(Exception e)
                    {
                        throw new WabacusConfigLoadingException("报表"+rb.getPath()+"配置的personalizeclass："+personalizeclass+"类对象无例化失败",e);
                    }
                    if(!(obj instanceof IReportPersonalizePersistence))
                    {
                        throw new WabacusConfigLoadingException("报表"+rb.getPath()+"配置的personalizeclass："+personalizeclass+"没有实现"
                                +IReportPersonalizePersistence.class.getName()+"接口");
                    }
                    rb.setPersonalizeObj((IReportPersonalizePersistence)obj);
                }
            }
            if(depends!=null)
            {
                depends=depends.trim();
                if(depends.equals(rb.getId()))
                {
                    throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，不能自己依赖自己");
                }
                if(rb.getRefreshid()!=null&&!rb.getRefreshid().trim().equals("")&&!rb.getRefreshid().trim().equals(rb.getId()))
                {
                    throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，此报表是从报表，不能配置refreshid，因为从报表永远只能刷新自己");
                }
                rb.setDependParentId(depends);
                if(dependstype!=null)
                {
                    dependstype=dependstype.toLowerCase().trim();
                    if(dependstype.equals(""))
                    {
                        rb.setDisplayOnParentNoData(true);
                    }else if(!dependstype.equals("hidden")&&!dependstype.equals("display"))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，此报表是从报表，其dependstype属性只能配置为display或hidden");
                    }else
                    {
                        rb.setDisplayOnParentNoData(dependstype.equals("display"));
                    }
                }
                if(dependsParams!=null&&!dependsParams.trim().equals(""))
                {
                    rb.setDependparams(dependsParams.trim());
                }
                rb.getPageBean().addRelateReports(rb);
            }
            if(servervalidateclass!=null)
            {
                List<Class> lstClasses=ConfigLoadAssistant.getInstance().convertStringToClassList(servervalidateclass.trim());
                rb.setLstServerValidateClasses(lstClasses);
            }
            if(border!=null)
            {
                border=border.toLowerCase().trim();
                if(border.equals("")) border=Consts_Private.REPORT_BORDER_ALL;
                if(!Consts_Private.lstAllReportBorderTypes.contains(border))
                {
                    log.warn("报表"+rb.getPath()+"配置的border属性"+border+"无效，将采用默认边框");
                    border=Consts_Private.REPORT_BORDER_ALL;
                }
                rb.setBorder(border);
            }
            if(bordercolor!=null)
            {
                rb.setBordercolor(bordercolor.trim());
            }
            if(scrollheight!=null)
            {
                scrollheight=scrollheight.trim();
                rb.setScrollheight(scrollheight.trim());
            }
            if(rb.getScrollheight()!=null&&!rb.getScrollheight().trim().equals(""))
            {
                String[] htmlsizeArr=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(rb.getScrollheight().trim());
                if(htmlsizeArr==null||htmlsizeArr[0].equals("")||htmlsizeArr[0].equals("0"))
                {//配置的html大小无效或配置为0，则相当于没有配置
                    rb.setScrollheight(null);
                }else
                {
                    if(htmlsizeArr[1]!=null&&htmlsizeArr[1].equals("%"))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败,配置的scrollheight不能是百分比,而必须配置为像素或其它单位");
                    }
                    rb.setScrollheight(htmlsizeArr[0]+htmlsizeArr[1]);
                }
            }
            if(scrollwidth!=null)
            {
                scrollwidth=scrollwidth.trim();
                rb.setScrollwidth(scrollwidth.trim());
            }
            if(rb.getScrollwidth()!=null&&!rb.getScrollwidth().trim().equals(""))
            {
                String[] htmlsizeArr=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(rb.getScrollwidth().trim());
                if(htmlsizeArr==null||htmlsizeArr[0].equals("")||htmlsizeArr[0].equals("0"))
                {
                    rb.setScrollwidth(null);
                }else
                {
                    if(htmlsizeArr[1]!=null&&htmlsizeArr[1].equals("%"))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败,配置的scrollwidth不能是百分比,而必须配置为像素或其它单位");
                    }
                    rb.setScrollwidth(htmlsizeArr[0]+htmlsizeArr[1]);
                }
            }
            if(cellresize==null)
            {
                if(rbParent==null)
                {
                    rb.setCellresize(Config.getInstance().getSystemConfigValue("default-cellresize",0));
                }
            }else
            {
                int icellresize=0;
                if(cellresize.trim().equals(""))
                {
                    icellresize=Config.getInstance().getSystemConfigValue("default-cellresize",0);
                }else
                {
                    try
                    {
                        icellresize=Integer.parseInt(cellresize.trim());
                    }catch(NumberFormatException e)
                    {
                        icellresize=0;
                    }
                    if(icellresize>2||icellresize<0) icellresize=0;
                }
                rb.setCellresize(icellresize);
            }
            if(celldrag==null)
            {
                if(rbParent==null)
                {
                    rb.setCelldrag(Config.getInstance().getSystemConfigValue("default-celldrag",0));
                }
            }else
            {
                int icelldrag=0;
                if(celldrag.trim().equals(""))
                {
                    icelldrag=Config.getInstance().getSystemConfigValue("default-celldrag",0);
                }else
                {
                    try
                    {
                        icelldrag=Integer.parseInt(celldrag.trim());
                    }catch(NumberFormatException e)
                    {
                        icelldrag=0;
                    }
                    if(icelldrag>2||icelldrag<0) icelldrag=0;
                }
                rb.setCelldrag(icelldrag);
            }
            if(formatclass!=null)
            {
                if(formatclass.trim().equals(""))
                {
                    rb.setLstFormatClasses(null);
                }else
                {
                    rb.setLstFormatClasses(ConfigLoadAssistant.getInstance().convertStringToClassList(formatclass));
                }
            }
            if(pojoclass!=null) rb.setPojo(pojoclass.trim());
            if(template!=null)
            {
                template=template.trim();
                if(template.equals(""))
                {
                    rb.setTplBean(null);
                    rb.setDynTplPath(null);
                }else
                {
                    if(ComponentConfigLoadAssistant.getInstance().isStaticTemplateResource(template))
                    {
                        rb.setTplBean(ComponentConfigLoadAssistant.getInstance().getStaticTemplateBeanByConfig(rb.getPageBean(),template));
                    }else
                    {//动态include的模板
                        //                   if(!template.toLowerCase().startsWith("http://")&&!template.toLowerCase().startsWith(Config.webroot))
                        //                       template=Config.webroot+"/"+template;
                        //                   template=Tools.replaceAll(template,"//","/");
                        rb.setDynTplPath(template);
                    }

                }
            }
            if(datastyleproperty!=null) rb.setDatastyleproperty(datastyleproperty.trim(),false);
            if(rb.getTplBean()==null&&(rb.getDynTplPath()==null||rb.getDynTplPath().trim().equals("")))
            {
                rb.setTplBean(Config.getInstance().getDefaultReportTplBean());
            }
            XmlElementBean eleDisplayBean=eleReportBean.getChildElementByName("display");
            if(eleDisplayBean!=null)
            {
                DisplayBean dbean=new DisplayBean(rb);
                rb.setDbean(dbean);
                dbean.setElementBean(eleDisplayBean);
                loadDisplayConfig(dbean,eleDisplayBean);
            }

            XmlElementBean eleSqlBean=eleReportBean.getChildElementByName("sql");
            if(eleSqlBean!=null)
            {
                SqlBean sbean=new SqlBean(rb);
                rb.setSbean(sbean);
                sbean.setElementBean(eleSqlBean);
                loadSqlConfig(sbean,eleSqlBean);
            }

            String format=null;
            List<String> lstImports=null;
            XmlElementBean eleFormatBean=eleReportBean.getChildElementByName("format");
            if(eleFormatBean!=null)
            {
                List<XmlElementBean> lstEleFormatBeans=new ArrayList<XmlElementBean>();
                lstEleFormatBeans.add(eleFormatBean);
                lstEleFormatBeans.addAll(ConfigLoadAssistant.getInstance().getRefElements(eleFormatBean.attributeValue("ref"),"format",null,rb));//取到所有被此<format ref=""/>引用的<format/>配置
                lstImports=getListImportPackages(lstEleFormatBeans);
                XmlElementBean eleFormatValueBean=null;
                for(XmlElementBean eleFormatBeanTmp:lstEleFormatBeans)
                {
                    eleFormatValueBean=eleFormatBeanTmp.getChildElementByName("value");
                    if(eleFormatValueBean!=null) break;//取到一个就跳出，优先级低的不再取
                }
                if(eleFormatValueBean!=null)
                {
                    format=eleFormatValueBean.getContent();
                    if(format!=null)
                    {
                        format=format.trim();
                        if(format.equals(""))
                        {//如果配置了<format/>的<value/>，但内容配置为空字符串，则显式将它的FormatBean对象置空，这在从父报表继承了format方法但本报表不想用的情况下有用
                            rb.setFbean(null);
                        }else
                        {
                            FormatBean fbean=new FormatBean(rb);
                            fbean.setFormatContent(format);
                            fbean.setLstImports(lstImports);
                            rb.setFbean(fbean);
                        }
                    }
                }
            }
            rb.setPojoClassCache(true);
            LoadExtendConfigManager.loadAfterExtendConfigForReporttype(rb,lstEleReportBeans);
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("报表"+rb.getPath()+"配置失败",e);
        }
    }

    public static boolean isValidNavigateObj(ReportBean rbean,Object navigateObj)
    {
        if(!(navigateObj instanceof String)&&!(navigateObj instanceof TemplateBean))
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"的navigate对象类型："+navigateObj.getClass().getName()
                    +"不合法，必须为String或TemplateBean类型之一");
        }
        if(navigateObj instanceof TemplateBean)
        {
            TemplateBean tplbean=(TemplateBean)navigateObj;
            if(tplbean.getLstTagChildren()!=null)
            {
                for(AbsTagInTemplate tagbeanTmp:tplbean.getLstTagChildren())
                {
                    if(Consts_Private.TAGNAME_NAVIGATE.equals(tagbeanTmp.getTagname()))
                    {//如果当前是<wx:navigate/>标签，则必须要指定其type属性
                        if(tagbeanTmp.getMTagAttributes()==null||tagbeanTmp.getMTagAttributes().get("type")==null
                                ||tagbeanTmp.getMTagAttributes().get("type").trim().equals(""))
                        {
                            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"的翻页导航栏所使用的静态模板内容包括<wx:navigate/>标签，但没有为它指定type属性，会造成死循环");
                        }
                    }
                }
            }
        }
        return true;
    }

    public static List<Integer> parsePagesize(ReportBean rbean,String pagesize)
    {
        List<String> lstTemp=Tools.parseStringToList(pagesize,"|",false);
        List<Integer> lstPageSize=new UniqueArrayList<Integer>();
        try
        {
            for(String strSizeTmp:lstTemp)
            {
                if(strSizeTmp==null||strSizeTmp.trim().equals("")) continue;
                int isize=Integer.parseInt(strSizeTmp.trim());
                if(isize==0&&(rbean==null||rbean.isListReportType()))
                {
                    isize=10;
                }else if(isize<-1)
                {
                    isize=-1;
                }
                lstPageSize.add(isize);
            }
        }catch(NumberFormatException e1)
        {
            if(rbean==null)
            {
                throw new WabacusConfigLoadingException("配置的default-pagesize："+pagesize+"包含非法数字",e1);
            }else
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的pagesize："+pagesize+"包含非法数字",e1);
            }
        }
        if(lstPageSize.contains(0)&&lstPageSize.size()>1&&rbean!=null&&rbean.isDetailReportType())
        {
            if(lstPageSize.contains(1))
            {//如果已经有页大小为1的pagesize，则直接删除掉这个0
                lstPageSize.remove(new Integer(0));
            }else
            {
                int idx=lstPageSize.indexOf(new Integer(0));
                lstPageSize.remove(new Integer(0));
                lstPageSize.add(idx,1);
            }
        }
        return lstPageSize;
    }

    public static List<String> getListImportPackages(List<XmlElementBean> lstEleFormatBeans)
    {
        if(lstEleFormatBeans==null||lstEleFormatBeans.size()==0) return null;
        List<String> lstResults=new UniqueArrayList<String>();
        List<String> lstImportTmp;
        for(XmlElementBean eleFormatBeanTmp:lstEleFormatBeans)
        {
            if(eleFormatBeanTmp==null) continue;
            lstImportTmp=ConfigLoadAssistant.getInstance().loadImportsConfig(eleFormatBeanTmp);
            if(lstImportTmp!=null) lstResults.addAll(lstImportTmp);
        }
        return lstResults;
    }

    private static void loadInterceptorInfo(XmlElementBean eleReportBean,ReportBean rbean) throws ClassNotFoundException,InstantiationException,
            IllegalAccessException
    {
        String interceptor=eleReportBean.attributeValue("interceptor");
        Class c=null;
        if(interceptor!=null)
        {
            interceptor=interceptor.trim();
            if(interceptor.equals(""))
            {
                rbean.setInterceptor(null);
            }else
            {
                if(Tools.isDefineKey("$",interceptor))
                {
                    rbean.setInterceptor((IInterceptor)Config.getInstance().getResourceObject(null,rbean.getPageBean(),interceptor,true));
                }else
                {
                    c=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(interceptor);
                }
            }
        }else
        {
            XmlElementBean eleInterceptorBean=eleReportBean.getChildElementByName("interceptor");
            if(eleInterceptorBean!=null)
            {
                List<String> lstImportPackages=ConfigLoadAssistant.getInstance().loadImportsConfig(eleInterceptorBean);
                XmlElementBean elePreAction=eleInterceptorBean.getChildElementByName("preaction");
                String preaction=elePreAction==null?null:elePreAction.getContent();
                XmlElementBean elePostAction=eleInterceptorBean.getChildElementByName("postaction");
                String postaction=elePostAction==null?null:elePostAction.getContent();
                XmlElementBean eleSaveaction=eleInterceptorBean.getChildElementByName("saveaction");
                String saveaction=eleSaveaction==null?null:eleSaveaction.getContent();
                XmlElementBean eleSaverowaction=eleInterceptorBean.getChildElementByName("saveaction-perrow");
                String saverowaction=eleSaverowaction==null?null:eleSaverowaction.getContent();
                XmlElementBean eleSavesqlaction=eleInterceptorBean.getChildElementByName("saveaction-peraction");
                String savesqlaction=eleSavesqlaction==null?null:eleSavesqlaction.getContent();
                XmlElementBean eleBeforeLoadData=eleInterceptorBean.getChildElementByName("beforeloaddata");
                String beforeloaddata=eleBeforeLoadData==null?null:eleBeforeLoadData.getContent();
                XmlElementBean eleAfterLoadData=eleInterceptorBean.getChildElementByName("afterloaddata");
                String afterloaddata=eleAfterLoadData==null?null:eleAfterLoadData.getContent();
                XmlElementBean eleBeforeDisplay=eleInterceptorBean.getChildElementByName("beforedisplay");
                String beforedisplay=eleBeforeDisplay==null?null:eleBeforeDisplay.getContent();
                XmlElementBean eleDisplayPerRow=eleInterceptorBean.getChildElementByName("beforedisplay-perrow");
                String displayperrow=eleDisplayPerRow==null?null:eleDisplayPerRow.getContent();
                XmlElementBean eleDisplayPerCol=eleInterceptorBean.getChildElementByName("beforedisplay-percol");
                String displaypercol=eleDisplayPerCol==null?null:eleDisplayPerCol.getContent();

                if(Tools.isEmpty(preaction,true)&&Tools.isEmpty(postaction,true)&&Tools.isEmpty(saveaction,true)&&Tools.isEmpty(saverowaction,true)
                        &&Tools.isEmpty(savesqlaction,true)&&Tools.isEmpty(beforeloaddata,true)&&Tools.isEmpty(afterloaddata,true)
                        &&Tools.isEmpty(beforedisplay,true)&&Tools.isEmpty(displayperrow,true)&&Tools.isEmpty(displaypercol,true))
                {
                    rbean.setInterceptor(null);
                }else
                {
                    c=ReportAssistant.getInstance().buildInterceptorClass(rbean.getPageBean().getId()+rbean.getId(),lstImportPackages,preaction,
                            postaction,saveaction,saverowaction,savesqlaction,beforeloaddata,afterloaddata,beforedisplay,displayperrow,displaypercol);
                }
            }
        }
        if(c!=null)
        {
            rbean.setInterceptor((IInterceptor)c.newInstance());
        }
    }

    private static void loadButtonsInfo(IComponentConfigBean ccbean,XmlElementBean eleButtonsBean)
    {
        if(eleButtonsBean==null) return;
        List<XmlElementBean> lstEleButtonsBeans=new ArrayList<XmlElementBean>();
        lstEleButtonsBeans.add(eleButtonsBean);
        lstEleButtonsBeans.addAll(ConfigLoadAssistant.getInstance().getRefElements(eleButtonsBean.attributeValue("ref"),"buttons",null,ccbean));
        ButtonsBean buttonsBean=new ButtonsBean(ccbean);
        ccbean.setButtonsBean(buttonsBean);
        Map<String,String> mButtonsProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleButtonsBeans,
                new String[] { "buttonspacing", "align", "titleposition" });//组装所有<buttons/>配置的这些属性
        String buttonspacing=mButtonsProperties.get("buttonspacing");
        if(buttonspacing!=null&&!buttonspacing.trim().equals(""))
        {
            try
            {
                buttonsBean.setButtonspacing(Integer.parseInt(buttonspacing.trim()));
            }catch(NumberFormatException nfe)
            {
                log.warn("为组件"+ccbean.getPath()+"的<buttons/>配置的buttonspacing不是合法数字",nfe);
            }
        }
        String align=mButtonsProperties.get("align");
        if(align!=null)
        {//<buttons/>中的align属性只对容器显示在top和bottom的按钮和对报表显示在title上的按钮有效
            buttonsBean.setAlign(align.toLowerCase().trim());
        }
        String titleposition=mButtonsProperties.get("titleposition");
        if(titleposition!=null) buttonsBean.setTitleposition(titleposition.toLowerCase().trim());
        List<XmlElementBean> lstEleButtons=new ArrayList<XmlElementBean>();
        getEleButtonBeans(lstEleButtonsBeans,lstEleButtons,null,ccbean);
        if(lstEleButtons!=null&&lstEleButtons.size()>0)
        {
            AbsButtonType buttonObj=null;
            for(XmlElementBean eleButtonBeanTmp:lstEleButtons)
            {
                if(eleButtonBeanTmp==null) continue;
                buttonObj=loadButtonConfig(ccbean,eleButtonBeanTmp);
                addButtonToPositions(ccbean,buttonObj);
            }
        }
    }

    private static void loadHeaderFooterConfig(IComponentConfigBean ccbean,XmlElementBean eleComponentBean,String headerfooter)
    {
        XmlElementBean eleHeaderFooter=eleComponentBean.getChildElementByName(headerfooter);
        if(eleHeaderFooter==null) return;
        String content=eleHeaderFooter.getContent().trim();
        TemplateBean tplBean=null;
        if(!content.equals(""))
        {
            if(ComponentConfigLoadAssistant.getInstance().isStaticTemplateResource(content))
            {
                if(Tools.isDefineKey("$",content))
                {//取资源文件中的显示值
                    Object obj=Config.getInstance().getResourceObject(null,ccbean.getPageBean(),content,true);
                    if(obj==null) obj="";
                    if(obj instanceof TemplateBean)
                    {
                        tplBean=(TemplateBean)obj;
                    }else
                    {
                        tplBean=new TemplateBean();
                        tplBean.setContent(obj.toString());
                    }
                }else
                {//取html/htm文件中的模板
                    tplBean=TemplateParser.parseTemplateByPath(content);
                }
            }else
            {
                //                tplBean.setContent(content.trim());
                tplBean=TemplateParser.parseTemplateByContent(content.trim());
            }
        }
        if(headerfooter.equals("outerheader"))
        {
            ccbean.setOuterHeaderTplBean(tplBean);
        }else if(headerfooter.equals("header"))
        {
            ccbean.setHeaderTplBean(tplBean);
        }else if(headerfooter.equals("footer"))
        {
            ccbean.setFooterTplBean(tplBean);
        }else if(headerfooter.equals("outerfooter"))
        {
            ccbean.setOuterFooterTplBean(tplBean);
        }
    }

    private static void getEleButtonBeans(List<XmlElementBean> lstEleButtonsBeans,List<XmlElementBean> lstResults,List<String> lstButtonsName,
            IComponentConfigBean ccbean)
    {
        if(lstEleButtonsBeans==null||lstEleButtonsBeans.size()==0) return;
        List<XmlElementBean> lstEleBeanTmp;
        if(lstButtonsName==null) lstButtonsName=new ArrayList<String>();
        for(XmlElementBean eleButtonsBeanTmp:lstEleButtonsBeans)
        {
            lstEleBeanTmp=eleButtonsBeanTmp.getLstChildElements();
            if(lstEleBeanTmp==null||lstEleBeanTmp.size()==0) continue;
            List<String> lstNameTmp=new ArrayList<String>();//存放当前<buttons/>的所有name属性，用于判断是否存在重复name属性
            String buttonNameTmp;
            for(XmlElementBean eleChildBeanTmp:lstEleBeanTmp)
            {
                if("button".equals(eleChildBeanTmp.getName()))
                {//是<button/>配置
                    buttonNameTmp=eleChildBeanTmp.attributeValue("name");
                    if(lstNameTmp.contains(buttonNameTmp))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+ccbean.getPath()+"的按钮失败，存在重复name属性的配置");
                    }
                    lstNameTmp.add(buttonNameTmp);
                    if(lstButtonsName.contains(buttonNameTmp))
                    {
                        continue;
                    }
                    lstButtonsName.add(buttonNameTmp);
                    lstResults.add(eleChildBeanTmp);
                }else if("ref".equals(eleChildBeanTmp.getName()))
                {//当前是<ref/>标签
                    getEleButtonBeans(ConfigLoadAssistant.getInstance().getRefElements(eleChildBeanTmp.attributeValue("key"),"buttons",null,ccbean),
                            lstResults,lstButtonsName,ccbean);
                }
            }
        }
    }

    public static void addButtonToPositions(IComponentConfigBean ccbean,AbsButtonType buttonObj)
    {
        ButtonsBean buttonsBean=ccbean.getButtonsBean();
        if(buttonsBean==null)
        {
            buttonsBean=new ButtonsBean(ccbean);
            ccbean.setButtonsBean(buttonsBean);
        }
        String position=buttonObj.getPosition();
        if(position==null||position.trim().equals(""))
        {
            position=Consts.OTHER_PART;
        }
        List<String> lstPosis=Tools.parseStringToList(position,"|",false);
        for(String positionTemp:lstPosis)
        {
            if(positionTemp==null||positionTemp.trim().equals(""))
            {
                positionTemp=Consts.OTHER_PART;
            }
            buttonsBean.addButton(buttonObj,positionTemp.trim());
        }
    }

    public static AbsButtonType loadButtonConfig(IComponentConfigBean ccbean,XmlElementBean eleButtonBean)
    {
        mergeAllParentsButtonConfig(ccbean,eleButtonBean,null);
        AbsButtonType buttonObj=null;
        String buttonclass=eleButtonBean.attributeValue("class");
        try
        {
            Class c=null;
            if(buttonclass==null||buttonclass.trim().equals(""))
            {
                c=WabacusButton.class;
            }else
            {
                c=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(buttonclass);
            }
            Object o=c.getConstructor(new Class[] { IComponentConfigBean.class }).newInstance(new Object[] { ccbean });
            if(!(o instanceof AbsButtonType))
            {
                throw new WabacusConfigLoadingException("配置的按钮插件类"+c.getName()+"没有继承"+AbsButtonType.class.getName()+"类");
            }
            buttonObj=(AbsButtonType)o;
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("配置的按钮类："+buttonclass+"无法加载或实例化对象",e);
        }
        String buttonname=eleButtonBean.attributeValue("name");
        if(buttonname!=null) buttonObj.setName(buttonname.trim());
        String label=eleButtonBean.attributeValue("label");
        String menulabel=eleButtonBean.attributeValue("menulabel");
        String menugroup=eleButtonBean.attributeValue("menugroup");
        String position=eleButtonBean.attributeValue("position");
        String positionorder=eleButtonBean.attributeValue("positionorder");
        String styleproperty=eleButtonBean.attributeValue("styleproperty");
        String disabledstyleproperty=eleButtonBean.attributeValue("disabledstyleproperty");
        String confirmessage=eleButtonBean.attributeValue("confirmessage");
        String cancelmethod=eleButtonBean.attributeValue("cancelmethod");
        if(position!=null) buttonObj.setPosition(position.trim());
        if(positionorder!=null)
        {
            positionorder=positionorder.trim();
            if(!positionorder.equals(""))
            {
                try
                {
                    buttonObj.setPositionorder(Integer.parseInt(positionorder));
                }catch(NumberFormatException e)
                {
                    log.warn("组件"+ccbean.getPath()+"上的按钮"+buttonname+"配置的positionorder属性"+positionorder+"不是有效数字");
                }
            }else
            {
                buttonObj.setPositionorder(0);
            }
        }
        String refer=eleButtonBean.attributeValue("refer");
        if(refer!=null&&!refer.trim().equals(""))
        {
            if(!(ccbean instanceof AbsContainerConfigBean))
            {
                throw new WabacusConfigLoadingException("组件"+ccbean.getPath()+"不是容器，不能将其按钮配置为通过refer属性引用其它按钮");
            }
            buttonObj.setRefer(refer.trim());
            String referedbutton=eleButtonBean.attributeValue("referedbutton");
            if(referedbutton!=null)
            {
                if(referedbutton.toLowerCase().trim().equals("display"))
                {
                    buttonObj.setReferedbutton("display");
                }else
                {
                    buttonObj.setReferedbutton("hidden");
                }
            }
        }else
        {
            if(label!=null)
            {
                buttonObj.setLabel(Config.getInstance().getResourceString(null,ccbean.getPageBean(),label,true).trim());
            }
            if(menulabel!=null)
            {
                buttonObj.setMenulabel(Config.getInstance().getResourceString(null,ccbean.getPageBean(),menulabel.trim(),true));
            }
            if(buttonObj.getMenulabel()==null||buttonObj.getMenulabel().trim().equals(""))
            {
                buttonObj.setMenulabel(buttonObj.getLabel());
            }
            if(menugroup!=null) buttonObj.setMenugroup(menugroup.trim());
            if(styleproperty!=null) buttonObj.setStyleproperty(styleproperty.trim());
            if(disabledstyleproperty!=null) buttonObj.setDisabledstyleproperty(disabledstyleproperty.trim());
            String clickevent=eleButtonBean.getContent();
            if(clickevent!=null&&!clickevent.trim().equals(""))
            {
                if(clickevent.indexOf('\"')>=0)
                {
                    throw new WabacusConfigLoadingException("加载组件"+ccbean.getPath()+"的按钮"+buttonname+"失败，按钮事件中不能用双引号，只能用单引用，如果有多级，可以加上转义字符\\");
                }
                buttonObj.setClickEvent(Tools.formatStringBlank(clickevent.trim()));
            }else
            {//动态事件字符串（用JAVA代码生成）
                List<String> lstImports=ConfigLoadAssistant.getInstance().loadImportsConfig(eleButtonBean);
                XmlElementBean eleDynEventBean=eleButtonBean.getChildElementByName("dynevent");
                if(eleDynEventBean!=null)
                {
                    String dynevent=eleDynEventBean.getContent();
                    if(dynevent!=null&&!dynevent.trim().equals(""))
                    {
                        buttonObj.setClickEvent(ReportAssistant.getInstance().createButtonEventGeneratorObject(
                                ccbean.getPageBean().getId()+"_"+ccbean.getId()+buttonname,dynevent,lstImports));
                    }
                }
            }
        }
        if(confirmessage!=null&&!confirmessage.trim().equals(""))
        {
            buttonObj.setConfirmessage(Config.getInstance().getResourceString(null,ccbean.getPageBean(),confirmessage.trim(),true));
            String confirmtitle=eleButtonBean.attributeValue("confirmtitle");
            if(confirmtitle!=null)
                buttonObj.setConfirmtitle(Config.getInstance().getResourceString(null,ccbean.getPageBean(),confirmtitle.trim(),true));
            buttonObj.setCancelmethod(cancelmethod==null||cancelmethod.trim().equals("")?"null":cancelmethod.trim());
        }
        buttonObj.loadExtendConfig(eleButtonBean);
        return buttonObj;
    }

    private static void mergeAllParentsButtonConfig(IComponentConfigBean ccbean,XmlElementBean eleButtonBean,List<String> lstExtendedParentKeys)
    {
        String extendsParent=eleButtonBean.attributeValue("extends");
        if(extendsParent==null||extendsParent.trim().equals("")) return;
        if(lstExtendedParentKeys==null) lstExtendedParentKeys=new ArrayList<String>();
        extendsParent=extendsParent.trim();
        if(lstExtendedParentKeys.contains(extendsParent))
        {
            throw new WabacusConfigLoadingException("加载组件"+ccbean.getPath()+"下配置的按钮失败，被直接或间接继承的父按钮对应的KEY："+extendsParent+"存在循环继承");
        }
        lstExtendedParentKeys.add(extendsParent);
        if(!Tools.isDefineKey("$",extendsParent))
        {
            throw new WabacusConfigLoadingException("加载组件"+ccbean.getPath()+"下配置的按钮失败，被直接或间接继承的父按钮对应的KEY："+extendsParent+"不是合法的资源项KEY");
        }
        XmlElementBean parentButtonConfig=(XmlElementBean)Config.getInstance().getResourceObject(null,ccbean.getPageBean(),extendsParent,true);
        if(parentButtonConfig==null)
        {
            throw new WabacusConfigLoadingException("加载组件"+ccbean.getPath()+"下配置的按钮失败，根据KEY"+extendsParent+"没有从资源文件中找到被直接或间接继承的父按钮对象");
        }
        extendsParent=parentButtonConfig.attributeValue("extends");
        if(extendsParent!=null&&!extendsParent.trim().equals(""))
        {
            mergeAllParentsButtonConfig(ccbean,parentButtonConfig,lstExtendedParentKeys);
        }
        XmlAssistant.getInstance().mergeXmlElementBeans(eleButtonBean,parentButtonConfig);
    }

    private static void loadSqlConfig(SqlBean sbean,XmlElementBean eleSqlBean)
    {
        List<XmlElementBean> lstEleSqlBeans=new ArrayList<XmlElementBean>();
        lstEleSqlBeans.add(eleSqlBean);
        lstEleSqlBeans.addAll(ConfigLoadAssistant.getInstance().getRefElements(eleSqlBean.attributeValue("ref"),"sql",null,sbean.getReportBean()));
        //        LoadExtendConfigManager.loadBeforeExtendConfigForPagetype(sbean,lstEleSqlBeans);
        LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(sbean,lstEleSqlBeans);

        Map<String,String> mSqlProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleSqlBeans,
                new String[] { "preparedstatement", "datasource", "beforesearch" });//组装所有<sql/>配置的这些属性

        String preparedstatement=mSqlProperties.get("preparedstatement");
        if(preparedstatement!=null) sbean.setPreparedStatement(preparedstatement.trim());
        String datasource=mSqlProperties.get("datasource");
        if(datasource==null||datasource.trim().equals(""))
        {
            datasource=Consts.DEFAULT_KEY;
        }
        sbean.setDatasource(datasource.trim());
        String beforesearch=mSqlProperties.get("beforesearch");
        if(beforesearch!=null)
        {
            sbean.setBeforeSearchMethod(beforesearch.trim());
        }
        //        }else
        List<ReportDataSetBean> lstDatasetBeans=getLstDatasetBeans(sbean,lstEleSqlBeans);//从所有<sql/>中得到配置的所有<dataset/>及其<value/>对象
        if(lstDatasetBeans!=null)
        {
            List<ReportDataSetBean> lstRealDatasetBeans=new ArrayList<ReportDataSetBean>();
            for(ReportDataSetBean rdsgbeanTmp:lstDatasetBeans)
            {
                if(rdsgbeanTmp.loadDatasetValues()) lstRealDatasetBeans.add(rdsgbeanTmp);
            }
            sbean.setLstDatasetBeans(lstRealDatasetBeans);
        }
        List<XmlElementBean> lstCondition=getLstEleSqlConditionBeans(lstEleSqlBeans);//从所有<sql/>中得到name属性不重复的<condition/>对象集合
        if(lstCondition!=null&&lstCondition.size()>0)
        {
            loadReportConditionConfig(lstCondition,sbean);
        }
        sbean.afterSqlLoad();
        LoadExtendConfigManager.loadAfterExtendConfigForReporttype(sbean,lstEleSqlBeans);
    }

    private static List<ReportDataSetBean> getLstDatasetBeans(SqlBean sbean,List<XmlElementBean> lstEleSqlBeans)
    {
        if(lstEleSqlBeans==null||lstEleSqlBeans.size()==0) return null;
        List<ReportDataSetBean> lstResults=new ArrayList<ReportDataSetBean>();
        List<String> lstExistDatasetGroupids=new ArrayList<String>();//存放已经处理过的<dataset/>的id属性
        boolean isExistNoIdGroup=false;//是否已经存在没有配置id的<dataset/>
        XmlElementBean eleDatasetParentEleBean=getEleDatasetParentBean(sbean,lstEleSqlBeans);
        if(eleDatasetParentEleBean==null) return null;
        ReportBean rbean=sbean.getReportBean();
        List<XmlElementBean> lstValueBeansTmp=null;
        String dsidTmp;
        ReportDataSetBean dsbeanTmp;
        List<XmlElementBean> lstDatasetBeans=eleDatasetParentEleBean.getLstChildElementsByName("dataset");
        if(lstDatasetBeans!=null&&lstDatasetBeans.size()>0)
        {
            for(XmlElementBean eleDatasetBeanTmp:lstDatasetBeans)
            {
                lstValueBeansTmp=eleDatasetBeanTmp.getLstChildElementsByName("value");
                if(lstValueBeansTmp==null||lstValueBeansTmp.size()==0) return null;
                dsbeanTmp=new ReportDataSetBean(sbean);
                dsbeanTmp.setElementBean(eleDatasetBeanTmp);
                dsidTmp=eleDatasetBeanTmp.attributeValue("id");
                if(dsidTmp==null||dsidTmp.trim().equals(""))
                {
                    if(lstResults.size()>0)
                    {//已经有<dataset/>，说明配置了多个<dataset/>
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的<dataset/>失败，当使用多个<dataset/>时，所有<dataset/>标签必须配置id属性，且不能重复");
                    }
                    dsidTmp=Consts.DEFAULT_KEY;
                    isExistNoIdGroup=true;
                }else
                {
                    if(isExistNoIdGroup)
                    {//已经存在没有配置id的<value/>
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的<dataset/>失败，当使用多个<dataset/>时，所有<dataset/>标签必须配置id属性，且不能重复");
                    }
                    if(lstExistDatasetGroupids.contains(dsidTmp))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的<dataset/>失败，id属性为"+dsidTmp+"的<dataset/>存在重复");
                    }
                    lstExistDatasetGroupids.add(dsidTmp);
                }
                dsbeanTmp.setId(dsidTmp.trim());
                String mergetop=eleDatasetBeanTmp.attributeValue("mergetop");
                if(mergetop!=null) dsbeanTmp.setMergetop(mergetop.toLowerCase().trim().equals("true"));
                if(dsbeanTmp.isMergetop()&&lstResults.size()>0)
                {//如果当前是数据集是合并到上一个数据集，则它们的groupid相同
                    dsbeanTmp.setGroupid(lstResults.get(lstResults.size()-1).getGroupid());
                }else
                {//第一个<dataset/>或者独立的<dataset/>，它们的groupid就是它们的id
                    dsbeanTmp.setGroupid(dsbeanTmp.getId());
                }
                String styleproperty=eleDatasetBeanTmp.attributeValue("styleproperty");
                if(styleproperty!=null)
                {
                    dsbeanTmp.setDatasetstyleproperty(Config.getInstance().getResourceString(null,sbean.getPageBean(),styleproperty,true),false);
                }
                dsbeanTmp.setDatasource(eleDatasetBeanTmp.attributeValue("datasource"));
                dsbeanTmp.setLstEleValueBeans(lstValueBeansTmp);
                lstResults.add(dsbeanTmp);
            }
        }else
        {
            lstValueBeansTmp=eleDatasetParentEleBean.getLstChildElementsByName("value");
            if(lstValueBeansTmp==null||lstValueBeansTmp.size()==0) return null;
            dsbeanTmp=new ReportDataSetBean(sbean);
            dsbeanTmp.setId(Consts.DEFAULT_KEY);
            dsbeanTmp.setGroupid(Consts.DEFAULT_KEY);
            dsbeanTmp.setLstEleValueBeans(lstValueBeansTmp);
            lstResults.add(dsbeanTmp);
        }
        return lstResults;
    }

    private static XmlElementBean getEleDatasetParentBean(SqlBean sbean,List<XmlElementBean> lstEleSqlBeans)
    {
        List<XmlElementBean> lstTmps;
        XmlElementBean eleSelectBeanTmp, eleDatasetParentBean=null;
        for(XmlElementBean eleSqlBeanTmp:lstEleSqlBeans)
        {
            eleSelectBeanTmp=eleSqlBeanTmp.getChildElementByName("select");
            if(eleSelectBeanTmp!=null)
            {
                String titlelabelcolumn=eleSelectBeanTmp.attributeValue("titlelabelcolumn");
                String titlevaluecolumn=eleSelectBeanTmp.attributeValue("titlevaluecolumn");
                titlelabelcolumn=titlelabelcolumn==null?"":titlelabelcolumn.trim();
                titlevaluecolumn=titlevaluecolumn==null?"":titlevaluecolumn.trim();
                if(!titlelabelcolumn.equals("")||!titlevaluecolumn.equals(""))
                {
                    if(titlelabelcolumn.equals("")) titlelabelcolumn=titlevaluecolumn;
                    if(titlevaluecolumn.equals("")) titlevaluecolumn=titlelabelcolumn;
                    sbean.setHdsTitleLabelColumn(titlelabelcolumn);
                    sbean.setHdsTitleValueColumn(titlevaluecolumn);
                    String titlecolumndatasetid=eleSelectBeanTmp.attributeValue("titlecolumndatasetid");
                    if(titlecolumndatasetid!=null) sbean.setTitlecolumndatasetid(titlecolumndatasetid.trim());
                }
                String preparedstatement=eleSelectBeanTmp.attributeValue("preparedstatement");
                if(preparedstatement!=null) sbean.setSelectPreparedStatement(preparedstatement.trim());
                eleDatasetParentBean=eleSelectBeanTmp;
            }else
            {
                lstTmps=eleSqlBeanTmp.getLstChildElementsByName("dataset");
                if(lstTmps!=null&&lstTmps.size()>0)
                {
                    eleDatasetParentBean=eleSqlBeanTmp;
                }else
                {
                    lstTmps=eleSqlBeanTmp.getLstChildElementsByName("value");
                    if(lstTmps!=null&&lstTmps.size()>0)
                    {
                        eleDatasetParentBean=eleSqlBeanTmp;
                    }
                }
            }
            if(eleDatasetParentBean!=null) return eleDatasetParentBean;
        }
        return null;
    }

    private static List<XmlElementBean> getLstEleSqlConditionBeans(List<XmlElementBean> lstEleSqlBeans)
    {
        List<XmlElementBean> lstResults=new ArrayList<XmlElementBean>();
        if(lstEleSqlBeans==null||lstEleSqlBeans.size()==0) return lstResults;
        XmlElementBean eleSelectBeanTmp;
        List<XmlElementBean> lstConTemps;
        List<String> lstConNames=new ArrayList<String>();//存放已经处理过的<condition/>的name属性，以便收集所有name属性不同的<condition/>对象
        for(XmlElementBean eleSqlBeanTmp:lstEleSqlBeans)
        {
            eleSelectBeanTmp=eleSqlBeanTmp.getChildElementByName("select");
            lstConTemps=null;
            if(eleSelectBeanTmp!=null)
            {
                lstConTemps=eleSelectBeanTmp.getLstChildElementsByName("condition");
            }else
            {
                lstConTemps=eleSqlBeanTmp.getLstChildElementsByName("condition");
            }
            if(lstConTemps!=null&&lstConTemps.size()>0)
            {
                for(XmlElementBean eleConBeanTmp:lstConTemps)
                {
                    if(eleConBeanTmp==null) continue;
                    if(lstConNames.contains(eleConBeanTmp.attributeValue("name"))) continue;
                    lstResults.add(eleConBeanTmp);
                    lstConNames.add(eleConBeanTmp.attributeValue("name"));
                }
            }
        }
        return lstResults;
    }

    private static void loadReportConditionConfig(List<XmlElementBean> lstConditionElements,SqlBean sbean)
    {
        List<ConditionBean> lstConditions=new ArrayList<ConditionBean>();
        sbean.setLstConditions(lstConditions);
        List<String> lstConNamesTmp=new ArrayList<String>();
        for(XmlElementBean eleConditionBeanTmp:lstConditionElements)
        {
            if(eleConditionBeanTmp==null) continue;
            ConditionBean cb=new ConditionBean(sbean);
            cb.setElementBean(eleConditionBeanTmp);
            String name=eleConditionBeanTmp.attributeValue("name");
            if(name==null||name.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("报表"+sbean.getReportBean().getPath()+"配置的查询条件没有配置name属性");
            }
            name=name.trim();
            if(lstConNamesTmp.contains(name))
            {
                throw new WabacusConfigLoadingException("报表"+sbean.getReportBean().getPath()+"存在多个name为"+name+"的查询条件");
            }
            lstConNamesTmp.add(name);
            lstConditions.add(cb);
            cb.setName(name.trim()); //此输入框的name属性
            List<XmlElementBean> lstEleConditionBeans=new ArrayList<XmlElementBean>();
            lstEleConditionBeans.add(eleConditionBeanTmp);
            LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(cb,lstEleConditionBeans);
            String label=eleConditionBeanTmp.attributeValue("label");
            if(label!=null)
            {
                cb.setLabel(Config.getInstance().getResourceString(null,sbean.getPageBean(),label,true));
            }
            String labelposition=eleConditionBeanTmp.attributeValue("labelposition");
            String hidden=eleConditionBeanTmp.attributeValue("hidden");
            String constant=eleConditionBeanTmp.attributeValue("constant");
            String br=eleConditionBeanTmp.attributeValue("br");
            String splitlike=eleConditionBeanTmp.attributeValue("splitlike");
            String defaultvalue=eleConditionBeanTmp.attributeValue("defaultvalue");
            String keepkeywords=eleConditionBeanTmp.attributeValue("keepkeywords");
            String source=eleConditionBeanTmp.attributeValue("source");
            String left=eleConditionBeanTmp.attributeValue("left");
            String right=eleConditionBeanTmp.attributeValue("right");
            String belongto=eleConditionBeanTmp.attributeValue("belongto");
            String onsetvalue=eleConditionBeanTmp.attributeValue("onsetvalue");
            String ongetvalue=eleConditionBeanTmp.attributeValue("ongetvalue");
            if(defaultvalue!=null) cb.setDefaultvalue(defaultvalue.trim());
            if(splitlike!=null) cb.setSplitlike(splitlike.trim());
            IDataType typeObj=ConfigLoadAssistant.loadDataType(eleConditionBeanTmp);
            if((typeObj instanceof BlobType)||(typeObj instanceof ClobType))
            {
                throw new WabacusConfigLoadingException("报表"+cb.getReportBean().getPath()+"的查询条件"+cb.getName()+"配置不合法，不允许指定其type为clob或blob");
            }
            cb.setDatatypeObj(typeObj);
            if(labelposition!=null&&!labelposition.trim().equals(""))
            {
                labelposition=labelposition.toLowerCase().trim();
                if(!labelposition.equals(ConditionBean.LABELPOSITION_INNER)&&!labelposition.equals(ConditionBean.LABELPOSITION_LEFT)
                        &&!labelposition.equals(ConditionBean.LABELPOSITION_RIGHT))
                {
                    throw new WabacusConfigLoadingException("报表"+cb.getReportBean().getPath()+"配置的查询条件"+cb.getName()+"中labelposition配置值无效");
                }
            }else
            {
                labelposition=Config.getInstance().getSystemConfigValue("default-condition-labelposition",ConditionBean.LABELPOSITION_INNER);
                if(!labelposition.equals(ConditionBean.LABELPOSITION_INNER)&&!labelposition.equals(ConditionBean.LABELPOSITION_LEFT)
                        &&!labelposition.equals(ConditionBean.LABELPOSITION_RIGHT))
                {
                    throw new WabacusConfigLoadingException("在wabacus.cfg.xml中指定的default-condition-labelposition配置值无效");
                }
            }
            cb.setLabelposition(labelposition);
            if(keepkeywords!=null&&keepkeywords.trim().equalsIgnoreCase("true")) cb.setKeepkeywords(true);
            if(hidden!=null)
            {
                hidden=hidden.toLowerCase().trim();
                if(hidden.equals(""))
                {
                    cb.setHidden(false);
                }else if(!hidden.equals("true")&&!hidden.equals("false"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的查询条件"+cb.getName()+"失败，其配置的hidden属性："+hidden
                            +"值不合法，只能是true或false");
                }else
                {
                    cb.setHidden(Boolean.parseBoolean(hidden));
                }
            }
            if(constant!=null)
            {
                constant=constant.toLowerCase().trim();
                if(constant.equals(""))
                {
                    cb.setConstant(false);
                }else if(!constant.equals("true")&&!constant.equals("false"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的查询条件"+cb.getName()+"失败，其配置的constant属性："+constant
                            +"值不合法，只能是true或false");
                }else
                {
                    cb.setConstant(Boolean.parseBoolean(constant));
                }
            }
            if(cb.isConstant())
            {
                XmlElementBean eleValueBeanTmp=eleConditionBeanTmp.getChildElementByName("value");
                if(eleValueBeanTmp==null)
                {
                    throw new WabacusConfigLoadingException("报表"+cb.getReportBean().getPath()+"的查询条件"+cb.getName()+"是常量查询条件，必须配置<value/>并在其中指定常量值");
                }
                String conValueTmp=eleValueBeanTmp.getContent();
                ConditionExpressionBean cebean=new ConditionExpressionBean();
                cebean.setValue(conValueTmp==null?"":conValueTmp.trim());
                cb.setConditionExpression(cebean);
                cb.setHidden(true);
                continue;
            }
            if(source!=null) cb.setSource(source);
            if(br!=null&&br.trim().equalsIgnoreCase("true"))
            {
                cb.setBr(true);
            }
            if(left!=null)
            {
                left=left.trim();
                if(left.equals("")) left="0";
                cb.setLeft(Integer.parseInt(left));
                if(cb.getLeft()<0) cb.setLeft(0-cb.getLeft());
            }
            if(right!=null)
            {
                right=right.trim();
                if(right.equals("")) right="0";
                cb.setRight(Integer.parseInt(right));
                if(cb.getRight()<0) cb.setRight(0-cb.getRight());
            }
            if(belongto!=null) cb.setBelongto(belongto.trim());
            if(cb.isConditionWithInputbox())
            {//需要显示输入框
                String iterator=eleConditionBeanTmp.attributeValue("iterator");
                if(iterator!=null)
                {
                    iterator=iterator.trim();
                    if(iterator.equals("")) iterator="0";
                    cb.setIterator(Integer.parseInt(iterator));
                }
                String innerlogic=eleConditionBeanTmp.attributeValue("innerlogic");
                if(innerlogic!=null)
                {
                    cb.setInnerlogic(innerlogic.trim());
                }
                List<String> lstChildOrders=new ArrayList<String>();//存放各子元素的配置顺序，以便决定它们的显示顺序（主要是<inputbox/>、<columns/>、<values/>三个子标签的顺序）
                cb.setLstChildDisplayOrder(lstChildOrders);
                List<XmlElementBean> lstChildrenElements=eleConditionBeanTmp.getLstChildElements();
                if(lstChildrenElements!=null&&lstChildrenElements.size()>0)
                {
                    for(XmlElementBean xebeanTmp:lstChildrenElements)
                    {
                        if(xebeanTmp.getName().equals("innerlogic"))
                        {
                            if(lstChildOrders.contains("innerlogic"))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它配置多个<innerlogic/>子标签");
                            }
                            if(cb.getInnerlogic()!=null&&!cb.getInnerlogic().trim().equals(""))
                            {//已经在<condition/>中配置了innerlogic属性
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能在<condition/>中即配置innerlogic属性，又配置<innerlogic/>子标签");
                            }
                            lstChildOrders.add("innerlogic");
                            ConditionSelectorBean cilbean=new ConditionSelectorBean(cb,ConditionBean.SELECTTYPE_INNERLOGIC);
                            cilbean.loadConfig(xebeanTmp,"logic");
                            if(cilbean.isEmpty()) continue;
                            cb.setCinnerlogicbean(cilbean);
                        }else if(xebeanTmp.getName().equals("inputbox"))
                        {
                            if(lstChildOrders.contains("inputbox"))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它配置多个<inputbox/>子标签");
                            }
                            lstChildOrders.add("inputbox");
                            AbsInputBox box=Config.getInstance().getInputBoxTypeByName(xebeanTmp.attributeValue("type"));
                            box=(AbsInputBox)box.clone(cb);
                            box.loadInputBoxConfig(xebeanTmp);
                            cb.setInputbox(box);
                        }else if(xebeanTmp.getName().equals("columns"))
                        {
                            if(lstChildOrders.contains("columns"))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它配置多个<columns/>子标签");
                            }
                            lstChildOrders.add("columns");
                            ConditionSelectorBean csbean=new ConditionSelectorBean(cb,ConditionBean.SELECTORTYPE_COLUMNS);
                            csbean.loadConfig(xebeanTmp,"column");
                            if(csbean.isEmpty()) continue;
                            cb.setCcolumnsbean(csbean);
                        }else if(xebeanTmp.getName().equals("values"))
                        {
                            if(lstChildOrders.contains("values"))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它配置多个<values/>子标签");
                            }
                            if(cb.getConditionExpression()!=null)
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它同时配置<values/>和<value/>子标签");
                            }
                            lstChildOrders.add("values");
                            ConditionSelectorBean csbean=new ConditionSelectorBean(cb,ConditionBean.SELECTORTYPE_VALUES);
                            csbean.loadConfig(xebeanTmp,"value");
                            if(csbean.isEmpty()) continue;
                            cb.setCvaluesbean(csbean);
                        }else if(xebeanTmp.getName().equals("value"))
                        {
                            if(cb.getConditionExpression()!=null)
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它配置多个<value/>子标签");
                            }
                            if(lstChildOrders.contains("values"))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它同时配置<values/>和<value/>子标签");
                            }
                            Object valueObj=loadConditionValueConfig(cb,xebeanTmp);
                            if(valueObj instanceof ConditionExpressionBean)
                            {
                                cb.setConditionExpression((ConditionExpressionBean)valueObj);
                            }else
                            {//在<value/>中配置了多个<column/>，可能是要提供多个列的选择搜索
                                ConditionSelectorBean cvaluesbean=new ConditionSelectorBean(cb,ConditionBean.SELECTORTYPE_VALUES);
                                cb.setCvaluesbean(cvaluesbean);
                                List<ConditionSelectItemBean> lstValuesBean=new ArrayList<ConditionSelectItemBean>();
                                cvaluesbean.setLstSelectItemBeans(lstValuesBean);
                                ConditionValueSelectItemBean cvbean=new ConditionValueSelectItemBean(cb);
                                cvbean.setLstColumnsBean((List<ConditionSelectItemBean>)valueObj);
                                lstValuesBean.add(cvbean);
                                lstChildOrders.add("values");
                            }
                        }
                    }
                }
                if(!lstChildOrders.contains("inputbox"))
                {//没有配置<inputbox/>，则取默认
                    lstChildOrders.add("inputbox");//如果没有配置<inputbox/>，则将它放在最后显示
                    AbsInputBox box=Config.getInstance().getInputBoxTypeByName(null);
                    box=(AbsInputBox)box.clone(cb);
                    cb.setInputbox(box);
                }
            }else
            {
                XmlElementBean eleValueBean=eleConditionBeanTmp.getChildElementByName("value");
                if(eleValueBean!=null)
                {
                    String expression=eleValueBean.getContent();
                    if(expression!=null&&!expression.trim().equals(""))
                    {
                        ConditionExpressionBean cexpressionBean=new ConditionExpressionBean();
                        cexpressionBean.setValue(expression.trim());
                        cb.setConditionExpression(cexpressionBean);
                    }
                }
            }
            if(onsetvalue!=null) cb.setOnsetvalueMethod(onsetvalue.trim());
            if(ongetvalue!=null) cb.setOngetvalueMethod(ongetvalue.trim());
            LoadExtendConfigManager.loadAfterExtendConfigForReporttype(cb,lstEleConditionBeans);
        }
    }

    public static Object loadConditionValueConfig(ConditionBean cbean,XmlElementBean eleValuebean)
    {
        List<XmlElementBean> lstEleColumnsElement=eleValuebean.getLstChildElementsByName("column");
        String conditionexpression=eleValuebean.getContent();
        if(conditionexpression!=null&&!conditionexpression.trim().equals("")&&lstEleColumnsElement!=null&&lstEleColumnsElement.size()>0)
        {
            throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()
                    +"的<condition/>下<values/>的子标签<value/>失败，不能同时为它配置标签内容和<column/>子标签");
        }
        if(conditionexpression!=null&&!conditionexpression.trim().equals(""))
        {//直接配置条件表达式
            ConditionExpressionBean cebean=new ConditionExpressionBean();
            cebean.setValue(conditionexpression.trim());
            return cebean;
        }else if(lstEleColumnsElement!=null&&lstEleColumnsElement.size()>0)
        {//在<value/>下配置了多个<column/>
            List<ConditionSelectItemBean> lstColumnBeans=new ArrayList<ConditionSelectItemBean>();
            List<String> lstColIdsTmp=new ArrayList<String>();
            for(XmlElementBean xebeanTmp:lstEleColumnsElement)
            {
                String refid=xebeanTmp.attributeValue("refid");
                String expression=xebeanTmp.getContent();
                if(refid==null||refid.trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()
                            +"的<condition/>下<values/>的子标签<value/>失败，其下的<column/>子标签配置的refid："+refid+"不能为空");
                }
                refid=refid.trim();
                if(lstColIdsTmp.contains(refid))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()
                            +"的<condition/>下<values/>的子标签<value/>失败，其下的<column/>子标签配置的refid："+refid+"存在重复");
                }
                lstColIdsTmp.add(refid);
                ConditionSelectItemBean ccbean=new ConditionSelectItemBean(cbean);
                ccbean.setId(refid);
                if(expression==null||expression.trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()+"的<condition/>下<values/>的子标签<value/>失败，其下refid为"
                            +refid+"的<column/>子标签没有配置条件表达式");
                }
                ConditionExpressionBean cexpressionBean=new ConditionExpressionBean();
                cexpressionBean.setValue(expression.trim());
                ccbean.setConditionExpression(cexpressionBean);
                lstColumnBeans.add(ccbean);
            }
            return lstColumnBeans;
        }
        return null;
    }

    public static List<ConditionBean> loadConditionsInOtherPlace(XmlElementBean eleParentBean,ReportBean rbean)
    {
        List<XmlElementBean> lstConditionEles=eleParentBean.getLstChildElementsByName("condition");
        if(lstConditionEles==null||lstConditionEles.size()==0) return null;
        List<ConditionBean> lstConditions=new ArrayList<ConditionBean>();
        List<String> lstConNames=new ArrayList<String>();
        for(XmlElementBean eleConBeanTmp:lstConditionEles)
        {
            if(eleConBeanTmp==null) continue;
            ConditionBean cbTmp=loadHiddenConditionConfig(eleConBeanTmp,rbean);
            if(lstConNames.contains(cbTmp.getName()))
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"的查询条件失败，<condition/>的name属性不能重复");
            }
            lstConNames.add(cbTmp.getName());
            if(cbTmp.getConditionExpression()==null||cbTmp.getConditionExpression().getValue()==null
                    ||cbTmp.getConditionExpression().getValue().trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，必须为<value/>配置条件表达式");
            }
            lstConditions.add(cbTmp);
        }
        return lstConditions;
    }

    public static List<ConditionBean> loadCommonDatasetConditios(ReportBean rbean,XmlElementBean eleDatasetConditions)
    {
        if(eleDatasetConditions==null) return null;
        List<ConditionBean> lstConditionBeans=new ArrayList<ConditionBean>();
        List<XmlElementBean> lstConditionEles=eleDatasetConditions.getLstChildElementsByName("condition");
        if(lstConditionEles!=null&&lstConditionEles.size()>0)
        {//在<tablecondtions/>中配置了条件
            ConditionBean cbTmp;
            for(XmlElementBean eleConBeanTmp:lstConditionEles)
            {
                if(eleConBeanTmp==null) continue;
                cbTmp=loadHiddenConditionConfig(eleConBeanTmp,rbean);
                if(cbTmp==null) continue;
                if(!Tools.isDefineKey("ref",cbTmp.getName())
                        &&(cbTmp.getConditionExpression()==null||cbTmp.getConditionExpression().getValue()==null||cbTmp.getConditionExpression()
                                .getValue().trim().equals("")))
                {//如果没有引用在<sql/>中配置的条件，则必须在<condition/>中指定条件表达式
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"的name为"+cbTmp.getName()
                            +"的<condition/>失败，没有引用<sql/>中的条件时必须为<value/>配置条件表达式");
                }
                lstConditionBeans.add(cbTmp);
            }
        }
        return lstConditionBeans.size()==0?null:lstConditionBeans;
    }
    
    public static ConditionBean loadHiddenConditionConfig(XmlElementBean eleConditionBean,ReportBean rbean)
    {
        if(eleConditionBean==null) return null;
        ConditionBean conbean=new ConditionBean(null);
        String name=eleConditionBean.attributeValue("name");
        String source=eleConditionBean.attributeValue("source");
        String keepkeywords=eleConditionBean.attributeValue("keepkeywords");
        String constant=eleConditionBean.attributeValue("constant");
        name=name==null?"":name.trim();
        if(name.equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"的<condition/>失败，name属性不能为空");
        }
        conbean.setName(name);
        IDataType typeObj=ConfigLoadAssistant.loadDataType(eleConditionBean);
        if((typeObj instanceof BlobType)||(typeObj instanceof ClobType))
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"的name为"+name+"的<condition/>配置不合法，不允许指定其type为clob或blob");
        }
        conbean.setDatatypeObj(typeObj);
        if(source!=null)
        {
            conbean.setSource(source.trim());
        }
        conbean.setHidden(true);
        if(constant!=null)
        {
            conbean.setConstant(constant.trim().equalsIgnoreCase("true"));
        }
        if(keepkeywords!=null)
        {
            conbean.setKeepkeywords(keepkeywords.trim().equalsIgnoreCase("true"));
        }
        XmlElementBean eleConValue=eleConditionBean.getChildElementByName("value");
        if(eleConValue!=null)
        {
            String value=eleConValue.getContent();
            if(value!=null&&!value.trim().equals(""))
            {
                ConditionExpressionBean cebeanTmp=new ConditionExpressionBean();
                cebeanTmp.setValue(value.trim());
                conbean.setConditionExpression(cebeanTmp);
            }
        }
        return conbean;
    }

    private static void loadDisplayConfig(DisplayBean dbean,XmlElementBean eleDisplayBean)
    {
        List<XmlElementBean> lstEleDisplayBeans=new ArrayList<XmlElementBean>();
        lstEleDisplayBeans.add(eleDisplayBean);
        lstEleDisplayBeans.addAll(ConfigLoadAssistant.getInstance().getRefElements(eleDisplayBean.attributeValue("ref"),"display",null,
                dbean.getReportBean()));
        LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(dbean,lstEleDisplayBeans);
        Map<String,String> mDisplayProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleDisplayBeans,
                new String[] { "dataheader", "pagecolselect","dataexportcolselect","colselectwidth", "colselectmaxheight","colselectlabelposition", "labelstyleproperty", "valuestyleproperty" });
        String dataheader=mDisplayProperties.get("dataheader");
        String pagecolselect=mDisplayProperties.get("pagecolselect");
        String dataexportcolselect=mDisplayProperties.get("dataexportcolselect");
        String colselectwidth=mDisplayProperties.get("colselectwidth");//列选择窗口的宽度
        String colselectmaxheight=mDisplayProperties.get("colselectmaxheight");
        String labelstyleproperty=mDisplayProperties.get("labelstyleproperty");
        String valuestyleproperty=mDisplayProperties.get("valuestyleproperty");
        String colselectlabelposition=mDisplayProperties.get("colselectlabelposition");
        if(dataheader!=null)
        {
            dbean.setDataheader(Config.getInstance().getResourceString(null,dbean.getPageBean(),dataheader,true));
        }
        boolean isListReportType=Config.getInstance().getReportType(dbean.getReportBean().getType()) instanceof AbsListReportType;
        if(Tools.isEmpty(pagecolselect))
        {
            pagecolselect=Config.getInstance().getSystemConfigValue(
                    isListReportType?"default-listreport-pagecolselect":"default-detailreport-pagecolselect","");
        }
        dbean.setPageColselect("true".equalsIgnoreCase(pagecolselect.trim()));
        if(Tools.isEmpty(dataexportcolselect))
        {
            dataexportcolselect=Config.getInstance().getSystemConfigValue(
                    isListReportType?"default-listreport-dataexportcolselect":"default-detailreport-dataexportcolselect","");
        }
        dbean.setDataexportColselect("true".equalsIgnoreCase(dataexportcolselect.trim()));
        if(colselectwidth==null||colselectwidth.trim().equals(""))
        {
            colselectwidth=Config.getInstance().getSystemConfigValue("default-colselect-width","");
        }
        if(colselectwidth!=null) dbean.setColselectwidth(Tools.getWidthHeightIntValue(colselectwidth.trim()));
        if(colselectmaxheight!=null) dbean.setColselectmaxheight(Tools.getWidthHeightIntValue(colselectmaxheight.trim()));
        if(labelstyleproperty!=null) dbean.setLabelstyleproperty(labelstyleproperty.trim(),false);
        if(valuestyleproperty!=null) dbean.setValuestyleproperty(valuestyleproperty.trim(),false);
        if(colselectlabelposition!=null) dbean.setColselectlabelposition(colselectlabelposition.toLowerCase().trim());
        loadColInfo(lstEleDisplayBeans,dbean);
        LoadExtendConfigManager.loadAfterExtendConfigForReporttype(dbean,lstEleDisplayBeans);
    }

    private static void loadColInfo(List<XmlElementBean> lstEleDisplayBeans,DisplayBean dbean)
    {
        List<XmlElementBean> lstAllEleCols=new ArrayList<XmlElementBean>();
        getAllEleCols(lstEleDisplayBeans,lstAllEleCols,dbean.getReportBean());//获取到所有要显示的<col/>配置
        List<ColBean> lstColBeans=new ArrayList<ColBean>();
        for(XmlElementBean eleColBeanTmp:lstAllEleCols)
        {
            if(eleColBeanTmp!=null) lstColBeans.add(loadColConfig(eleColBeanTmp,dbean));
        }
        dbean.setLstCols(lstColBeans);
    }

    private static void getAllEleCols(List<XmlElementBean> lstEleDisplayBeans,List<XmlElementBean> lstAllEleCols,ReportBean rbean)
    {
        if(lstEleDisplayBeans==null||lstEleDisplayBeans.size()==0) return;
        List<XmlElementBean> lstTmps;
        for(XmlElementBean eleDisplayBeanTmp:lstEleDisplayBeans)
        {//将所有被引用的<display/>标签中的<col/>子标签放入进来加载
            lstTmps=eleDisplayBeanTmp.getLstChildElements();
            if(lstTmps==null||lstTmps.size()==0) continue;
            for(XmlElementBean eleChilds:lstTmps)
            {
                if("col".equals(eleChilds.getName()))
                {
                    lstAllEleCols.add(eleChilds);
                }else if("ref".equals(eleChilds.getName()))
                {
                    getAllEleCols(ConfigLoadAssistant.getInstance().getRefElements(eleChilds.attributeValue("key"),"display",null,rbean),
                            lstAllEleCols,rbean);
                }
            }
        }
    }

    public static ColBean loadColConfig(XmlElementBean eleColBean,DisplayBean dbean)
    {
        if(eleColBean==null) return null;
        ColBean cb=new ColBean(dbean);
        cb.setElementBean(eleColBean);
        String column=eleColBean.attributeValue("column");
        if(column==null||column.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+dbean.getReportBean().getPath()+"失败，在<col/>中必须配置column属性");
        }
        column=column.trim();
        int idx=column.indexOf(".");
        if(idx>0)
        {
            String datasetids=column.substring(0,idx).trim();
            List<String> lstDatasetids=new ArrayList<String>();
            if(datasetids.startsWith("(")&&datasetids.endsWith(")"))
            {//需要匹配多个<dataset/>下面的<value/>
                datasetids=datasetids.substring(1,datasetids.length()-1).trim();
                lstDatasetids.addAll(Tools.parseStringToList(datasetids,";",false));
            }else
            {
                if(!datasetids.equals("")) lstDatasetids.add(datasetids);
            }
            cb.setLstDatasetValueids(lstDatasetids.size()==0?null:lstDatasetids);
            column=column.substring(idx+1).trim();
        }
        cb.setColumn(column);
        column=cb.getColumn();
        String property=eleColBean.attributeValue("property");
        if(property==null||property.trim().equals(""))
        {
            if(cb.isNonFromDbCol())
            {//如果当前列的数据不是从DB中获取，则必须配置property属性值
                throw new WabacusConfigLoadingException("加载报表"+dbean.getReportBean().getPath()+"失败，对于column配置为"+Consts_Private.NON_FROMDB
                        +"的列，必须配置其property属性");
            }
            property=column;
        }
        cb.setProperty(property.trim());

        List<XmlElementBean> lstEleColBeans=new ArrayList<XmlElementBean>();
        lstEleColBeans.add(eleColBean);
        LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(cb,lstEleColBeans);
        String label=eleColBean.attributeValue("label");
        if(label!=null)
        {
            cb.setLabel(Config.getInstance().getResourceString(null,dbean.getPageBean(),label,true));
        }
        cb.setDatatypeObj(ConfigLoadAssistant.loadDataType(eleColBean));
        String displaytype=eleColBean.attributeValue("displaytype");
        String labelstyleproperty=eleColBean.attributeValue("labelstyleproperty");
        String valuestyleproperty=eleColBean.attributeValue("valuestyleproperty");
        if(valuestyleproperty==null) valuestyleproperty="";
        if(labelstyleproperty==null) labelstyleproperty="";
        cb.setValuestyleproperty(valuestyleproperty,false);
        cb.setLabelstyleproperty(labelstyleproperty,false);
        //        if(printlabelstyleproperty!=null)
        //        {
        String plainexcelwidth=eleColBean.attributeValue("plainexcelwidth");
        if(plainexcelwidth!=null)
        {
            if(plainexcelwidth.trim().equals(""))
            {
                cb.setPlainexcelwidth(0f);
            }else
            {
                cb.setPlainexcelwidth(Float.parseFloat(plainexcelwidth.trim()));
            }
        }
        String pdfwidth=eleColBean.attributeValue("pdfwidth");
        if(pdfwidth!=null)
        {
            if(pdfwidth.trim().equals(""))
            {
                cb.setPdfwidth(0f);
            }else
            {
                cb.setPdfwidth(Float.parseFloat(pdfwidth.trim()));
            }
        }
        String printwidth=eleColBean.attributeValue("printwidth");
        if(printwidth!=null)
        {
            cb.setPrintwidth(printwidth.trim());
        }
        //            cb.setFormatProperty(format.trim());
        if(labelstyleproperty!=null&&!labelstyleproperty.trim().equals(""))
        {
            cb.setLabelalign(Tools.getPropertyValueByName("align",labelstyleproperty,true));
        }
        if(valuestyleproperty!=null&&!valuestyleproperty.trim().equals(""))
        {
            cb.setValuealign(Tools.getPropertyValueByName("align",valuestyleproperty,true));
        }
        if(displaytype!=null) 
        {
            displaytype=displaytype.toLowerCase().trim();
            idx=displaytype.indexOf("|");
            if(idx>=0)
            {
                cb.setDisplaytype(new String[] { displaytype.substring(0,idx).trim(), displaytype.substring(idx+1).trim() });
            }else
            {
                cb.setDisplaytype(new String[] { displaytype, displaytype });
            }
        }
        String tagcontent=eleColBean.getContent();
        if(tagcontent!=null&&!tagcontent.trim().equals(""))
        {
            cb.setTagcontent(Config.getInstance().getResourceString(null,cb.getPageBean(),tagcontent.trim(),false));
        }
        LoadExtendConfigManager.loadAfterExtendConfigForReporttype(cb,lstEleColBeans);
        return cb;
    }

    public static void loadEditableReportConfig(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans,String key)
    {
        EditableReportBean erbean=(EditableReportBean)reportbean.getExtendConfigDataForReportType(key);
        if(erbean==null)
        {
            erbean=new EditableReportBean(reportbean);
            reportbean.setExtendConfigDataForReportType(key,erbean);
        }
        XmlElementBean eleReportBean=lstEleReportBeans.get(0);
        String checkdirtydata=eleReportBean.attributeValue("checkdirtydata");
        if(checkdirtydata!=null)
        {
            checkdirtydata=checkdirtydata.toLowerCase().trim();
            if(checkdirtydata.equals(""))
            {
                erbean.setCheckdirtydata(null);
            }else
            {
                erbean.setCheckdirtydata(!checkdirtydata.equals("false"));
            }
        }
        String savedatatype=eleReportBean.attributeValue("savedatatype");
        if(savedatatype!=null)
        {
            erbean.setSavedatatype(savedatatype.toLowerCase().trim());
        }
    }

    public static void loadEditableColConfig(ColBean colbean,XmlElementBean eleColBean,String reportTypeKey)
    {
        if(eleColBean==null||colbean==null) return;
        colbean.setDisplayNameValueProperty(true);//所有可编辑报表类型都要显示value_name和value
        EditableReportColBean ercbean=(EditableReportColBean)colbean.getExtendConfigDataForReportType(reportTypeKey);
        if(ercbean==null)
        {
            ercbean=new EditableReportColBean(colbean);
            colbean.setExtendConfigDataForReportType(reportTypeKey,ercbean);
        }
        String defaultvalue=eleColBean.attributeValue("defaultvalue");
        if(defaultvalue!=null) ercbean.setDefaultvalue(defaultvalue.trim());
        if(!colbean.isNonValueCol()&&!colbean.isSequenceCol()&&!colbean.isControlCol())
        {
            if(!Consts.COL_DISPLAYTYPE_HIDDEN.equals(colbean.getDisplaytype(true)))
            {
                String updatecol=eleColBean.attributeValue("updatecol");
                if(updatecol!=null) ercbean.setUpdatecolDest(updatecol.trim());
                XmlElementBean eleInputboxBean=eleColBean.getChildElementByName("inputbox");
                if(eleInputboxBean!=null)
                {
                    String inputbox=eleInputboxBean.attributeValue("type");
                    String box_defaultvalue=eleInputboxBean.attributeValue("defaultvalue");
                    AbsInputBox box=Config.getInstance().getInputBoxTypeByName(inputbox);
                    box=(AbsInputBox)box.clone(ercbean);
                    if(box_defaultvalue!=null) box.setDefaultvalue(box_defaultvalue.trim());
                    box.loadInputBoxConfig(eleInputboxBean);
                    ercbean.setInputbox(box);
                }
                String formatemplate=eleColBean.attributeValue("formatemplate");
                if(formatemplate!=null)
                {
                    formatemplate=Config.getInstance().getResourceString(null,colbean.getPageBean(),formatemplate,true);
                    ercbean.setFormatemplate(formatemplate);
                }
            }
            String onsetvalue=eleColBean.attributeValue("onsetvalue");
            if(onsetvalue!=null) ercbean.setOnsetvalueMethod(onsetvalue.trim());
            String ongetvalue=eleColBean.attributeValue("ongetvalue");
            if(ongetvalue!=null) ercbean.setOngetvalueMethod(ongetvalue.trim());
        }
    }

    public static void loadEditableSqlConfig(SqlBean sqlbean,List<XmlElementBean> lstEleSqlBeans,String reportTypeKey)
    {
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(reportTypeKey);
        if(ersqlbean==null)
        {
            ersqlbean=new EditableReportSqlBean(sqlbean);
            sqlbean.setExtendConfigDataForReportType(reportTypeKey,ersqlbean);
        }
        Map<String,String> mSqlProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleSqlBeans,
                new String[] { "beforesave", "aftersave", "savebinding", "deletebinding" });//组装所有<sql/>配置的这些属性 

        String beforeSaveAction=mSqlProperties.get("beforesave");
        if(beforeSaveAction!=null&&!beforeSaveAction.trim().equals(""))
        {
            ersqlbean.setBeforeSaveAction(beforeSaveAction.trim());
        }
        String afterSaveAction=mSqlProperties.get("aftersave");
        if(afterSaveAction!=null&&!afterSaveAction.trim().equals(""))
        {
            ersqlbean.setAfterSaveAction(loadUpdatePostAction(sqlbean,afterSaveAction.trim()));
        }
        String saveBinding=mSqlProperties.get("savebinding");//绑定保存
        if(saveBinding!=null&&!saveBinding.trim().equals(""))
        {
            ersqlbean.setLstSaveBindingReportIds(Tools.parseStringToList(saveBinding,";",false));
        }
        String deletebinding=mSqlProperties.get("deletebinding");
        if(deletebinding!=null&&!deletebinding.trim().equals(""))
        {
            ersqlbean.setLstDeleteBindingReportIds(Tools.parseStringToList(deletebinding,";",false));
        }

        ersqlbean.setInsertbean((EditableReportInsertDataBean)loadEditConfig(ersqlbean,new EditableReportInsertDataBean(ersqlbean),
                getEleSqlUpdateBean(lstEleSqlBeans,"insert")));
        ersqlbean.setUpdatebean((EditableReportUpdateDataBean)loadEditConfig(ersqlbean,new EditableReportUpdateDataBean(ersqlbean),
                getEleSqlUpdateBean(lstEleSqlBeans,"update")));
        ersqlbean.setDeletebean((EditableReportDeleteDataBean)loadEditConfig(ersqlbean,new EditableReportDeleteDataBean(ersqlbean),
                getEleSqlUpdateBean(lstEleSqlBeans,"delete")));
    }

    public static AbsEditableReportEditDataBean loadEditConfig(IEditableReportEditGroupOwnerBean editGroupOwnerBean,
            AbsEditableReportEditDataBean editBean,XmlElementBean eleEditBean)
    {
        if(eleEditBean==null) return null;
        boolean hasValidActionscript=loadAllEditActionsConfig(eleEditBean,editBean);
        if(!hasValidActionscript) return null;//为此<insert/>、<update/>、<delete/>没有配置有效的更新脚本
        String preparedstatement=eleEditBean.attributeValue("preparedstatement");
        if(preparedstatement!=null) editBean.setPreparedStatement(preparedstatement.trim());
        String datasource=eleEditBean.attributeValue("datasource");
        if(datasource!=null) editBean.setDatasource(datasource.trim());
        String refreshparentonsave=eleEditBean.attributeValue("refreshparentonsave");
        if(refreshparentonsave!=null)
        {
            refreshparentonsave=refreshparentonsave.trim();
            if(refreshparentonsave.trim().equals(""))
            {
                editBean.setRefreshParentidOnSave(null);
            }else
            {
                if(refreshparentonsave.indexOf("|")>0)
                {
                    editBean.setResetNavigateInfoOnRefreshParent(refreshparentonsave.substring(refreshparentonsave.indexOf("|")+1).toLowerCase()
                            .trim().equals("true"));
                    editBean.setRefreshParentidOnSave(refreshparentonsave.substring(0,refreshparentonsave.indexOf("|")).trim());
                }else
                {
                    editBean.setResetNavigateInfoOnRefreshParent(false);
                    editBean.setRefreshParentidOnSave(refreshparentonsave.trim());
                }
            }
        }
        loadEditParamsConfig(eleEditBean.getChildElementByName("params"),editBean);
        loadPreconditionsConfig(editBean,eleEditBean.getChildElementByName("preconditions"));
        if(editBean instanceof EditableReportDeleteDataBean)
        {//加载<delete/>
            String confirmessage=eleEditBean.attributeValue("confirmessage");
            if(confirmessage!=null&&!confirmessage.trim().equals(""))
            {
                ((EditableReportDeleteDataBean)editBean).setDeleteConfirmMessage(Config.getInstance().getResourceString(null,
                        editGroupOwnerBean.getReportBean().getPageBean(),confirmessage,true));
            }
        }
        return editBean;
    }

    private static boolean loadAllEditActionsConfig(XmlElementBean eleEditBean,AbsEditableReportEditDataBean editBean)
    {
        boolean hasValidActionscript=false;
        List<XmlElementBean> lstValueBeans=eleEditBean.getLstChildElementsByName("value");
        AbsUpdateActionProvider actionProviderTmp;
        String providerTmp;
        if(lstValueBeans!=null&&lstValueBeans.size()>0)
        {//配置了<value/>子标签
            for(XmlElementBean eleActionBeanTmp:lstValueBeans)
            {
                if(eleActionBeanTmp==null) continue;
                providerTmp=eleActionBeanTmp.attributeValue("provider");
                actionProviderTmp=Config.getInstance().getUpdateDatasetActionProvider(providerTmp);
                if(actionProviderTmp==null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+editBean.getOwner().getReportBean().getPath()+"的更新数据集类型"+providerTmp
                            +"失败，没有在wabacus.cfg.xml中配置此name对应的更新数据集类型");
                }
                try
                {
                    actionProviderTmp=actionProviderTmp.getClass().newInstance();
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("加载报表"+editBean.getOwner().getReportBean().getPath()+"的更新数据集类型"+providerTmp
                            +"失败，无法实例化其对应的provider",e);
                }
                actionProviderTmp.setOwnerUpdateBean(editBean);
                actionProviderTmp.setPrecondition(eleActionBeanTmp.attributeValue("precondition"));
                if(!actionProviderTmp.loadConfig(eleActionBeanTmp)) continue;
                hasValidActionscript=true;
                editBean.addEditActionProvider(actionProviderTmp);
            }
        }else
        {//没有配置<value/>子标签，则直接加载<insert/>、<update/>、<delete/>标签本身的脚本
            actionProviderTmp=Config.getInstance().getUpdateDatasetActionProvider(null);
            if(actionProviderTmp==null)
            {
                throw new WabacusConfigLoadingException("加载报表"+editBean.getOwner().getReportBean().getPath()
                        +"的更新数据集类型失败，没有在wabacus.cfg.xml中配置默认的更新数据集类型，因此必须为更新的<value/>配置provider");
            }
            try
            {
                actionProviderTmp=actionProviderTmp.getClass().newInstance();
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("加载报表"+editBean.getOwner().getReportBean().getPath()+"的更新数据集类型失败，默认的更新数据集provider类无法实例化",e);
            }
            actionProviderTmp.setOwnerUpdateBean(editBean);
            if(actionProviderTmp.loadConfig(eleEditBean))
            {
                editBean.addEditActionProvider(actionProviderTmp);
                hasValidActionscript=true;
            }
        }
        return hasValidActionscript;
    }

    public static XmlElementBean getEleSqlUpdateBean(List<XmlElementBean> lstEleSqlBeans,String updatetype)
    {
        if(lstEleSqlBeans==null||lstEleSqlBeans.size()==0) return null;
        XmlElementBean eleUpdateBeanTmp;
        for(XmlElementBean eleSqlBeanTmp:lstEleSqlBeans)
        {
            eleUpdateBeanTmp=eleSqlBeanTmp.getChildElementByName(updatetype);
            if(eleUpdateBeanTmp!=null) return eleUpdateBeanTmp;
        }
        return null;
    }

    public static String[] loadUpdatePostAction(SqlBean sqlbean,String postaction)
    {
        if(postaction==null||postaction.trim().equals("")) return null;
        postaction=postaction.trim();
        String[] action=new String[2];
        int idx=postaction.indexOf("|");
        if(idx>0)
        {
            action[0]=postaction.substring(0,idx).trim();
            String flag=postaction.substring(idx+1).toLowerCase().trim();
            if(!flag.equals("true")&&!flag.equals("false"))
            {
                throw new WabacusConfigLoadingException("报表"+sqlbean.getReportBean().getPath()+"配置的属性"+postaction+"配置不合法");
            }
            action[1]=flag;
        }else
        {
            action[0]=postaction;
            action[1]="false";
        }
        return action;
    }

    private static void loadEditParamsConfig(XmlElementBean eleEditParamsBean,AbsEditableReportEditDataBean editBean)
    {
        if(eleEditParamsBean==null) return;
        List<XmlElementBean> lstParamEles=eleEditParamsBean.getLstChildElementsByName("param");
        if(lstParamEles==null||lstParamEles.size()==0) return;
        List<EditableReportExternalValueBean> lstExternalValues=new ArrayList<EditableReportExternalValueBean>();
        for(XmlElementBean eleParamBeanTmp:lstParamEles)
        {
            String name=eleParamBeanTmp.attributeValue("name");
            String value=eleParamBeanTmp.attributeValue("value");
            if(name==null||name.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载<params/>的<value/>失败，name属性不能为空");
            }
            value=value==null?"":value.trim();

            EditableReportExternalValueBean valueBean=new EditableReportExternalValueBean(editBean);
            valueBean.setName(name.trim());
            valueBean.setValue(value);
            if(Tools.isDefineKey("@",value)||Tools.isDefineKey("#",value))
            {
                String datatype=eleParamBeanTmp.attributeValue("datatype");
                if(datatype==null||datatype.trim().equals(""))
                {
                    valueBean.setTypeObj(null);
                }else
                {//配置了自己的类型，则使用新的类型覆盖掉源类型
                    valueBean.setTypeObj(ConfigLoadAssistant.loadDataType(eleParamBeanTmp));
                }
            }else
            {
                valueBean.setTypeObj(ConfigLoadAssistant.loadDataType(eleParamBeanTmp));
            }
            if(value.trim().equals("now{}"))
            {
                if(!(valueBean.getTypeObj() instanceof AbsDateTimeType))
                {
                    throw new WabacusConfigLoadingException("配置为取当前时间（now()）的参数值的数据类型必须配置为日期类型");
                }
            }
            lstExternalValues.add(valueBean);
        }
        if(lstExternalValues.size()>0) editBean.setLstExternalValues(lstExternalValues);
    }

    private static void loadPreconditionsConfig(AbsEditableReportEditDataBean editBean,XmlElementBean elePreconditions)
    {
        if(elePreconditions==null) return;
        List<XmlElementBean> lstPreconditions=elePreconditions.getLstChildElementsByName("precondition");
        if(lstPreconditions==null||lstPreconditions.size()==0) return;
        Map<String,String> mPreconditions=new HashMap<String,String>();
        String nameTmp, valueTmp;
        for(XmlElementBean eleTmp:lstPreconditions)
        {
            nameTmp=eleTmp.attributeValue("name");
            valueTmp=eleTmp.attributeValue("value");
            if(Tools.isEmpty(nameTmp))
            {
                throw new WabacusConfigLoadingException("加载报表"+editBean.getOwner().getReportBean().getPath()
                        +"的<preconditions/>失败，所有<precondition/>都必须配置name属性");
            }
            if(mPreconditions.containsKey(nameTmp))
            {
                throw new WabacusConfigLoadingException("加载报表"+editBean.getOwner().getReportBean().getPath()
                        +"的<preconditions/>失败，<precondition/>配置的name属性"+nameTmp+"存在重复");
            }
            if(Tools.isEmpty(valueTmp))
            {
                throw new WabacusConfigLoadingException("加载报表"+editBean.getOwner().getReportBean().getPath()
                        +"的<preconditions/>失败，所有<precondition/>都必须配置value属性");
            }
            mPreconditions.put(nameTmp,valueTmp);
        }
        editBean.setMPreconditionExpressions(mPreconditions);
    }
    
    public static void doEditableReportTypePostLoad(ReportBean reportbean,String reportTypeKey)
    {
        DisplayBean dbean=reportbean.getDbean();
        if(dbean!=null) processAllUpdateCol(dbean,reportTypeKey);
        SqlBean sqlbean=reportbean.getSbean();
        if(sqlbean==null) return;
        ButtonsBean bbeans=reportbean.getButtonsBean();
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(EditableReportSqlBean.class);
        if(ersqlbean==null)
        {
            if(bbeans!=null)
            {
                bbeans.removeAllCertainTypeButtons(UpdateButton.class);
                bbeans.removeAllCertainTypeButtons(AddButton.class);
                bbeans.removeAllCertainTypeButtons(DeleteButton.class);
                bbeans.removeAllCertainTypeButtons(SaveButton.class);
                bbeans.removeAllCertainTypeButtons(CancelButton.class);
                bbeans.removeAllCertainTypeButtons(ResetButton.class);
            }
            return;
        }
        String systemjsfile="/webresources/script/wabacus_editsystem.js";
        //            systemjsfile="/webresources/script/wabacus_editsystem.js";
        //        }else
        //            systemjsfile="/webresources/script/"+encode.toLowerCase()+"/wabacus_editsystem.js";
        systemjsfile=Tools.replaceAll(Config.webroot+"/"+systemjsfile,"//","/");
        reportbean.getPageBean().addMyJavascriptFile(systemjsfile,0);
        List<ReportBean> lstBindingReportBeans=getLstBindedReportBeans(reportbean,ersqlbean.getLstSaveBindingReportIds(),"savebinding",true);
        if(lstBindingReportBeans!=null&&lstBindingReportBeans.size()>0)
        {
            ersqlbean.setLstSaveBindingReportBeans(lstBindingReportBeans);
        }
        ersqlbean.setLstSaveBindingReportIds(null);//清空，因为运行时用不上
        lstBindingReportBeans=getLstBindedReportBeans(reportbean,ersqlbean.getLstDeleteBindingReportIds(),"deletebinding",true);
        if(lstBindingReportBeans!=null&&lstBindingReportBeans.size()>0)
        {
            ersqlbean.setLstDeleteBindingReportBeans(lstBindingReportBeans);
        }
        ersqlbean.setLstDeleteBindingReportIds(null);

        if(ersqlbean.getInsertbean()!=null)
        {
            int result=ersqlbean.getInsertbean().parseActionscripts(reportTypeKey);
            if(result<=0) ersqlbean.setInsertbean(null);
        }
        if(ersqlbean.getUpdatebean()!=null)
        {
            int result=ersqlbean.getUpdatebean().parseActionscripts(reportTypeKey);
            if(result<=0) ersqlbean.setUpdatebean(null);
        }
        if(ersqlbean.getDeletebean()!=null)
        {
            int result=ersqlbean.getDeletebean().parseActionscripts(reportTypeKey);
            if(result<=0) ersqlbean.setDeletebean(null);
        }
    }

    public static List<ReportBean> getLstBindedReportBeans(ReportBean reportbean,List<String> lstBindedReportIds,String bindtype,
            boolean isAutoHideBindReportButton)
    {
        if(lstBindedReportIds==null||lstBindedReportIds.size()==0) return null;
        List<ReportBean> lstBindedReportBeans=new ArrayList<ReportBean>();
        List<String> lstReportIdsTmp=new ArrayList<String>();
        ReportBean rbBindedTmp;
        boolean isContainsMe=false;
        for(String bindedReportidTmp:lstBindedReportIds)
        {
            if(bindedReportidTmp==null||bindedReportidTmp.trim().equals("")||lstReportIdsTmp.contains(bindedReportidTmp))
            {
                continue;
            }
            lstReportIdsTmp.add(bindedReportidTmp);
            if(bindedReportidTmp.equals(reportbean.getId()))
            {
                lstBindedReportBeans.add(reportbean);
                isContainsMe=true;
            }else
            {
                rbBindedTmp=reportbean.getPageBean().getReportChild(bindedReportidTmp,true);
                if(rbBindedTmp==null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，通过"+bindtype+"属性绑定操作的报表"+bindedReportidTmp+"不存在");
                }
                EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbBindedTmp.getSbean().getExtendConfigDataForReportType(
                        EditableReportSqlBean.class);
                if(ersqlbean==null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，通过"+bindtype+"属性绑定操作的报表"+bindedReportidTmp
                            +"不是可编辑报表类型");
                }
                if(isAutoHideBindReportButton)
                {
                    if(bindtype.equals("savebinding"))
                    {
                        Config.getInstance().authorize(rbBindedTmp.getGuid(),Consts.BUTTON_PART,"type{"+Consts_Private.SAVE_BUTTON+"}",
                                Consts.PERMISSION_TYPE_DISPLAY,"false");
                    }else if(bindtype.equals("deletebinding"))
                    {
                        Config.getInstance().authorize(rbBindedTmp.getGuid(),Consts.BUTTON_PART,"type{"+Consts_Private.DELETE_BUTTON+"}",
                                Consts.PERMISSION_TYPE_DISPLAY,"false");
                    }
                }
                lstBindedReportBeans.add(rbBindedTmp);
            }
        }
        if(lstBindedReportBeans.size()==0) return null;
        if(lstBindedReportBeans.size()==1&&isContainsMe) return null;
        if(!isContainsMe) lstBindedReportBeans.add(0,reportbean);//不包含自己，则将自己放在第一个位置，因为在绑定保存时，本报表默认是第一个保存
        return lstBindedReportBeans;
    }

    private static void processAllUpdateCol(DisplayBean dbean,String reportTypeKey)
    {
        List<ColBean> lstCols=dbean.getLstCols();
        if(lstCols==null||lstCols.size()==0) return;
        for(ColBean cbean:lstCols)
        {
            if(cbean==null||Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(true))) continue;
            if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) continue;
            if(cbean.isNonValueCol()||cbean.isSequenceCol()||cbean.isControlCol()) continue;
            ColBean cbUpdateDest=cbean.getUpdateColBeanDest(false);
            if(cbUpdateDest==null) continue;
            EditableReportColBean ercbeanUpdateDestTmp=(EditableReportColBean)cbUpdateDest.getExtendConfigDataForReportType(reportTypeKey);
            if(ercbeanUpdateDestTmp==null)
            {
                ercbeanUpdateDestTmp=new EditableReportColBean(cbUpdateDest);
                cbUpdateDest.setExtendConfigDataForReportType(reportTypeKey,ercbeanUpdateDestTmp);
            }else if(ercbeanUpdateDestTmp.getUpdatecolSrc()!=null&&!ercbeanUpdateDestTmp.getUpdatecolSrc().trim().equals(""))
            {
                throw new WabacusConfigLoadingException("报表"+dbean.getReportBean().getPath()+"的column属性为"+cbUpdateDest.getColumn()
                        +"的<col/>被多个<col/>通过updatecol属性引用");
            }
            ercbeanUpdateDestTmp.setUpdatecolSrc(cbean.getProperty());//将property设置到被引用的扩展配置对象中，以便下次能通过它取到被哪个<col/>引用到
        }
    }

    public static void doEditableReportTypePostLoadFinally(ReportBean reportbean,String reportTypeKey)
    {
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)reportbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
        if(ersqlbean==null) return;
        if(ersqlbean.getInsertbean()!=null) ersqlbean.getInsertbean().doPostLoadFinally();
        if(ersqlbean.getUpdatebean()!=null) ersqlbean.getUpdatebean().doPostLoadFinally();
        if(ersqlbean.getDeletebean()!=null) ersqlbean.getDeletebean().doPostLoadFinally();
    }
}
