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
package com.wabacus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.WabacusAssistant;


public class DesEncryptTools
{
    private static Log log=LogFactory.getLog(DesEncryptTools.class);

    private static final String Algorithm="DESede"; 

    public static SecretKey KEY_OBJ=null;

    public static boolean IS_NEWKEY;

    public static String encrypt(String originalString)
    {
        if(originalString==null||originalString.trim().equals("")) return "";
        if(KEY_OBJ==null)
        {
            log.warn("没有加载密钥对象，无法加密");
            return originalString;
        }
        try
        {
            Cipher c1=Cipher.getInstance(Algorithm);
            c1.init(Cipher.ENCRYPT_MODE,KEY_OBJ);
            return base64Encode(c1.doFinal(originalString.getBytes()));
        }catch(Exception e)
        {
            log.error("加密字符串"+originalString+"失败",e);
            return null;
        }
    }

    public static String decrypt(String encryptedString)
    {
        try
        {
            if(KEY_OBJ==null)
            {
                log.warn("没有加载密钥，无法解密字符串："+encryptedString);
                return encryptedString;
            }
            byte[] b=base64Decode(encryptedString);
            Cipher c1=Cipher.getInstance(Algorithm);
            c1.init(Cipher.DECRYPT_MODE,KEY_OBJ);
            return new String(c1.doFinal(b));
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("解密字符串"+encryptedString+"失败",e);
        }
    }

    private static String base64Encode(byte[] b)
    {
        if(b==null) return null;

        try
        {
            return Base64.encodeBase64String(b);
        }catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
        //        try
        //            return null;
    }

    private static byte[] base64Decode(String s)
    {
        if(s==null) return null;
        try
        {
            return new Base64().decode(s);
        }catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }

        //        {
        //        }
    }

    public static void initEncryptKey() throws IOException
    {
        KEY_OBJ=null;
        IS_NEWKEY=false;
        String filepath=Config.getInstance().getSystemConfigValue("3des-keyfilepath","");
        if(filepath.equals(""))
        {
            log.warn("没有在wabacus.cfg.xml中通过配置项3des-keyfilepath配置3des密钥存放路径");
            return;
        }
        InputStream istream=null;
        File file=null;
        ObjectInputStream ois=null;
        try
        {
            if(Tools.isDefineKey("classpath",filepath))
            {
                filepath=Tools.getRealKeyByDefine("classpath",filepath);
                filepath=Tools.replaceAll(filepath,"\\","/");
                filepath=Tools.replaceAll(filepath,"//","/");
                while(filepath.startsWith("/"))
                    filepath=filepath.substring(1);
                istream=ConfigLoadManager.currentDynClassLoader.getResourceAsStream(filepath);
                if(istream==null)
                {
                    int idx=filepath.lastIndexOf("/");
                    String filename=null;
                    if(idx>0)
                    {
                        filename=filepath.substring(idx+1);
                        filepath=filepath.substring(0,idx);
                    }else
                    {
                        filename=filepath;
                        filepath="";
                    }
                    file=WabacusAssistant.getInstance().getFileObjByPathInClasspath(filepath,filename);
                }
            }else
            {
                file=new File(WabacusAssistant.getInstance().parseConfigPathToRealPath(filepath,Config.webroot_abspath));
                if(file.exists())
                {
                    istream=new FileInputStream(file);
                }
            }
            if(istream==null&&file!=null)
            {
                log.info("正在路径"+file.getPath()+"下创建密钥文件");
                createEncryptKey(file);
                istream=new FileInputStream(file);
                IS_NEWKEY=true;
            }
            ois=new ObjectInputStream(istream);
            KEY_OBJ=(SecretKey)ois.readObject();
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("初始化3DES密钥失败",e);
        }finally
        {
            try
            {
                if(istream!=null) istream.close();
            }catch(IOException e)
            {
                e.printStackTrace();
            }
            try
            {
                if(ois!=null) ois.close();
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static boolean createEncryptKey(File file)
    {
        SecretKey deskey=null;
        try
        {
            KeyGenerator keygen=KeyGenerator.getInstance("DESede");
            deskey=keygen.generateKey();
        }catch(NoSuchAlgorithmException e)
        {
            throw new WabacusConfigLoadingException("生成密钥失败",e);
        }
        if(deskey==null) return false;
        FileOutputStream fos=null;
        ObjectOutputStream oos=null;
        try
        {
            fos=new FileOutputStream(file);
            oos=new ObjectOutputStream(fos);
            oos.writeObject(deskey);
        }catch(FileNotFoundException e)
        {
            throw new WabacusConfigLoadingException("生成密钥失败，无法创建密钥文件"+file.getPath(),e);
        }catch(IOException e)
        {
            throw new WabacusConfigLoadingException("生成密钥失败，将密钥写入文件"+file.getPath()+"失败",e);
        }finally
        {
            try
            {
                if(fos!=null) fos.close();
            }catch(IOException e)
            {
                e.printStackTrace();
                return false;
            }
            try
            {
                if(oos!=null) oos.close();
            }catch(IOException e)
            {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args)
    {
        Config.webroot_abspath="D:\\eclipse\\workspace\\Wabacus\\";
        ConfigLoadManager.currentDynClassLoader=new WabacusClassLoader(DesEncryptTools.class.getClassLoader());
        //        DesEncryptTools.loadEncryptKey("classpath{/reportconfig/3des2.xml}");
        String str=DesEncryptTools.encrypt("This is 测试");
        System.out.println("密文："+str);
        str=DesEncryptTools.decrypt(str);
        System.out.println("明文："+str);
    }
}
