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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.server.ServerWebExchange;

/**
 *
 * @author Jason Yu
 * @since 5.0
 */
public class GroovyMarkupView extends AbstractUrlBasedView {

	@Nullable
	MarkupTemplateEngine engine;

	public void setTemplateEngine(MarkupTemplateEngine templateEngine) {
		this.engine = templateEngine;
	}

	protected MarkupTemplateEngine autodetectMarkupTemplateEngine() throws BeansException {
		try {
			return BeanFactoryUtils
					.beanOfTypeIncludingAncestors(
							obtainApplicationContext(),
							GroovyMarkupConfig.class, true, false)
					.getTemplateEngine();
		} catch (NoSuchBeanDefinitionException e) {
			throw new ApplicationContextException("Expected a single GroovyMarkupConfig bean in the current " +
					"Servlet web application context or the parent root context: GroovyMarkupConfigurer is " +
					"the usual implementation. This bean may have any name.", e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (this.engine == null) {
			setTemplateEngine(autodetectMarkupTemplateEngine());
		}
	}

	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		Assert.state(this.engine != null, "No MarkupTemplateEngine set");
		try {
			this.engine.resolveTemplate(getUrl());
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	@Override
	protected Mono<Void> renderInternal(
			Map<String, Object> renderAttributes,
			@Nullable MediaType contentType,
			ServerWebExchange exchange) {

		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		Assert.state(this.engine != null, "No MarkupTemplateEngine set");

		DataBuffer dataBuffer = exchange.getResponse().bufferFactory().allocateBuffer();
		try {
			Template template = this.engine.createTemplateByPath(url);
			Charset charset = getCharset(contentType);
			Writer writer = new OutputStreamWriter(dataBuffer.asOutputStream(), charset);
			template.make(renderAttributes).writeTo(new BufferedWriter(writer));
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}
		return exchange.getResponse().writeWith(Flux.just(dataBuffer));
	}

	private Charset getCharset(@Nullable MediaType mediaType) {
		return Optional.ofNullable(mediaType).map(MimeType::getCharset).orElse(getDefaultCharset());
	}

}
