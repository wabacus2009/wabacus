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

import com.wabacus.util.Consts_Private;

public class DataImportAssistant
{
    private final static DataImportAssistant instance=new DataImportAssistant();
    
    private DataImportAssistant()
    {}
    
    public static DataImportAssistant getInstance()
    {
        return instance;
    }
    
    public String[] getRealFileNameAndImportType(String filename)
    {
        String[] resultsArr=new String[2];
        String realFileName=filename;
        String dynimporttypeTmp=null;
        if(filename.startsWith("["+Consts_Private.DATAIMPORTTYPE_APPEND+"]"))
        {
            realFileName=filename
                    .substring(("["+Consts_Private.DATAIMPORTTYPE_APPEND+"]").length()).trim();
            dynimporttypeTmp=Consts_Private.DATAIMPORTTYPE_APPEND;
        }else if(filename.startsWith("["+Consts_Private.DATAIMPORTTYPE_OVERWRITE+"]"))
        {
            realFileName=filename.substring(
                    ("["+Consts_Private.DATAIMPORTTYPE_OVERWRITE+"]").length()).trim();
            dynimporttypeTmp=Consts_Private.DATAIMPORTTYPE_OVERWRITE;
        }else if(filename.startsWith("["+Consts_Private.DATAIMPORTTYPE_DELETE+"]"))
        {
            realFileName=filename
                    .substring(("["+Consts_Private.DATAIMPORTTYPE_DELETE+"]").length()).trim();
            dynimporttypeTmp=Consts_Private.DATAIMPORTTYPE_DELETE;
        }
        resultsArr[0]=realFileName;
        resultsArr[1]=dynimporttypeTmp;
        return resultsArr;
    }
}

