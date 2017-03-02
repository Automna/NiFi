/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.automna;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.nifi.annotation.behavior.DynamicProperty;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.AttributeExpression;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.stream.io.BufferedInputStream;
import org.apache.nifi.util.StopWatch;
import org.apache.nifi.util.Tuple;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@EventDriven
@SideEffectFree
@SupportsBatching
@Tags({"parser", "automna"})
@InputRequirement(Requirement.INPUT_REQUIRED)
@CapabilityDescription("Converts PM Files from an Equipment Vendor into Automna friendly format. "
        + "Transformed content is routed to the 'success' relationship after successful conversion. If the transform "
        + "fails, the original FlowFile is routed to the 'failure' relationship")
@DynamicProperty(name = "An XSLT transform parameter name", value = "An XSLT transform parameter value", supportsExpressionLanguage = true,
        description = "These XSLT parameters are passed to the transformer")
public class Parser extends AbstractProcessor {
	
	public static final Boolean INDENT_OUTPUT = true;
	public static final Integer CACHE_SIZE = 10;
	public static final Long CACHE_TTL_AFTER_LAST_ACCESS = 60L; // in seconds
	
    public static final PropertyDescriptor VENDOR = new PropertyDescriptor.Builder()
            .name("Vendor")
            .description("The Equipment Vendor for the file being parsed")
            .required(true)
            .expressionLanguageSupported(false)
            .allowableValues("ERICSSON", "NOKIA")
            .build();

    public static final PropertyDescriptor FORMAT = new PropertyDescriptor.Builder()
            .name("Format")
            .displayName("File Format")
            .description("The format of the source file.")
            .required(true)
            .allowableValues("XML", "CSV")
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("The FlowFile with transformed content will be routed to this relationship")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("If a FlowFile fails processing for any reason (for example, the FlowFile is not valid XML), it will be routed to this relationship")
            .build();

    private List<PropertyDescriptor> properties;
    private Set<Relationship> relationships;
    private LoadingCache<String, Templates> cache;
    
    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(VENDOR);
        properties.add(FORMAT);
        this.properties = Collections.unmodifiableList(properties);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {
        return new PropertyDescriptor.Builder()
                .name(propertyDescriptorName)
                .expressionLanguageSupported(true)
                .addValidator(StandardValidators.createAttributeExpressionLanguageValidator(AttributeExpression.ResultType.STRING, true))
                .required(false)
                .dynamic(true)
                .build();
    }

    private Templates newTemplates(String path) throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        return factory.newTemplates(new StreamSource(path));
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        final ComponentLog logger = getLogger();

        if (CACHE_SIZE > 0) {
            CacheBuilder cacheBuilder = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE);
            if (CACHE_TTL_AFTER_LAST_ACCESS > 0) {
                cacheBuilder = cacheBuilder.expireAfterAccess(CACHE_TTL_AFTER_LAST_ACCESS, TimeUnit.SECONDS);
            }

            cache = cacheBuilder.build(
               new CacheLoader<String, Templates>() {
                   public Templates load(String path) throws TransformerConfigurationException {
                       return newTemplates(path);
                   }
               });
        } else {
            cache = null;
            logger.warn("Stylesheet cache disabled because cache size is set to 0");
        }
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        final FlowFile original = session.get();
        if (original == null) {
            return;
        }

        final ComponentLog logger = getLogger();
        final StopWatch stopWatch = new StopWatch(true);
        //final String xsltFileName = context.getProperty(XSLT_FILE_NAME)
        //.evaluateAttributeExpressions(original)
        //.getValue();
        
        final String xsltVendor = context.getProperty(VENDOR)
                .evaluateAttributeExpressions(original)
                .getValue();
        final String xsltFileType = context.getProperty(FORMAT)
                    .evaluateAttributeExpressions(original)
                    .getValue();
        String xsltFileName = "";
        logger.info(xsltVendor + xsltFileType);
        if (xsltVendor == "ERICSSON" && xsltFileType == "XML")
        {
        	xsltFileName = "Automna/ericsson_xml.xslt";
        	logger.info(xsltFileName);
        } 
        else if (xsltVendor == "NOKIA" && xsltFileType == "XML")
    	{
        	xsltFileName = "Automna/nokia_xml.xslt";
        	logger.info(xsltFileName);
    	}
        else
    	{
        	xsltFileName = "Automna/ericsson_xml.xslt";
        	logger.info("AUTOMNA: Default vendor/format applied");
    	}
        
        try {
            FlowFile transformed = session.write(original, new StreamCallback() {
                @Override
                public void process(final InputStream rawIn, final OutputStream out) throws IOException {
                    try (final InputStream in = new BufferedInputStream(rawIn)) {
                    	
                        final Templates templates;
                        if (cache != null) {
                            templates = cache.get(xsltFileName);
                        } else {
                            templates = newTemplates(xsltFileName);
                        }

                        final Transformer transformer = templates.newTransformer();
                        transformer.setOutputProperty(OutputKeys.INDENT, (INDENT_OUTPUT ? "yes" : "no"));

                        // pass all dynamic properties to the transformer
                        for (final Map.Entry<PropertyDescriptor, String> entry : context.getProperties().entrySet()) {
                            if (entry.getKey().isDynamic()) {
                                String value = context.newPropertyValue(entry.getValue()).evaluateAttributeExpressions(original).getValue();
                                transformer.setParameter(entry.getKey().getName(), value);
                            }
                        }

                        // use a StreamSource with Saxon
                        StreamSource source = new StreamSource(in);
                        StreamResult result = new StreamResult(out);
                        transformer.transform(source, result);
                    } catch (final Exception e) {
                        throw new IOException(e);
                    }
                }
            });
            session.transfer(transformed, REL_SUCCESS);
            session.getProvenanceReporter().modifyContent(transformed, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            logger.info("Transformed {}", new Object[]{original});
        } catch (ProcessException e) {
            logger.error("Unable to transform {} due to {}", new Object[]{original, e});
            session.transfer(original, REL_FAILURE);
        }
    }
    
    @SuppressWarnings("unused")
    private static final class XsltValidator implements Validator {

        private volatile Tuple<String, ValidationResult> cachedResult;

        @Override
        public ValidationResult validate(final String subject, final String input, final ValidationContext validationContext) {
            final Tuple<String, ValidationResult> lastResult = this.cachedResult;
            if (lastResult != null && lastResult.getKey().equals(input)) {
                return lastResult.getValue();
            } else {
                String error = null;
                final File stylesheet = new File(input);
                final TransformerFactory tFactory = new net.sf.saxon.TransformerFactoryImpl();
                final StreamSource styleSource = new StreamSource(stylesheet);

                try {
                    tFactory.newTransformer(styleSource);
                } catch (final Exception e) {
                    error = e.toString();
                }

                this.cachedResult = new Tuple<>(input, new ValidationResult.Builder()
                        .input(input)
                        .subject(subject)
                        .valid(error == null)
                        .explanation(error)
                        .build());
                return this.cachedResult.getValue();
            }
        }
    }

}
