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
package com.wabacus.system;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.exception.WabacusRuntimeTerminateException;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.chart.FusionChartsReportType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class WabacusResponse
{
    private static Log log=LogFactory.getLog(WabacusResponse.class);
    
    private PrintWriter out;

    private JspWriter jspout;
    
    private StringBuilder outBuf;

    private ReportRequest rrequest;
    
    private MessageCollector messageCollector;

    private int statecode=Consts.STATECODE_SUCCESS;

    private List<String[]> lstSuccessOnloadMethods;

    private List<String[]> lstFailedOnloadMethods;

    private Map<String,List<String[]>> mChartOnloadMethods;
    
    private Map<String,DisplayInputboxBean> mDisplayInputboxsOnload;//运行时要在onload中显示的输入框集合
    
    private Set<String> sChildSelectboxidsOnload;
    
    private List<String> lstUpdateReportGuids;
    
    private String dynamicRefreshComponentGuid;
    
    private String dynamicSlaveReportId;
    
    private HttpServletResponse response;
    
    private boolean hasInitOutput;//标识有没有初始化输出
    
    public WabacusResponse(HttpServletResponse response)
    {
        this.response=response;
        messageCollector=new MessageCollector(this);
    }

    public MessageCollector getMessageCollector()
    {
        return messageCollector;
    }

    public void clearOnloadMethod()
    {
        this.lstSuccessOnloadMethods=null;
        this.lstFailedOnloadMethods=null;
    }

    public void addOnloadMethod(String methodname,Map<String,String> mParams,boolean isInsertFirst)
    {
        addOnloadMethod(methodname,mParams,isInsertFirst,Consts.STATECODE_SUCCESS);
    }
    
    public void addOnloadMethod(String methodname,String methodparams,boolean isInsertFirst)
    {
        addOnloadMethod(methodname,methodparams,isInsertFirst,Consts.STATECODE_SUCCESS);
    }

    public void addOnloadMethod(String methodname,Map<String,String> mParams,boolean isInsertFirst,int statecode)
    {
        StringBuffer paramsBuf=new StringBuffer();
        if(mParams!=null)
        {
            for(Entry<String,String> entryTmp:mParams.entrySet())
            {
                paramsBuf.append(entryTmp.getKey()).append(":\"").append(entryTmp.getValue()).append("\",");
            }
        }
        String params=paramsBuf.toString();
        if(!params.trim().equals("")) params="{"+params+"}";
        addOnloadMethod(methodname,params,isInsertFirst,statecode);
    }
    
    public void addOnloadMethod(String methodname,String methodparams,boolean isInsertFirst,int statecode)
    {
        if(methodname==null||methodname.trim().equals("")) return;
        if(methodparams!=null&&!methodparams.trim().equals(""))
        {
            methodparams=methodparams.trim();
            if(!methodparams.startsWith("{")||!methodparams.endsWith("}"))
            {
                methodparams="{"+methodparams+"}";
            }
        }
        String[] methodArr=new String[]{methodname,methodparams};
        if(this.lstFailedOnloadMethods==null) this.lstFailedOnloadMethods=new ArrayList<String[]>();
        if(this.lstSuccessOnloadMethods==null) this.lstSuccessOnloadMethods=new ArrayList<String[]>();
        if(statecode==Consts.STATECODE_FAILED)
        {
            if(isInsertFirst)
            {
                lstFailedOnloadMethods.add(0,methodArr);
            }else
            {
                lstFailedOnloadMethods.add(methodArr);
            }
        }else if(statecode==Consts.STATECODE_SUCCESS)
        {//只在成功时调用
            if(isInsertFirst)
            {
                lstSuccessOnloadMethods.add(0,methodArr);
            }else
            {
                lstSuccessOnloadMethods.add(methodArr);
            }
        }else
        {
            if(isInsertFirst)
            {
                lstSuccessOnloadMethods.add(0,methodArr);
                lstFailedOnloadMethods.add(0,methodArr);
            }else
            {//加在后面调用
                lstSuccessOnloadMethods.add(methodArr);
                lstFailedOnloadMethods.add(methodArr);
            }
        }
    }
    
    public void addChartOnloadMethod(String reportid,String methodname,String methodparams,boolean isInsertFirst)
    {
        FusionChartsReportType fcreportTypeObj=(FusionChartsReportType)rrequest.getDisplayReportTypeObj(reportid);
        if(fcreportTypeObj.getFcrbean().isLinkChart())
        {
            throw new WabacusRuntimeException("报表"+fcreportTypeObj.getReportBean().getPath()+"是linkchart，不能添加chartonload函数");
        }
        if(this.mChartOnloadMethods==null) this.mChartOnloadMethods=new HashMap<String,List<String[]>>();
        if(methodparams!=null&&!methodparams.trim().equals(""))
        {
            methodparams=methodparams.trim();
            if(!methodparams.startsWith("{")||!methodparams.endsWith("}"))
            {
                methodparams="{"+methodparams+"}";
            }
        }
        List<String[]> lstMethods=this.mChartOnloadMethods.get(reportid);
        if(lstMethods==null)
        {
            lstMethods=new ArrayList<String[]>();
            this.mChartOnloadMethods.put(reportid,lstMethods);
        }
        if(isInsertFirst)
        {
            lstMethods.add(0,new String[]{methodname,methodparams});
        }else
        {
            lstMethods.add(new String[]{methodname,methodparams});
        }
    }
    
    public List<String[]> getLstChartOnloadMethods(String reportid)
    {
        if(this.mChartOnloadMethods==null) return null;
        return this.mChartOnloadMethods.get(reportid);
    }
    
    public HttpServletResponse getResponse()
    {
        return response;
    }

    public void setResponse(HttpServletResponse response)
    {
        this.response=response;
    }

    public StringBuilder getOutBuf()
    {
        return outBuf;
    }

    public int getStatecode()
    {
        return statecode;
    }

    public void setStatecode(int statecode)
    {
        if(statecode!=Consts.STATECODE_NONREFRESHPAGE&&statecode!=Consts.STATECODE_FAILED&&statecode!=Consts.STATECODE_SUCCESS)
        {
            log.warn("设置的响应状态码"+statecode+"不支持，将用默认的成功状态码");
            statecode=Consts.STATECODE_SUCCESS;
        }
        this.statecode=statecode;
    }
    
    public void terminateResponse(int statecode)
    {
        setStatecode(statecode);
        throw new WabacusRuntimeTerminateException();
    }
    
    public void addUpdateReportGuid(String reportguid)
    {
        if(reportguid==null||reportguid.trim().equals("")) return;
        if(this.lstUpdateReportGuids==null)
        {
            this.lstUpdateReportGuids=new ArrayList<String>();
        }else if(this.lstUpdateReportGuids.contains(reportguid))
        {
            return;
        }
        this.lstUpdateReportGuids.add(reportguid);
    }

    public ReportRequest getRRequest()
    {
        return rrequest;
    }

    public void setRRequest(ReportRequest rrequest)
    {
        this.rrequest=rrequest;
    }

    public void setJspout(JspWriter jspout)
    {
        this.jspout=jspout;
    }

    public String invokeOnloadMethodsFirstTime()
    {
        List<String[]> lstOnloadMethods=getLstRealOnloadMethods();
        if(lstOnloadMethods==null||lstOnloadMethods.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        for(String[] methodTmp:lstOnloadMethods)
        {
            if(methodTmp==null||methodTmp.length!=2) continue;
            resultBuf.append(methodTmp[0]).append("(");
            if(methodTmp[1]!=null&&!methodTmp[1].trim().equals(""))
            {
                resultBuf.append(methodTmp[1]);
            }
            resultBuf.append(");");
        }
        return resultBuf.toString();
    }
    
    public String assembleResultsInfo(Throwable t)
    {
        String defaultErrorPrompt=null;
        if(t!=null&&!(t instanceof WabacusRuntimeTerminateException))
        {
            defaultErrorPrompt=Config.getInstance().getResources().getString(rrequest,rrequest.getPagebean(),Consts.LOADERROR_MESS_KEY,false);
            if(Tools.isEmpty(defaultErrorPrompt))
            {
                defaultErrorPrompt="<strong>System is busy,Please try later</strong>";
            }else
            {
                defaultErrorPrompt=rrequest.getI18NStringValue(defaultErrorPrompt.trim());
            }
        }
        if(rrequest.getPagebean()==null)
        {
            log.error("没有取到"+rrequest.getStringAttribute("PAGEID","")+"对应的页面配置");
            return "没有取到"+rrequest.getStringAttribute("PAGEID","")+"对应的页面配置";
        }
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(showPageUrlSpan());//输出URL放在前面，以便用户在onload函数中能获取到
        if(!rrequest.isLoadedByAjax()||(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&rrequest.getShowtype()!=Consts.DISPLAY_ON_PRINT))
        {
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                String promptmessages=this.messageCollector.promptMessageFirstTime(defaultErrorPrompt);
                if(!Tools.isEmpty(promptmessages))
                {
                    resultBuf.append("<span style='display:none'>templary</span>");
                    resultBuf.append("<script type=\"text/javascript\">");
                    resultBuf.append(promptmessages);
                    resultBuf.append("</script>");
                }
            }else
            {
                resultBuf.append(this.messageCollector.promptMessageInNonPage(defaultErrorPrompt));
            }
        }else
        {
            String pageid=rrequest.getPagebean().getId();
            resultBuf.append("<RESULTS_INFO-").append(pageid).append(">").append("{");
            String confirmessage=this.messageCollector.getConfirmmessage();
            if(confirmessage!=null&&!confirmessage.trim().equals(""))
            {
                resultBuf.append("confirmessage:\"").append(confirmessage).append("\"");
                resultBuf.append(",confirmkey:\"").append(this.messageCollector.getConfirmkey()).append("\"");
                resultBuf.append(",confirmurl:\"").append(this.messageCollector.getConfirmurl()).append("\"");
            }else
            {
                resultBuf.append("pageurl:\"").append(rrequest.getUrl()).append("\",");
                if(rrequest.getPagebean().isShouldProvideEncodePageUrl())
                {
                    resultBuf.append("pageEncodeUrl:\"").append(Tools.convertBetweenStringAndAscii(rrequest.getUrl(),true)).append("\",");
                }
                if(dynamicRefreshComponentGuid!=null&&!dynamicRefreshComponentGuid.trim().equals(""))
                {//本次是动态产生的刷新组件ID
                    resultBuf.append("dynamicRefreshComponentGuid:\"").append(dynamicRefreshComponentGuid).append("\",");
                    if(dynamicSlaveReportId!=null&&!dynamicSlaveReportId.trim().equals(""))
                    {
                        resultBuf.append("dynamicSlaveReportId:\"").append(dynamicSlaveReportId).append("\",");
                    }
                }
                resultBuf.append("statecode:").append(this.statecode).append(",");
                resultBuf.append(this.messageCollector.promptMessageByRefreshJs(defaultErrorPrompt));
                List<String[]> lstOnloadMethods=getLstRealOnloadMethods();
                if(lstOnloadMethods!=null&&lstOnloadMethods.size()>0)
                {
                    resultBuf.append("onloadMethods:[");
                    for(String[] methodTmp:lstOnloadMethods)
                    {
                        if(methodTmp==null||methodTmp.length!=2) continue;
                        resultBuf.append("{methodname:").append(methodTmp[0]);
                        if(methodTmp[1]!=null&&!methodTmp[1].trim().equals(""))
                        {
                            resultBuf.append(",methodparams:").append(methodTmp[1]);
                        }
                        resultBuf.append("},");
                    }
                    if(resultBuf.charAt(resultBuf.length()-1)==',')
                    {
                        resultBuf.deleteCharAt(resultBuf.length()-1);
                    }
                    resultBuf.append("],");
                }
                if(lstUpdateReportGuids!=null&&lstUpdateReportGuids.size()>0)
                {//本次更新的报表GUID列表
                    resultBuf.append("updateReportGuids:[");
                    for(String rguidTmp:lstUpdateReportGuids)
                    {
                        resultBuf.append("{value:\"").append(rguidTmp).append("\"},");
                    }
                    if(resultBuf.charAt(resultBuf.length()-1)==',')
                    {
                        resultBuf.deleteCharAt(resultBuf.length()-1);
                    }
                    resultBuf.append("],");
                }
                if(resultBuf.charAt(resultBuf.length()-1)==',')
                {
                    resultBuf.deleteCharAt(resultBuf.length()-1);
                }
            }
            resultBuf.append("}").append("</RESULTS_INFO-").append(pageid).append(">");
        }
        return resultBuf.toString();
    }
    
    private boolean hasDisplayPageUrlSpan=false;
    
    public String showPageUrlSpan()
    {
        if(hasDisplayPageUrlSpan) return "";
        hasDisplayPageUrlSpan=true;
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE
                &&(!rrequest.isLoadedByAjax()||rrequest.getStringAttribute("WX_ISOUTERPAGE","").equals("true")))
        {//输出当前页面的URL（不是通过refreshComponent()方法请求的页面，或者是其它页面通过refreshComponent()进行带“返回”功能跳转到此页面，都要输出存放URL的<span/>和所有父URL）
            String pageurlspan="<span id=\""+rrequest.getPagebean().getId()+"_url_id\" style=\"display:none;\" value=\""
                    +Tools.htmlEncode(Tools.jsParamEncode(rrequest.getUrl()))+"\"";
            if(rrequest.getPagebean().isShouldProvideEncodePageUrl())
            {
                pageurlspan=pageurlspan+" encodevalue=\""+Tools.convertBetweenStringAndAscii(rrequest.getUrl(),true)+"\"";
            }
            String ancestorUrls=rrequest.getStringAttribute("ancestorPageUrls","");
            if(!ancestorUrls.equals(""))
            {
                pageurlspan=pageurlspan+" ancestorPageUrls=\""+ancestorUrls+"\"";
            }
            return pageurlspan+"></span>";
        }
        return "";
    }
    
    private List<String[]> getLstRealOnloadMethods()
    {
        if(this.statecode==Consts.STATECODE_FAILED) return this.lstFailedOnloadMethods;
        List<String[]> lstOnloadMethodsResult=new ArrayList<String[]>();
        if(this.lstSuccessOnloadMethods!=null) lstOnloadMethodsResult.addAll(this.lstSuccessOnloadMethods);
        String[] displayInputboxMethod=getDisplayInputboxOnloadMethod();
        if(displayInputboxMethod!=null) lstOnloadMethodsResult.add(displayInputboxMethod);
        String[] initChildSelectboxMethod=getInitChildSelectboxIdsOnloadMethod();
        if(initChildSelectboxMethod!=null) lstOnloadMethodsResult.add(initChildSelectboxMethod);
        return lstOnloadMethodsResult;
    }
    
    public void setDynamicRefreshComponentGuid(String componentguid,String slaveReportId)
    {
        this.dynamicRefreshComponentGuid=componentguid;
        this.dynamicSlaveReportId=slaveReportId;
    }
    
    public boolean isOutputImmediately()
    {
        return this.response!=null;
    }
    
    public void initOutput(String attachFilename)
    {
        hasInitOutput=true;
        if(response!=null&&rrequest.getRequest()!=null&&!rrequest.isExportToLocalFile())
        {//本次是直接输出到页面，且不是以落地的方式导出数据文件
            rrequest.getRequest().getSession();
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_RICHEXCEL||rrequest.getShowtype()==Consts.DISPLAY_ON_WORD)
            {
                String filesuffix=rrequest.getShowtype()==Consts.DISPLAY_ON_RICHEXCEL?".xls":".doc";
                attachFilename=WabacusAssistant.getInstance().encodeAttachFilename(rrequest.getRequest(),attachFilename);
                response.setHeader("Content-disposition","attachment;filename="+attachFilename+filesuffix);
            }
            try
            {
                out=response.getWriter();
            }catch(IOException e)
            {
                throw new WabacusRuntimeException("初始化页面输出失败",e);
            }
            if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PRINT)
            {
                out
                        .println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
                out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+Config.encode+"\">");
            }
        }else
        {
            this.outBuf=new StringBuilder();
            outBuf
                    .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
            outBuf.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+Config.encode+"\">");
        }
    }
    
    public void println(String content)
    {
        println(content,false);
    }
    
    public void println(String content,boolean overwrite)
    {
        if(!hasInitOutput) initOutput(null);
        if(content==null) return;
        content=Tools.replaceAll(content,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin());
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_RICHEXCEL||rrequest.getShowtype()==Consts.DISPLAY_ON_WORD
                ||rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
        {
            content=WabacusAssistant.getInstance().replaceAllImgPathInExportDataFile(rrequest.getRequest(),content);
        }
        if(rrequest.isExportToLocalFile())
        {//当前是在落地的数据导出，则直接输出到文件中
            if(overwrite) outBuf=new StringBuilder();
            outBuf.append(content);
            if(outBuf.length()>10240) writeBufDataToLocalFile();
        }else if(jspout!=null)
        {//当前是通过<wx:report/>显示报表，则从jsp中取out进行输出
            try
            {
                jspout.println(content);
            }catch(IOException e)
            {
                log.debug("向页面输出字符串："+content+"时失败",e);
                jspout=null;
                if(out!=null)
                {
                    out.println(content);
                }else
                {
                    if(overwrite) outBuf=new StringBuilder();
                    outBuf.append(content);
                }
            }
        }else if(out!=null)
        {
            out.println(content);
        }else
        {
            if(overwrite) outBuf=new StringBuilder();
            outBuf.append(content);
        }
        /*if(mReportsWithIncludePage!=null&&mReportsWithIncludePage.size()>0)
        {
            String starttagTmp=ReportAssistant.getInstance().getStartTagOfIncludeFilePlaceholder(rrequest.getPagebean());
            String endtagTmp=ReportAssistant.getInstance().getEndTagOfIncludeFilePlaceholder(rrequest.getPagebean());
            int idx=content.indexOf(starttagTmp);
            String reportidTmp;
            AbsReportType reportTypeObjTmp;
            *
             * 每个包含外部页面的格式为<tag>reportid</tag>，其中<tag>和</tag>分别是starttagTmp和endtagTmp
             *//*
            String dynTplPathTmp=null;
            while(true)
            {
                if(idx<0) break;
                out.println(content.substring(0,idx));
                content=content.substring(idx+starttagTmp.length());
                idx=content.indexOf(endtagTmp);
                if(idx<0) break;
                reportidTmp=content.substring(0,idx);
                if(reportidTmp==null||reportidTmp.trim().equals("")||!this.mReportsWithIncludePage.containsKey(reportidTmp))
                {
                    idx=content.indexOf(starttagTmp);
                    continue;
                }
                reportTypeObjTmp=mReportsWithIncludePage.get(reportidTmp);
                rrequest.getRequest().setAttribute("WX_COMPONENT_OBJ",reportTypeObjTmp);
                if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
                {
                    dynTplPathTmp=reportTypeObjTmp.getReportBean().getDynTplPath();
                }else
                {
                    dynTplPathTmp=reportTypeObjTmp.getReportBean().getDynDataExportTplPath();
                }
                RequestDispatcher rd=rrequest.getRequest().getRequestDispatcher(dynTplPathTmp);
                rd.include(rrequest.getRequest(),response);
                content=content.substring(idx+endtagTmp.length());
                idx=content.indexOf(starttagTmp);
            }
        }*/
    }
    
    public void print(String content)
    {
        print(content,false);
    }
    
    public void print(String content,boolean overwrite)
    {
        if(!hasInitOutput) initOutput(null);
        if(content==null) return;
        content=Tools.replaceAll(content,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin());
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_RICHEXCEL||rrequest.getShowtype()==Consts.DISPLAY_ON_WORD
                ||rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
        {
            content=WabacusAssistant.getInstance().replaceAllImgPathInExportDataFile(rrequest.getRequest(),content);
        }
        if(rrequest.isExportToLocalFile())
        {
            if(overwrite) outBuf=new StringBuilder();
            outBuf.append(content);
            if(outBuf.length()>10240) writeBufDataToLocalFile();
        }else if(jspout!=null)
        {//当前是通过<wx:report/>显示报表，则从jsp中取out进行输出
            try
            {
                jspout.print(content);
            }catch(IOException e)
            {
                log.debug("向页面输出字符串："+content+"时失败",e);
                jspout=null;
                if(out!=null)
                {
                    out.print(content);
                }else
                {
                    if(overwrite) outBuf=new StringBuilder();
                    outBuf.append(content);
                }
            }
        }else if(out!=null)
        {
            out.print(content);
        }else
        {
            if(overwrite) outBuf=new StringBuilder();
            outBuf.append(content);
        }
    }
    
    public void writeBufDataToLocalFile()
    {
        if(!rrequest.isExportToLocalFile()||this.outBuf.length()==0) return;
        BufferedWriter dataExportFileWriter=null;
        try
        {
            dataExportFileWriter=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(rrequest.getDataExportFilepath()),true),Config.encode));
            dataExportFileWriter.write(this.outBuf.toString());
            this.outBuf=new StringBuilder();
        }catch(Exception e)
        {//此异常不可能产生，因为不存在时会自动创建，创建不成功会抛出异常
            throw new WabacusRuntimeException("导出数据文件时，写数据文件"+rrequest.getDataExportFilepath()+"失败",e);
        }finally
        {
            try
            {
                if(dataExportFileWriter!=null) dataExportFileWriter.close();
            }catch(IOException e)
            {
                throw new WabacusRuntimeException("导出数据文件时，关闭数据文件"+rrequest.getDataExportFilepath()+"失败",e);
            }
        }
    }
    
    public void sendRedirect(String url)
    {
        if(url==null||url.trim().equals("")) return;
        if(!rrequest.isLoadedByAjax())
        {
            try
            {
                this.response.sendRedirect(url);
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }else
        {
            this.addOnloadMethod("wx_sendRedirect","{url:\""+url+"\"}",false);
        }
        throw new WabacusRuntimeTerminateException();
    }
    
    public void terminateResponse()
    {
        throw new WabacusRuntimeTerminateException();
    }
    
    public void addChildInputboxIdsToOnload(List<String> lstRealChildids)
    {
        if(Tools.isEmpty(lstRealChildids)) return;
        if(sChildSelectboxidsOnload==null) sChildSelectboxidsOnload=new HashSet<String>();
        this.sChildSelectboxidsOnload.addAll(lstRealChildids);
    }
    
    private String[] getInitChildSelectboxIdsOnloadMethod()
    {
        if(Tools.isEmpty(sChildSelectboxidsOnload)) return null;
        StringBuilder bufTmp=new StringBuilder();
        for(String childidTmp:this.sChildSelectboxidsOnload)
        {
            if(Tools.isEmpty(childidTmp)) continue;
            bufTmp.append(childidTmp).append(";");
        }
        if(bufTmp.length()>0&&bufTmp.charAt(bufTmp.length()-1)==';') bufTmp.deleteCharAt(bufTmp.length()-1);
        return bufTmp.length()>0?new String[] { "wx_showChildSelectboxOptionsOnload", "{childids:'"+bufTmp.toString()+"'}" }:null;
    }
    
    public void addDisplayInputbox(String ownerid,String realInputboxid,Set<String> lstParentIds)
    {
        if(this.mDisplayInputboxsOnload==null) this.mDisplayInputboxsOnload=new HashMap<String,DisplayInputboxBean>();
        DisplayInputboxBean dibean=this.mDisplayInputboxsOnload.get(ownerid);
        if(dibean==null)
        {
            dibean=new DisplayInputboxBean();
            dibean.setLstParentInputboxId(lstParentIds);
            this.mDisplayInputboxsOnload.put(ownerid,dibean);
        }
        List<String> lstInputboxIds=dibean.getLstInputboxIds();
        if(lstInputboxIds==null)
        {
            lstInputboxIds=new ArrayList<String>();
            dibean.setLstInputboxIds(lstInputboxIds);
        }
        if(!lstInputboxIds.contains(realInputboxid)) lstInputboxIds.add(realInputboxid);
    }
    
    private String[] getDisplayInputboxOnloadMethod()
    {
        if(this.mDisplayInputboxsOnload==null||this.mDisplayInputboxsOnload.size()==0) return null;
        Map<String, DisplayInputboxBean> mTmp1=new HashMap<String, DisplayInputboxBean>();
        mTmp1.putAll(this.mDisplayInputboxsOnload);
        Map<String, DisplayInputboxBean> mNonDisplayedInputBox=new HashMap<String, DisplayInputboxBean>();
        DisplayInputboxBean dibTmp;
        StringBuilder bufTmp=new StringBuilder();
        while(mTmp1.size()>0)
        {
            mNonDisplayedInputBox.clear();
            mNonDisplayedInputBox.putAll(mTmp1);
            for(Entry<String,DisplayInputboxBean> entryTmp:mTmp1.entrySet())
            {
                dibTmp=entryTmp.getValue();
                if(isDisplayedAllParentInputbox(mNonDisplayedInputBox,dibTmp.getLstParentInputboxId()))
                {
                    bufTmp.append(dibTmp.getInputBoxIdsAsString());
                    mNonDisplayedInputBox.remove(entryTmp.getKey());
                }
            }
            if(mNonDisplayedInputBox.size()==mTmp1.size()&&mNonDisplayedInputBox.size()>0)
            {
                throw new WabacusRuntimeException("显示输入框失败，输入框存在循环依赖关系");
            }
            mTmp1.clear();
            mTmp1.putAll(mNonDisplayedInputBox);
        }
        if(bufTmp.length()==0) return null;
        return new String[]{"showComboxAddOptionsOnload","{ids:'"+bufTmp.toString()+"'}"};
    }
    
    private boolean isDisplayedAllParentInputbox(Map<String, DisplayInputboxBean> mNonDisplayedInputBox,Set<String> lstParentIds)
    {
        if(mNonDisplayedInputBox==null||Tools.isEmpty(lstParentIds)) return true;//此输入框不依赖任何输入框
        for(String parentidTmp:lstParentIds)
        {
            if(Tools.isEmpty(parentidTmp)) continue;
            if(mNonDisplayedInputBox.containsKey(parentidTmp)) return false;
        }
        return true;
    }
    
    private class DisplayInputboxBean
    {
        private Set<String> lstParentIds;

        private List<String> lstInputboxIds;

        public Set<String> getLstParentInputboxId()
        {
            return lstParentIds;
        }

        public void setLstParentInputboxId(Set<String> lstParentInputboxId)
        {
            this.lstParentIds=lstParentInputboxId;
        }

        public List<String> getLstInputboxIds()
        {
            return lstInputboxIds;
        }

        public void setLstInputboxIds(List<String> lstInputboxIds)
        {
            this.lstInputboxIds=lstInputboxIds;
        }
        
        public String getInputBoxIdsAsString()
        {
            if(Tools.isEmpty(lstInputboxIds)) return "";
            StringBuilder resultBuf=new StringBuilder();
            for(String idTmp:lstInputboxIds)
            {
                if(Tools.isEmpty(idTmp)) continue;
                resultBuf.append(idTmp).append(";");
            }
            return resultBuf.toString();
        }
    }
}
