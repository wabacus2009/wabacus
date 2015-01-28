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
package com.wabacus.config.print;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.template.TemplateParser;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.PrintButton;
import com.wabacus.system.print.AbsPrintProvider;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public abstract class AbsPrintProviderConfigBean implements Cloneable
{
    private final static Log log=LogFactory.getLog(AbsPrintProviderConfigBean.class);

    protected String jobname;//打印任务的名字，可以与组件的title的配置方法完全一致，可以配置常量、和从request/session取的动态变量，也可以是它们的混合，这里存放解析后这里存放带占位符的字符串

    protected Map<String,String> mDynJobnameParts;

    protected String button;

    protected boolean isPreview;

    protected String previewbutton;

    protected boolean isSetting;//是否需要打印设置功能

    protected String settingbutton;
    
    protected String printpagesize;

    protected IComponentConfigBean owner;

    protected List<String> lstIncludeApplicationIds;

    protected String includeApplicationids;//运行时由lstApplicationIds生成

    protected Map<String,Integer> mReportidAndPagesize;
    
    protected List<PrintSubPageBean> lstPrintPageBeans;

    protected String printPageInfo;

    protected Boolean isTemplatePrintValue;

    protected boolean isUseDefaultPrintTemplate;//当前<print/>是否是属于使用全局默认的打印静态模板的报表的配置（只有当前是报表的<print/>，且没有配置打印内容时，此<print/>才会使用全局默认的打印静态模板）

    protected XmlElementBean elePrintBean;//用于加载时临时存放<print/>标签，以便在doPolstLoad()方法解析时能用上，解析完后将会被赋为null

    private int placeholderIndex=0;

    public AbsPrintProviderConfigBean(IComponentConfigBean owner)
    {
        this.owner=owner;
    }

    public IComponentConfigBean getOwner()
    {
        return owner;
    }

    public void setOwner(IComponentConfigBean owner)
    {
        this.owner=owner;
    }

    public String getJobname(ReportRequest rrequest)
    {
        String realjobname=null;
        if(jobname==null||jobname.trim().equals(""))
        {//没有配置任务名
            realjobname=this.owner.getTitle(rrequest);
            if(realjobname==null||realjobname.trim().equals("")) realjobname=this.owner.getId();
        }else
        {
            realjobname=WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.jobname,this.mDynJobnameParts,"");
        }
        return realjobname;
    }

    public boolean isPreview()
    {
        return isPreview;
    }

    public boolean isSetting()
    {
        return isSetting;
    }

    public String getPreviewbutton()
    {
        return previewbutton;
    }

    public String getSettingbutton()
    {
        return settingbutton;
    }

    public List<String> getLstIncludeApplicationIds()
    {
        return lstIncludeApplicationIds;
    }

    public String getIncludeApplicationids()
    {
        return includeApplicationids;
    }

    public String getButton()
    {
        return button;
    }

    public List<PrintSubPageBean> getLstPrintPageBeans()
    {
        return lstPrintPageBeans;
    }

    public boolean isUseGlobalDefaultPrintTemplate()
    {
        return isUseDefaultPrintTemplate;
    }

    public void initPrint(ReportRequest rrequest)
    {}

    public Boolean isTemplatePrintValue()
    {
        if(isTemplatePrintValue==null) return false;
        return isTemplatePrintValue.booleanValue();
    }

    public int getPrintPageSize(String reportid)
    {
        if(this.mReportidAndPagesize==null||this.mReportidAndPagesize.get(reportid)==null||mReportidAndPagesize.get(reportid)==Integer.MIN_VALUE)
            return 0;
        return this.mReportidAndPagesize.get(reportid).intValue();
    }

    public Map<String,Integer> getMReportidAndPagesize()
    {
        return mReportidAndPagesize;
    }

    public int getPlaceholderIndex()
    {
        return placeholderIndex++;
    }

    public abstract AbsPrintProvider createPrintProvider(ReportRequest rrequest);

    public void loadConfig(XmlElementBean elePrintBean)
    {
        String printbtn=elePrintBean.attributeValue("button");
        if(printbtn!=null&&!printbtn.trim().equals(""))
        {
            this.button=Config.getInstance().getResourceString(null,this.owner.getPageBean(),printbtn,false);
        }
        String preview=elePrintBean.attributeValue("preview");
        this.isPreview=preview!=null&&preview.toLowerCase().trim().equals("true");
        String previewbtn=elePrintBean.attributeValue("previewbutton");
        if(previewbtn!=null&&!previewbtn.trim().equals(""))
        {
            this.previewbutton=Config.getInstance().getResourceString(null,this.owner.getPageBean(),previewbtn,false);
        }
        String setting=elePrintBean.attributeValue("setting");
        this.isSetting=setting!=null&&setting.toLowerCase().trim().equals("true");
        String settingbtn=elePrintBean.attributeValue("settingbutton");
        if(settingbtn!=null&&!settingbtn.trim().equals(""))
        {
            this.settingbutton=Config.getInstance().getResourceString(null,this.owner.getPageBean(),settingbtn,false);
        }
        this.printpagesize=elePrintBean.attributeValue("printpagesize");
        String include=elePrintBean.attributeValue("include");
        if(include!=null&&!include.trim().equals(""))
        {
            this.lstIncludeApplicationIds=Tools.parseStringToList(include,";",false);
        }
        String jobname=elePrintBean.attributeValue("jobname");
        if(jobname!=null)
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(this.owner.getPageBean(),jobname.trim());
            this.jobname=(String)objArr[0];
            this.mDynJobnameParts=(Map<String,String>)objArr[1];
        }
        XmlElementBean elePageinfoBean=elePrintBean.getChildElementByName("pageinfo");
        if(elePageinfoBean!=null)
        {
            String pageinfo=elePageinfoBean.getContent();
            this.printPageInfo=pageinfo==null?"":pageinfo.trim();
            if(Tools.isDefineKey("$",this.printPageInfo))
                this.printPageInfo=Config.getInstance().getResourceString(null,this.owner.getPageBean(),this.printPageInfo,true);
        }else
        {
            this.printPageInfo=Config.getInstance().getResourceString(null,this.owner.getPageBean(),"${print.pageinfo.default}",true);//如果没有配置<pageinfo/>，则用全局默认页面信息
        }
        //        width=width==null?"":width.trim();
        this.elePrintBean=elePrintBean;
    }

    public void doPostLoad()
    {
        checkedAndAddButtons(Consts.PRINTTYPE_PRINT);
        if(this.isPreview) checkedAndAddButtons(Consts.PRINTTYPE_PRINTPREVIEW);
        if(this.isSetting) checkedAndAddButtons(Consts.PRINTTYPE_PRINTSETTING);
        processIncludeApplicationIds();
        this.lstPrintPageBeans=new ArrayList<PrintSubPageBean>();
        loadSubpageConfig();//加载<print/>中的<subpage/>
        createPrintJsScript();
        this.owner.getPageBean().addPrintBean(this);
    }

    protected void checkedAndAddButtons(String printButtonType)
    {
        if(!(this.owner instanceof ReportBean)&&!(this.owner instanceof AbsContainerConfigBean))
        {
            throw new WabacusConfigLoadingException("组件"+this.owner.getPath()+"不是报表和容器，不能配置<print/>打印功能，可以在其父容器中配置<print/>，然后指定打印本组件的内容");
        }
        List<AbsButtonType> lstPrintButtons=null;
        if(this.owner.getButtonsBean()!=null) lstPrintButtons=this.owner.getButtonsBean().getLstPrintTypeButtons(printButtonType);
        if(lstPrintButtons==null||lstPrintButtons.size()==0)
        {
            AbsButtonType buttonObj=Config.getInstance().getResourceButton(null,this.owner,Consts.M_PRINT_DEFAULTBUTTONS.get(printButtonType),
                    PrintButton.class);
            buttonObj.setDefaultNameIfNoName();
            if(this.owner instanceof AbsContainerConfigBean)
            {
                buttonObj.setPosition("top");
            }
            ComponentConfigLoadManager.addButtonToPositions(this.owner,buttonObj);
        }
    }

    private void processIncludeApplicationIds()
    {
        Object[] objResult=ComponentConfigLoadAssistant.getInstance().parseIncludeApplicationids(this.owner,this.lstIncludeApplicationIds);
        this.includeApplicationids=(String)objResult[0];
        this.lstIncludeApplicationIds=(List<String>)objResult[1];
        this.mReportidAndPagesize=(Map<String,Integer>)objResult[2];
    }
    
    protected void addIncludeApplicationId(String appid)
    {
        if(appid==null||appid.trim().equals("")||this.lstIncludeApplicationIds.contains(appid)) return;
        appid=appid.trim();
        if(this.owner.getPageBean().getApplicationChild(appid,true)==null)
        {
            throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"上的打印配置失败，其include属性配置的应用ID"+appid+"不存在");
        }
        ReportBean rbean=this.owner.getPageBean().getReportChild(appid,true);
        if(rbean!=null) this.mReportidAndPagesize.put(appid,Integer.MIN_VALUE);
        this.lstIncludeApplicationIds.add(appid);
        this.includeApplicationids+=appid+";";
    }

    private void loadSubpageConfig()
    {
        if(this.elePrintBean==null) return;
        List<XmlElementBean> lstEleSubpageBeans=this.elePrintBean.getLstChildElementsByName("subpage");//取到所有的<subpage/>子标签
        PrintSubPageBean pspagebeanTmp;
        String printContentTmp;
        if(lstEleSubpageBeans==null||lstEleSubpageBeans.size()==0)
        {//没有配置<subpage/>，则整个<print/>视为一个<subpage/>
            printContentTmp=this.elePrintBean.getContent();
            if(printContentTmp!=null&&!printContentTmp.trim().equals(""))
            {//直接在<print/>中配置了打印内容
                pspagebeanTmp=new PrintSubPageBean(this);
                pspagebeanTmp.addIncludeSplitPrintReportids(this.lstIncludeApplicationIds);
                parsePrintContent(pspagebeanTmp,printContentTmp);
                this.lstPrintPageBeans.add(pspagebeanTmp);
            }
        }else
        {//配置了<subpage/>
            for(XmlElementBean eleSubpageBeanTmp:lstEleSubpageBeans)
            {
                pspagebeanTmp=new PrintSubPageBean(this);
                String mergeup=eleSubpageBeanTmp.attributeValue("mergeup");
                pspagebeanTmp.setMergeUp(mergeup==null||!mergeup.toLowerCase().trim().equals("false"));
                String splitprintreport=eleSubpageBeanTmp.attributeValue("splitprintreport");
                if(splitprintreport!=null&&!splitprintreport.trim().equals(""))
                {
                    pspagebeanTmp.addIncludeSplitPrintReportids(Tools.parseStringToList(splitprintreport,";",false));
                }
                if(pspagebeanTmp.isSplitPrintPage())
                {
                    String minpagecount=eleSubpageBeanTmp.attributeValue("minpagecount");
                    if(minpagecount!=null&&!minpagecount.trim().equals(""))
                    {
                        try
                        {
                            pspagebeanTmp.setMinpagecount(Integer.parseInt(minpagecount));
                        }catch(NumberFormatException e)
                        {
                            log.warn("组件"+this.owner.getPath()+"中配置的<subpage/>的minpagecount："+minpagecount+"不是有效数字",e);
                        }
                    }
                    String maxpagecount=eleSubpageBeanTmp.attributeValue("maxpagecount");
                    if(maxpagecount!=null&&!maxpagecount.trim().equals(""))
                    {
                        try
                        {
                            pspagebeanTmp.setMaxpagecount(Integer.parseInt(maxpagecount));
                        }catch(NumberFormatException e)
                        {
                            log.warn("组件"+this.owner.getPath()+"中配置的<subpage/>的maxpagecount："+maxpagecount+"不是有效数字",e);
                        }
                    }
                }
                printContentTmp=eleSubpageBeanTmp.getContent();
                if(printContentTmp==null||printContentTmp.trim().equals("")) continue;//对于<print/>中配置的<subpage/>，如果没有打印内容，则此子页相当于没配置
                parsePrintContent(pspagebeanTmp,printContentTmp);
                this.lstPrintPageBeans.add(pspagebeanTmp);
            }
        }
        this.elePrintBean=null;//解析完转换成相应对象后变为null
        PrintTemplateElementBean ptelebeanTmp;
        if(this.lstPrintPageBeans.size()==0)
        {//没有在<print/>中配置有效的打印代码
            this.isTemplatePrintValue=Boolean.TRUE;
            if(this.owner instanceof ReportBean)
            {
                pspagebeanTmp=new PrintSubPageBean(this);
                pspagebeanTmp.addIncludeSplitPrintReportids(this.lstIncludeApplicationIds);
                ptelebeanTmp=new PrintTemplateElementBean(this.getPlaceholderIndex());
                ptelebeanTmp.setType(PrintTemplateElementBean.ELEMENT_TYPE_APPLICATIONID);
                ptelebeanTmp.setValueObj(this.owner.getId());//这里不直接将全局打印模板存在这里，是为了后面打印时会取到此报表<report/>的printwidth，如果直接存全局静态模板，则打印时不会取用此报表的printwidth
                //                ptelebeanTmp.setValueObj(Config.getInstance().getDefaultReportPrintTplBean());//用全局报表静态打印模板
                pspagebeanTmp.addPrintElement(ptelebeanTmp);
                this.lstPrintPageBeans.add(pspagebeanTmp);
                isUseDefaultPrintTemplate=true;
            }else
            {
                for(String appidTmp:this.lstIncludeApplicationIds)
                {
                    pspagebeanTmp=new PrintSubPageBean(this);
                    pspagebeanTmp.setMergeUp(false);//各自做为一页进行打印
                    pspagebeanTmp.addIncludeSplitPrintReportid(appidTmp);
                    ptelebeanTmp=new PrintTemplateElementBean(this.getPlaceholderIndex());
                    ptelebeanTmp.setType(PrintTemplateElementBean.ELEMENT_TYPE_APPLICATIONID);
                    ptelebeanTmp.setValueObj(appidTmp);
                    pspagebeanTmp.addPrintElement(ptelebeanTmp);
                    this.lstPrintPageBeans.add(pspagebeanTmp);
                }
            }
        }
    }

    protected void parsePrintContent(PrintSubPageBean pspagebean,String printContent)
    {
        this.isTemplatePrintValue=Boolean.TRUE;
        PrintTemplateElementBean ptelebeanTmp=new PrintTemplateElementBean(this.getPlaceholderIndex());
        if(ComponentConfigLoadAssistant.getInstance().isStaticTemplateResource(printContent))
        {
            ptelebeanTmp.setType(PrintTemplateElementBean.ELEMENT_TYPE_STATICTPL);
            ptelebeanTmp.setValueObj(ComponentConfigLoadAssistant.getInstance().getStaticTemplateBeanByConfig(this.owner.getPageBean(),printContent));
        }else if(Tools.isDefineKey("jsp",printContent))
        {
            ptelebeanTmp.setType(PrintTemplateElementBean.ELEMENT_TYPE_DYNTPL);
            ptelebeanTmp.setValueObj(Tools.getRealKeyByDefine("jsp",printContent));
        }else
        {//直接在<print></print>中配置静态模板内容
            ptelebeanTmp.setType(PrintTemplateElementBean.ELEMENT_TYPE_STATICTPL);
            ptelebeanTmp.setValueObj(TemplateParser.parseTemplateByContent(printContent));
        }
        pspagebean.addPrintElement(ptelebeanTmp);
    }

    protected abstract void createPrintJsScript();

    public String getPrintJsMethodName()
    {
        return this.owner.getGuid()+"_print";
    }

    public Object clone()
    {
        try
        {
            return super.clone();
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
