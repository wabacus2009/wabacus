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
package com.wabacus.system.buttons;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public abstract class AbsButtonType implements Cloneable,Comparable<AbsButtonType>
{
    private final static Log log=LogFactory.getLog(AbsButtonType.class);

    protected String name="";

    protected String label=""; 

    protected String menulabel;
    
    protected Object clickhandler="";

    private String styleproperty;//控制按钮显示样式字符串
    
    private String disabledstyleproperty;

    protected String position;
    
    protected String menugroup="0";

    protected int positionorder;
    
    protected String refer;//只对显示在容器中的按钮有效，用于指定引用某个报表上的按钮进行显示
    
    protected AbsButtonType referedButtonObj;
    
    protected String referedbutton="hidden";
    
    protected boolean isReferedHiddenButton;
    
    protected String confirmessage;
    
    protected String confirmtitle;//点击此按钮弹出确认提示信息窗口的标题
    
    protected String cancelmethod;
    
    protected IComponentConfigBean ccbean;

    public AbsButtonType(IComponentConfigBean ccbean)
    {
        this.ccbean=ccbean;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name=name;
    }

    public void setDefaultNameIfNoName()
    {
        if(Tools.isEmpty(this.name)) this.name="wx_temp_"+Tools.getRandomString(10)+"_temp_wx";
    }
    
    public String getRefer()
    {
        return refer;
    }

    public void setRefer(String refer)
    {
        this.refer=refer;
    }

    public String getReferedbutton()
    {
        return referedbutton;
    }

    public void setReferedbutton(String referedbutton)
    {
        this.referedbutton=referedbutton;
    }

    public String getLabel()
    {
        return label;
    }

    public String getMenulabel()
    {
        return menulabel;
    }

    public void setMenulabel(String menulabel)
    {
        this.menulabel=menulabel;
    }

    public void setStyleproperty(String styleproperty)
    {
        this.styleproperty=styleproperty;
    }

    public void setCcbean(IComponentConfigBean ccbean)
    {
        this.ccbean=ccbean;
    }
    
    public IComponentConfigBean getCcbean()
    {
        return ccbean;
    }

    public void setDisabledstyleproperty(String disabledstyleproperty)
    {
        this.disabledstyleproperty=disabledstyleproperty;
    }

    protected String getStyleproperty(boolean isEnabled)
    {
        String realStyleproperty=null;
        if(isEnabled)
        {
            realStyleproperty=this.styleproperty;
        }else
        {
            realStyleproperty=Tools.isEmpty(this.disabledstyleproperty)?this.styleproperty:this.disabledstyleproperty;
        }
        if(realStyleproperty==null) realStyleproperty="";
        String buttonid=Tools.getPropertyValueByName("id",realStyleproperty,false);
        String buttonname=Tools.getPropertyValueByName("name",realStyleproperty,false);
        if(Tools.isEmpty(buttonid)||Tools.isEmpty(buttonname))
        {
            String tmp="btn";
            if(this.ccbean!=null) tmp+="_"+this.ccbean.getGuid();
            if(!Tools.isEmpty(this.name)&&!isDefaultGenerateName())
            {
                tmp+="_"+this.name;
            }else if(!Tools.isEmpty(this.getButtonType()))
            {
                tmp+="_"+this.getButtonType();
            }
            if(Tools.isEmpty(buttonid)) realStyleproperty+=" id='"+tmp+"_id' ";
            if(Tools.isEmpty(buttonname)) realStyleproperty+=" name='"+tmp+"_name' ";
            if(isEnabled||Tools.isEmpty(this.disabledstyleproperty))
            {
                this.styleproperty=realStyleproperty;
            }else
            {
                this.disabledstyleproperty=realStyleproperty;
            }
        }
        return realStyleproperty;
    }
    
    private boolean isDefaultGenerateName()
    {
        return this.name.startsWith("wx_temp_")&&this.name.endsWith("_temp_wx")&&this.name.length()=="wx_temp__temp_wx".length()+10;
    }
    
    public String getPosition()
    {
        return position;
    }

    public void setPosition(String position)
    {
        this.position=position;
    }

    public int getPositionorder()
    {
        return positionorder;
    }

    public void setPositionorder(int positionorder)
    {
        this.positionorder=positionorder;
    }

    public void setLabel(String label)
    {
        this.label=label;
    }
    
    public void setClickEvent(Object clickevent)
    {
        this.clickhandler=clickevent;
    }
    
    public String getMenugroup()
    {
        return menugroup;
    }

    public void setMenugroup(String menugroup)
    {
        this.menugroup=menugroup;
    }

    public AbsButtonType getReferedButtonObj()
    {
        return referedButtonObj;
    }

    public void setReferedButtonObj(AbsButtonType referedButtonObj)
    {
        this.referedButtonObj=referedButtonObj;
    }

    public boolean isReferedHiddenButton()
    {
        return isReferedHiddenButton;
    }

    public void setReferedHiddenButton(boolean isReferedHiddenButton)
    {
        this.isReferedHiddenButton=isReferedHiddenButton;
    }

    public void setConfirmessage(String confirmessage)
    {
        this.confirmessage=confirmessage;
    }

    public void setConfirmtitle(String confirmtitle)
    {
        this.confirmtitle=confirmtitle;
    }

    public void setCancelmethod(String cancelmethod)
    {
        this.cancelmethod=cancelmethod;
    }

    public String getClickEvent(ReportRequest rrequest)
    {
        if(clickhandler==null) return "";
        if(clickhandler instanceof IButtonClickeventGenerate)
        {
            String dynclickevent=Tools.formatStringBlank(((IButtonClickeventGenerate)clickhandler).generateClickEvent(rrequest,this));
            if(dynclickevent.indexOf('\"')>=0)
            {
                throw new WabacusRuntimeException("显示组件"+ccbean.getPath()+"的按钮"+this.name+"失败，动态生成的按钮事件不能用双引号，只能用单引用，如果有多级，可以加上转义字符\\");
            }
            return dynclickevent;
        }
        return clickhandler.toString();
    }
    
    protected boolean checkDisplayPermission(ReportRequest rrequest)
    {
        //if(ccbean==null) return true;//如果当前按钮不从属于某一个报表，则无法对其进行授权，因此直接返回true
        if(this.getButtonType()!=null&&!this.getButtonType().trim().equals(""))
        {
            return rrequest.checkPermission(ccbean.getId(),Consts.BUTTON_PART,"type{"+this.getButtonType()+"}",Consts.PERMISSION_TYPE_DISPLAY);
        }else if(this.name!=null&&!this.name.trim().equals(""))
        {
            return rrequest.checkPermission(ccbean.getId(),Consts.BUTTON_PART,name,Consts.PERMISSION_TYPE_DISPLAY);
        }
        return false;
    }
    protected boolean checkDisabledPermission(ReportRequest rrequest)
    {
//        if(ccbean==null) return false;//如果当前按钮不从属于某一个报表，则无法对其进行授权，因此直接返回false
        if(this.getButtonType()!=null&&!this.getButtonType().trim().equals(""))
        {//如果有按钮类型的，只能根据按钮类型授权，不能根据其name属性授权，比如框架内置的功能按钮
            return rrequest.checkPermission(ccbean.getId(),Consts.BUTTON_PART,"type{"+this.getButtonType()+"}",Consts.PERMISSION_TYPE_DISABLED);
        }else if(this.name!=null&&!this.name.trim().equals(""))
        {
            return rrequest.checkPermission(ccbean.getId(),Consts.BUTTON_PART,name,Consts.PERMISSION_TYPE_DISABLED);
        }
        return true;
    }
    
    public String getButtonType()
    {
        return null;
    }
    
    /*public boolean isDisplayedOnContextMenu()
    {
        if(position==null||position.equals("")) return false;
        if(position.indexOf(Consts.CONTEXTMENU_PART)>=0) return true;
        return false;
    }*/
    public abstract String showButton(ReportRequest rrequest,String dynclickevent);

//    /**
//     * @return
//     */
    
    public abstract String showButton(ReportRequest rrequest,String dynclickevent,String button);
    
    public abstract String showMenu(ReportRequest rrequest,String dynclickevent);
    
//    /**
//     * @return
//     */
    
    public abstract void loadExtendConfig(XmlElementBean eleButtonBean);
    
    public int compareTo(AbsButtonType otherButton)
    {
        if(otherButton==null) return 1;
//        {//两个都显示在右键菜单中
//            {//如果两个按钮不在同一个分组，则直接比较分组
//                this.menugroup.compareTo(otherButton.getMenugroup());
        if(this.positionorder>otherButton.getPositionorder()) return 1;
        if(this.positionorder<otherButton.getPositionorder()) return -1;
        return 0;
    }

    protected Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    public AbsButtonType clone(IComponentConfigBean ccbean)
    {
        try
        {
            AbsButtonType newButton=(AbsButtonType)clone();
            newButton.setCcbean(ccbean);
            return newButton;
        }catch(CloneNotSupportedException e)
        {
            log.error("clone按钮对象失败",e);
            return null;
        }
    }

    private boolean hasDoPostLoad;
    
    public void doPostLoad()
    {
        if(hasDoPostLoad) return;
        hasDoPostLoad=true;
    }

    private boolean hasDoPostLoadFinally;
    
    public void doPostLoadFinally()
    {
        if(hasDoPostLoadFinally) return;
        hasDoPostLoadFinally=true;
    }
}
