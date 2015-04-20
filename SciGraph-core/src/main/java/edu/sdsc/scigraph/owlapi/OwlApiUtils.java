/**
 * Copyright (C) 2014 The SciGraph authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.sdsc.scigraph.owlapi;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.validator.routines.UrlValidator;
import org.coode.owlapi.obo12.parser.OBO12ParserFactory;
import org.coode.owlapi.oboformat.OBOFormatParserFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.io.OWLParserFactoryRegistry;
import org.semanticweb.owlapi.io.XMLUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.google.common.base.Optional;

public class OwlApiUtils {

  private static final Logger logger = Logger.getLogger(OwlApiUtils.class.getName());

  private static final UrlValidator validator = UrlValidator.getInstance();

  private static final Set<String> unknownLanguages = new HashSet<>();

  /*** 
   * @param literal An OWLLiteral
   * @return an optional correctly typed Java object from the OWLLiteral
   */
  public static Optional<Object> getTypedLiteralValue(OWLLiteral literal) {
    Object literalValue = null;
    if (literal.isBoolean()) {
      literalValue = literal.parseBoolean();
    } else if (literal.isInteger()) {
      literalValue = literal.parseInteger();
    } else if (literal.isFloat()) {
      literalValue = literal.parseFloat();
    } else if (literal.isDouble()) {
      literalValue = literal.parseDouble();
    } else {
      // TODO: Ignore non-english literals for now
      if (literal.hasLang() && !literal.getLang().equals("en")) {
        if (!unknownLanguages.contains(literal.getLang())) {
          unknownLanguages.add(literal.getLang());
          logger.warning("Ignoring *all* literals with unsupported langauge: \"" + literal.getLang() + "\".");
        }
        return Optional.absent();
      }
      literalValue = literal.getLiteral();
    }
    return Optional.of(literalValue);
  }

  public static String getIri(OWLDeclarationAxiom expression) {
    return expression.getEntity().getIRI().toString();
  }

  public static String getIri(OWLClassExpression expression) {
    if (expression.isAnonymous()) {
      return "_:" + expression.hashCode();
    } else {
      return expression.asOWLClass().getIRI().toString();
    }
  }

  public static String getIri(OWLObjectPropertyExpression property) {
    if (property.isAnonymous()) {
      return "_:" + property.hashCode();
    } else {
      return property.asOWLObjectProperty().getIRI().toString();
    }
  }

  public static String getIri(OWLIndividual individual) {
    if (individual.isAnonymous()) {
      return ((OWLAnonymousIndividual)individual).getID().getID();
    } else {
      return individual.asOWLNamedIndividual().getIRI().toString();
    }
  }

  public static String getIri(OWLAnnotationProperty property) {
    return property.asOWLAnnotationProperty().getIRI().toString();
  }

  public static String getFragment(OWLRDFVocabulary vocab) {
    return XMLUtils.getNCNameSuffix(vocab.getIRI());
  }

  /***
   * @param manager The ontology manager to use
   * @param ontology A string representing the ontology to load. This can be an URI or a file path
   * @return The loaded ontology
   * @throws OWLOntologyCreationException
   */
  public static OWLOntology loadOntology(OWLOntologyManager manager, String ontology) throws OWLOntologyCreationException {
    logger.info(String.format("Loading ontology with owlapi: %s", ontology));
    String origThreadName = Thread.currentThread().getName();
    Thread.currentThread().setName("read - " + ontology);
    OWLOntology ont;
    if (validator.isValid(ontology)) {
      ont = manager.loadOntology(IRI.create(ontology));
    } else {
      ont = manager.loadOntologyFromOntologyDocument(new File(ontology));
    }
    logger.info(String.format("Finished loading ontology with owlapi: %s", ontology));
    Thread.currentThread().setName(origThreadName);
    return ont;
  }

  public static void silenceOboParser() {
    OWLManager.createOWLOntologyManager();
    /* TODO: Why does this logging never become silent?
     * Logger logger = Logger.getLogger("org.obolibrary");
    logger.setLevel(java.util.logging.Level.SEVERE);
    Handler[] handlers = logger.getHandlers();
    for (Handler handler : handlers) {
      handler.setLevel(Level.SEVERE);
    }*/
    OWLParserFactoryRegistry registry = OWLParserFactoryRegistry.getInstance();
    List<OWLParserFactory> factories = registry.getParserFactories();
    for (OWLParserFactory factory : factories) {
      if (factory instanceof OBOFormatParserFactory ||
          factory instanceof OBO12ParserFactory) {
        registry.unregisterParserFactory(factory);
      }
    }
  }
}
