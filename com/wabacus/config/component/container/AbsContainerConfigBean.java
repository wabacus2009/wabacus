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
package com.wabacus.config.component.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.component.AbsComponentConfigBean;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.IApplicationConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.other.ButtonsBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public abstract class AbsContainerConfigBean extends AbsComponentConfigBean
{
    protected String tagname;
    
    protected String margin_left;
    
    protected String margin_right;
    
    protected String margin_top;
    
    protected String margin_bottom;//容器内部右边缘的间距
    
    protected int border=-1;

    protected String bordercolor;
    
    protected String titleposition="top";
    
    protected boolean scrollX;
    
    protected boolean scrollY;//是否显示纵向滚动条

//    protected String contentHeight;//显示内容的高度（当有垂直滚动条时除掉了margin-top和margin-bottom部分的高度）
    
    protected int colspan_total;

    protected Map<String,IComponentConfigBean> mChildren;

    protected List<String> lstChildrenIDs;

    public AbsContainerConfigBean(AbsContainerConfigBean parentContainer,String tagname)
    {
        super(parentContainer);
        this.tagname=tagname;
        mChildren=new HashMap<String,IComponentConfigBean>();
        lstChildrenIDs=new ArrayList<String>();
    }

    public String getTagname()
    {
        return tagname;
    }

    public String getMargin_left()
    {
        return margin_left;
    }

    public void setMargin_left(String margin_left)
    {
        this.margin_left=margin_left;
    }

    public String getMargin_right()
    {
        return margin_right;
    }

    public void setMargin_right(String margin_right)
    {
        this.margin_right=margin_right;
    }

    public String getMargin_top()
    {
        return margin_top;
    }

    public void setMargin_top(String margin_top)
    {
        this.margin_top=margin_top;
    }

    public String getMargin_bottom()
    {
        return margin_bottom;
    }

    public void setMargin_bottom(String margin_bottom)
    {
        this.margin_bottom=margin_bottom;
    }

    public int getBorder()
    {
        return border;
    }

    public void setBorder(int border)
    {
        this.border=border;
    }

    public String getBordercolor()
    {
        return bordercolor;
    }

    public void setBordercolor(String bordercolor)
    {
        this.bordercolor=bordercolor;
    }

    public int getColspan_total()
    {
        return colspan_total;
    }

    public void setColspan_total(int colspan_total)
    {
        this.colspan_total=colspan_total;
    }

    public boolean isTitleInLeft()
    {
        if(this.titleposition==null||this.titleposition.trim().equals("")) return false;
        return this.titleposition.toLowerCase().trim().equals("left");
    }

    public boolean isTitleInRight()
    {
        if(this.titleposition==null||this.titleposition.trim().equals("")) return false;
        return this.titleposition.toLowerCase().trim().equals("right");
    }
    
    public boolean isTitleInTop()
    {
        if(this.titleposition==null||this.titleposition.trim().equals("")) return true;
        return !isTitleInLeft()&&!isTitleInRight()&&!isTitleInBottom();
    }
    
    public boolean isTitleInBottom()
    {
        if(this.titleposition==null||this.titleposition.trim().equals("")) return false;
        return this.titleposition.toLowerCase().trim().equals("bottom");
    }
    
    public void setTitleposition(String titleposition)
    {
        this.titleposition=titleposition;
    }

    public Map<String,IComponentConfigBean> getMChildren()
    {
        return mChildren;
    }

    public void setMChildren(Map<String,IComponentConfigBean> children)
    {
        mChildren=children;
    }

    public List<String> getLstChildrenIDs()
    {
        return lstChildrenIDs;
    }

    public void setLstChildrenIDs(List<String> lstChildrenIDs)
    {
        this.lstChildrenIDs=lstChildrenIDs;
    }

    public boolean isScrollX()
    {
        return scrollX;
    }

    public void setScrollX(boolean scrollX)
    {
        this.scrollX=scrollX;
    }

    public boolean isScrollY()
    {
        return scrollY;
    }

    public void setScrollY(boolean scrollY)
    {
        this.scrollY=scrollY;
    }

    public IComponentConfigBean getConfigBeanWithValidParentTitle()
    {
        if(this.parenttitle!=null&&!this.parenttitle.trim().equals("")
                ||this.title!=null&&!this.title.trim().equals(""))
            return this;
        IComponentConfigBean childBeanTmp;
        for(String childidTmp:this.lstChildrenIDs)
        {
            childBeanTmp=this.mChildren.get(childidTmp);
            if(childBeanTmp==null) continue;
            childBeanTmp=childBeanTmp.getConfigBeanWithValidParentTitle();
            if(childBeanTmp!=null) return childBeanTmp;
        }
        return null;
    }

    public boolean isExistChildId(String childid,boolean includeme,boolean inherit)
    {
        if(includeme&&this.id.equals(childid)) return true;
        if(this.lstChildrenIDs==null||this.lstChildrenIDs.size()==0)
        {
            return false;
        }
        if(childid==null||childid.trim().equals("")) return false;
        if(mChildren.containsKey(childid)) return true;
        if(!inherit) return false;
        for(Entry<String,IComponentConfigBean> entryTmp:mChildren.entrySet())
        {
            if(!(entryTmp.getValue() instanceof AbsContainerConfigBean)) continue;
            boolean flag=((AbsContainerConfigBean)entryTmp.getValue()).isExistChildId(childid,includeme,
                    inherit);
            if(flag) return true;
        }
        return false;
    }

    public ReportBean getReportChild(String reportid,boolean inherit)
    {
        if(this.mChildren==null) return null;
        Object obj=this.mChildren.get(reportid);
        if(obj==null||!(obj instanceof ReportBean))
        {
            obj=null;
            if(inherit&&this.mChildren.size()>0)
            {
                for(Entry<String,IComponentConfigBean> entryTmp:this.mChildren.entrySet())
                {
                    if(entryTmp.getValue() instanceof AbsContainerConfigBean)
                    {
                        obj=((AbsContainerConfigBean)entryTmp.getValue()).getReportChild(reportid,true);
                        if(obj!=null) break;
                    }
                }
            }
        }
        return (ReportBean)obj;
    }

    public IApplicationConfigBean getApplicationChild(String applicationid,boolean inherit)
    {
        if(this.mChildren==null) return null;
        Object obj=this.mChildren.get(applicationid);
        if(obj==null||!(obj instanceof IApplicationConfigBean))
        {
            obj=null;
            if(inherit&&this.mChildren.size()>0)
            {
                for(Entry<String,IComponentConfigBean> entryTmp:this.mChildren.entrySet())
                {
                    if(entryTmp.getValue() instanceof AbsContainerConfigBean)
                    {
                        obj=((AbsContainerConfigBean)entryTmp.getValue()).getApplicationChild(applicationid,true);
                        if(obj!=null) break;
                    }
                }
            }
        }
        return (IApplicationConfigBean)obj;
    }
    
    public List<ReportBean> getLstAllReportBeans(boolean inherit)
    {
        if(this.mChildren==null||lstChildrenIDs==null) return null;
        List<ReportBean> lstReportBeans=new UniqueArrayList<ReportBean>();
        Object objTmp;
        for(String childidTmp:lstChildrenIDs)
        {
            objTmp=this.mChildren.get(childidTmp);
            if(objTmp==null) continue;
            if(objTmp instanceof ReportBean)
            {
                lstReportBeans.add((ReportBean)objTmp);
            }else if(inherit&&(objTmp instanceof AbsContainerConfigBean))
            {
                List<ReportBean> lstTmp=((AbsContainerConfigBean)objTmp).getLstAllReportBeans(true);
                if(lstTmp!=null&&lstTmp.size()>0) lstReportBeans.addAll(lstTmp);
            }
        }
        return lstReportBeans;
    }
    
    public List<String> getLstAllChildApplicationIds(boolean inherit)
    {
        if(this.mChildren==null||this.lstChildrenIDs==null) return null;
        List<String> lstResults=new UniqueArrayList<String>();
        Object objTmp;
        for(String childidTmp:lstChildrenIDs)
        {
            objTmp=this.mChildren.get(childidTmp);
            if(objTmp==null) continue;
            if(objTmp instanceof IApplicationConfigBean)
            {
                lstResults.add(((IApplicationConfigBean)objTmp).getId());
            }else if(inherit&&(objTmp instanceof AbsContainerConfigBean))
            {
                List<String> lstTmp=((AbsContainerConfigBean)objTmp).getLstAllChildApplicationIds(true);
                if(lstTmp!=null&&lstTmp.size()>0) lstResults.addAll(lstTmp);
            }
        }
        return lstResults;
    }
    
    public IComponentConfigBean getChildComponentBean(String childid,boolean inerit)
    {
        if(this.mChildren==null) return null;
        IComponentConfigBean childObj=this.mChildren.get(childid);
        if(childObj!=null) return childObj;
        if(!inerit) return null;
        for(Entry<String,IComponentConfigBean> entryTmp:this.mChildren.entrySet())
        {
            if(entryTmp.getValue()==null||!(entryTmp.getValue() instanceof AbsContainerConfigBean)) continue;
            childObj=((AbsContainerConfigBean)entryTmp.getValue()).getChildComponentBean(childid,inerit);
            if(childObj!=null) return childObj;
        }
        return null;
    }

    public void doPostLoad()
    {
        super.doPostLoad();
        if(this.refreshid==null||this.refreshid.trim().equals("")) this.refreshid=this.id;
        processContainerButtonsStart();
        if(this.mChildren!=null&&this.mChildren.size()>0)
        {
            IComponentConfigBean childComponentTmp;
            for(Entry<String,IComponentConfigBean> entryTmp:this.mChildren.entrySet())
            {
                childComponentTmp=entryTmp.getValue();
                childComponentTmp.doPostLoad();
                if(!(childComponentTmp instanceof ReportBean)||!((ReportBean)childComponentTmp).isSlaveReportDependsonListReport())
                {
                    String childRefreshIdTmp=childComponentTmp.getRefreshid();
                    if(childRefreshIdTmp==null||childRefreshIdTmp.trim().equals("")) continue;
                    this.refreshid=this.getPageBean().getCommonRefreshIdOfComponents(this.refreshid,childRefreshIdTmp);
                }
            }
        }
        processContainerButtonsEnd();//这个方法要放在所有子组件都doPostLoad()完成后再调用，因为很多报表按钮是在doPostLoad()方法时才会新建的。
        JavaScriptAssistant.getInstance().createComponentOnloadScript(this);
//            int deltaSize=0;
//            if(!this.margin_bottom.trim().equals("")&&this.margin_bottom.indexOf("%")<0)
//            {
    }

    public void doPostLoadFinally()
    {
        if(this.mChildren!=null&&this.mChildren.size()>0)
        {
            for(Entry<String,IComponentConfigBean> entryTmp:this.mChildren.entrySet())
            {
                entryTmp.getValue().doPostLoadFinally();
            }
        }
    }
    
    protected void processContainerButtonsStart()
    {
        ButtonsBean bbeans=this.getButtonsBean();
        if(bbeans==null) return;
        List<AbsButtonType> lstButtons=bbeans.getAllDistinctButtonsList();
        if(lstButtons==null||lstButtons.size()==0) return;
        for(AbsButtonType buttonObjTmp:lstButtons)
        {
            if(buttonObjTmp.getRefer()==null||buttonObjTmp.getRefer().trim().equals("")) continue;
            String refer=buttonObjTmp.getRefer();
            int idx=refer.indexOf(".");
            if(idx<=0)
            {
                throw new WabacusConfigLoadingException("容器"+this.getPath()+"配置的按钮"+buttonObjTmp.getName()+"的refer属性："+refer+"不合法");
            }
            String referReportid=refer.substring(0,idx).trim();
            ReportBean referedReportBean=this.getReportChild(referReportid,true);
            if(referedReportBean==null)
            {
                throw new WabacusConfigLoadingException("容器"+this.getPath()+"配置的按钮"+buttonObjTmp.getName()+"的refer属性："+refer+"引用的报表不存在或不属于此容器");
            }
            if(referedReportBean.isSlaveReportDependsonListReport())
            {
                throw new WabacusConfigLoadingException("容器"+this.getPath()+"配置的按钮"+buttonObjTmp.getName()+"的refer属性："+refer
                        +"引用的报表是依赖数据自动列表报表的从报表，不能引用它的按钮");
            }
            if(referedReportBean.getRefreshid()==null||referedReportBean.getRefreshid().trim().equals(""))
                referedReportBean.setRefreshid(referedReportBean.getId());
            this.refreshid=this.getPageBean().getCommonRefreshIdOfComponents(this.refreshid,referedReportBean.getRefreshid());//更新容器的时候也要更新此报表，否则取不到报表上的按钮进行显示
            referedReportBean.setRefreshid(this.getPageBean().getCommonRefreshIdOfComponents(this.id,referedReportBean.getRefreshid()));
        }
    }
    
    protected void processContainerButtonsEnd()
    {
        ButtonsBean bbeans=this.getButtonsBean();
        if(bbeans==null) return;
        List<AbsButtonType> lstButtons=bbeans.getAllDistinctButtonsList();
        if(lstButtons==null||lstButtons.size()==0) return;
        for(AbsButtonType buttonObjTmp:lstButtons)
        {
            if(buttonObjTmp.getRefer()==null||buttonObjTmp.getRefer().trim().equals("")) continue;
            String refer=buttonObjTmp.getRefer();
            int idx=refer.indexOf(".");
            String referReportid=refer.substring(0,idx).trim();
            String referButton=refer.substring(idx+1).trim();
            ReportBean referedReportBean=this.getReportChild(referReportid,true);
            ButtonsBean bbeansTmp=referedReportBean.getButtonsBean();
            if(bbeansTmp==null)
            {
                throw new WabacusConfigLoadingException("容器"+this.getPath()+"配置的按钮"+buttonObjTmp.getName()+"的refer属性："+refer+"引用的报表没有配置按钮");
            }
            if(Tools.isDefineKey("type",referButton))
            {
                List<AbsButtonType> lstButtonsObjTmp=bbeansTmp.getLstButtonsByTypeName(Tools.getRealKeyByDefine("type",referButton));
                if(lstButtonsObjTmp==null||lstButtonsObjTmp.size()==0)
                {
                    throw new WabacusConfigLoadingException("容器"+this.getPath()+"配置的按钮"+buttonObjTmp.getName()+"的refer属性："+refer+"引用的报表没有type为"
                            +referButton+"的按钮");
                }
                if(!"display".equals(buttonObjTmp.getReferedbutton()))
                {
                    for(AbsButtonType referedButtonObjTmp:lstButtonsObjTmp)
                    {
                        referedButtonObjTmp.setReferedHiddenButton(true);
                    }
                }
                buttonObjTmp.setReferedButtonObj(lstButtonsObjTmp.get(0));//记录下被引用的按钮，以便显示时可以直接调用此按钮进行显示，只要记录其中一个即可
            }else
            {
                if(bbeansTmp.getButtonByName(referButton)==null)
                {
                    throw new WabacusConfigLoadingException("容器"+this.getPath()+"配置的按钮"+buttonObjTmp.getName()+"的refer属性："+refer+"引用的报表没有配置name为"
                            +referButton+"的按钮");
                }
                if(!"display".equals(buttonObjTmp.getReferedbutton()))
                {
                    bbeansTmp.getButtonByName(referButton).setReferedHiddenButton(true);
                }
                buttonObjTmp.setReferedButtonObj(bbeansTmp.getButtonByName(referButton));
            }
        }
    }
    
    public IComponentConfigBean clone(AbsContainerConfigBean parentContainer)
    {
        AbsContainerConfigBean containerBeanNew=(AbsContainerConfigBean)super
                .clone(parentContainer);
        if(this.mChildren!=null)
        {
            Map<String,IComponentConfigBean> mChildrenNew=new HashMap<String,IComponentConfigBean>();
            Iterator<String> itKeys=this.mChildren.keySet().iterator();
            while(itKeys.hasNext())
            {
                String key=itKeys.next();
                if(key==null) continue;
                IComponentConfigBean childObj=this.mChildren.get(key);
                if(childObj==null) continue;
                mChildrenNew.put(key,childObj.clone(containerBeanNew));
            }
            containerBeanNew.setMChildren(mChildrenNew);
        }
        if(this.lstChildrenIDs!=null)
        {
            containerBeanNew.setLstChildrenIDs((List<String>)((ArrayList<String>)lstChildrenIDs)
                    .clone());
        }
        return containerBeanNew;
    }

    public String getChildRefreshGuid(String childid)
    {
        return this.getRefreshGuid();
    }

    public boolean invokeCheckPermissionByChild(ReportRequest rrequest,IComponentConfigBean childConfigBean,String permissiontype,
            String permissionvalue)
    {
        return rrequest.checkPermission(this.id,Consts.DATA_PART,null,permissiontype,permissionvalue);
    }
}
