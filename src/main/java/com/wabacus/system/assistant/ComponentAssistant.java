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

import java.io.IOException;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.config.template.tags.AbsTagInTemplate;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.intercept.AbsPageInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class ComponentAssistant
{
    private final static ComponentAssistant instance=new ComponentAssistant();

    protected ComponentAssistant()
    {}

    public static ComponentAssistant getInstance()
    {
        return instance;
    }

    public void validComponentHeaderTpl(IComponentConfigBean ccbean,TemplateBean tplBean)
    {
        if(tplBean==null) return;
        if(tplBean.getLstTagChildren()==null) return;
        for(AbsTagInTemplate tagObjTmp:tplBean.getLstTagChildren())
        {
            if(Consts_Private.TAGNAME_HEADER.equals(tagObjTmp.getTagname()))
            {//如果是用在header中的模板，不能在模板内容中出现<wx:header/>
                throw new WabacusConfigLoadingException("报表"+ccbean.getPath()+"的header的模板内容包括<wx:header/>，导致死循环");
            }
        }
    }
    
    public void validComponentFooterTpl(IComponentConfigBean ccbean,TemplateBean tplBean)
    {
        if(tplBean==null) return;
        if(tplBean.getLstTagChildren()==null) return;
        for(AbsTagInTemplate tagObjTmp:tplBean.getLstTagChildren())
        {
            if(Consts_Private.TAGNAME_FOOTER.equals(tagObjTmp.getTagname()))
            {//如果是用在footer中的模板，不能在模板内容中出现<wx:footer/>
                throw new WabacusConfigLoadingException("组件"+ccbean.getPath()+"的footer的模板内容包括<wx:footer/>，导致死循环");
            }
        }
    }
    
    public void doPostLoadForComponentScroll(IComponentConfigBean ccbean,boolean showScrollX,boolean showScrollY,String scrollWidth,String scrollHeight,
            String scrollstyle)
    {
        if(!Consts_Private.SCROLLSTYLE_IMAGE.equals(scrollstyle)) return;
        if(!showScrollX&&!showScrollY) return;
        String scrolljs="/webresources/script/wabacus_scroll.js";
//        {
//            scrolljs="/webresources/script/wabacus_scroll.js";
//            {
//            scrolljs="/webresources/script/"+encode.toLowerCase()+"/wabacus_scroll.js";
        scrolljs=Config.webroot+"/"+scrolljs;
        scrolljs=Tools.replaceAll(scrolljs,"//","/");
        ccbean.getPageBean().addMyJavascriptFile(scrolljs,-1);
        String css=Config.webroot+"/webresources/skin/"+Consts_Private.SKIN_PLACEHOLDER+"/wabacus_scroll.css";
        css=Tools.replaceAll(css,"//","/");
        ccbean.getPageBean().addMyCss(css);

        if(showScrollX&&showScrollY)
        {//显示纵横滚动条
            ccbean.addOnloadMethod(new OnloadMethodBean(Consts_Private.ONlOAD_IMGSCROLL,"showComponentScroll('"+ccbean.getGuid()+"','"+scrollHeight
                    +"',23)"));
        }else if(showScrollX)
        {
            ccbean.addOnloadMethod(new OnloadMethodBean(Consts_Private.ONlOAD_IMGSCROLL,"showComponentScroll('"+ccbean.getGuid()+"','-1',22)"));
        }else if(showScrollY)
        {
            ccbean.addOnloadMethod(new OnloadMethodBean(Consts_Private.ONlOAD_IMGSCROLL,"showComponentScroll('"+ccbean.getGuid()+"','"+scrollHeight
                    +"',21)"));
        }
    }
    
    public String showComponentScrollStartPart(IComponentConfigBean ccbean,boolean showScrollX,boolean showScrollY,String scrollWidth,String scrollHeight,
            String scrollstyle)
    {
        if(!showScrollX&&!showScrollY) return "";
        StringBuffer resultBuf=new StringBuffer();
        if(Consts_Private.SCROLLSTYLE_NORMAL.equals(scrollstyle))
        {
            resultBuf.append("<div><div onmouseover=\"this.style.height='100%'\" style=\"margin:0;");
            if(showScrollX)
            {//要显示横向滚动条
                resultBuf.append("width:").append(scrollWidth).append(";overflow-x:auto;");
            }else
            {
                resultBuf.append("width:100%;overflow-x:hidden;");
            }
            if(showScrollY)
            {
                resultBuf.append("max-height:").append(scrollHeight).append(";overflow-y:auto;");
                resultBuf.append("height:expression(this.scrollHeight>parseInt('").append(scrollHeight).append("')?'").append(scrollHeight).append(
                        "':'auto');");
            }else
            {
                resultBuf.append("overflow-y:hidden;");
            }
            resultBuf.append("\">");
        }else
        {
            resultBuf.append("<div id=\"scroll_").append(ccbean.getGuid()).append("\"");
            resultBuf.append(" style=\"overflow-x:hidden;overflow-y:hidden;margin:0;");
            if(showScrollX)
            {
                resultBuf.append("width:").append(scrollWidth).append(";");
            }
            resultBuf.append("\">");
        }
        return resultBuf.toString();
    }

    public String showComponentScrollEndPart(String scrollstyle,boolean showScrollX,boolean showScrollY)
    {
        if(!showScrollX&&!showScrollY) return "";
        if(Consts_Private.SCROLLSTYLE_NORMAL.equals(scrollstyle))
        {//显示普通滚动条
            return "</div></div>";
        }else
        {
            return "</div>";
        }
    }
    
    public Class buildPageInterceptorClass(PageBean pbean,List<String> lstImports,String preaction,String beforesaveaction,String aftersaveaction,String postaction)
    {
        try
        {
            ClassPool pool=new ClassPool();
            pool.appendSystemPath();
            pool.insertClassPath(new ClassClassPath(ComponentAssistant.class));
            String classname=Consts.BASE_PACKAGE_NAME+".Page_"+pbean.getId()+"_Interceptor";
            CtClass pt=pool.makeClass(classname);
            if(lstImports==null) lstImports=new UniqueArrayList<String>();
            lstImports.add("com.wabacus.system.intercept");
            ClassPoolAssistant.getInstance().addImportPackages(pool,lstImports);
            pt.setSuperclass(pool.get(AbsPageInterceptor.class.getName()));
            preaction=preaction==null?"":preaction.trim();
            postaction=postaction==null?"":postaction.trim();
            beforesaveaction=beforesaveaction==null?"":beforesaveaction.trim();
            aftersaveaction=aftersaveaction==null?"":aftersaveaction.trim();
            
            StringBuffer sbuffer=new StringBuffer();
            sbuffer.append("public void doStart("+ReportRequest.class.getName()+" rrequest) {").append(preaction).append(" \n}");
            CtMethod preMethod=CtNewMethod.make(sbuffer.toString(),pt);
            pt.addMethod(preMethod);

            sbuffer=new StringBuffer();
            sbuffer.append("public void doStartSave("+ReportRequest.class.getName()+" rrequest,"+List.class.getName()+" lstSaveReportBeans) {").append(beforesaveaction).append(" \n}");
            CtMethod beforeSaveMethod=CtNewMethod.make(sbuffer.toString(),pt);
            pt.addMethod(beforeSaveMethod);
            
            sbuffer=new StringBuffer();
            sbuffer.append("public void doEndSave("+ReportRequest.class.getName()+" rrequest,"+List.class.getName()+" lstSaveReportBeans) {").append(aftersaveaction).append(" \n}");
            CtMethod afterSaveMethod=CtNewMethod.make(sbuffer.toString(),pt);
            pt.addMethod(afterSaveMethod);
            
            sbuffer=new StringBuffer();
            sbuffer.append("public void doEnd("+ReportRequest.class.getName()+" rrequest) {").append(postaction).append(" \n}");
            CtMethod postMethod=CtNewMethod.make(sbuffer.toString(),pt);
            pt.addMethod(postMethod);

            Class c=ConfigLoadManager.currentDynClassLoader.loadClass(classname,pt.toBytecode());
            pt.detach();
            pool.clearImportedPackages();
            pool=null;
            return c;
        }catch(NotFoundException e)
        {
            throw new WabacusConfigLoadingException("为页面"+pbean.getId()+"生成拦截器字节码时，执行pool.get()失败",e);
        }catch(CannotCompileException e)
        {
            throw new WabacusConfigLoadingException("为页面"+pbean.getId()+"生成拦截器字节码时无法编译",e);
        }catch(IOException ioe)
        {
            throw new WabacusConfigLoadingException("为页面"+pbean.getId()+"生成拦截器字节码时无法将生成的字节码写到本地文件系统",ioe);
        }
    }
    
    public List<String> initDisplayCss(ReportRequest rrequest)
    {
        List<String> lstCsses=new UniqueArrayList<String>();
        String css=rrequest.getStringAttribute("CSS","");
        if(!css.equals(""))
        {
            css=Tools.htmlEncode(css);
            List<String> lstCssesTmp=Tools.parseStringToList(css,",",false);
            String[] cssArray=lstCssesTmp.toArray(new String[lstCssesTmp.size()]);
            for(int k=0;k<cssArray.length;k++)
            {
                if(cssArray[k]==null||cssArray[k].trim().equals("")) continue;
                cssArray[k]=Config.webroot+"/"+cssArray[k];
                cssArray[k]=Tools.replaceAll(cssArray[k],"//","/");
                lstCsses.add(Tools.replaceAll(cssArray[k],Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin()));
            }
        }
        if(lstCsses.size()==0&&rrequest.getPagebean().getUlstMyCss()!=null)
        {
            for(String cssTmp:rrequest.getPagebean().getUlstMyCss())
            {
                lstCsses.add(Tools.replaceAll(cssTmp,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin())); 
            }
        }
        List<String> lstSystemCss=rrequest.getPagebean().getUlstSystemCss();
        if(lstSystemCss!=null)
        {
            for(String cssTmp:lstSystemCss)
            {
                lstCsses.add(Tools.replaceAll(cssTmp,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin())); 
            }
        }
        return lstCsses;
    }
    
    public String showButton(IComponentConfigBean ccbean,AbsButtonType buttonObj,ReportRequest rrequest,String dynclickevent)
    {
        if(ccbean instanceof ReportBean && buttonObj.isReferedHiddenButton()) return "";
        if(buttonObj.getReferedButtonObj()==null) return buttonObj.showButton(rrequest,dynclickevent);
        return buttonObj.getReferedButtonObj().showButton(rrequest,dynclickevent);
    }
    
    public String showButton(IComponentConfigBean ccbean,AbsButtonType buttonObj,ReportRequest rrequest,String dynclickevent,String button)
    {
        if(ccbean instanceof ReportBean && buttonObj.isReferedHiddenButton()) return "";//如果当前按钮属于报表，且被容器引用显示，并不在源报表处显示
        if(buttonObj.getReferedButtonObj()==null) return buttonObj.showButton(rrequest,dynclickevent,button);
        return buttonObj.getReferedButtonObj().showButton(rrequest,dynclickevent,button);
    }
    
    public String showButtonMenu(IComponentConfigBean ccbean,AbsButtonType buttonObj,ReportRequest rrequest,String dynclickevent)
    {
        if(ccbean instanceof ReportBean && buttonObj.isReferedHiddenButton()) return "";
        if(buttonObj.getReferedButtonObj()==null) return buttonObj.showMenu(rrequest,dynclickevent);
        return buttonObj.getReferedButtonObj().showMenu(rrequest,dynclickevent);
    }
    
    public String getDataExportTypeByShowType(int showtype)
    {
        if(showtype==Consts.DISPLAY_ON_PLAINEXCEL) return Consts.DATAEXPORT_PLAINEXCEL;
        if(showtype==Consts.DISPLAY_ON_RICHEXCEL) return Consts.DATAEXPORT_RICHEXCEL;
        if(showtype==Consts.DISPLAY_ON_WORD) return Consts.DATAEXPORT_WORD;
        if(showtype==Consts.DISPLAY_ON_PDF) return Consts.DATAEXPORT_PDF;
        return null;
    }
}
