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
package com.wabacus.system.component.container.page;

import java.util.Collections;
import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.other.JavascriptFileBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.buttons.BackButton;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.tags.component.AbsComponentTag;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class PageType extends AbsContainerType
{
    private List<String> lstCsses=null;

    private List<String> lstDynCsses=null;

    private List<JavascriptFileBean> lstJavascripts=null;

    private PageBean pagebean;

    public PageType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        this.pagebean=(PageBean)comCfgBean;
    }

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        rrequest.addParamToUrl("CSS","rrequest{CSS}",false);
        rrequest.addParamToUrl("JS","rrequest{JS}",false);
        super.initUrl(applicationConfigBean,rrequest);
    }

    public List<ReportBean> initDisplayOnPage()
    {
        initJsCssResources();
        return super.initDisplayOnPage();
    }
    
    private boolean hasInitJsCssFiles=false;
    
    private void initJsCssResources()
    {
        if(hasInitJsCssFiles) return;
        hasInitJsCssFiles=true;
        this.lstCsses=ComponentAssistant.getInstance().initDisplayCss(rrequest);
        this.lstJavascripts=new UniqueArrayList<JavascriptFileBean>();
        this.lstJavascripts.addAll(pagebean.getLstSystemJavascriptFiles());
        String js=rrequest.getStringAttribute("JS","");
        if(!js.equals(""))
        {
            js=Tools.htmlEncode(js);
            List<String> lstJsTmp=Tools.parseStringToList(js,",",false);
            for(String jsTmp:lstJsTmp)
            {
                if(jsTmp==null||jsTmp.trim().equals("")) continue;
                if(!jsTmp.trim().startsWith(Config.webroot)&&!jsTmp.trim().toLowerCase().startsWith("http://"))
                {
                    jsTmp=Tools.replaceAll(Config.webroot+"/"+jsTmp,"//","/");
                }
                this.lstJavascripts.add(new JavascriptFileBean(jsTmp,0));
            }
        }else if(pagebean.getLstMyJavascriptFiles()!=null)
        {
            this.lstJavascripts.addAll(pagebean.getLstMyJavascriptFiles());
        }
    }

    public void displayOnPage(AbsComponentTag displayTag)
    {
        if(!rrequest.checkPermission(pagebean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            wresponse.println("&nbsp;");
            return;
        }
        wresponse.println(showStartWebResources());
        wresponse.println("<div id=\"WX_CONTENT_"+pagebean.getGuid()+"\">");//顶层<page/>的内容必须用<span/>完整括住，这样更新页面时才能更新整个页面内容
        wresponse.println(showContainerStartPart());
        wresponse.println(showContainerTableTag());//显示容器最里层的<table>标签
        if(rrequest.checkPermission(pagebean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            IComponentType childObjTmp;
            for(String childIdTmp:lstChildrenIds)
            {
                wresponse.println("<tr>");
                childObjTmp=this.mChildren.get(childIdTmp);
                showChildObj(childObjTmp,null);
                wresponse.println("</tr>");
            }
        }
        String backbutton=showBackButtonInPage();
        if(!backbutton.trim().equals(""))
        {
            wresponse.println("<tr><td align=\"center\">");
            wresponse.println(backbutton);
            wresponse.println("</td></tr>");
        }
        wresponse.println("</table>");
        wresponse.println(showContainerEndPart());
        wresponse.println("<div id=\"wx_titletree_container\" style=\"display:none;\" class=\"titletree_container\">");
        wresponse.println("<div id=\"titletree_container_inner\" class=\"titletree_container_inner\">");
        wresponse
                .println("<div id=\"tree\" class=\"bbit-tree\"><div class=\"bbit-tree-bwrap\"><div class=\"bbit-tree-body\" id=\"wx_titletree_content\">");
        wresponse.println("</div></div></div></div>");
        wresponse.println("<div id=\"wx_titletree_buttoncontainer\" style=\"padding-top: 3px;padding-bottom:5px;text-align:center\"></div>");
        wresponse.println("</div>");
        wresponse.println("<div id=\"LOADING_IMG_ID\" class=\"cls-loading-img\"></div>");
        if(pagebean.getLstPrintBeans()!=null)
        {
            for(AbsPrintProviderConfigBean ppcbeanTmp:pagebean.getLstPrintBeans())
            {
                ppcbeanTmp.initPrint(rrequest);
            }
        }
        wresponse.println("</div>");
        wresponse.println(showEndWebResources());
    }

    private String showBackButtonInPage()
    {
        StringBuffer resultBuf=new StringBuffer();
        String clickevent=rrequest.getStringAttribute("BACK_ACTION_EVENT","");
        if(rrequest.getLstAncestorUrls()!=null&&rrequest.getLstAncestorUrls().size()>0&&clickevent.equals(""))
        {
            if(this.pagebean.getButtonsBean()!=null&&this.pagebean.getButtonsBean().getcertainTypeButton(BackButton.class)!=null)
            {//如果此页面配置了“返回”按钮，则不在这里自动显示（因为稍后显示按钮时会显示“返回”按钮）
                return "";
            }
            BackButton buttonObj=(BackButton)Config.getInstance().getResourceButton(rrequest,rrequest.getPagebean(),Consts.BACK_BUTTON_DEFAULT,
                    BackButton.class);
            resultBuf.append("<table height='3'><tr><td>&nbsp;</td></tr></table>");
            resultBuf.append("<table width='100%' align='center'><tr><td align=\"center\">").append(buttonObj.showButton(rrequest,null)).append(
                    "</td></tr></table>");
        }
        return resultBuf.toString();
    }

    private static String systemheadjs="";
    static
    {
        //            systemheadjs="/webresources/script/wabacus_systemhead.js";
        //        {
        //            }
        //            systemheadjs="/webresources/script/"+encode.toLowerCase()+"/wabacus_systemhead.js";
        systemheadjs=Config.webroot+"/webresources/script/wabacus_systemhead.js";
        systemheadjs=Tools.replaceAll(systemheadjs,"//","/");
    }

    private boolean hasDisplayOuterHeader,hasDisplayOuterFooter,hasDisplayStartWebResources, hasDisplayEndWebResources;

    public String showOuterHeader()
    {
        if(rrequest.isLoadedByAjax()||hasDisplayOuterHeader) return "";
        hasDisplayOuterHeader=true;
        return getRealHeaderFooterDisplayValue(pagebean.getOuterHeaderTplBean(),"outerheader");
    }
    
    public String showOuterFooter()
    {
        if(rrequest.isLoadedByAjax()||hasDisplayOuterFooter) return "";
        hasDisplayOuterFooter=true;
        return getRealHeaderFooterDisplayValue(pagebean.getOuterFooterTplBean(),"outerfooter");
    }
    
    public String showStartWebResources()
    {
        if(rrequest.isLoadedByAjax()||hasDisplayStartWebResources) return "";
        hasDisplayStartWebResources=true;
        StringBuilder resultBuf=new StringBuilder();
        if(rrequest.isDisplayOnPage())
        {
            initJsCssResources();
            if(this.lstCsses!=null)
            {
                for(String cssTmp:this.lstCsses)
                {
                    resultBuf.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\"").append(cssTmp).append("\"/>");
                }
            }
            resultBuf.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\"").append(Config.webroot).append(
                    "webresources/skin/colselected_tree.css\"/>");
            resultBuf.append("<script language=\"javascript\" src=\"");
            resultBuf.append(systemheadjs).append("\"></script>");
        }
        return resultBuf.toString();
    }

    public String showEndWebResources()
    {
        if(rrequest.isLoadedByAjax()||hasDisplayEndWebResources) return "";
        hasDisplayEndWebResources=true;
        StringBuilder resultBuf=new StringBuilder();
        if(rrequest.isDisplayOnPage())
        {
            initJsCssResources();
            if(this.lstJavascripts!=null)
            {
                Collections.sort(this.lstJavascripts);
                for(JavascriptFileBean jsBeanTmp:this.lstJavascripts)
                {
                    resultBuf.append("<script type=\"text/javascript\"  src=\"").append(jsBeanTmp.getJsfileurl()).append("\"></script>");
                }
            }
            if(lstDynCsses!=null)
            {
                for(String cssTmp:lstDynCsses)
                {
                    resultBuf.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\"").append(cssTmp).append("\"/>");
                }
            }
            if(!rrequest.isLoadedByAjax())
            {//如果不是ajax加载，即第一次访问，则通过如下方式执行onload函数
                resultBuf.append(rrequest.getWResponse().showPageUrlSpan());
                String confirmessage=rrequest.getWResponse().getMessageCollector().getConfirmmessage();
                if(confirmessage!=null&&!confirmessage.trim().equals(""))
                {
                    resultBuf.append("<script type=\"text/javascript\">");
                    resultBuf.append("WX_serverconfirm_key='").append(rrequest.getWResponse().getMessageCollector().getConfirmkey()).append("';");
                    resultBuf.append("WX_serverconfirm_url='").append(rrequest.getWResponse().getMessageCollector().getConfirmurl()).append("';");
                    resultBuf.append("wx_confirm('"+confirmessage+"',null,null,null,okServerConfirm,cancelServerConfirm);");
                    resultBuf.append("</script>");
                }else
                {
                    String onloadMethods=rrequest.getWResponse().invokeOnloadMethodsFirstTime();
                    if(onloadMethods!=null&&!onloadMethods.trim().equals(""))
                    {
                        resultBuf.append("<script type=\"text/javascript\">").append(onloadMethods).append("</script>");
                    }
                    if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&rrequest.getLstReportWithDefaultSelectedRows()!=null
                            &&rrequest.getLstReportWithDefaultSelectedRows().size()>0)
                    {
                        resultBuf.append("<script type=\"text/javascript\">");
                        for(String reportidTmp:rrequest.getLstReportWithDefaultSelectedRows())
                        {
                            resultBuf.append("selectDefaultSelectedDataRows(getReportMetadataObj(getComponentGuidById(");
                            resultBuf.append("'"+this.pagebean.getId()+"','"+reportidTmp+"')));");
                        }
                        resultBuf.append("</script>");
                    }
                }
            }
        }
        return resultBuf.toString();
    }

    public String getRealParenttitle()
    {
        return null;//因为<page/>是顶层容器，没有父容器
    }

    public void addDynCsses(List<String> lstCsses)
    {
        if(lstCsses==null||lstCsses.size()==0) return;
        if(this.lstDynCsses==null) this.lstDynCsses=new UniqueArrayList<String>();
        for(String cssTmp:lstCsses)
        {
            if(cssTmp==null||cssTmp.trim().equals("")) continue;
            this.lstDynCsses.add(Tools.replaceAll(cssTmp,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin()));
        }
    }

    public void addDynJsFileBeans(List<JavascriptFileBean> lstJsFileBeans)
    {
        if(lstJsFileBeans==null||lstJsFileBeans.size()==0) return;
        if(this.lstJavascripts==null) this.lstJavascripts=new UniqueArrayList<JavascriptFileBean>();
        this.lstJavascripts.addAll(lstJsFileBeans);
    }

    public AbsContainerConfigBean loadConfig(XmlElementBean eleContainer,AbsContainerConfigBean parent,String tagname)
    {
        return null;
    }

    protected String getComponentTypeName()
    {
        return "container.page";
    }
}
