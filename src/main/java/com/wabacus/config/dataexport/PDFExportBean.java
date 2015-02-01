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
package com.wabacus.config.dataexport;

import java.util.List;

import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.PrintButton;
import com.wabacus.system.intercept.AbsPdfInterceptor;
import com.wabacus.util.Consts;

public class PDFExportBean extends AbsDataExportBean
{
    private String pdftemplate;
    
    private int pagesize=Integer.MIN_VALUE;
    
    private Rectangle pdfpagesizeObj;

    private float width;
    
    private boolean fullpagesplit;//指定报表分页时，是否每页都显示所有内容
    
    private int titlefontsize;
    
    private int dataheaderfontsize;
    
    private int datafontsize;
    
    private boolean isPrint;
    
    private AbsPdfInterceptor interceptorObj;//拦截器对象
    
    public PDFExportBean(IComponentConfigBean owner,String type)
    {
        super(owner,type);
    }

    public int getPagesize()
    {
        return pagesize;
    }

    public Rectangle getPdfpagesizeObj()
    {
        return pdfpagesizeObj;
    }

    public String getPdftemplate()
    {
        return pdftemplate;
    }

    public void setPdftemplate(String pdftemplate)
    {
        this.pdftemplate=pdftemplate;
    }

    public float getWidth()
    {
        return width;
    }

    public boolean isPrint()
    {
        return isPrint;
    }

    public void setPrint(boolean isPrint)
    {
        this.isPrint=isPrint;
    }

    public boolean isFullpagesplit()
    {
        return fullpagesplit;
    }

    public int getTitlefontsize()
    {
        return titlefontsize;
    }

    public int getDataheaderfontsize()
    {
        return dataheaderfontsize;
    }

    public int getDatafontsize()
    {
        return datafontsize;
    }

    public AbsPdfInterceptor getInterceptorObj()
    {
        return interceptorObj;
    }

    public void setInterceptorObj(AbsPdfInterceptor interceptorObj)
    {
        this.interceptorObj=interceptorObj;
    }

    public void loadConfig(XmlElementBean eleDataExport)
    {
        super.loadConfig(eleDataExport);
        if(this.isPrint)
        {
            pdftemplate=eleDataExport.getContent();
        }else
        {
            pdftemplate=eleDataExport.attributeValue("template");
        }
        if(pdftemplate!=null) pdftemplate=pdftemplate.trim();
        String strpagesize=eleDataExport.attributeValue("pagesize");
        if(strpagesize!=null&&!strpagesize.trim().equals(""))
        {
            this.pagesize=Integer.parseInt(strpagesize);
        }
        String pdfpagesize=eleDataExport.attributeValue("printpagesize");
        if(pdfpagesize==null||pdfpagesize.trim().equals("")) pdfpagesize="A3";
        try
        {
            pdfpagesizeObj=(Rectangle)PageSize.class.getDeclaredField(pdfpagesize).get(null);
            if(pdfpagesizeObj==null)
            {
                pdfpagesizeObj=PageSize.A3;
            }
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"数据导出功能失败，配置的pagesize属性"+pdfpagesize+"不支持",e);
        }
        String strwidth=eleDataExport.attributeValue("width");
        if(strwidth!=null&&!strwidth.trim().equals(""))
        {
            width=Float.parseFloat(strwidth);
        }
        String fullpagesplit=eleDataExport.attributeValue("fullpagesplit");
        if(fullpagesplit!=null&&!fullpagesplit.trim().equals(""))
        {
            this.fullpagesplit=!fullpagesplit.toLowerCase().trim().equals("false");
        }else
        {
            this.fullpagesplit=true;
        }
        String titlefontsize=eleDataExport.attributeValue("titlefontsize");
        if(titlefontsize!=null&&!titlefontsize.trim().equals("")) this.titlefontsize=Integer.parseInt(titlefontsize);
        String dataheaderfontsize=eleDataExport.attributeValue("dataheaderfontsize");
        if(dataheaderfontsize!=null&&!dataheaderfontsize.trim().equals("")) this.dataheaderfontsize=Integer.parseInt(dataheaderfontsize);
        String datafontsize=eleDataExport.attributeValue("datafontsize");
        if(datafontsize!=null&&!datafontsize.trim().equals("")) this.datafontsize=Integer.parseInt(datafontsize);
        String interceptor=eleDataExport.attributeValue("interceptor");
        if(interceptor!=null)
        {
            interceptor=interceptor.trim();
            if(interceptor.equals(""))
            {
                this.interceptorObj=null;
            }else
            {
                Object objTmp=null;
                try
                {
                    objTmp=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(interceptor).newInstance();
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("为组件"+this.owner.getPath()+"配置的导出到PDF文件中指定的拦截器类"+interceptor+"无法实例化",e);
                }
                if(!(objTmp instanceof AbsPdfInterceptor))
                {
                    throw new WabacusConfigLoadingException("为组件"+this.owner.getPath()+"配置的导出到PDF文件中指定的拦截器类"+interceptor+"没有继承框架父类"
                            +AbsPdfInterceptor.class.getName());
                }
                this.interceptorObj=(AbsPdfInterceptor)objTmp;
            }
        }
    }

    public void doPostLoad()
    {
        super.doPostLoad();
        if(this.pagesize==Integer.MIN_VALUE&&this.owner instanceof ReportBean)
        {
            this.pagesize=((ReportBean)this.owner).getLstPagesize().get(0);
        }
        if(this.isPrint)
        {
            List<AbsButtonType> lstPrintButtons=null;
            if(this.owner.getButtonsBean()!=null) lstPrintButtons=this.owner.getButtonsBean().getLstPrintTypeButtons(Consts.PRINTTYPE_PRINT);
            if(lstPrintButtons==null||lstPrintButtons.size()==0)
            {
                AbsButtonType buttonObj=Config.getInstance().getResourceButton(null,this.owner,
                        Consts.M_PRINT_DEFAULTBUTTONS.get(Consts.PRINTTYPE_PRINT),PrintButton.class);
                buttonObj.setDefaultNameIfNoName();
                if(this.owner instanceof AbsContainerConfigBean)
                {
                    buttonObj.setPosition("top");//对于容器，默认位置在顶部
                }
                ComponentConfigLoadManager.addButtonToPositions(this.owner,buttonObj);
            }
            lstPrintButtons=this.owner.getButtonsBean().getLstPrintTypeButtons(Consts.PRINTTYPE_PRINT);
            for(int j=0;j<lstPrintButtons.size();j++)
            {
                ((PrintButton)lstPrintButtons.get(j)).setPdfPrint(true);
            }
        }
    }
}
