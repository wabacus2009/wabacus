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
package com.wabacus.config.component.container.panel;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.component.container.panel.TabsPanel;
import com.wabacus.util.Consts;

public class TabsPanelBean extends AbsContainerConfigBean
{   
    private boolean isAsyn=true;
    
    private String titlestyle;

    private String titlewidth;
    
    private int displaycount;//显示tab标题个数，如果超过这个个数，将会自动提供左右/上下移动的箭头（根据titlestyle属性定是上下箭头还是左右箭头）
    
    private List<TabItemBean> lstTabItems;
    
    private String switchbeforecallback;//当isAsyn为true时，点击切换标签页前的客户端回调函数
    
    public TabsPanelBean(AbsContainerConfigBean parentContainer,String tagname)
    {
        super(parentContainer,tagname);
    }
    
    public boolean isAsyn()
    {
        return isAsyn;
    }

    public void setAsyn(boolean isAsyn)
    {
        this.isAsyn=isAsyn;
    }

    public String getTitlestyle()
    {
        return titlestyle;
    }

    public void setTitlestyle(String titlestyle)
    {
        this.titlestyle=titlestyle;
    }

    public String getTitlewidth()
    {
        return titlewidth;
    }

    public void setTitlewidth(String titlewidth)
    {
        this.titlewidth=titlewidth;
    }

    public String getTitlealign()
    {
        return titlealign;
    }

    public void setTitlealign(String titlealign)
    {
        this.titlealign=titlealign;
    }

    public int getDisplaycount()
    {
        return displaycount;
    }

    public void setDisplaycount(int displaycount)
    {
        this.displaycount=displaycount;
    }

    public String getSwitchbeforecallback()
    {
        return switchbeforecallback;
    }

    public void setSwitchbeforecallback(String switchbeforecallback)
    {
        this.switchbeforecallback=switchbeforecallback;
    }

    public List<TabItemBean> getLstTabItems()
    {
        return lstTabItems;
    }

    public void setLstTabItems(List<TabItemBean> lstTabItems)
    {
        this.lstTabItems=lstTabItems;
    }

    public String getRefreshGuid()
    {
       return this.refreshGuid;
    }
    
    public String getRefreshGuid(int tabitemindex)
    {
        if(tabitemindex<0||tabitemindex>=this.lstTabItems.size()) return null;
        return this.lstTabItems.get(tabitemindex).getRefreshGuid();
    }
    
    public String getChildRefreshGuid(String childid)
    {
        childid=childid==null?"":childid.trim();
        if(childid.equals("")||lstTabItems==null) return super.getChildRefreshGuid(childid);
        for(TabItemBean tibTmp:lstTabItems)
        {
            if(!tibTmp.isExistChildId(childid,true)) continue;
            return tibTmp.getRefreshGuid();
        }
        throw new WabacusConfigLoadingException("容器"+this.getGuid()+"下没有id为"+childid+"的子组件");
    }

    public void setRefreshGuid(String refreshGuid)
    {
        throw new WabacusRuntimeException("对于tabpanel，不支持setRefreshGuid(String refreshGuid)方法");
    }

    public boolean invokeCheckPermissionByChild(ReportRequest rrequest,IComponentConfigBean childConfigBean,String permissiontype,
            String permissionvalue)
    {
        if(lstChildrenIDs==null||lstChildrenIDs.size()==0) return false;
        int i=0,len=lstChildrenIDs.size();
        for(;i<len;i++)
        {
            if(childConfigBean.getId().equals(lstChildrenIDs.get(i))) break;
        }
        if(i==len) return false;
        return rrequest.checkPermission(this.id,Consts.DATA_PART,String.valueOf(i),permissiontype,permissionvalue);
    }

    public void doPostLoad()
    {
        this.lstTabItems=new ArrayList<TabItemBean>();
        if(this.mChildren==null||this.mChildren.size()==0) return;
        if(this.refreshid==null||this.refreshid.trim().equals("")) this.refreshid=this.id;
        processContainerButtonsStart();
        IComponentConfigBean childComponentTmp;
        String maxRefreshId=this.id;//存放本<tabpanel/>中所有tabitem的最大refreshid，
        for(String childidTmp:this.lstChildrenIDs)
        {
            childComponentTmp=this.mChildren.get(childidTmp);
            childComponentTmp.doPostLoad();
            if(childComponentTmp instanceof ReportBean)
            {
                ReportBean rbTmp=(ReportBean)childComponentTmp;
                if(rbTmp.isSlaveReport()||(rbTmp.getMDependChilds()!=null&&rbTmp.getMDependChilds().size()>0))
                {
                    this.setAsyn(false);
                }
            }else if(childComponentTmp instanceof AbsContainerConfigBean)
            {//如果直接子组件是一个容器
                if(hasChildRelateWithOuterReport((AbsContainerConfigBean)childComponentTmp,(AbsContainerConfigBean)childComponentTmp))
                {
                    this.setAsyn(false);
                }
            }
            TabItemBean tibean=new TabItemBean(childComponentTmp);
            lstTabItems.add(tibean);
            tibean.setRefreshid(this.getPageBean().getCommonRefreshIdOfComponents(this.id,childComponentTmp.getRefreshid()));
            if(tibean.getRefreshid()==null||tibean.getRefreshid().trim().equals("")) tibean.setRefreshid(this.id);
            if(maxRefreshId==null||maxRefreshId.trim().equals(""))
            {
                maxRefreshId=tibean.getRefreshid();
            }else
            {
                maxRefreshId=this.getPageBean().getCommonRefreshIdOfComponents(maxRefreshId,tibean.getRefreshid());
            }
        }
        this.refreshid=this.getPageBean().getCommonRefreshIdOfComponents(this.refreshid,maxRefreshId);
        if(this.getPageBean().getId().equals(this.refreshid))
        {
            this.refreshGuid=this.getPageBean().getId();
        }else
        {
            this.refreshGuid=this.getPageBean().getChildComponentBean(this.refreshid,true).getGuid();
        }
        processContainerButtonsEnd();
        if(this.isAsyn&&hasReferedChildReportButton())
        {
            this.setAsyn(false);
        }
        if(this.printBean!=null) this.printBean.doPostLoad();
        JavaScriptAssistant.getInstance().createComponentOnloadScript(this);
    }
    
    private boolean hasChildRelateWithOuterReport(AbsContainerConfigBean rootContainerBean,AbsContainerConfigBean parentContainerBean)
    {
        IComponentConfigBean childComponentTmp;
        for(String childidTmp:parentContainerBean.getLstChildrenIDs())
        {
            childComponentTmp=parentContainerBean.getMChildren().get(childidTmp);
            if(childComponentTmp instanceof ReportBean)
            {
                ReportBean rbTmp=(ReportBean)childComponentTmp;
                if(rbTmp.isSlaveReport()&&rootContainerBean.getChildComponentBean(rbTmp.getDependParentId(),true)==null)
                {//如果当前子报表依赖于rootContainerBean所在tabpanel容器其它标签页或其外面容器的报表
                    return true;
                }
                if(rbTmp.getMDependChilds()!=null)
                {
                    for(String idTmp:rbTmp.getMDependChilds().keySet())
                    {
                        if(rootContainerBean.getChildComponentBean(idTmp,true)==null) return true;
                    }
                }
            }else if(childComponentTmp instanceof AbsContainerConfigBean)
            {
                boolean flag=hasChildRelateWithOuterReport(rootContainerBean,(AbsContainerConfigBean)childComponentTmp);
                if(flag) return true;
            }
        }
        return false;
    }
    
    private boolean hasReferedChildReportButton()
    {
        AbsContainerConfigBean containerConfigBean=this;
        while(containerConfigBean!=null)
        {
            if(containerConfigBean.getButtonsBean()!=null)
            {
                List<AbsButtonType> lstButtons=containerConfigBean.getButtonsBean().getAllDistinctButtonsList();
                if(lstButtons!=null&&lstButtons.size()>0)
                {
                    for(AbsButtonType buttonObjTmp:lstButtons)
                    {
                        if(buttonObjTmp.getRefer()==null||buttonObjTmp.getRefer().trim().equals("")) continue;
                        String referReportid=buttonObjTmp.getRefer().substring(0,buttonObjTmp.getRefer().indexOf(".")).trim();
                        if(this.getReportChild(referReportid,true)!=null) return true;
                    }
                }
            }
            containerConfigBean=containerConfigBean.getParentContainer();
        }
        return false;
    }
    
    public boolean isInSameTabItem(IComponentConfigBean childConfigBean1,IComponentConfigBean childConfigBean2)
    {
        if(this.getChildComponentBean(childConfigBean1.getId(),true)==null||this.getChildComponentBean(childConfigBean2.getId(),true)==null)
            return false;//其中有一个子组件不在<tabpanel/>中
        AbsContainerConfigBean parentConfigBean=childConfigBean1.getParentContainer();
        while(!parentConfigBean.getId().equals(this.id))
        {
            if(parentConfigBean.getChildComponentBean(childConfigBean2.getId(),true)!=null) return true;//它们的公共父容器在tabpanel的一个标签页中
            parentConfigBean=parentConfigBean.getParentContainer();
        }
        return false;
    }
    
    private class TabItemBean
    {
        private IComponentConfigBean childComponentBean;
        
        private String refreshid;
        
        private String refreshGuid;
        
        public TabItemBean(IComponentConfigBean childComponentBean)
        {
            this.childComponentBean=childComponentBean;
        }
        
        public IComponentConfigBean getChildComponentBean()
        {
            return childComponentBean;
        }

        public String getRefreshid()
        {
            return refreshid;
        }

        public void setRefreshid(String refreshid)
        {
            this.refreshid=refreshid;
        }

        public String getRefreshGuid()
        {
            if(this.refreshGuid==null||this.refreshGuid.trim().equals(""))
            {
                this.refreshGuid=ComponentConfigLoadAssistant.getInstance().createComponentRefreshGuidByRefreshId(TabsPanelBean.this.getPageBean(),
                        TabsPanelBean.this.id,this.refreshid);
            }
            return refreshGuid;
        }
        
        public boolean isExistChildId(String childid,boolean inherit)
        {
            if(childComponentBean==null) return false;
            if(childid==null||childid.trim().equals("")) return false;
            if(childid.trim().equals(childComponentBean.getId())) return true;
            if(!inherit) return false;
            if(!(childComponentBean instanceof AbsContainerConfigBean)) return false;//如果本容器的子组件不是一个容器
            return ((AbsContainerConfigBean)childComponentBean).getChildComponentBean(childid,true)!=null;
        }
    }

    public IComponentType createComponentTypeObj(ReportRequest rrequest,AbsContainerType parentContainer)
    {
        return new TabsPanel(parentContainer,this,rrequest);
    }
}
