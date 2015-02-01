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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.IApplicationConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.print.AbsPrintProvider;
import com.wabacus.system.print.LodopPrintProvider;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class LodopPrintProviderConfigBean extends AbsPrintProviderConfigBean
{
    private final static Log log=LogFactory.getLog(LodopPrintProviderConfigBean.class);

    private Boolean isLodopCodePrintValue=null;
    
    public LodopPrintProviderConfigBean(IComponentConfigBean owner)
    {
        super(owner);
    }
    
    public boolean isLodopCodePrintValue()
    {
        if(isLodopCodePrintValue==null) return false;
        return isLodopCodePrintValue.booleanValue();
    }

    public void initPrint(ReportRequest rrequest)
    {
        if(rrequest.getAttribute("LODOP_PRINT_INITIALIZED")==null)
        {
            rrequest.getAttributes().put("LODOP_PRINT_INITIALIZED","true");
            rrequest.getWResponse().println("<object id=\"LODOP_OBJECT\" classid=\"clsid:2105C259-1E0C-4534-8141-A753534CB4CA\" width=0 height=0>");
            rrequest.getWResponse().println("<embed id=\"LODOP_EM\" width=0 height=0 type=\"application/x-print-lodop\" pluginspage=\"install_lodop.exe\"></embed>");
            rrequest.getWResponse().println("</object>");
        }
    }

    public AbsPrintProvider createPrintProvider(ReportRequest rrequest)
    {
        return new LodopPrintProvider(rrequest,this);
    }

    public void doPostLoad()
    {
        super.doPostLoad();
        String lodopjs=Config.webroot+"/webresources/component/lodop/LodopFuncs.js";
        lodopjs=Tools.replaceAll(lodopjs,"//","/");
        this.owner.getPageBean().addMyJavascriptFile(lodopjs,0);
    }

    protected void parsePrintContent(PrintSubPageBean pspagebean,String printContent)
    {
        if(printContent==null||printContent.trim().equals("")) return;
        printContent=printContent.trim();
        if(printContent.indexOf("LODOP_OBJ")<0||printContent.indexOf(".")<0
                ||printContent.indexOf("(")<0||printContent.indexOf(")")<0||printContent.indexOf(";")<0)
        {
            if(this.isLodopCodePrintValue==Boolean.TRUE)
            {
                throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印代码失败，不能在打印内容中同时配置lodop代码和模板");
            }
            this.isLodopCodePrintValue=Boolean.FALSE;
            super.parsePrintContent(pspagebean,printContent);
        }else
        {
            if(this.isLodopCodePrintValue==Boolean.FALSE)
            {
                throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印代码失败，不能在打印内容中同时配置lodop代码和模板");
            }
            this.isLodopCodePrintValue=Boolean.TRUE;
            printContent=parseCertainTypeDynValueInLodopCode(pspagebean,printContent,"wx_content");
            printContent=parseCertainTypeDynValueInLodopCode(pspagebean,printContent,"request");//解析里面request{...}格式的动态内容
            printContent=parseCertainTypeDynValueInLodopCode(pspagebean,printContent,"session");
            pspagebean.setTagContent(printContent);
        }
    }
    
    private String parseCertainTypeDynValueInLodopCode(PrintSubPageBean pspagebean,String lodopcode,String dyntype)
    {
        dyntype=dyntype+"{";
        int idx=lodopcode.indexOf(dyntype);
        String str1, str2, dyncontentTmp, appidTmp;
        PrintTemplateElementBean tplElementBeanTmp;
        while(idx>=0)
        {
            str1=lodopcode.substring(0,idx);
            str2=lodopcode.substring(idx+dyntype.length());
            idx=str2.indexOf("}");
            if(idx<0)
            {
                throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，解析其打印代码时，没有找到“"+dyntype+"”闭合的“}”号");
            }
            dyncontentTmp=str2.substring(0,idx).trim();
            str2=str2.substring(idx+1).trim();
            if(dyncontentTmp.equals(""))
            {
                throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，解析其打印代码时，“"+dyntype+"}”中间为空");
            }
            tplElementBeanTmp=new PrintTemplateElementBean(this.getPlaceholderIndex());
            if(dyntype.equals("wx_content{"))
            {
                List<String> lstConfigValues=Tools.parseStringToList(dyncontentTmp,".",false);
                appidTmp=lstConfigValues.get(0);
                if(appidTmp.equals("this"))
                {
                    if(!(this.owner instanceof ReportBean))
                    {
                        throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，此组件不是报表，不能在其打印内容中出现this关键字");
                    }
                    appidTmp=this.owner.getId();
                    lstConfigValues.remove(0);//将this替换成真正的reportid
                    lstConfigValues.add(0,appidTmp);
                }
                parseTplApplicationElement(lstConfigValues);
                tplElementBeanTmp.setType(PrintTemplateElementBean.ELEMENT_TYPE_APPLICATION);
                tplElementBeanTmp.setValueObj(lstConfigValues);
            }else
            {
                tplElementBeanTmp.setType(PrintTemplateElementBean.ELEMENT_TYPE_OTHER);
                if(dyntype.equals("request{")||dyntype.equals("session{")) dyncontentTmp=dyntype+"{"+dyncontentTmp+"}";//保留requset/session{key}的方式，以便打印时知道是从request/session中取数据
                tplElementBeanTmp.setValueObj(dyncontentTmp);
            }
            
            if(pspagebean.isSplitPrintPage())
            {
                lodopcode=str1+"getAdvancedPrintRealValueForPage(content,'"+pspagebean.getPlaceholder()+"_'+i,'"+tplElementBeanTmp.getPlaceholder()+"')"+str2;
            }else
            {
                lodopcode=str1+"getAdvancedPrintRealValue(content,'"+tplElementBeanTmp.getPlaceholder()+"')"+str2;
            }
            pspagebean.addPrintElement(tplElementBeanTmp);
            //tplElementBeanTmp.setPrintAsHtml(isPrintContentAsHtml(str1));//因为当前打印内容的打印代码肯定在打印内容的前面，所以从前面字符串str1中找到当前打印内容是html还是text类型
            idx=lodopcode.indexOf(dyntype);
        }
        return lodopcode;
    }

    private boolean parseTplApplicationElement(List<String> lstConfigValues)
    {
        IComponentConfigBean ccbean=this.owner.getPageBean().getChildComponentBean(lstConfigValues.get(0),true);
        if(ccbean==null)
        {
            throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，指定的打印组件ID："+lstConfigValues.get(0)+"在本页面中不存在");
        }
        if(!(ccbean instanceof IApplicationConfigBean))
        {
            throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，不能在打印代码的“wx_content{}”中指定容器ID");
        }
        if(!this.lstIncludeApplicationIds.contains(ccbean.getId()))
        {//打印内容中指定的应用ID没有出现在此组件<print/>的include属性中，则加进去
            log.warn("组件"+this.owner.getPath()+"的打印内容中配置的组件ID："+ccbean.getId()+"没有出现在其<print/>的include属性中");
            this.addIncludeApplicationId(ccbean.getId());
        }
        IApplicationConfigBean acbean=(IApplicationConfigBean)ccbean;
        if(acbean instanceof ReportBean)
        {
            if(lstConfigValues.size()==1) return true;
            if(lstConfigValues.size()>1&&!Consts.lstReportParts.contains(lstConfigValues.get(1)))
            {
                throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，在wx_content{}中指定打印报表的"+lstConfigValues.get(1)+"部分不存在");
            }
            if(lstConfigValues.size()==2&&Consts.BUTTON_PART.equals(lstConfigValues.get(1)))
            {//即this.button格式
                throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，在wx_content{"+lstConfigValues.get(0)
                        +".button}中没有指定要打印的按钮");
            }
            ReportBean rbean=(ReportBean)acbean;
            if(lstConfigValues.get(1).equals("data"))
            {
                if(lstConfigValues.size()==2) return true;
                if(lstConfigValues.get(2).equals("[title]")||lstConfigValues.get(2).equals("[data]"))
                {
                    if(rbean.isDetailReportType()||rbean.isChartReportType())
                    {
                        throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，id为"+rbean.getId()
                                +"的报表为细览/图形报表，不能指定为reportid.data.[title]/reportid.data.[data]格式");
                    }
                    if(lstConfigValues.size()>3)
                    {
                        throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()
                                +"的打印配置失败，不能指定为reportid.data.[title].xxx/reportid.data.[data].xxx格式");
                    }
                    return lstConfigValues.get(2).equals("[data]");
                }
                ColBean cbean=rbean.getDbean().getColBeanByColProperty(lstConfigValues.get(2));
                if(cbean==null)
                {
                    throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，报表"+rbean.getPath()+"中不存在property/column为"
                            +lstConfigValues.get(2)+"的<col/>，无法打印其内容");
                }
                if(lstConfigValues.size()==3) return true;//reportid.data.col格式
                if(!lstConfigValues.get(3).equals("label")&&!lstConfigValues.get(3).equals("value"))
                {
                    throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，不能指定为reportid.data.col."+lstConfigValues.get(3));
                }
                return lstConfigValues.get(3).equals("value");
            }
        }else if(lstConfigValues.size()>1)
        {
            throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，在wx_content{}中指定打印组件"+acbean.getId()
                    +"时，因为它不是报表，因此不能指定打印其某一部分");
        }
        return false;
    }

    protected void createPrintJsScript()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("function "+this.getPrintJsMethodName()+"(jobname,content,printtype){");
        resultBuf.append("  if(content==null||content=='') return;");
        resultBuf.append("var currentpageno=1;var totalpagecount=1;");
        if(this.printPageInfo!=null&&!this.printPageInfo.trim().equals(""))
        {
            resultBuf.append("totalpagecount=getPrintPageCount(content,'WX_PRINT_TOTAL');");
            resultBuf.append("if(totalpagecount<=0) return;");
        }
        resultBuf.append("  LODOP_OBJ=getLodop(document.getElementById('LODOP_OBJECT'),document.getElementById('LODOP_EM'));");
        resultBuf.append("  if(LODOP_OBJ==null||typeof(LODOP_OBJ.VERSION)=='undefined') return;if(jobname!=null&&jobname!='') LODOP_OBJ.PRINT_INIT(jobname);");
        if(this.printpagesize!=null&&!this.printpagesize.trim().equals(""))
        {
            resultBuf.append("LODOP_OBJ.SET_PRINT_PAGESIZE("+this.printpagesize+");");
        }
        for(PrintSubPageBean pspbeanTmp:this.lstPrintPageBeans)
        {
            if(pspbeanTmp.isSplitPrintPage())
            {//如果是分页打印部分
                String pagecountName="pagecount_"+pspbeanTmp.getPlaceholder();
                resultBuf.append(" var "+pagecountName+"=getPrintPageCount(content,'"+pspbeanTmp.getPlaceholder()+"');");
                resultBuf.append("for(var i=0;i<"+pagecountName+";i++){");
                if(!pspbeanTmp.isMergeUp())
                {
                    resultBuf.append("if(i==0&&currentpageno!=1) LODOP_OBJ.NewPage();");//本页是本子页的第一页，且上面已经有打印页，则新增一页打印此页（如果本页是所有打印页的第一页，则不用新增一页，否则会在第一页多出一空白页；如果本页是本<subpage/>第2页及其后面的页，也不用在这里新增一页，因为上面的页打印完后都会自动新增一页，如果这里再新增一页，则也会多出一空白页）
                }
                resultBuf.append(printPageContent(pspbeanTmp));
                if(pspbeanTmp.isMergeUp())
                {
                    resultBuf.append("if(i>0||currentpageno==1){"+createCurrentPageInfoScript()+"currentpageno++;}");
                }else
                {
                    resultBuf.append(createCurrentPageInfoScript()).append("currentpageno++;");
                }
                resultBuf.append("if(i<"+pagecountName+"-1){LODOP_OBJ.NewPage();}");//当前页不是此<subpage/>的最后一页，则新增一页显示后面的内容，如果是本<subpage/>的最后一页，则不newpage，因为后面的页可能mergeup为true，因此放在下一个<subpage/>中决定是否先新增一页再显示，还是显示完一页后再新增一页
                resultBuf.append("}");
            }else
            {
                if(!pspbeanTmp.isMergeUp())
                {//不与上面的内容打印在一页中
                    resultBuf.append(" if(currentpageno!=1) LODOP_OBJ.NewPage();");
                }
                resultBuf.append(printPageContent(pspbeanTmp));
                if(pspbeanTmp.isMergeUp())
                {
                    resultBuf.append("if(currentpageno==1){"+createCurrentPageInfoScript()+"currentpageno++;}");
                }else
                {
                    resultBuf.append(createCurrentPageInfoScript()).append("currentpageno++;");
                }
            }
            pspbeanTmp.setTagContent(null);
        }
        resultBuf.append("  if(printtype=='"+Consts.PRINTTYPE_PRINT+"'){ LODOP_OBJ.PRINT();");//直接打印
        resultBuf.append("  }else if(printtype=='"+Consts.PRINTTYPE_PRINTPREVIEW+"'){ LODOP_OBJ.PREVIEW();");
        resultBuf.append("  }else if(printtype=='"+Consts.PRINTTYPE_PRINTSETTING+"'){ LODOP_OBJ.PRINT_DESIGN();}");
        resultBuf.append("}");
        JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(this.owner.getPageBean(),resultBuf.toString());
    }
    
    private String printPageContent(PrintSubPageBean pspbean)
    {
        if(this.isLodopCodePrintValue!=null&&this.isLodopCodePrintValue==Boolean.TRUE)
        {
            return pspbean.getTagContent();
        }
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("LODOP_OBJ.ADD_PRINT_HTM('10','10','96%','100%',getAdvancedPrintRealValueForPage(content,'"+pspbean.getPlaceholder());
        if(pspbean.isSplitPrintPage())
        {
            resultBuf.append("_'+i");
        }else
        {
            resultBuf.append("'");
        }
        resultBuf.append("));");
        return resultBuf.toString();
    }
    
    private String createCurrentPageInfoScript()
    {
        if(this.printPageInfo==null||this.printPageInfo.trim().equals("")) return "";
        String content=this.printPageInfo;
        content=Tools.replaceAll(content,"wx_content{pageno}","currentpageno");
        content=Tools.replaceAll(content,"wx_content{pagecount}","totalpagecount");
        if(content.startsWith("LODOP_OBJ.")) return content;//直接配置的是lodop代码
        return "LODOP_OBJ.ADD_PRINT_HTM('96%', 1, '100%', '100%',"+content+");";
    }
}
