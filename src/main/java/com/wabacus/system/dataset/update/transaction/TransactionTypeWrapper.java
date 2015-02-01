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
package com.wabacus.system.dataset.update.transaction;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.system.ReportRequest;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;

public class TransactionTypeWrapper
{
    private static Log log=LogFactory.getLog(TransactionTypeWrapper.class);
    
    private ITransactionType transObj;

    private boolean isStartTrans;

    private boolean isCommited;

    private boolean isRollback;

    public TransactionTypeWrapper(ITransactionType transObj)
    {
        this.transObj=transObj;
    }
    
    public boolean isStartTrans()
    {
        return isStartTrans;
    }

    public boolean isCommited()
    {
        return isCommited;
    }

    public boolean isRollback()
    {
        return isRollback;
    }

    public void beginTransaction(ReportRequest rrequest,List<AbsUpdateAction> lstAllUpdateActions)
    {
        if(isStartTrans) return;
        isStartTrans=true;
        isCommited=false;
        isRollback=false;
        transObj.beginTransaction(rrequest,lstAllUpdateActions);
    }

    public void commitTransaction(ReportRequest rrequest,List<AbsUpdateAction> lstAllUpdateActions)
    {
        if(!isStartTrans)
        {
            log.warn("不能提交页面"+rrequest.getPagebean().getId()+"上保存操作，因为还没有启动事务");
            return;
        }
        if(isCommited||isRollback) return;
        isCommited=true;
        isStartTrans=false;
        transObj.commitTransaction(rrequest,lstAllUpdateActions);
    }

    public void rollbackTransaction(ReportRequest rrequest,List<AbsUpdateAction> lstAllUpdateActions)
    {
        if(!isStartTrans)
        {
            log.warn("不能回滚页面"+rrequest.getPagebean().getId()+"上保存操作，因为还没有启动事务");
            return;
        }
        if(isRollback||isCommited) return;
        isRollback=true;
        isStartTrans=false;
        transObj.rollbackTransaction(rrequest,lstAllUpdateActions);
    }
}

