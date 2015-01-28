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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.print.AbsPrintProvider;
import com.wabacus.system.print.DefaultPrintProvider;
import com.wabacus.util.Tools;

public class DefaultPrintProviderConfigBean extends AbsPrintProviderConfigBean
{
    private String paperstyleproperty;//纸张所在<div/>的样式字符串
    
    private String paperwidth;
    
    private List<String> lstPrintPagesizes;
    
    public final static Map<String,String> mPrintPagesize=new HashMap<String,String>();
    
    static
    {
        mPrintPagesize.put("A0_WIDTH","889mm");
        mPrintPagesize.put("A1_WIDTH","597mm");
        mPrintPagesize.put("A2_WIDTH","420mm");
        mPrintPagesize.put("A3_WIDTH","297mm");
        mPrintPagesize.put("A4_WIDTH","210mm");
        mPrintPagesize.put("A5_WIDTH","148mm");
        mPrintPagesize.put("A6_WIDTH","105mm");
        mPrintPagesize.put("B0_WIDTH","787mm");
        mPrintPagesize.put("B1_WIDTH","520mm");
        mPrintPagesize.put("B2_WIDTH","370mm");
        mPrintPagesize.put("B3_WIDTH","260mm");
        mPrintPagesize.put("B4_WIDTH","185mm");
        mPrintPagesize.put("B5_WIDTH","130mm");
        mPrintPagesize.put("A0_HEIGHT","1194mm");
        mPrintPagesize.put("A1_HEIGHT","840mm");
        mPrintPagesize.put("A2_HEIGHT","597mm");
        mPrintPagesize.put("A3_HEIGHT","420mm");
        mPrintPagesize.put("A4_HEIGHT","297mm");
        mPrintPagesize.put("A5_HEIGHT","210mm");
        mPrintPagesize.put("A6_HEIGHT","148mm");
        mPrintPagesize.put("B0_HEIGHT","1092mm");
        mPrintPagesize.put("B1_HEIGHT","740mm");
        mPrintPagesize.put("B2_HEIGHT","520mm");
        mPrintPagesize.put("B3_HEIGHT","370mm");
        mPrintPagesize.put("B4_HEIGHT","260mm");
        mPrintPagesize.put("B5_HEIGHT","185mm");
    }
    
    public DefaultPrintProviderConfigBean(IComponentConfigBean owner)
    {
        super(owner);
    }
    
    public AbsPrintProvider createPrintProvider(ReportRequest rrequest)
    {
        return new DefaultPrintProvider(rrequest,this);
    }

    public List<String> getLstPrintPagesizes()
    {
        return lstPrintPagesizes;
    }

    public void setLstPrintPagesizes(List<String> lstPrintPagesizes)
    {
        this.lstPrintPagesizes=lstPrintPagesizes;
    }

    public String getPaperstyleproperty()
    {
        return paperstyleproperty;
    }

    public String getPaperwidth()
    {
        return paperwidth;
    }

    public void loadConfig(XmlElementBean elePrintBean)
    {
        super.loadConfig(elePrintBean);
        if(printpagesize!=null&&!printpagesize.trim().equals(""))
        {
            printpagesize=printpagesize.toUpperCase().trim();
            List<String> lstTmp=Tools.parseStringToList(printpagesize,";",false);
            this.lstPrintPagesizes=new ArrayList<String>();
            for(String pagesizeTmp:lstTmp)
            {
                pagesizeTmp=pagesizeTmp.trim();
                if(!mPrintPagesize.containsKey(pagesizeTmp+"_WIDTH")) continue;
                this.lstPrintPagesizes.add(pagesizeTmp);
            }
        }
        String styleproperty=elePrintBean.attributeValue("paperstyleproperty");
        if(styleproperty!=null&&!styleproperty.trim().equals(""))
        {
            this.paperstyleproperty=styleproperty.trim();
            styleproperty=Tools.getPropertyValueByName("style",this.paperstyleproperty,false);
        }
        String widthInStyle=Tools.getPropertyValueFromStyle("width",styleproperty);
        //String heightInStyle=Tools.getPropertyValueFromStyle("height",styleproperty);
        String pagewidth=null;
        if(this.lstPrintPagesizes!=null&&this.lstPrintPagesizes.size()>0)
        {
            if(widthInStyle!=null&&!widthInStyle.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的打印配置失败，为其<print/>配置了printpagesize后，不能再在paperstyleproperty中指定width");
            }
            pagewidth=mPrintPagesize.get(this.lstPrintPagesizes.get(0)+"_WIDTH");
            this.paperwidth=pagewidth;
        }else
        {
            if(widthInStyle!=null&&!widthInStyle.trim().equals(""))
            {//在paperstyleproperty中配置了宽度
                this.paperwidth=widthInStyle;
            }else
            {
                pagewidth=mPrintPagesize.get("A4_WIDTH");
                this.paperwidth=pagewidth;
            }
        }
        String defaultborder=null;
        String border=Tools.getPropertyValueFromStyle("border",styleproperty);
        if(border==null||border.trim().equals(""))
        {
            defaultborder="1px solid #aaaaaa";
        }
        if(this.paperstyleproperty==null) this.paperstyleproperty="";
        if(pagewidth!=null&&!pagewidth.trim().equals("")||defaultborder!=null&&!defaultborder.trim().equals(""))
        {
            this.paperstyleproperty=Tools.removePropertyValueByName("style",this.paperstyleproperty);
            if(styleproperty==null) styleproperty="";
            if(!styleproperty.equals("")&&!styleproperty.endsWith(";")) styleproperty=styleproperty+";";
            if(pagewidth!=null&&!pagewidth.trim().equals(""))
            {//需要加默认宽度
                styleproperty=styleproperty+"width:"+pagewidth+";";
            }
            if(defaultborder!=null&&!defaultborder.trim().equals(""))
            {
                styleproperty=styleproperty+"border:"+defaultborder+";";
            }
            this.paperstyleproperty=this.paperstyleproperty+" style=\""+styleproperty+"\"";
        }
        this.isSetting=false;
        this.isPreview=false;
        this.printPageInfo=null;
    }

    protected void createPrintJsScript()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("function "+this.getPrintJsMethodName()+"(jobname,content,printtype){");
        resultBuf.append("  if(content==null||content=='') return;");
        resultBuf.append("  var code=\"<body \";");
        //resultBuf.append("  if(printtype=='"+Consts.PRINTTYPE_PRINT+"'&&ISOPERA) code=code+\" onload='window.print()'\";");//opera浏览器不能调用printwin.window.print()方法进行打印，所以放在onload中，而IE浏览器如果放在onload中会提示是否允许ActiveX控件，所以放在后面调用printwin.window.print()进行打印
        resultBuf.append("  code=code+\">\";");
        resultBuf.append("  code=code+content+\"</body>\";");
        resultBuf.append("  var printwin=window.open('','win_"+this.owner.getId()+"','');");
        resultBuf.append("  printwin.opener = null;printwin.document.write(code);printwin.document.close();");
        resultBuf.append("}");
        JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(this.owner.getPageBean(),resultBuf.toString());
    }
}

