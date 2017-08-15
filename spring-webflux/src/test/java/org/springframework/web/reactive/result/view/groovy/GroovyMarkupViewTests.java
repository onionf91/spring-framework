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

import java.io.Reader;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;

import static org.junit.Assert.*;


/**
 * @author Jason Yu
 */
public class GroovyMarkupViewTests {

	private static final String RESOURCE_LOADER_PATH = "classpath*:org/springframework/web/reactive/result/view/groovy";

	private final MockServerWebExchange exchange = MockServerHttpRequest.get("/path").toExchange();

	private GenericApplicationContext context;

//	private GroovyMarkupConfig groovyMarkupConfig;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.refresh();

//		GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
//		configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH);
//		this.groovyMarkupConfig = configurer;
	}


	@Test
	public void missingGroovyMarkupConfig() throws Exception {
		this.exception.expect(ApplicationContextException.class);
		this.exception.expectMessage("Expected a single GroovyMarkupConfig bean in");

		GroovyMarkupView view = new GroovyMarkupView();
		view.setApplicationContext(this.context);
		view.setUrl("sampleView");
		view.afterPropertiesSet();
	}

	@Test
	public void customTemplateEngine() throws Exception {
		GroovyMarkupView view = new GroovyMarkupView();
		view.setTemplateEngine(new TestTemplateEngine());
		view.setApplicationContext(this.context);

		DirectFieldAccessor accessor = new DirectFieldAccessor(view);
		TemplateEngine engine = (TemplateEngine)accessor.getPropertyValue("engine");
		assertNotNull(engine);
		assertEquals(TestTemplateEngine.class, engine.getClass());
	}

	public class TestTemplateEngine extends MarkupTemplateEngine {

		public TestTemplateEngine() {
			super(new TemplateConfiguration());
		}

		@Override
		public Template createTemplate(Reader reader) {
			return null;
		}
	}
}
