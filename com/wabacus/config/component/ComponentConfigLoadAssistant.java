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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.IApplicationConfigBean;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.other.ButtonsBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.config.template.TemplateParser;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.util.Tools;

public class ComponentConfigLoadAssistant
{
    private final static Log log=LogFactory.getLog(ComponentConfigLoadAssistant.class);
    
    private final static ComponentConfigLoadAssistant instance=new ComponentConfigLoadAssistant();

    private ComponentConfigLoadAssistant()
    {}

    public static ComponentConfigLoadAssistant getInstance()
    {
        return instance;
    }

    public void checkAndAddButtons(ReportBean reportbean,Class buttonType,String defaultkey)
    {
        ButtonsBean bbeans=reportbean.getButtonsBean();
        if(bbeans==null)
        {
            bbeans=new ButtonsBean(reportbean);
            reportbean.setButtonsBean(bbeans);
        }
        List<AbsButtonType> lstButtons=bbeans.getAllCertainTypeButtonsList(buttonType);
        if((lstButtons==null||lstButtons.size()==0)&&defaultkey!=null&&!defaultkey.trim().equals(""))
        {
            AbsButtonType buttonObj=Config.getInstance().getResourceButton(null,reportbean,defaultkey,buttonType);
            buttonObj.setDefaultNameIfNoName();
            ComponentConfigLoadManager.addButtonToPositions(reportbean,buttonObj);
        }
    }
    
    public String createComponentRefreshGuidByRefreshId(PageBean pbean,String componentId,String refreshid)
    {
        IComponentConfigBean ccbean=pbean.getChildComponentBean(componentId,true);
        if(ccbean==null) throw new WabacusRuntimeException("在页面"+pbean.getId()+"中没有取到id为"+componentId+"的组件");
        if(refreshid==null||refreshid.trim().equals("")) refreshid=componentId;
        String refreshGuid=null;
        if(refreshid.equals(pbean.getId()))
        {
            refreshGuid=pbean.getGuid();
        }else if(refreshid.equals(componentId))
        {
            refreshGuid=ccbean.getGuid();
        }else
        {
            IComponentConfigBean refreshContainerObj=pbean.getChildComponentBean(refreshid,true);//取到父容器对象
            if(!(refreshContainerObj instanceof AbsContainerConfigBean))
            {
                throw new WabacusRuntimeException("生成组件"+ccbean.getGuid()+"的refreshGuid失败，其refreshid："+refreshid+"即不是自己的ID，也不是父容器的ID");
            }
            refreshGuid=((AbsContainerConfigBean)refreshContainerObj).getChildRefreshGuid(componentId);
        }
        return refreshGuid;
    }
    
    public void validateApplicationRefreshid(IApplicationConfigBean applicationBean)
    {
        if(applicationBean.getRefreshid()==null||applicationBean.getRefreshid().trim().equals("")) return;
        if(applicationBean.getRefreshid().trim().equals(applicationBean.getId())) return;
        IComponentConfigBean refreshComponentBean=null;
        if(applicationBean.getPageBean().getId().equals(applicationBean.getRefreshid()))
        {
            refreshComponentBean=applicationBean.getPageBean();
        }else
        {
            refreshComponentBean=applicationBean.getPageBean().getChildComponentBean(applicationBean.getRefreshid(),true);
        }
        if(refreshComponentBean==null)
        {
            throw new WabacusConfigLoadingException(applicationBean.getPath()+"的refreshid："+applicationBean.getRefreshid()+"对应的组件不存在");
        }
        if(!(refreshComponentBean instanceof AbsContainerConfigBean))
        {
            throw new WabacusConfigLoadingException(applicationBean.getPath()+"的refreshid："+applicationBean.getRefreshid()
                    +"对应的组件不是容器，不能将其它应用配置为此应用的刷新组件");
        }
        if(((AbsContainerConfigBean)refreshComponentBean).getChildComponentBean(applicationBean.getId(),true)==null)
        {
            throw new WabacusConfigLoadingException(applicationBean.getPath()+"的refreshid："+applicationBean.getRefreshid()+"对应的容器不是当前应用的父容器");
        }
    }
    
    public boolean isStaticTemplateResource(String template)
    {
        if(Tools.isDefineKey("classpath",template)) return true;
        if(Tools.isDefineKey("$",template)) return true;
        if(Tools.isDefineKey("absolute",template)) return true;
        if(Tools.isDefineKey("relative",template)) return true;
        return false;
    }
    
    public TemplateBean getStaticTemplateBeanByConfig(PageBean pbean,String template)
    {
        TemplateBean tplBean=null;
        template=template==null?"":template.trim();
        if(Tools.isDefineKey("$",template))
        {
            tplBean=(TemplateBean)Config.getInstance().getResourceObject(null,pbean,template,true);
        }else
        {//取html/htm文件中的模板
            tplBean=Config.getInstance().getFileTemplate(template);
            if(tplBean==null)
            {
                tplBean=TemplateParser.parseTemplateByPath(template);
                if(tplBean!=null) Config.getInstance().addFileTemplate(template,tplBean);
            }
        }
        return tplBean;
    }
    
    public Object[] parseIncludeApplicationids(IComponentConfigBean ccbeanOwner,List<String> lstConfigApplicationids)
    {
        if(ccbeanOwner instanceof AbsContainerConfigBean)
        {
            if(lstConfigApplicationids==null||lstConfigApplicationids.size()==0)
            {//如果没有配置include属性，则取其下包括的所有子应用ID
                lstConfigApplicationids=((AbsContainerConfigBean)ccbeanOwner).getLstAllChildApplicationIds(true);
            }
        }else if(lstConfigApplicationids==null||lstConfigApplicationids.size()==0)
        {
            lstConfigApplicationids=new ArrayList<String>();
            lstConfigApplicationids.add(ccbeanOwner.getId());
        }
        StringBuffer appidsBuf=new StringBuffer();
        List<String> lstAppids=new ArrayList<String>();
        Map<String,Integer> mReportidsAndPagesize=new HashMap<String,Integer>();
        for(String appidTmp:lstConfigApplicationids)
        {
            if(appidTmp==null||appidTmp.trim().equals("")||lstAppids.contains(appidTmp.trim())) continue;
            appidTmp=appidTmp.trim();
            int idxLeft=appidTmp.indexOf("{");
            int idxRight=appidTmp.indexOf("}");
            int ipagesize=Integer.MIN_VALUE;
            if(idxLeft>0&&idxRight==appidTmp.length()-1)
            {
                String pagesize=appidTmp.substring(idxLeft+1,idxRight).trim();
                appidTmp=appidTmp.substring(0,idxLeft).trim();
                if(appidTmp.equals("")) continue;
                if(!pagesize.equals("")) ipagesize=Integer.parseInt(pagesize);
            }
            ReportBean rbean=ccbeanOwner.getPageBean().getReportChild(appidTmp,true);
            if(rbean!=null) mReportidsAndPagesize.put(appidTmp,ipagesize);//当前应用是报表
            if(ccbeanOwner.getPageBean().getApplicationChild(appidTmp,true)==null)
            {
                throw new WabacusConfigLoadingException("加载组件"+ccbeanOwner.getPath()+"上的打印配置失败，其include属性配置的应用ID"+appidTmp+"不存在");
            }
            lstAppids.add(appidTmp);
            appidsBuf.append(appidTmp+";");
        }
//        {//如果是报表，且没有在include中指定其id，则加上
//            mReportidsAndPagesize.put(ccbeanOwner.getId(),Integer.MIN_VALUE);//用默认值
        return new Object[] { appidsBuf.toString(), lstAppids, mReportidsAndPagesize };
    }
    
    public List<ConditionBean> cloneLstConditionBeans(AbsConfigBean parent,List<ConditionBean> lstConditions)
    {
        if(lstConditions==null) return null;
        List<ConditionBean> lstConNew=new ArrayList<ConditionBean>();
        for(ConditionBean cbTmp:lstConditions)
        {
            lstConNew.add((ConditionBean)cbTmp.clone(parent));
        }
        return lstConNew;
    }
}
