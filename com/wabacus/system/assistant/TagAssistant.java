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
package com.wabacus.system.assistant;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.dataexport.DataExportLocalStroageBean;
import com.wabacus.config.other.JavascriptFileBean;
import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.AddButton;
import com.wabacus.system.buttons.BackButton;
import com.wabacus.system.buttons.CancelButton;
import com.wabacus.system.buttons.DataExportButton;
import com.wabacus.system.buttons.DataImportButton;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.buttons.ResetButton;
import com.wabacus.system.buttons.SaveButton;
import com.wabacus.system.buttons.SearchButton;
import com.wabacus.system.buttons.UpdateButton;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.system.component.application.report.EditableDetailReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsDetailReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class TagAssistant
{
    private static Log log=LogFactory.getLog(TagAssistant.class);

    private final static TagAssistant instance=new TagAssistant();

    protected TagAssistant()
    {}

    public static TagAssistant getInstance()
    {
        return instance;
    }

    public String showTopSpace(String spaceheight)
    {
        StringBuffer resultBuf=new StringBuffer();
        if(spaceheight!=null&&!spaceheight.trim().equals(""))
        {
            resultBuf.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"MARGIN:0;width:100%;\">");
            resultBuf.append("<tr><td height=\""+spaceheight+"\">&nbsp;</td></tr></table>");
        }
        return resultBuf.toString();
    }

    public String showConditionBox(ReportRequest rrequest,ConditionBean cbean,String iteratorindex,String dynstyleproperty)
    {
        StringBuilder resultBuf=new StringBuilder();
        ReportBean rbean=cbean.getReportBean();
        if(iteratorindex!=null&&!iteratorindex.trim().equals(""))
        {//在<wx:searchbox/>标签中指定了iteratorindex属性
            int iiteratorindex=Integer.parseInt(iteratorindex.trim());
            if(cbean.getIterator()<=1&&iiteratorindex>=0)
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"的查询条件"+cbean.getName()+"的iterator属性为"+cbean.getIterator()
                        +"，显示时不能指定iteratorindex为大于等于0的数，即只能为它显示一套输入框");
            }else if(cbean.getIterator()>1&&iiteratorindex<0)
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"的查询条件"+cbean.getName()+"的iterator属性为"+cbean.getIterator()
                        +"，即显示多套输入框，因此必须指定当前显示的下标iteratorindex，且不能超过iterator配置值："+cbean.getIterator());
            }
            resultBuf.append(cbean.getDisplayString(rrequest,dynstyleproperty,iiteratorindex));
        }else
        {
            if(cbean.getIterator()>1)
            {
                String conDisplayTmp;
                for(int i=0;i<cbean.getIterator();i++)
                {
                    conDisplayTmp=cbean.getDisplayString(rrequest,dynstyleproperty,i);
                    if(conDisplayTmp==null||conDisplayTmp.trim().equals("")) continue;
                    resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(cbean.getLeft()));
                    resultBuf.append(conDisplayTmp);
                    resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(cbean.getRight()));
                }
            }else
            {
                resultBuf.append(cbean.getDisplayString(rrequest,dynstyleproperty,-1));
            }
        }
        return resultBuf.toString();
    }

    public String getTitleDisplayValue(AbsReportType reportTypeObj,String type,String top)
    {
        ReportBean rbean=reportTypeObj.getReportBean();
        ReportRequest rrequest=reportTypeObj.getReportRequest();
        if(type==null||type.trim().equals(""))
        {
            String titlestr=reportTypeObj.showTitle();
            if(titlestr==null||titlestr.trim().equals("")) return "";
            StringBuffer resultBuf=new StringBuffer();
            resultBuf.append(TagAssistant.getInstance().showTopSpace(top));
            resultBuf.append(titlestr);
            return resultBuf.toString();
        }else if(type.equals("title"))
        {//只显示标题部分
            return Tools.htmlEncode(rbean.getTitle(rrequest));
        }else if(type.equals("subtitle"))
        {
            return Tools.htmlEncode(rbean.getSubtitle(rrequest));
        }else
        {
            throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，其所用的静态模板中为<wx:title/>指定的type属性："+type+"不合法，只能指定为空、title、subtitle三个值之一");
        }
    }

    public String getReportDataPartDisplayValue(AbsReportType reportTypeObj,Map<String,String> attributes)
    {
        boolean isShowlabel=true, isShowdata=true;
        String top=null, styleproperty=null;
        int irowidx=-2;
        ColBean cbean=null;
        if(attributes!=null)
        {
            top=attributes.get("top");
            String col=attributes.get("col");
            if(col!=null&&!col.trim().equals(""))
            {
                cbean=reportTypeObj.getReportBean().getDbean().getColBeanByColProperty(col);
                if(cbean==null)
                    throw new WabacusRuntimeException("显示报表"+reportTypeObj.getReportBean().getPath()+"失败，没有取到property(或column)属性为"+col+"的<col/>");
            }
            String showlabel=attributes.get("showlabel");
            String showdata=attributes.get("showdata");
            showlabel=showlabel==null||showlabel.trim().equals("")?"true":showlabel.toLowerCase().trim();
            showdata=showdata==null||showdata.trim().equals("")?"true":showdata.toLowerCase().trim();
            if(!showlabel.equals("true")&&!showlabel.equals("false"))
            {
                throw new WabacusRuntimeException("showlabel属性只能配置为true或false");
            }
            if(!showdata.equals("true")&&!showdata.equals("false"))
            {
                throw new WabacusRuntimeException("showdata属性只能配置为true或false");
            }
            isShowlabel=Boolean.parseBoolean(showlabel);
            isShowdata=Boolean.parseBoolean(showdata);
            String rowidx=attributes.get("rowidx");
            rowidx=rowidx==null?"":rowidx.trim();
            if(!rowidx.equals(""))
            {
                try
                {
                    irowidx=Integer.parseInt(rowidx);
                }catch(NumberFormatException e)
                {
                    throw new WabacusRuntimeException("显示报表"+reportTypeObj.getReportBean().getPath()+"失败，传入<wx:data/>的rowidx："+rowidx+"不是有效数字",e);
                }
            }
            styleproperty=attributes.get("styleproperty");
        }
        if(cbean==null)
        {
            StringBuilder resultBuf=new StringBuilder();
            resultBuf.append(showTopSpace(top));
            if(isShowlabel&&isShowdata)
            {
                reportTypeObj.showReportData(resultBuf);
            }else
            {
                if(reportTypeObj instanceof AbsDetailReportType)
                {
                    throw new WabacusRuntimeException("显示报表"+reportTypeObj.getReportBean().getPath()
                            +"失败，对于细览报表，如果当前<wx:data/>标签是显示整个报表数据，则不能将showdata或showlabel配置为false");
                }
                AbsListReportType listReportObj=(AbsListReportType)reportTypeObj;
                if(isShowlabel)
                {//只显示头部
                    listReportObj.showReportData(false,resultBuf);
                }else
                {
                    listReportObj.showReportData(true,resultBuf);
                }
            }
            return resultBuf.toString();
        }
        if(reportTypeObj instanceof AbsListReportType)
        {
            if(irowidx<-1) return ((AbsListReportType)reportTypeObj).showColData(cbean,-2);
            return ((AbsListReportType)reportTypeObj).showColData(cbean,irowidx);
        }else if(reportTypeObj instanceof AbsDetailReportType)
        {
            if(isShowdata)
            {
                return ((AbsDetailReportType)reportTypeObj).showColData(cbean,true,styleproperty);
            }else if(isShowlabel)
            {
                return ((AbsDetailReportType)reportTypeObj).showColData(cbean,false,null);
            }else
            {
                throw new WabacusRuntimeException("显示细览报表的某列时，必须指定showdata和showlabel其中一个为true");
            }
        }else
        {
            throw new WabacusRuntimeException("报表"+reportTypeObj.getReportBean().getPath()+"的报表类型"+reportTypeObj.getReportBean().getType()
                    +"不支持显示某一列的数据");
        }
    }

    public String getButtonDisplayValue(AbsComponentType componentObj,Map<String,String> attributes)
    {
        IComponentConfigBean ccbean=componentObj.getConfigBean();
        ReportRequest rrequest=componentObj.getReportRequest();
        String type=attributes.get("type");
        String name=attributes.get("name");
        String label=attributes.get("label");
        type=type==null?"":type.trim().toLowerCase();
        name=name==null?"":name.trim();
        if(!name.equals("")&&!type.equals(""))
        {
            log.warn("在<wx:button/>显示组件"+ccbean.getPath()+"按钮时，如果同时指定name属性和type属性时，只有name属性有效");
        }else if(name.equals("")&&type.equals(""))
        {
            throw new WabacusRuntimeException("在<wx:button/>显示组件"+ccbean.getPath()+"按钮时，必须通过type属性或name属性指定要显示的按钮");
        }
        if(name!=null&&!name.equals(""))
        {//按<button/>的name属性来显示相应的按钮
            if(ccbean.getButtonsBean()==null) return "";
            AbsButtonType buttonObj=ccbean.getButtonsBean().getButtonByName(name);
            if(buttonObj==null)
            {
                throw new WabacusRuntimeException("在组件"+ccbean.getPath()+"中没有配置name属性为"+name+"的<button/>");
            }
            return showButton(ccbean,rrequest,buttonObj,label);
        }
        if(Consts.lstDataExportTypes.contains(type))
        {
            String componentids=attributes.get("componentids");
            if(componentids==null||componentids.trim().equals("")) componentids=ccbean.getId();
            if(componentids.equals(ccbean.getId()))
            {//本导出按钮只是导出当前报表的数据
                List<AbsButtonType> lstDataExportButtons=null;
                if(ccbean.getButtonsBean()!=null) lstDataExportButtons=ccbean.getButtonsBean().getLstDataExportTypeButtons(type);
                if(lstDataExportButtons!=null&&lstDataExportButtons.size()>0)
                {
                    return ComponentAssistant.getInstance().showButton(ccbean,lstDataExportButtons.get(0),rrequest,null,label);
                }
            }
            AbsButtonType buttonObj=Config.getInstance().getResourceButton(rrequest,ccbean.getPageBean(),
                    Consts.M_DATAEXPORT_DEFAULTBUTTONS.get(type),DataExportButton.class);
            return ((DataExportButton)buttonObj).showButtonTag(rrequest,type,componentids,label,createDataExportStroageBean(attributes));
        }else if(Consts.M_PRINT_DEFAULTBUTTONS.containsKey(type))
        {
            List<AbsButtonType> lstPrintButtons=null;
            if(componentObj.getConfigBean().getButtonsBean()!=null)
                lstPrintButtons=componentObj.getConfigBean().getButtonsBean().getLstPrintTypeButtons(type);
            if(lstPrintButtons==null||lstPrintButtons.size()==0) return "";
            return lstPrintButtons.get(0).showButton(rrequest,null);
        }else if(type.equals(Consts_Private.FORWARDWITHBACK_BUTTON))
        {
            String pageurl=attributes.get("pageurl");
            if(pageurl==null||pageurl.trim().equals(""))
            {
                throw new WabacusRuntimeException("显示组件"+ccbean.getPath()+"跳转按钮失败，这种按钮类型必定指定其pageurl属性为跳转的目标页面");
            }
            String beforecallback=attributes.get("beforecallback");
            beforecallback=beforecallback==null||beforecallback.trim().equals("")?null:beforecallback.trim();
            if(!rrequest.checkPermission(ccbean.getPageBean().getId(),Consts.BUTTON_PART,"type{"+Consts_Private.FORWARDWITHBACK_BUTTON+"}",
                    Consts.PERMISSION_TYPE_DISPLAY)) return "";//没有显示权限
            if(label==null||label.trim().equals("")) label="<input type='button' class='cls-button' value='跳转'>";
            if(rrequest.checkPermission(ccbean.getPageBean().getId(),Consts.BUTTON_PART,"type{"+Consts_Private.FORWARDWITHBACK_BUTTON+"}",
                    Consts.PERMISSION_TYPE_DISABLED)) return label;
            return "<a href=\"#\" onclick=\"try{"+rrequest.forwardPageWithBack(pageurl,beforecallback)+"}catch(e){logErrorsAsJsFileLoad(e);}\">"
                    +label+"</a>";
        }else if(type.equals(Consts_Private.BACK_BUTTON))
        {
            BackButton buttonObj=null;
            if(ccbean.getButtonsBean()!=null) buttonObj=(BackButton)ccbean.getButtonsBean().getcertainTypeButton(BackButton.class);
            if(buttonObj==null)
            {
                buttonObj=(BackButton)Config.getInstance().getResourceButton(rrequest,ccbean,Consts.BACK_BUTTON_DEFAULT,BackButton.class);
            }
            return showButton(ccbean,rrequest,buttonObj,label);
        }
        if(ccbean.getButtonsBean()==null) return "";
        if(!(componentObj instanceof AbsReportType))
        {
            throw new WabacusRuntimeException("显示组件"+ccbean.getPath()+"失败，它不是报表类型，不能显示"+type+"类型的按钮");
        }
        AbsReportType reportTypeObj=(AbsReportType)componentObj;
        ReportBean rbean=(ReportBean)ccbean;
        AbsButtonType buttonObj=null;
        if(type.equals(Consts_Private.SEARCH_BUTTON))
        {
            buttonObj=rbean.getButtonsBean().getcertainTypeButton(SearchButton.class);
        }else if(type.equals(Consts_Private.SAVE_BUTTON))
        {//保存
            if(!(reportTypeObj instanceof IEditableReportType))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"不是可编辑报表类型，不能显示保存按钮");
            }
            buttonObj=(SaveButton)rbean.getButtonsBean().getcertainTypeButton(SaveButton.class);
            String savebinding=attributes.get("savebinding");
            if(savebinding!=null&&!savebinding.trim().equals(""))
            {
                List<ReportBean> lstDynBindedReportBeans=ComponentConfigLoadManager.getLstBindedReportBeans(rbean,Tools.parseStringToList(
                        savebinding,";",false),"savebinding",false);
                if(lstDynBindedReportBeans!=null&&lstDynBindedReportBeans.size()>0)
                {
                    rrequest.setAttribute(rbean.getId()+"_DYN_BINDED_REPORTS",lstDynBindedReportBeans);
                }
            }
        }else if(type.equals(Consts_Private.ADD_BUTTON))
        {
            if(!(reportTypeObj instanceof IEditableReportType))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"不是可编辑报表类型，不能显示添加按钮");
            }
            buttonObj=rbean.getButtonsBean().getcertainTypeButton(AddButton.class);
        }else if(type.equals(Consts_Private.DELETE_BUTTON))
        {
            if(!(reportTypeObj instanceof IEditableReportType))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"不是可编辑报表类型，不能显示删除按钮");
            }
            buttonObj=rbean.getButtonsBean().getcertainTypeButton(DeleteButton.class);
            String deletebinding=attributes.get("deletebinding");
            if(deletebinding!=null&&!deletebinding.trim().equals(""))
            {
                List<ReportBean> lstDynBindedReportBeans=ComponentConfigLoadManager.getLstBindedReportBeans(rbean,Tools.parseStringToList(
                        deletebinding,";",false),"deletebinding",false);
                if(lstDynBindedReportBeans!=null&&lstDynBindedReportBeans.size()>0)
                {
                    rrequest.setAttribute(rbean.getId()+"_DYN_BINDED_REPORTS",lstDynBindedReportBeans);
                }
            }
        }else if(type.equals(Consts_Private.MODIFY_BUTTON))
        {//修改按钮
            if(!(reportTypeObj instanceof IEditableReportType))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"不是可编辑报表类型，不能显示修改按钮");
            }
            if(!(reportTypeObj instanceof EditableDetailReportType))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"的可编辑报表类型，不需显示修改按钮");
            }
            buttonObj=rbean.getButtonsBean().getcertainTypeButton(UpdateButton.class);
        }else if(type.equals(Consts_Private.RESET_BUTTON))
        {
            if(!(reportTypeObj instanceof IEditableReportType))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"不是可编辑报表类型，不能显示重置按钮");
            }
            if(!(reportTypeObj instanceof EditableDetailReportType))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"的可编辑报表类型，不能显示重置按钮");
            }
            buttonObj=rbean.getButtonsBean().getcertainTypeButton(ResetButton.class);
        }else if(type.equals(Consts_Private.CANCEL_BUTTON))
        {
            if(!(reportTypeObj instanceof IEditableReportType))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"不是可编辑报表类型，不能显示取消按钮");
            }
            if(!(reportTypeObj instanceof EditableDetailReportType))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"的可编辑报表类型，不能显示取消按钮");
            }
            buttonObj=rbean.getButtonsBean().getcertainTypeButton(CancelButton.class);
        }else if(type.equals(Consts.IMPORT_DATA))
        {
            buttonObj=rbean.getButtonsBean().getcertainTypeButton(DataImportButton.class);
        }else
        {
            throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"按钮失败，错误的按钮类型："+type);
        }
        if(buttonObj==null) return "";
        return showButton(rbean,rrequest,buttonObj,label);
    }

    public String getButtonDisplayValueForContainer(AbsContainerType containerTypeObj,Map<String,String> attributes)
    {
        AbsContainerConfigBean ccbean=containerTypeObj.getContainerConfigBean();
        ReportRequest rrequest=containerTypeObj.getReportRequest();
        String type=attributes.get("type");
        String name=attributes.get("name");
        String label=attributes.get("label");
        type=type==null?"":type.trim().toLowerCase();
        name=name==null?"":name.trim();
        if(!name.equals("")&&!type.equals(""))
        {
            log.warn("在<wx:button/>显示报表"+ccbean.getPath()+"按钮时，如果同时指定name属性和type属性时，只有name属性有效");
        }else if(name.equals("")&&type.equals(""))
        {
            throw new WabacusRuntimeException("在<wx:button/>显示容器"+ccbean.getPath()+"按钮时，必须通过type属性或name属性指定要显示的按钮");
        }
        if(name!=null&&!name.equals(""))
        {//按<button/>的name属性来显示相应的按钮
            if(ccbean.getButtonsBean()==null) return "";
            AbsButtonType buttonObj=ccbean.getButtonsBean().getButtonByName(name);
            if(buttonObj==null)
            {
                throw new WabacusRuntimeException("没有配置name属性为"+name+"的<button/>");
            }
            return showButton(ccbean,rrequest,buttonObj,label);
        }
        if(Consts.lstDataExportTypes.contains(type))
        {
            String componentids=attributes.get("componentids");
            if(componentids==null||componentids.trim().equals(""))
            {//如果没有指定要导出的报表ID
                List<AbsButtonType> lstDataExportButtons=null;
                if(ccbean.getButtonsBean()!=null) lstDataExportButtons=ccbean.getButtonsBean().getLstDataExportTypeButtons(type);
                if(lstDataExportButtons!=null&&lstDataExportButtons.size()>0)
                {
                    return ComponentAssistant.getInstance().showButton(ccbean,lstDataExportButtons.get(0),rrequest,null,label);
                }
                throw new WabacusRuntimeException("容器"+ccbean.getPath()+"没有配置类型为"+type+"的数据导出功能，在使用自定义标签显示它时又没有通过reportid属性为它指定要导出的报表");
            }else
            {
                AbsButtonType buttonObj=Config.getInstance().getResourceButton(rrequest,ccbean.getPageBean(),
                        Consts.M_DATAEXPORT_DEFAULTBUTTONS.get(type),DataExportButton.class);
                return ((DataExportButton)buttonObj).showButtonTag(rrequest,type,componentids,label,createDataExportStroageBean(attributes));
            }
        }else if(type.equals(Consts_Private.FORWARDWITHBACK_BUTTON))
        {
            String pageurl=attributes.get("pageurl");
            if(pageurl==null||pageurl.trim().equals(""))
            {
                throw new WabacusRuntimeException("显示组件"+ccbean.getPath()+"跳转按钮失败，这种按钮类型必定指定其pageurl属性为跳转的目标页面");
            }
            String beforecallback=attributes.get("beforecallback");
            beforecallback=beforecallback==null||beforecallback.trim().equals("")?null:beforecallback.trim();
            if(!rrequest.checkPermission(ccbean.getPageBean().getId(),Consts.BUTTON_PART,"type{"+Consts_Private.FORWARDWITHBACK_BUTTON+"}",
                    Consts.PERMISSION_TYPE_DISPLAY)) return "";
            if(label==null||label.trim().equals("")) label="<input type='button' class='cls-button' value='跳转'>";
            if(rrequest.checkPermission(ccbean.getPageBean().getId(),Consts.BUTTON_PART,"type{"+Consts_Private.FORWARDWITHBACK_BUTTON+"}",
                    Consts.PERMISSION_TYPE_DISABLED)) return label;//没有点击权限 
            return "<a href=\"#\" onclick=\"try{"+rrequest.forwardPageWithBack(pageurl,beforecallback)+"}catch(e){logErrorsAsJsFileLoad(e);}\">"+label+"</a>";
        }else if(type.equals(Consts_Private.BACK_BUTTON))
        {
            BackButton buttonObj=null;
            if(ccbean.getButtonsBean()!=null) buttonObj=(BackButton)ccbean.getButtonsBean().getcertainTypeButton(BackButton.class);
            if(buttonObj==null)
            {
                buttonObj=(BackButton)Config.getInstance().getResourceButton(rrequest,ccbean,Consts.BACK_BUTTON_DEFAULT,BackButton.class);
            }
            return showButton(ccbean,rrequest,buttonObj,label);
        }else
        {
            throw new WabacusRuntimeException("显示组件"+ccbean.getPath()+"按钮失败，在容器的自定义标签中不支持按钮类型："+type);
        }
    }

    private DataExportLocalStroageBean createDataExportStroageBean(Map<String,String> mAttributes)
    {
        if(mAttributes==null||mAttributes.size()==0) return null;
        String localstroage=mAttributes.get("localstroage");
        if(!"true".equalsIgnoreCase(localstroage)) return null;
        String directorydateformat=mAttributes.get("directorydateformat");
        String download=mAttributes.get("download");
        String autodelete=mAttributes.get("autodelete");
        String zip=mAttributes.get("zip");
        DataExportLocalStroageBean bean=new DataExportLocalStroageBean();
        bean.setDownload(!"false".equalsIgnoreCase(download));
        bean.setAutodelete(!"false".equalsIgnoreCase(autodelete));
        bean.setZip("true".equalsIgnoreCase(zip));
        bean.setDirectorydateformat(directorydateformat);
        bean.doPostLoad();
        return bean;
    }
    
    private String showButton(IComponentConfigBean ccbean,ReportRequest rrequest,AbsButtonType buttonObj,String label)
    {
        if(label==null||label.trim().equals(""))
        {//用户没有在<wx:button/>的标签内容中指定要显示的按钮，则用默认按钮
            return ComponentAssistant.getInstance().showButton(ccbean,buttonObj,rrequest,null);
        }else
        {
            return ComponentAssistant.getInstance().showButton(ccbean,buttonObj,rrequest,null,label);
        }
    }

    public String getNavigateDisplayInfo(AbsReportType reportTypeObj,String type,String minlength,String initcount,String maxcount,String top,
            String label)
    {
        if(!reportTypeObj.shouldDisplayNavigateBox()) return "";
        ReportBean rbean=reportTypeObj.getReportBean();
        ReportRequest rrequest=reportTypeObj.getReportRequest();
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        label=label==null?"":label.trim();
        if(Tools.isDefineKey("$",label))
        {
            label=Config.getInstance().getResourceString(rrequest,rbean.getPageBean(),label,false);
        }
        if(Tools.isDefineKey("i18n",label))
        {//是从国际化资源文件中获取
            label=rrequest.getI18NStringValue(label);
        }
        int pagecount=cdb.getPagecount();
        int pageno=cdb.getFinalPageno();
        if(type==null||type.trim().equals(""))
        {
            if(!rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return "";
            String navigatestr=reportTypeObj.showNavigateBox();
            if(navigatestr==null||navigatestr.trim().equals("")) return "";
            StringBuffer resultBuf=new StringBuffer();
            resultBuf.append(TagAssistant.getInstance().showTopSpace(top));
            resultBuf.append(navigatestr);
            return resultBuf.toString();
        }else if(type.equals(Consts_Private.NAVIGATE_FIRST))
        {
            if(!rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_FIRST,Consts.PERMISSION_TYPE_DISPLAY)) return "";
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return "";
            if(label.equals("")) label="<span class=\"cls-navigate-link\"><input type=\"button\" class=\"cls-navigate-firstbutton\"/></span>";
            if(pageno==1||rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_FIRST,Consts.PERMISSION_TYPE_DISABLED))
            {
                return label;
            }
            return ReportAssistant.getInstance().getNavigatePagenoWithEvent(rrequest,rbean,1,label);
        }else if(type.equals(Consts_Private.NAVIGATE_PREVIOUS))
        {
            if(!rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_PREVIOUS,Consts.PERMISSION_TYPE_DISPLAY))
                return "";
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return "";
            if(label.equals("")) label="<span class=\"cls-navigate-link\"><input type=\"button\" class=\"cls-navigate-previousbutton\"/></span>";
            if(pageno<=1
                    ||rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_PREVIOUS,Consts.PERMISSION_TYPE_DISABLED))
            {
                return label;
            }else
            {
                return ReportAssistant.getInstance().getNavigatePagenoWithEvent(rrequest,rbean,pageno-1,label);
            }
        }else if(type.equals(Consts_Private.NAVIGATE_NEXT))
        {
            if(!rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_NEXT,Consts.PERMISSION_TYPE_DISPLAY)) return "";
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return "";
            if(label.equals("")) label="<span class=\"cls-navigate-link\"><input type=\"button\" class=\"cls-navigate-nextbutton\"/></span>";
            if(pageno==pagecount
                    ||rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_NEXT,Consts.PERMISSION_TYPE_DISABLED))
            {
                return label;
            }else
            {
                return ReportAssistant.getInstance().getNavigatePagenoWithEvent(rrequest,rbean,pageno+1,label);
            }
        }else if(type.equals(Consts_Private.NAVIGATE_LAST))
        {
            if(!rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_LAST,Consts.PERMISSION_TYPE_DISPLAY)) return "";
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return "";
            if(label.equals("")) label="<span class=\"cls-navigate-link\"><input type=\"button\" class=\"cls-navigate-lastbutton\"/></span>";
            if(pagecount==pageno
                    ||rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_LAST,Consts.PERMISSION_TYPE_DISABLED))
            {
                return label;
            }
            return ReportAssistant.getInstance().getNavigatePagenoWithEvent(rrequest,rbean,pagecount,label);
        }else if(type.equals(Consts_Private.NAVIGATE_SEQUENCE))
        {
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return "";
            if(!rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_SEQUENCE,Consts.PERMISSION_TYPE_DISPLAY))
                return "";
            return showNavigateSequence(reportTypeObj,label,minlength,initcount,maxcount);
        }else if(type.equals(Consts_Private.NAVIGATE_PAGENO))
        {
            if(!rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_PAGENO,Consts.PERMISSION_TYPE_DISPLAY))
                return "";
            if(label.equals("")||label.equalsIgnoreCase("text")||rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
            {
                return String.valueOf(pageno);
            }else if(label.equalsIgnoreCase("textbox"))
            {
                return ReportAssistant.getInstance().getNavigateTextBox(rrequest,rbean);
            }else if(label.equalsIgnoreCase("selectbox"))
            {
                return ReportAssistant.getInstance().getNavigateSelectBox(rrequest,rbean);
            }else
            {
                throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，在模板的<wx:navigate/>的type属性为"+Consts_Private.NAVIGATE_PAGENO+"时，其标签内容"
                        +label+"无效");
            }
        }else if(type.equals(Consts_Private.NAVIGATE_PAGESIZE))
        {
            if(!rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_PAGESIZE,Consts.PERMISSION_TYPE_DISPLAY))
                return "";
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
            {
                if(cdb.getPrintPagesize()<=0) return "1";
                return String.valueOf(cdb.getPrintPagesize());
            }
            if(rbean.getLstPagesize().size()<2)
            {
                return String.valueOf(cdb.getPagesize());
            }else
            {
                return ReportAssistant.getInstance().getNavigateSelectBoxForPagesizeConvert(rrequest,rbean);
            }
        }else if(type.equals(Consts_Private.NAVIGATE_PAGECOUNT))
        {
            if(!rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_PAGECOUNT,Consts.PERMISSION_TYPE_DISPLAY))
                return "";
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
            {
                if(cdb.getPrintPagesize()<=0) return "1";
                return String.valueOf(cdb.getPrintPagecount());
            }
            return String.valueOf(cdb.getPagecount());
        }else if(type.equals(Consts_Private.NAVIGATE_RECORDCOUNT))
        {
            if(!rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_RECORDCOUNT,Consts.PERMISSION_TYPE_DISPLAY))
                return "";
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
            {
                if(reportTypeObj.getLstReportData()==null) return "0";
                return String.valueOf(reportTypeObj.getLstReportData().size());
            }
            return String.valueOf(cdb.getRecordcount());
        }else
        {
            throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，其所使用的模板中<wx:navigate/>的type属性"+type+"无效");
        }
    }

    private String showNavigateSequence(AbsReportType reportTypeObj,String label,String minlength,String initcount,String maxcount)
    {
        ReportBean rbean=reportTypeObj.getReportBean();
        ReportRequest rrequest=reportTypeObj.getReportRequest();
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        int iminlength=1;
        if(minlength!=null&&!minlength.trim().equals(""))
        {
            try
            {
                iminlength=Integer.parseInt(minlength.trim());
            }catch(NumberFormatException e)
            {
                log.warn("报表"+rbean.getPath()+"所用的模板中<wx:navigate/>的minlength指定的值："+minlength+"不是合法数字",e);
                iminlength=1;
            }
            if(iminlength<1)
            {
                log.warn("报表"+rbean.getPath()+"所用的模板中<wx:navigate/>的minlength指定的值："+minlength+"小于1");
                iminlength=1;
            }
        }
        int i_initcount=5;
        if(initcount!=null&&!initcount.trim().equals(""))
        {
            try
            {
                i_initcount=Integer.parseInt(initcount.trim());
            }catch(NumberFormatException e)
            {
                log.warn("报表"+rbean.getPath()+"所用的模板中<wx:navigate/>的initcount指定的值："+initcount+"不是合法数字",e);
                i_initcount=5;
            }
            if(i_initcount<1)
            {
                log.warn("报表"+rbean.getPath()+"所用的模板中<wx:navigate/>的initcount指定的值："+initcount+"小于1");
                i_initcount=5;
            }
        }

        int i_maxcount=i_initcount;
        if(maxcount!=null&&!maxcount.trim().equals(""))
        {
            try
            {
                i_maxcount=Integer.parseInt(maxcount.trim());
            }catch(NumberFormatException e)
            {
                log.warn("报表"+rbean.getPath()+"所用的模板中<wx:navigate/>的maxcount指定的值："+maxcount+"不是合法数字",e);
                i_maxcount=i_initcount;
            }
            if(i_maxcount<i_initcount)
            {
                log.warn("报表"+rbean.getPath()+"所用的模板中<wx:navigate/>的maxcount指定的值："+maxcount+"小于initcount指定值");
                i_maxcount=i_initcount;
            }
        }

        if(label==null||label.indexOf("%PAGENO%")<0)
        {
            label="%PAGENO%";
        }
        int pageno=cdb.getFinalPageno();
        int[] pagenosArr=null;
        if(i_initcount==i_maxcount)
        {
            pagenosArr=getDisplayPagenos(rrequest,cdb,i_initcount);//得到要显示的起止页码
        }else
        {
            if(pageno<=i_maxcount/2+1)
            {
                pagenosArr=new int[2];
                pagenosArr[0]=1;
                pagenosArr[1]=i_initcount+pageno-1;
                int pagecount=cdb.getPagecount();
                if(pagenosArr[1]>pagecount) pagenosArr[1]=pagecount;
            }else
            {
                pagenosArr=getDisplayPagenos(rrequest,cdb,i_maxcount);
            }
        }
        StringBuffer resultBuf=new StringBuffer();
        String pagelabeltmp="<span class=\"cls-navigate-link\">";
        String labelTmp;
        for(int i=pagenosArr[0];i<=pagenosArr[1];i++)
        {
            labelTmp=String.valueOf(i);
            if(iminlength>labelTmp.length())
            {
                int deltalen=iminlength-labelTmp.length();
                for(int j=0;j<deltalen;j++)
                {
                    labelTmp="0"+labelTmp;
                }
            }
            if(i==pageno||rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
            {
                resultBuf.append(pagelabeltmp+labelTmp+"</span>");
            }else
            {
                labelTmp=Tools.replaceAll(label,"%PAGENO%",labelTmp);
                labelTmp=pagelabeltmp+labelTmp+"</span>";
                if(rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_SEQUENCE,Consts.PERMISSION_TYPE_DISABLED))
                {
                    resultBuf.append(labelTmp);
                }else
                {
                    resultBuf.append(ReportAssistant.getInstance().getNavigatePagenoWithEvent(rrequest,rbean,i,labelTmp));
                }
            }
        }
        return resultBuf.toString();
    }

    private int[] getDisplayPagenos(ReportRequest rrequest,CacheDataBean cdb,int count)
    {
        int pagecount=cdb.getPagecount();
        int pageno=cdb.getFinalPageno();
        int startidx=0;
        int endidx=0;
        if(pagecount<=count)
        {
            startidx=1;
            endidx=pagecount;
        }else if(pageno<count/2+1)
        {
            startidx=1;
            endidx=count;
        }else if(count%2==1)
        {
            startidx=pageno-count/2;
            endidx=pageno+count/2;
        }else
        {
            startidx=pageno-count/2;
            endidx=pageno+count/2-1;
        }
        if(endidx>pagecount)
        {
            endidx=pagecount;
            startidx=pagecount-(count-1);//确保显示了count个页码出来
        }
        return new int[] { startidx, endidx };
    }

    public String getDataImportDisplayValue(String ref,String asyn,String popupparams,String initsize,String label,String interceptor,HttpServletRequest request)
    {
        if(ref==null||ref.trim().equals(""))
        {
            throw new WabacusRuntimeException("必须指定<wx:dataimport/>的ref属性");
        }
        StringBuffer refBuf=new StringBuffer();
        List<String> lst=Tools.parseStringToList(ref,";",false);
        for(String strTmp:lst)
        {
            if(strTmp.equals("")) continue;
            //                throw new JspException("<dataimport/>的ref属性必须引用资源文件中定义的数据导入项");
            //因为在jsp自定义标签中，“${expression}”是一个表达式，不能做为一个字符串，因此在ref属性中指定的直接是资源项的key
            Object obj=Config.getInstance().getResources().get(null,strTmp,true);
            if(!(obj instanceof AbsDataImportConfigBean))
            {
                throw new WabacusConfigLoadingException("<dataimport/>中通过ref属性引用的数据导出项"+strTmp+"对应的资源项不是数据导出项资源类型");
            }
            refBuf.append(strTmp).append(";");
        }
        if(refBuf.length()==0)
        {
            throw new WabacusRuntimeException("必须为<wx:dataimport/>指定有效的ref属性");
        }
        if(refBuf.charAt(refBuf.length()-1)==';')
        {
            refBuf.deleteCharAt(refBuf.length()-1);
        }
        popupparams=WabacusAssistant.getInstance().addDefaultPopupParams(popupparams,initsize,"300","160",null);
        String token="?";
        if(Config.showreport_url.indexOf("?")>0) token="&";
        String url=Config.showreport_url+token+"DATAIMPORT_REF="+refBuf.toString()+"&ACTIONTYPE=ShowUploadFilePage&FILEUPLOADTYPE="
                +Consts_Private.FILEUPLOADTYPE_DATAIMPORTTAG;
        if(interceptor!=null&&!interceptor.trim().equals(""))
        {
            url=url+"&INTERCEPTOR="+interceptor;
        }
        if(asyn!=null&&!asyn.trim().equals(""))
        {
            url=url+"&ASYN="+asyn;
        }
        String clickevent="wx_winpage('"+url+"',"+popupparams+")";
        StringBuffer labelBuf=new StringBuffer();
        if(label!=null&&!label.trim().equals(""))
        {
            labelBuf.append("<a onmouseover=\"this.style.cursor='pointer';\" onclick=\""+clickevent+"\">");
            labelBuf.append(label);
            labelBuf.append("</a>");
        }else
        {
            label=Config.getInstance().getResources().getString(null,Consts.DATAIMPORT_LABEL,true).trim();
            labelBuf.append("<input type=\"button\" class=\"cls-button2\" onClick=\""+clickevent+"\"");
            labelBuf.append(" value=\""+label+"\">");
        }
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(addPopupIncludeJsCss(request));
        resultBuf.append(labelBuf.toString());
        return resultBuf.toString();
    }

    public String getFileUploadDisplayValue(String maxsize,String allowtypes,String disallowtypes,String uploadcount,String newfilename,String savepath,
            String rooturl,String popupparams,String initsize,String interceptor,String label,String beforepopup,HttpServletRequest request)
    {
        popupparams=WabacusAssistant.getInstance().addDefaultPopupParams(popupparams,initsize,"300","160",null);
        if(savepath==null||savepath.trim().equals(""))
        {
            throw new WabacusRuntimeException("显示文件上传标签失败，没有指定保存路径");
        }
        int iuploadcount=1;
        if(uploadcount!=null&&!uploadcount.trim().equals(""))
        {
            iuploadcount=Integer.parseInt(uploadcount);
        }
        if(iuploadcount<=0)
        {
            throw new WabacusRuntimeException("显示文件上传标签失败，指定的文件上传输入框个数小于0");
        }
        if(beforepopup==null||beforepopup.trim().equals("")) beforepopup="null";
        String token="?";
        if(Config.showreport_url.indexOf("?")>0) token="&";
        String url=Config.showreport_url+token+"ACTIONTYPE=ShowUploadFilePage&FILEUPLOADTYPE="+Consts_Private.FILEUPLOADTYPE_FILETAG;
        url+="&SAVEPATH="+urlEncode(savepath)+"&UPLOADCOUNT="+iuploadcount;
        if(!Tools.isEmpty(rooturl)) url+="&ROOTURL="+urlEncode(rooturl);
        if(!Tools.isEmpty(newfilename)) url+="&NEWFILENAME="+urlEncode(newfilename);
        if(!Tools.isEmpty(maxsize)) url+="&MAXSIZE="+maxsize;
        if(!Tools.isEmpty(allowtypes)) url+="&ALLOWTYPES="+urlEncode(allowtypes);
        if(!Tools.isEmpty(disallowtypes)) url+="&DISALLOWTYPES="+urlEncode(disallowtypes);
        if(!Tools.isEmpty(interceptor)) url+="&INTERCEPTOR="+interceptor;
        String clickevent="wx_winpage('"+url+"',"+popupparams+","+beforepopup+");";
        StringBuilder resultBuf=new StringBuilder();
        if(!Tools.isEmpty(label))
        {
            resultBuf.append("<a onmouseover=\"this.style.cursor='pointer';\" onclick=\""+clickevent+"\">");
            resultBuf.append(label);
            resultBuf.append("</a>");
        }else
        {
            label=Config.getInstance().getResources().getString(null,Consts.FILEUPLOAD_LABEL,true).trim();
            resultBuf.append("<input type=\"button\" class=\"cls-button2\" onClick=\""+clickevent+"\"");
            resultBuf.append("  onFocus=\"this.select();\" value=\""+label+"\">");
        }
        if(request!=null) resultBuf.append(addPopupIncludeJsCss(request));
        return resultBuf.toString();
    }

    private String urlEncode(String urlparam)
    {
        if(urlparam==null||urlparam.trim().equals("")) return urlparam;
        try
        {
            return URLEncoder.encode(urlparam,"utf-8");
        }catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return urlparam;
        }
    }
    
    private String addPopupIncludeJsCss(HttpServletRequest request)
    {
        StringBuilder resultBuf=new StringBuilder();
        String includejscss=(String)request.getAttribute("wx_has_includejscss");
        if(includejscss==null)
        {//还没提供必须的js/css文件
            List<String> lstTmp=ConfigLoadAssistant.getInstance().getLstPopupComponentCss();
            for(String cssTmp:lstTmp)
            {
                resultBuf.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\"").append(cssTmp).append("\"/>");
            }
            String systemcssfile=Config.webroot+"/webresources/skin/"+Config.getInstance().getSkin(request,"")+"/wabacus_system.css";
            systemcssfile=Tools.replaceAll(systemcssfile,"//","/");
            resultBuf.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\"").append(systemcssfile).append("\"/>");
            resultBuf.append("<script language=\"javascript\"  src=\"").append(
                    Tools.replaceAll(Config.webroot+"/webresources/script/wabacus_api.js","//","/")).append("\"></script>");
            resultBuf.append("<script language=\"javascript\"  src=\"").append(
                    Tools.replaceAll(Config.webroot+"/webresources/script/wabacus_util.js","//","/")).append("\"></script>");
            resultBuf.append("<script language=\"javascript\"  src=\"").append(
                    Tools.replaceAll(Config.webroot+"/webresources/script/wabacus_tools.js","//","/")).append("\"></script>");
            resultBuf.append("<script language=\"javascript\"  src=\"").append(
                    Tools.replaceAll(Config.webroot+"/wxtmpfiles/js/generate_system.js","//","/")).append("\"></script>");
            List<JavascriptFileBean> lstJsTmp=ConfigLoadAssistant.getInstance().getLstPopupComponentJs();
            for(JavascriptFileBean jsBeanTmp:lstJsTmp)
            {
                resultBuf.append("<script language=\"javascript\"  src=\"").append(jsBeanTmp.getJsfileurl()).append("\"></script>");
            }
            request.setAttribute("wx_has_includejscss","true");
        }
        return resultBuf.toString();
    }
    
    public String getHeaderFooterDisplayValue(AbsComponentType componentTypeObj,String top,boolean type)
    {
        String displaystr=null;
        if(type)
        {
            displaystr=componentTypeObj.showHeader();
        }else
        {
            displaystr=componentTypeObj.showFooter();
        }
        if(displaystr==null||displaystr.trim().equals("")) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(TagAssistant.getInstance().showTopSpace(top));
        resultBuf.append(displaystr);
        return resultBuf.toString();
    }
}
