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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.RegexTools;
import com.wabacus.util.Tools;

public class WabacusAssistant
{
    private static Log log=LogFactory.getLog(WabacusAssistant.class);
    
    private final static WabacusAssistant instance=new WabacusAssistant();

    protected WabacusAssistant()
    {}

    public static WabacusAssistant getInstance()
    {
        return instance;
    }
    
    public String addDefaultPopupParams(String popupparams,String initsize,String defaultWidth,String defaultHeight,String defaultHandler)
    {
        popupparams=popupparams==null?"":popupparams.trim();
        if(popupparams.startsWith("{")&&popupparams.endsWith("}")) popupparams=popupparams.substring(1,popupparams.length()-1).trim();
        if(defaultWidth!=null&&!defaultWidth.trim().equals("")&&popupparams.toLowerCase().indexOf("width:")<0)
        {
            if(!popupparams.equals("")) popupparams+=",";
            popupparams+="width:"+defaultWidth;
        }
        if(defaultHeight!=null&&!defaultHeight.trim().equals("")&&popupparams.toLowerCase().indexOf("height:")<0)
        {
            if(!popupparams.equals("")) popupparams+=",";
            popupparams+="height:"+defaultHeight;
        }
        if(initsize!=null&&initsize.toLowerCase().trim().equals("max"))
        {
            if(!popupparams.equals("")) popupparams+=",";
            popupparams+="initsize:'max'";
        }else if(initsize!=null&&initsize.toLowerCase().trim().equals("min"))
        {
            if(!popupparams.equals("")) popupparams+=",";
            popupparams+="initsize:'min'";
        }
        if(defaultHandler!=null&&!defaultHandler.trim().equals("")&&popupparams.toLowerCase().indexOf("handler:")<0)
        {
            if(!popupparams.equals("")) popupparams+=",";
            popupparams+="handler:"+defaultHandler;
        }
        if(!popupparams.trim().equals("")) popupparams="{"+popupparams+"}";
        return popupparams;
    }
    
    public List<Map<String,String>> parseStringToList(String sourcestring)
    {
        if(sourcestring==null||sourcestring.trim().equals("")) return null;
        List<String> lstTemp=null;
        if(!sourcestring.startsWith("[")||!sourcestring.endsWith("]"))
        {
            lstTemp=new ArrayList<String>();
            lstTemp.add(sourcestring);
        }else
        {
            sourcestring=sourcestring.substring(1,sourcestring.length()-1);
            lstTemp=Tools.parseStringToList(sourcestring,"][",false);
        }
        List<Map<String,String>> lstResult=new ArrayList<Map<String,String>>();
        String temp;
        for(int i=0;i<lstTemp.size();i++)
        {
            temp=lstTemp.get(i);
            if(temp==null||temp.trim().equals("")) continue;
            Map<String,String> mParams=new HashMap<String,String>();
            List<String> lstParams=Tools.parseStringToList(temp,";;",false);
            for(int k=0;k<lstParams.size();k++)
            {
                temp=lstParams.get(k);
                int idx=temp.indexOf("=");
                if(idx<=0)
                {
                    throw new WabacusRuntimeException(sourcestring+"不合法，参数名与值之间必须用=号连接");
                }
                mParams.put(temp.substring(0,idx).trim(),temp.substring(idx+1).trim());
            }
            lstResult.add(mParams);
        }
        return lstResult;
    }
    
    public String replaceSystemPlaceHolder(String content)
    {
        if(content==null) return null;
        content=Tools.replaceAll(content,"%WEBROOT%",Config.webroot);
        return content;
    }
    
    public String parseConfigPathToRealPath(String configpath,String webroot_abspath)
    {
        if(configpath==null||configpath.trim().equals("")) return "";
        configpath=configpath.trim();
        if(Tools.isDefineKey("classpath",configpath))
        {//相对classpath的路径
            configpath=Tools.getRealKeyByDefine("classpath",configpath);
            while(configpath.startsWith("/"))
            {//因为这种配置方式是用ClassLoader进行加载，而不是Class，所以必须不能以/打头
                configpath=configpath.substring(1);
            }
            while(configpath.endsWith("/"))
            {
                configpath=configpath.substring(0,configpath.length()-1);
            }
            configpath="classpath{"+configpath+"}";
        }else if(Tools.isDefineKey("relative",configpath))
        {
            configpath=Tools.getRealKeyByDefine("relative",configpath);
            configpath=FilePathAssistant.getInstance().standardFilePath(webroot_abspath+"\\"+configpath+"\\");
        }else
        {
            if(Tools.isDefineKey("absolute",configpath)) configpath=Tools.getRealKeyByDefine("absolute",configpath);
            configpath=FilePathAssistant.getInstance().standardFilePath(configpath+"\\");
        }
        return configpath;
    }
    
    public String getRealFilePath(String rootpath,String fileRelativePath)
    {
        String abspath=null;
        if(Tools.isDefineKey("classpath",rootpath))
        {
            String homepath=Tools.getRealKeyByDefine("classpath",rootpath);
            if(homepath.equals(""))
            {
                abspath=fileRelativePath;
            }else
            {
                while(fileRelativePath.startsWith("/"))
                    fileRelativePath=fileRelativePath.substring(1).trim();
                abspath=homepath+"/"+fileRelativePath;
            }
            abspath=Tools.replaceAll(abspath,"//","/");
            while(abspath.startsWith("/"))
                abspath=abspath.substring(1);//因为这种配置方式是用ClassLoader进行加载，而不是Class，所以必须不能以/打头
        }else
        {
            abspath=rootpath+"\\"+fileRelativePath;
            abspath=FilePathAssistant.getInstance().standardFilePath(abspath);
        }
        return abspath;
    }
    
    public File getFileObjByPathInClasspath(String rootpath,String relativepath)
    {
        while(rootpath.startsWith("/"))
        {
            rootpath=rootpath.substring(1);
        }
        try
        {
            rootpath=ConfigLoadManager.currentDynClassLoader.getResource(rootpath).getPath();
            rootpath=URLDecoder.decode(rootpath,"UTF-8");
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("在classpath中没有取到文件"+rootpath,e);
        }
        if(rootpath.toLowerCase().startsWith("jar:")||rootpath.indexOf(".jar!/")>0)
        {
           throw new WabacusConfigLoadingException(rootpath+"为jar包中的目录，无法创建jar包中文件路径的File对象");
        }
        if(!rootpath.endsWith("/")) rootpath=rootpath+"/";
//        while(rootpath.startsWith("/")) rootpath=rootpath.trim();
        while(relativepath.startsWith("/"))
            relativepath=relativepath.substring(1);
        rootpath=rootpath+relativepath;
        return new File(rootpath);
    }
    
    public String replaceAllImgPathInExportDataFile(HttpServletRequest request,String displayvalue)
    {
        String serverName=request.getServerName();
        String serverPort=String.valueOf(request.getServerPort());
        return replaceAllImgPathInExportDataFile("http://"+serverName+":"+serverPort,displayvalue);
    }
    
    public String replaceAllImgPathInExportDataFile(String host,String displayvalue)
    {
        if(displayvalue==null) return displayvalue;
        List<Map<String,Object>> lstImgObjs=RegexTools.getMatchObjectArray(displayvalue,
                "<img[^>]*>",false);
        if(lstImgObjs!=null&&lstImgObjs.size()>0)
        {
            String valueTmp;
            int startidxTmp;
            int endidxTmp;
            Map<String,Object> mTmp=null;
            for(int i=lstImgObjs.size()-1;i>=0;i--)
            {//从后向前替换，这样不会导致没替换的<img/>的startidx和endidx改变
                mTmp=lstImgObjs.get(i);
                valueTmp=(String)mTmp.get("value");
                startidxTmp=(Integer)mTmp.get("startindex");
                endidxTmp=(Integer)mTmp.get("endindex");
                if(valueTmp==null) continue;
                valueTmp=replaceImgSrc(valueTmp,host);
                if(valueTmp==null||valueTmp.trim().equals("")) continue;
                displayvalue=displayvalue.substring(0,startidxTmp)+valueTmp
                        +displayvalue.substring(endidxTmp);
            }
        }
        //        lstImgObjs=RegexTools.getMatchObjectArray(displayvalue,"&lt;img[^&gt;]*&gt;",false);
        return displayvalue;
    }
    
    private String replaceImgSrc(String imgstr,String host)
    {
        imgstr=imgstr.trim();
        int idxSrc=imgstr.toLowerCase().indexOf(" src");
        StringBuffer resultBuf=new StringBuffer();
        while(idxSrc>0)
        {
            resultBuf.append(imgstr.substring(0,idxSrc+4));
            imgstr=imgstr.substring(idxSrc+4).trim();
            if(!imgstr.startsWith("="))
            {
                idxSrc=imgstr.toLowerCase().indexOf(" src");
                continue;
            }
            resultBuf.append("=");
            imgstr=imgstr.substring(1).trim();
            if(imgstr.equals("")) return null;
            String tmp;
            char c=imgstr.charAt(0);
            if(c=='\\')
            {//=后面是\号，则如果后面跟上'或"，则此\号为转义符，后面也要以\"或\'结尾
                if(imgstr.length()==1)
                {
                    return null;
                }else if(imgstr.charAt(1)=='\'')
                {
                    imgstr=imgstr.substring(2);
                    resultBuf.append("\\\'");
                    tmp="\\\'";
                }else if(imgstr.charAt(1)=='"')
                {
                    imgstr=imgstr.substring(2);
                    resultBuf.append("\\\"");
                    tmp="\\\"";
                }else
                {
                    tmp=" ";
                }
            }else if(c=='\'')
            {
                imgstr=imgstr.substring(1);
                resultBuf.append("\'");
                tmp="\'";
            }else if(c=='"')
            {
                imgstr=imgstr.substring(1);
                resultBuf.append("\"");
                tmp="\"";
            }else
            {
                tmp=" ";
            }
            if(tmp.equals(" "))
            {
                int idx=imgstr.indexOf(" ");
                if(idx==0) return null;
                if(idx<0)
                {
                    idx=imgstr.length()-1;
                }
                String src=imgstr.substring(0,idx).trim();
                if(src.equals("")) return null;
                if(src.toLowerCase().startsWith("http://")) return null;//已经包括http://xxx/，则不用再加上
                if(!src.startsWith("/")) return null;//相对路径，则不加上http://xxx
                src=host+src;
                resultBuf.append(src).append(imgstr.substring(idx));
            }else
            {
                int idx=imgstr.indexOf(tmp);
                if(idx<=0) return null;
                String src=imgstr.substring(0,idx).trim();
                if(src.equals("")) return null;
                if(src.toLowerCase().startsWith("http://")) return null;
                if(!src.startsWith("/")) return null;
                src=host+src;
                resultBuf.append(src).append(imgstr.substring(idx));
            }
            return resultBuf.toString();
        }
        return null;
    }
    
    public String getComponentIdByGuid(String cguid)
    {
        if(cguid==null||cguid.trim().equals("")) return null;
        int idx=cguid.lastIndexOf(Consts_Private.GUID_SEPERATOR);
        if(idx<=0)
        {
            return null;
        }
        String cid=cguid.substring(idx+Consts_Private.GUID_SEPERATOR.length()).trim();
        if(cid.equals(""))
        {
            return null;
        }
        return cid;
    }
    
    public String getReportGuidByInputboxId(String inputboxid)
    {
        if(Tools.isEmpty(inputboxid)) return null;
        inputboxid=inputboxid.trim();
        int idx=inputboxid.indexOf(Consts_Private.GUID_SEPERATOR);
        String pageid=inputboxid.substring(0,idx);
        inputboxid=inputboxid.substring(idx+Consts_Private.GUID_SEPERATOR.length());
        idx=inputboxid.indexOf("_wxcol_");
        if(idx<=0) idx=inputboxid.indexOf("_wxcondition_");
        if(idx<=0) return null;
        String reportid=inputboxid.substring(0,idx);
        return pageid+Consts_Private.GUID_SEPERATOR+reportid;
    }
    
    public String parseDynPartsInConfigValue(String configvalue,Map<String,String> mDynParts,String[] dynTypenamesArr)
    {
        if(dynTypenamesArr==null||dynTypenamesArr.length==0) return configvalue;
        String typenameTmp, strStart, strDynValue, strEnd, placeHolderTmp;
        for(int i=0;i<dynTypenamesArr.length;i++)
        {
            int placeholderIdxTmp=0;
            typenameTmp=dynTypenamesArr[i];
            int idx=configvalue.indexOf(typenameTmp+"{");
            while(idx>=0)
            {
                strStart=configvalue.substring(0,idx).trim();
                strEnd=configvalue.substring(idx);
                idx=strEnd.indexOf("}");
                if(idx<0) break;
                strDynValue=strEnd.substring(0,idx+1);//把右边的}号也括进来，即组成一个完整的type{...}
                strEnd=strEnd.substring(idx+1).trim();
                placeHolderTmp="[PLACE_HOLDER_"+typenameTmp+"_"+placeholderIdxTmp+"]";
                mDynParts.put(placeHolderTmp,strDynValue);
                configvalue=strStart+placeHolderTmp+strEnd;
                idx=configvalue.indexOf(typenameTmp+"{");
                placeholderIdxTmp++;
            }
        }
        return configvalue;
    }
    
    public boolean isGetRequestContextValue(String paramname)
    {
        if(paramname==null||paramname.trim().equals("")) return false;
        if(Tools.isDefineKey("url",paramname)) return true;
        if(Tools.isDefineKey("request",paramname)) return true;
        if(Tools.isDefineKey("rrequest",paramname)) return true;
        if(Tools.isDefineKey("session",paramname)) return true;
        return false;
    }
    
    public String getRequestContextStringValue(ReportRequest rrequest,String key,String defaultvalue)
    {
        Object obj=getRequestContextValue(rrequest,key);
        if(obj==null||obj.toString().trim().equals("")) return defaultvalue;
        return obj.toString().trim();
    }
    
    public String getRequestContextStringValue(HttpServletRequest request,String key,String defaultvalue)
    {
        Object obj=getRequestContextValue(request,key);
        if(obj==null||obj.toString().trim().equals("")) return defaultvalue;
        return obj.toString().trim();
    }
    
    public Object getRequestContextValue(ReportRequest rrequest,String key)
    {
        if(key==null||key.trim().equals("")) return null;
        if(Tools.isDefineKey("url",key)) return getValueFromUrl(rrequest.getRequest(),key);
        if(Tools.isDefineKey("rrequest",key)) return getValueFromReportRequest(rrequest,key);
        if(Tools.isDefineKey("request",key)) return getValueFromRequest(rrequest.getRequest(),key);
        if(Tools.isDefineKey("session",key)) return getValueFromSession(rrequest.getRequest().getSession(),key);
        return null;
    }
    
    public Object getRequestContextValue(HttpServletRequest request,String key)
    {
        if(key==null||key.trim().equals("")) return null;
        if(Tools.isDefineKey("url",key)) return getValueFromUrl(request,key);//从url中取值
        if(Tools.isDefineKey("request",key)) return getValueFromRequest(request,key);
        if(Tools.isDefineKey("session",key)) return getValueFromSession(request.getSession(),key);
        return null;
    }
    
    private String getValueFromUrl(HttpServletRequest request,String paramname)
    {
        if(request==null) return null;
        paramname=Tools.getRealKeyByDefine("url",paramname);
        if(paramname==null||paramname.trim().equals("")) return null;
        String value=request.getParameter(paramname);
        if(value==null||value.trim().equals("")) return null;
        return value.trim();
    }
    
    private Object getValueFromReportRequest(ReportRequest rrequest,String key)
    {
        if(rrequest==null) return null;
        String realkey=Tools.getRealKeyByDefine("rrequest",key);
        if(realkey==null||realkey.trim().equals("")) return null;
        int idx=realkey.lastIndexOf(".");
        if(idx>0)
        {
            String pojokey=realkey.substring(0,idx).trim();
            String propname=realkey.substring(idx+1).trim();
            if(pojokey.equals("")||propname.equals("")) return null;
            return getPojoPropertyValue(rrequest.getAttribute(pojokey),propname);
        }else
        {
            return rrequest.getAttribute(realkey);
        }
    }

    public Object getValueFromRequest(HttpServletRequest request,String key)
    {
        if(request==null) return null;
        String realkey=Tools.getRealKeyByDefine("request",key);
        if(realkey==null||realkey.trim().equals("")) return null;
        int idx=realkey.lastIndexOf(".");
        if(idx>0)
        {
            String pojokey=realkey.substring(0,idx).trim();
            String propname=realkey.substring(idx+1).trim();
            if(pojokey.equals("")||propname.equals("")) return null;
            return getPojoPropertyValue(request.getAttribute(pojokey),propname);
        }else
        {
            return request.getAttribute(realkey);
        }
    }
    
    public Object getValueFromSession(HttpSession session,String key)
    {
        if(session==null) return null;
        String realkey=Tools.getRealKeyByDefine("session",key);
        if(realkey==null||realkey.trim().equals("")) return null;
        int idx=realkey.lastIndexOf(".");
        if(idx>0)
        {
            String pojokey=realkey.substring(0,idx).trim();
            String propname=realkey.substring(idx+1).trim();
            if(pojokey.equals("")||propname.equals("")) return null;
            return getPojoPropertyValue(session.getAttribute(pojokey),propname);
        }else
        {
            return session.getAttribute(realkey);
        }
    }
    
    private Object getPojoPropertyValue(Object pojoObj,String propname)
    {
        if(pojoObj==null||propname==null||propname.trim().equals("")) return null;
        try
        {
            Method m=pojoObj.getClass().getMethod("get"+propname.substring(0,1).toUpperCase()+propname.substring(1),new Class[] {});
            return m.invoke(pojoObj,new Object[] {});
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("从"+pojoObj.getClass().getName()+"对象中取成员变量"+propname+"的值失败",e);
        }
    }
    
    public String encodeAttachFilename(HttpServletRequest request,String title)
    {
        if(title==null) title="";
        title=Tools.replaceAll(title,"&nbsp;","");
        title=title.replaceAll("<.*?\\>","").trim();
        try
        {
            if(Config.encode.equalsIgnoreCase("utf-8"))
            {
                if(request.getHeader("User-Agent").toLowerCase().indexOf("firefox")>0)
                {
                    title=new String(title.getBytes("UTF-8"),"iso-8859-1");
                }else
                {
                    title=URLEncoder.encode(title,"UTF-8");
                }
            }else
            {
                title=new String(title.getBytes(Config.encode),"iso-8859-1");
            }
        }catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        return title;
    }
    
    public Object[] parseStringWithDynPart(PageBean pbean,String value)
    {
        if(value==null||value.equals("")) return new Object[] { null, null };
        Map<String,String> mDynParts=new HashMap<String,String>();
        value=parseStringWithCertainDynPart(value,"$",mDynParts);
        if(mDynParts.size()>0)
        {
            String valueTmp;
            for(Entry<String,String> entryTmp:mDynParts.entrySet())
            {
                valueTmp=Config.getInstance().getResourceString(null,pbean,entryTmp.getValue(),false);
                if(valueTmp==null) valueTmp=entryTmp.getValue();
                value=Tools.replaceAll(value,entryTmp.getKey(),valueTmp);
            }
        }
        return parseStringWithDynPart(value);
    }
    
    public Object[] parseStringWithDynPart(String value)
    {
        if(value==null||value.equals("")) return new Object[] { null, null };
        Map<String,String> mDynParts=new HashMap<String,String>();
        value=parseStringWithCertainDynPart(value,"url",mDynParts);
        value=parseStringWithCertainDynPart(value,"rrequest",mDynParts);
        value=parseStringWithCertainDynPart(value,"request",mDynParts);
        value=parseStringWithCertainDynPart(value,"session",mDynParts);
        if(mDynParts.size()==0) mDynParts=null;
        return new Object[] { value, mDynParts };
    }
    
    private String parseStringWithCertainDynPart(String value,String partname,Map<String,String> mDynParts)
    {
        if(value==null||value.trim().equals("")) return value;
        int index=value.indexOf(partname+"{");
        int placeholderindex=0;
        String placeholderTmp;
        while(index>=0)
        {
            String valueTmp=value.substring(index);
            int index2=valueTmp.indexOf("}");
            if(index2<0) break;
            placeholderTmp="PLACE_HOLDER_"+partname.toUpperCase()+"_"+placeholderindex;
            mDynParts.put(placeholderTmp,valueTmp.substring(0,index2+1));//取到完整的partname{...}部分
            if(Tools.getRealKeyByDefine(partname,mDynParts.get(placeholderTmp)).trim().equals(""))
            {
                throw new WabacusConfigLoadingException("配置的字符串"+value+"中的"+partname+"{}为空");
            }
            value=value.substring(0,index)+placeholderTmp+valueTmp.substring(index2+1);
            index=value.indexOf(partname+"{");
            placeholderindex++;
        }
        return value;
    }
    
    public String getStringValueWithDynPart(ReportRequest rrequest,String value,Map<String,String> mDynParts,String defaultvalue)
    {
        if(mDynParts!=null&&mDynParts.size()>0)
        {
            for(Entry<String,String> entryTmp:mDynParts.entrySet())
            {
                value=Tools.replaceAll(value,entryTmp.getKey(),rrequest==null?entryTmp.getValue():getRequestContextStringValue(rrequest,entryTmp.getValue(),""));
            }
        }
        value=rrequest==null?value:rrequest.getI18NStringValue(value);
        if(value==null||value.equals("")) value=defaultvalue;
        return value;
    }
    
    public String getStringValueWithDynPart(HttpServletRequest request,String value,Map<String,String> mDynParts,String defaultvalue)
    {
        if(mDynParts!=null&&mDynParts.size()>0)
        {
            for(Entry<String,String> entryTmp:mDynParts.entrySet())
            {
                value=Tools.replaceAll(value,entryTmp.getKey(),request==null?entryTmp.getValue():getRequestContextStringValue(request,entryTmp
                        .getValue(),""));
            }
        }
        if(value==null||value.equals("")) value=defaultvalue;
        return value;
    }
    
    public Object[] parseStylepropertyWithDynPart(String styleproperty)
    {
        Object[] objArr=parseStringWithDynPart(styleproperty);
        if(objArr[1]==null||((Map<String,String>)objArr[1]).size()==0) return new Object[] { objArr[0], null };
        styleproperty=(String)objArr[0];
        if(styleproperty==null) styleproperty="";
        List<String> lstDynParts=new ArrayList<String>();
        for(String keyTmp:((Map<String,String>)objArr[1]).keySet())
        {
            styleproperty=Tools.replaceAll(styleproperty,keyTmp," ");
            lstDynParts.add(((Map<String,String>)objArr[1]).get(keyTmp));
        }
        return new Object[] { styleproperty.trim(), lstDynParts };
    }
    
    public String getStylepropertyWithDynPart(ReportRequest rrequest,String styleproperty,List<String> lstDynStylepropertyParts,String defaultvalue)
    {
        if(styleproperty==null) styleproperty="";
        if(lstDynStylepropertyParts!=null&&lstDynStylepropertyParts.size()>0)
        {
            for(String dynValTmp:lstDynStylepropertyParts)
            {//依次替换掉value中所有占位符为真正的值
                if(rrequest==null)
                {
                    styleproperty+=" "+dynValTmp;
                }else
                {
                    styleproperty=Tools.mergeHtmlTagPropertyString(styleproperty,getRequestContextStringValue(rrequest,dynValTmp,""),1);
                }
            }
        }
        return styleproperty.trim().equals("")?defaultvalue:styleproperty.trim();
    }
    
    public String parseAndGetRealValue(HttpServletRequest request,String originValue,String defaultvalue)
    {
        Object[] objArr=parseStringWithDynPart(originValue);
        return getStringValueWithDynPart(request,(String)objArr[0],(Map<String,String>)objArr[1],defaultvalue);
    }
    
    public String parseAndGetRealValue(ReportRequest rrequest,String originValue,String defaultvalue)
    {
        Object[] objArr=parseStringWithDynPart(originValue);
        return getStringValueWithDynPart(rrequest,(String)objArr[0],(Map<String,String>)objArr[1],defaultvalue);
    }
    
    public String getSpacingDisplayString(int spacenum)
    {
        if(spacenum<=0) return "";
        return "<span style=\"margin-left:"+spacenum+"px;\"></span>";
    }
    
    private static List<String> lstSupportHtmlSizeUnits=new ArrayList<String>();
    static
    {
        lstSupportHtmlSizeUnits.add("%");
        lstSupportHtmlSizeUnits.add("em");
        lstSupportHtmlSizeUnits.add("ex");
        lstSupportHtmlSizeUnits.add("px");
        lstSupportHtmlSizeUnits.add("pixel");
        lstSupportHtmlSizeUnits.add("in");
        lstSupportHtmlSizeUnits.add("cm");
        lstSupportHtmlSizeUnits.add("mm");
        lstSupportHtmlSizeUnits.add("pt");
        lstSupportHtmlSizeUnits.add("pc");
    }
    
    public String[] parseHtmlElementSizeValueAndType(String htmlsize)
    {
        if(htmlsize==null||htmlsize.trim().equals("")) return null;
        htmlsize=htmlsize.toLowerCase().trim();
        String sizevalue;
        String sizetype="";
        StringBuffer tmpBuf=new StringBuffer();
        int i=0,len=htmlsize.length();
        for(;i<len;i++)
        {
            if(htmlsize.charAt(i)>='0'&&htmlsize.charAt(i)<='9')
            {
                tmpBuf.append(htmlsize.charAt(i));
            }else
            {
                break;
            }
        }
        sizevalue=tmpBuf.toString();
        if(sizevalue.equals("")) return null;
        int isizevalue=0;
        try
        {
            isizevalue=Integer.parseInt(sizevalue);
        }catch(NumberFormatException e)
        {
            log.debug(e);
            return null;
        }
        if(isizevalue<0) return null;
        if(i<len) sizetype=htmlsize.substring(i,len).trim();//宽度类型（可能是百分比，也可能是像素等等）
        if(!sizetype.equals("")&&!lstSupportHtmlSizeUnits.contains(sizetype.toLowerCase()))
        {
            log.warn("配置的html元素大小"+htmlsize+"无效，它的值将变为"+isizevalue);
            sizetype="";
        }
        return new String[]{String.valueOf(isizevalue),sizetype};
    }
    
    public String readFileContentByPath(String filepath)
    {
        if(filepath==null||filepath.trim().equals("")) return "";
        filepath=filepath.trim();
        BufferedReader bis=null;
        try
        {
            bis=new BufferedReader(new InputStreamReader(readFile(filepath),Config.encode));
            StringBuffer contentBuf=new StringBuffer();
            String str=bis.readLine();
            while(str!=null)
            {
                contentBuf.append(str);
                str=bis.readLine();
            }
            return contentBuf.toString();
        }catch(FileNotFoundException e)
        {
            throw new WabacusConfigLoadingException("没有找到文件"+filepath,e);
        }catch(IOException ioe)
        {
            throw new WabacusConfigLoadingException("读取文件"+filepath+"失败",ioe);
        }finally
        {
            if(bis!=null)
            {
                try
                {
                    bis.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public InputStream readFile(String filepath) throws IOException
    {
        if(filepath==null||filepath.trim().equals("")) return null;
        filepath=filepath.trim();
        InputStream istream=null;
        if(Tools.isDefineKey("classpath",filepath))
        {
            filepath=Tools.getRealKeyByDefine("classpath",filepath).trim();
            while(filepath.startsWith("/"))
            {
                filepath=filepath.substring(1);
            }
            istream=ConfigLoadManager.currentDynClassLoader.getResourceAsStream(filepath);
        }else if(Tools.isDefineKey("relative",filepath))
        {
            filepath=Tools.getRealKeyByDefine("relative",filepath).trim();
            filepath=Config.webroot_abspath+"\\"+filepath;
            filepath=FilePathAssistant.getInstance().standardFilePath(filepath);
            istream=new FileInputStream(filepath);
        }else
        {
            if(Tools.isDefineKey("absolute",filepath))
            {
                filepath=Tools.getRealKeyByDefine("absolute",filepath).trim();
            }
            istream=new FileInputStream(filepath);
        }
        return istream;
    }
    
    public void includeDynTpl(ReportRequest rrequest,AbsComponentType comObj,String dyntplpath)
    {
        if(dyntplpath==null||dyntplpath.trim().equals("")||dyntplpath.trim().equals(Consts_Private.REPORT_TEMPLATE_NONE))
        {
            return;
        }
        if(rrequest.isExportToLocalFile())
        {
            throw new WabacusRuntimeException("当前是以在服务器端落地的方式导出组件"+comObj.getConfigBean().getPath()+"的数据，不能为它指定导出动态模板");
        }
        rrequest.getRequest().setAttribute("WX_COMPONENT_OBJ",comObj);
        try
        {
            RequestDispatcher rd=rrequest.getRequest().getRequestDispatcher(dyntplpath.trim());
            rd.include(rrequest.getRequest(),rrequest.getWResponse().getResponse());
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("通过模板"+dyntplpath+"显示组件"+comObj.getConfigBean().getPath()+"失败",e);
        }
    }
    
    public void release(Connection conn,Statement stmt)
    {
        try
        {
            if(stmt!=null)
            {
                stmt.close();
                stmt=null;
            }
        }catch(Exception e)
        {
            log.error("关闭数据库Statement失败",e);
        }
        try
        {
            if(conn!=null)
            {
                conn.close();
                conn=null;
            }

        }catch(Exception e)
        {
            log.error("关闭数据库连接失败",e);
        }
    }
}
