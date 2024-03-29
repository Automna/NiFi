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
@Tags({"xml", "csv", "parser", "ericsson", "automna"})
@InputRequirement(Requirement.INPUT_REQUIRED)
@CapabilityDescription("Takes Performance Management (PM) data files and converts them to a format easy to "
        + "use with Automna Analytics or store in a database. Original flow file is sent to 'original', "
        + "whilst the converted data is sent to 'converted'. Flowfiles that cannot be converted are send to 'failed'.")
@DynamicProperty(name = "An XSLT transform parameter name", value = "An XSLT transform parameter value", supportsExpressionLanguage = true,
        description = "These XSLT parameters are passed to the transformer")
public class FileValidator extends AbstractProcessor {

    public static final PropertyDescriptor VENDOR_NAME = new PropertyDescriptor.Builder()
            .name("Vendor Name")
            .description("Define the Vendor which generates the file (Ericsson, Nokia, etc.)")
            .required(true)
            .expressionLanguageSupported(false)
            .allowableValues("Ericsson", "Nokia")
            .build();

    public static final PropertyDescriptor FILE_TYPE = new PropertyDescriptor.Builder()
            .name("File type")
            .description("The format of the supplied file (CSV, XML)")
            .required(true)
            .defaultValue("XML")
            .allowableValues("CSV", "XML")
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("converted")
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
        properties.add(VENDOR_NAME);
        properties.add(FILE_TYPE);
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
        //final Integer cacheSize = context.getProperty(CACHE_SIZE).asInteger();
        //final Long cacheTTL = context.getProperty(CACHE_TTL_AFTER_LAST_ACCESS).asTimePeriod(TimeUnit.SECONDS);

        //if (cacheSize > 0) {
        //    CacheBuilder cacheBuilder = CacheBuilder.newBuilder().maximumSize(cacheSize);
        //    if (cacheTTL > 0) {
        //        cacheBuilder = cacheBuilder.expireAfterAccess(cacheTTL, TimeUnit.SECONDS);
        //    }

        //    cache = cacheBuilder.build(
        //       new CacheLoader<String, Templates>() {
        //           public Templates load(String path) throws TransformerConfigurationException {
        //               return newTemplates(path);
        //           }
        //       });
        //} else {
        //    cache = null;
            logger.warn("AUTOMNA: OnScheduled event called for FileParser Processor. ");
        //}
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        final FlowFile original = session.get();
        if (original == null) {
            return;
        }

        final ComponentLog logger = getLogger();
        final StopWatch stopWatch = new StopWatch(true);
        final String xsltFileName = context.getProperty(VENDOR_NAME)
            .evaluateAttributeExpressions(original)
            .getValue();
        final String xsltFileExtension = context.getProperty(FILE_TYPE)
                .evaluateAttributeExpressions(original)
                .getValue();
        final String xsltFile = xsltFileName + "." + xsltFileExtension; 
        //final Boolean indentOutput = context.getProperty(INDENT_OUTPUT).asBoolean();

        try {
            FlowFile transformed = session.write(original, new StreamCallback() {
                @Override
                public void process(final InputStream rawIn, final OutputStream out) throws IOException {
                    try (final InputStream in = new BufferedInputStream(rawIn)) {
                        final Templates templates;
                        if (cache != null) {
                            templates = cache.get(xsltFile);
                        } else {
                            templates = newTemplates(xsltFile);
                        }

                        final Transformer transformer = templates.newTransformer();
                        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                        //pass all dynamic properties to the transformer
                        //for (final Map.Entry<PropertyDescriptor, String> entry : context.getProperties().entrySet()) {
                        //    if (entry.getKey().isDynamic()) {
                        //        String value = context.newPropertyValue(entry.getValue()).evaluateAttributeExpressions(original).getValue();
                        //        transformer.setParameter(entry.getKey().getName(), value);
                        //    }
                        //}

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