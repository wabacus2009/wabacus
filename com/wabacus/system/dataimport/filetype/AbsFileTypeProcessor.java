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
package com.wabacus.system.dataimport.filetype;

import java.io.File;
import java.util.List;

import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;

public abstract class AbsFileTypeProcessor
{
    protected AbsDataImportConfigBean configBean;

    protected int startrecordindex;

    protected int recordcount;

    public AbsFileTypeProcessor(AbsDataImportConfigBean configBean)
    {
        this.configBean=configBean;
    }

    public AbsDataImportConfigBean getConfigBean()
    {
        return configBean;
    }

    public int getStartrecordindex()
    {
        return startrecordindex;
    }

    public int getRecordcount()
    {
        return recordcount;
    }

    public abstract void init(File datafile);

    public abstract List<String> getLstColnameData();
    
    public abstract List getRowData(int rowidx);

    public abstract boolean isEmpty();

    public abstract void destroy();
}
