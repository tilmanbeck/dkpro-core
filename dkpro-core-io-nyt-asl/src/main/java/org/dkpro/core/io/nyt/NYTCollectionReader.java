/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dkpro.core.io.nyt;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.dkpro.core.io.nyt.metadata.NYTArticleMetaData;

import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.MimeTypes;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Reader for New York Times articles from NITF files.
 */
@ResourceMetaData(name = "NITF Reader")
@DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({MimeTypes.APPLICATION_X_NITF_XML})
@TypeCapability(
        outputs = { 
                "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
                "org.dkpro.core.io.nyt.metadata.NYTArticleMetaData" })
public class NYTCollectionReader
    extends JCasResourceCollectionReader_ImplBase
{
    /**
     * A number of documents which will be skipped at the beginning.
     */
    public static final String PARAM_OFFSET = "offset";
    @ConfigurationParameter(name = PARAM_OFFSET, mandatory = false)
    private int offset = 0;

    /**
     * Counting variable to keep track of the already skipped documents.
     */
    private int skipped = 0;

    private NYTCorpusDocumentParser nytParser = new NYTCorpusDocumentParser();

    private void setDocumenText(JCas aJCas, String documentBody)
    {
        if (documentBody != null) {
            aJCas.setDocumentText(documentBody);
        }
        else {
            aJCas.setDocumentText("");
        }
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {

        while (isBelowOffset()) {
            nextFile();
            skipped++;
        }

        Resource xmlFile = nextFile();
        initCas(aJCas, xmlFile);
        NYTCorpusDocument nytDocument = nytParser
                .parseNYTCorpusDocumentFromFile(xmlFile.getInputStream(), false);
        setDocumenText(aJCas, nytDocument.getBody());
        NYTArticleMetaData articleMetaData = createNYTArticleMetaData(aJCas, nytDocument);
        articleMetaData.addToIndexes();
    }

    private boolean isBelowOffset()
    {
        return skipped < offset && getResourceIterator().hasNext();
    }

    private static StringArray toStringArray(List<String> stringList, JCas aJCas)
    {
        if (!stringList.isEmpty()) {
            String[] strings = stringList.toArray(new String[0]);
            int length = strings.length;
            StringArray stringArray = new StringArray(aJCas, length);
            stringArray.copyFromArray(strings, 0, 0, length);
            return stringArray;
        }
        else {
            return new StringArray(aJCas, 0);
        }
    }

    private NYTArticleMetaData createNYTArticleMetaData(JCas aJCas, NYTCorpusDocument doc)
    {
        NYTArticleMetaData articleMetaData = new NYTArticleMetaData(aJCas);
        articleMetaData.setGuid(doc.getGuid());

        URL alternateUrl = doc.getAlternateURL();
        if (alternateUrl != null) {
            articleMetaData.setAlternateUrl(alternateUrl.toString());
        }

        URL url = doc.getUrl();
        if (url != null) {
            articleMetaData.setAlternateUrl(url.toString());
        }

        articleMetaData.setAuthor(doc.getNormalizedByline());
        articleMetaData.setColumnName(doc.getColumnName());
        articleMetaData.setDescriptors(toStringArray(doc.getDescriptors(), aJCas));
        articleMetaData.setHeadline(doc.getHeadline());
        articleMetaData.setOnlineDescriptors(toStringArray(doc.getOnlineDescriptors(), aJCas));
        articleMetaData.setOnlineHeadline(doc.getOnlineHeadline());
        articleMetaData.setOnlineSection(doc.getOnlineSection());
        articleMetaData.setPublicationDate(doc.getPublicationDate().toString());
        articleMetaData.setSection(doc.getSection());
        articleMetaData
                .setTaxonomicClassifiers(toStringArray(doc.getTaxonomicClassifiers(), aJCas));
        articleMetaData.setTypesOfMaterial(toStringArray(doc.getTypesOfMaterial(), aJCas));
        return articleMetaData;
    }
}
