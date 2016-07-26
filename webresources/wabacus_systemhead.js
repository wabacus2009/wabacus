/**
 * 是否已加载完wabacus_system.js文件
 */
function isLoadedSystemJs()
{
	if(typeof WX_ISSYSTEM_LOADED=='undefined'){ return false;}
	return true;
}

/**
 * 是否已加载完wabacus_api.js文件
 */
function isLoadedApiJs()
{
	if(typeof WX_API_LOADED=='undefined') return false;
	return true;
}

/**
 * 是否已加载完wabacus_editsystem.js文件
 */
function isLoadedEditSystemJs()
{
	if(typeof WX_EDITSYSTEM_LOADED=='undefined') return false;
	return true;
}

/**
 * 是否已加载完wabacus_typeprompt.js文件
 */
function isLoadedTypepromptJs()
{
	if(typeof WX_TYPEPROMPT_LOADED=='undefined') return false;
	return true;
}

/**
 * 是否已加载完wabacus_util.js文件
 */
function isLoadedUtilJs()
{
	if(typeof WX_UTIL_LOADED=='undefined') return false;
	return true;
}

function isLoadedToolsJs()
{
	if(typeof WX_TOOLS_LOADED=='undefined') return false;
	return true;
}

/**
 * 是否加载完所有内置的js
 */
function isLoadedAllBuiltinJs()
{
	return isLoadedSystemJs()&&isLoadedApiJs()&&isLoadedEditSystemJs()&&isLoadedTypepromptJs()&&isLoadedUtilJs()&&isLoadedToolsJs();
}

function logErrorsAsJsFileLoad(error)
{
	if(error==null) return;
	if(error instanceof TypeError||error instanceof ReferenceError)
	{//可能是因为使用了没有定义的方法，或没有定义的变量，
		//alert(error);
		if(!isLoadedSystemJs()||!isLoadedUtilJs()||!isLoadedApiJs()||!isLoadedToolsJs())
		{//如果没有加载wabacus_system.js、wabacus_util.js、wabacus_api.js，则可能是JS文件没加载完所致
			//alert(error+' true');
			wabacus_log(error+'，可能是页面还没有加载完时进行的操作，不用处理');
		}else
		{
			throw error;
		}
	}else
	{//其它类型的异常，则抛出
		throw error;
	}
}

/**
 * 向浏览器控制台打印log日志
 */
function wabacus_log(message)
{
	if (typeof console == 'object')
	{
		console.log(message);
	} else if (typeof opera == 'object')
	{
		opera.postError(message);
	} else if (typeof java == 'object' && typeof java.lang == 'object')
	{
		java.lang.System.out.println(message);
	}
}

/**
 * 向浏览器控制台打印info日志
 */
function wabacus_info(message)
{
	if (typeof console == 'object')
	{
		console.info(message);
	} else if (typeof opera == 'object')
	{
		opera.postError(message);
	} else if (typeof java == 'object' && typeof java.lang == 'object')
	{
		java.lang.System.out.println(message);
	}
}

/**
 * 向浏览器控制台打印warn日志
 */
function wabacus_warn(message)
{
	if (typeof console == 'object')
	{
		console.warn(message);
	} else if (typeof opera == 'object')
	{
		opera.postError(message);
	} else if (typeof java == 'object' && typeof java.lang == 'object')
	{
		java.lang.System.out.println(message);
	}
}

/**
 * 向浏览器控制台打印error日志
 */
function wabacus_error(message)
{
	if (typeof console == 'object')
	{
		console.error(message);
	} else if (typeof opera == 'object')
	{
		opera.postError(message);
	} else if (typeof java == 'object' && typeof java.lang == 'object')
	{
		java.lang.System.out.println(message);
	}
}
