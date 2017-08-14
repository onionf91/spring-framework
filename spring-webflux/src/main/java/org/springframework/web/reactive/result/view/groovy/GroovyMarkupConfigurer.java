/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.view.groovy;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import groovy.text.markup.TemplateResolver;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 *
 * @author Jason Yu
 * @since 5.0
 */
public class GroovyMarkupConfigurer extends TemplateConfiguration
		implements GroovyMarkupConfig, ApplicationContextAware, InitializingBean {

	private String resourceLoaderPath = "classpath:";

	@Nullable
	private MarkupTemplateEngine templateEngine;

	@Nullable
	private ApplicationContext applicationContext;

	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	public java.lang.String getResourceLoaderPath() {
		return resourceLoaderPath;
	}

	public void setTemplateEngine(MarkupTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	@Override
	public MarkupTemplateEngine getTemplateEngine() {
		Assert.state(this.templateEngine != null, "No MarkupTemplateEngine set");
		return this.templateEngine;
	}

	protected ApplicationContext getApplicationContext() {
		Assert.state(this.applicationContext != null, "No ApplicationContext set");
		return this.applicationContext;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.templateEngine == null) {
			this.templateEngine = createTemplateEngine();
		}
	}

	protected MarkupTemplateEngine createTemplateEngine() throws IOException {
		if (this.templateEngine == null) {
			ClassLoader templateClassLoader = createTemplateClassLoader();
			this.templateEngine = new MarkupTemplateEngine(templateClassLoader, this, new LocaleTemplateResolver());
		}
		return this.templateEngine;
	}

	protected ClassLoader createTemplateClassLoader() throws IOException {
		String[] paths = StringUtils.commaDelimitedListToStringArray(getResourceLoaderPath());
		List<URL> urls = new ArrayList<>();
		for (String path: paths) {
			Resource[] resources = getApplicationContext().getResources(path);
			if (resources.length > 0) {
				for (Resource resource : resources) {
					if (resource.exists()) {
						urls.add(resource.getURL());
					}
				}
			}
		}
		ClassLoader classLoader = getApplicationContext().getClassLoader();
		Assert.state(classLoader != null, "No ClassLoader");
		return (urls.size() > 0 ? new URLClassLoader(urls.toArray(new URL[urls.size()]), classLoader) : classLoader);
	}

	protected URL resolveTemplate(ClassLoader classLoader, String templatePath) throws IOException {
		MarkupTemplateEngine.TemplateResource resource = MarkupTemplateEngine.TemplateResource.parse(templatePath);
		Locale locale = LocaleContextHolder.getLocale();
		URL url = classLoader.getResource(resource.withLocale(locale.toString().replace("-", "_")).toString());
		if (url == null) {
			url = classLoader.getResource(resource.withLocale(locale.getLanguage()).toString());
		}
		if (url == null) {
			url = classLoader.getResource(resource.withLocale(null).toString());
		}
		if (url == null) {
			throw new IOException("Unable to load template:" + templatePath);
		}
		return url;
	}

	private class LocaleTemplateResolver implements TemplateResolver {

		@Nullable
		private ClassLoader classLoader;

		@Override
		public void configure(ClassLoader templateClassLoader, TemplateConfiguration configuration) {
			this.classLoader = templateClassLoader;
		}

		@Override
		public URL resolveTemplate(String templatePath) throws IOException {
			Assert.state(this.classLoader != null, "No template Classloader available");
			return GroovyMarkupConfigurer.this.resolveTemplate(this.classLoader, templatePath);
		}
	}
}
