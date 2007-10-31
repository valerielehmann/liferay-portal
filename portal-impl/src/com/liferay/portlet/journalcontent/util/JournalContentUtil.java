/**
 * Copyright (c) 2000-2007 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet.journalcontent.util;

import com.liferay.portal.kernel.cache.MultiVMPoolUtil;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringMaker;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.journal.model.JournalArticleDisplay;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.util.CollectionFactory;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="JournalContentUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @author Raymond Augé
 * @author Michael Young
 *
 */
public class JournalContentUtil {

	public static final String CACHE_NAME = JournalContentUtil.class.getName();

	public static String ARTICLE_SEPARATOR = "_ARTICLE_";

	public static String TEMPLATE_SEPARATOR = "_TEMPLATE_";

	public static String LANGUAGE_SEPARATOR = "_LANGUAGE_";

	public static String PAGE_SEPARATOR = "_PAGE_";

	public static void clearCache() {
		_cache.removeAll();
	}

	public static void clearCache(
		long groupId, String articleId, String templateId) {

		articleId = GetterUtil.getString(articleId).toUpperCase();
		templateId = GetterUtil.getString(templateId).toUpperCase();

		String groupKey = _encodeGroupKey(groupId, articleId, templateId);

		MultiVMPoolUtil.clearGroup(_groups, groupKey, _cache);
	}

	public static String getContent(
		long groupId, String articleId, String languageId,
		ThemeDisplay themeDisplay) {

		return getContent(groupId, articleId, null, languageId, themeDisplay);
	}

	public static String getContent(
		long groupId, String articleId, String templateId, String languageId,
		ThemeDisplay themeDisplay) {

		return getContent(
			groupId, articleId, null, languageId, themeDisplay, false);
	}

	public static String getContent(
		long groupId, String articleId, String templateId, String languageId,
		ThemeDisplay themeDisplay, boolean disableCaching) {

		JournalArticleDisplay articleDisplay = getDisplay(
			groupId, articleId, templateId, languageId, themeDisplay,
			disableCaching);

		if (articleDisplay != null) {
			return articleDisplay.getContent();
		}
		else {
			return null;
		}
	}

	public static JournalArticleDisplay getDisplay(
		long groupId, String articleId, String templateId, String languageId,
		ThemeDisplay themeDisplay, boolean disableCaching) {

		return getDisplay(
			groupId, articleId, templateId, languageId, themeDisplay,
			disableCaching, 1, null);
	}

	public static JournalArticleDisplay getDisplay(
		long groupId, String articleId, String templateId, String languageId,
		ThemeDisplay themeDisplay, boolean disableCaching, int page, 
		String xmlRequest) {

		long start = System.currentTimeMillis();
		
		articleId = GetterUtil.getString(articleId).toUpperCase();
		templateId = GetterUtil.getString(templateId).toUpperCase();

		if (disableCaching) {
			return _getArticleDisplay(
				groupId, articleId, templateId, languageId, themeDisplay,
				page, xmlRequest);
		}

		String key = 
			_encodeKey(groupId, articleId, templateId, languageId, page);

		JournalArticleDisplay articleDisplay =
			(JournalArticleDisplay)MultiVMPoolUtil.get(_cache, key);

		if (articleDisplay == null) {
			articleDisplay = _getArticleDisplay(
				groupId, articleId, templateId, languageId, themeDisplay,
				page, xmlRequest);

			if (articleDisplay != null) {
				String groupKey = _encodeGroupKey(
					groupId, articleId, templateId);

				MultiVMPoolUtil.put(_cache, key, _groups, groupKey, articleDisplay);
			}
		}

		_log.debug("[" + articleId + "," + groupId + "," + page + "] " + (System.currentTimeMillis() - start));

		return articleDisplay;
	}

	private static String _encodeGroupKey(
		long groupId, String articleId, String templateId) {

		return _encodeKey(groupId, articleId, templateId, null, 0);
	}

	private static String _encodeKey(
		long groupId, String articleId, String templateId, String languageId, 
		int page) {

		StringMaker sm = new StringMaker();

		sm.append(CACHE_NAME);
		sm.append(StringPool.POUND);
		sm.append(groupId);
		sm.append(ARTICLE_SEPARATOR);
		sm.append(articleId);
		sm.append(TEMPLATE_SEPARATOR);
		sm.append(templateId);

		if (Validator.isNotNull(languageId)) {
			sm.append(LANGUAGE_SEPARATOR);
			sm.append(languageId);
		}
		
		if (page > 0) {
			sm.append(PAGE_SEPARATOR);
			sm.append(page);
		}

		return sm.toString();
	}

	private static JournalArticleDisplay _getArticleDisplay(
		long groupId, String articleId, String templateId, String languageId,
		ThemeDisplay themeDisplay, int page, String xmlRequest) {

		try {
			return JournalArticleLocalServiceUtil.getArticleDisplay(
				groupId, articleId, templateId, languageId, page, themeDisplay,
				xmlRequest);
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to get display for " + groupId + " " +
						articleId + " " + languageId);
			}

			return null;
		}
	}

	private static Log _log = LogFactory.getLog(JournalContentUtil.class);

	private static PortalCache _cache = MultiVMPoolUtil.getCache(CACHE_NAME);

	private static Map _groups = CollectionFactory.getSyncHashMap();

}