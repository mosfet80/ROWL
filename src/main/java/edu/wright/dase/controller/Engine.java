package edu.wright.dase.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.protege.editor.owl.ui.prefix.PrefixUtilities;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.swrlapi.core.IRIResolver;
import org.swrltab.ui.ProtegeIRIResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wright.dase.model.Constants;
import edu.wright.dase.model.RuleModel;
import edu.wright.dase.model.RuleTableModel;
import edu.wright.dase.view.RuleEditorPanel;
import edu.wright.dase.view.RuleTablePanel;

public class Engine {

	private static final Logger log = LoggerFactory.getLogger(Engine.class);
	
	private OWLOntology activeOntology;
	private TreeMap<String, Set<OWLAxiom>> axiomsWithID;
	private TreeMap<String, Set<OWLObjectProperty>> newlyCreatedObjectPropertiesWithID;
	private TreeMap<String, RuleModel> rulesWithID;
	private PrefixManager prefixManager;
	private OWLAnnotationProperty fixedAnnotationProperty;

	private RuleTableModel ruleTableModel;

	private RuleTablePanel ruleTablePanel;

	private RuleEditorPanel ruleEditorPanel;

	private ProtegeIRIResolver iriResolver;

	private String defaultPrefix;


	/**
	 * @return the prefixManager
	 */
	public PrefixManager getPrefixManager() {
		return this.prefixManager;
	}

	/**
	 * @param prefixManager
	 *            the prefixManager to set
	 */
	public void setPrefixManager(PrefixManager prefixManager) {
		this.prefixManager = prefixManager;
	}

	private OWLOntologyManager owlOntologyManager;

	/**
	 * @return the activeOntology
	 */
	public OWLOntology getActiveOntology() {
		return activeOntology;
	}

	/**
	 * @param activeOntology
	 *            the activeOntology to set
	 */
	public void setActiveOntology(OWLOntology activeOntology) {
		this.activeOntology = activeOntology;
	}

	/**
	 * @return the owlOntologyManager
	 */
	public OWLOntologyManager getOwlOntologyManager() {
		return owlOntologyManager;
	}

	/**
	 * @param owlOntologyManager
	 *            the owlOntologyManager to set
	 */
	public void setOwlOntologyManager(OWLOntologyManager owlOntologyManager) {
		this.owlOntologyManager = owlOntologyManager;
	}

	private OWLDataFactory owlDataFactory;

	/**
	 * @return the owlDataFactory
	 */
	public OWLDataFactory getOwlDataFactory() {
		return owlDataFactory;
	}

	/**
	 * @param owlDataFactory
	 *            the owlDataFactory to set
	 */
	public void setOwlDataFactory(OWLDataFactory owlDataFactory) {
		this.owlDataFactory = owlDataFactory;
	}

	/**
	 * @return the iriResolver
	 */
	public IRIResolver getIriResolver() {
		return iriResolver;
	}

	/**
	 * @param iriResolver
	 *            the iriResolver to set
	 */
	public void setIriResolver(ProtegeIRIResolver iriResolver) {
		this.iriResolver = iriResolver;
	}

	/**
	 * @return the ruleEditorPanel
	 */
	public RuleEditorPanel getRuleEditorPanel() {
		return ruleEditorPanel;
	}

	/**
	 * @param ruleEditorPanel
	 *            the ruleEditorPanel to set
	 */
	public void setRuleEditorPanel(RuleEditorPanel ruleEditorPanel) {
		this.ruleEditorPanel = ruleEditorPanel;
	}

	/**
	 * @return the ruleTablePanel
	 */
	public RuleTablePanel getRuleTablePanel() {
		return ruleTablePanel;
	}

	/**
	 * @param ruleTablePanel
	 *            the ruleTablePanel to set
	 */
	public void setRuleTablePanel(RuleTablePanel ruleTablePanel) {
		this.ruleTablePanel = ruleTablePanel;
	}

	/**
	 * @return the ruleTableModel
	 */
	public RuleTableModel getRuleTableModel() {
		return ruleTableModel;
	}

	/**
	 * @param ruleTableModel
	 *            the ruleTableModel to set
	 */
	public void setRuleTableModel(RuleTableModel ruleTableModel) {
		this.ruleTableModel = ruleTableModel;
	}

	public Engine(OWLOntology activeOntology, ProtegeIRIResolver iriResolver) {
		this.activeOntology = activeOntology;
		this.owlOntologyManager = this.activeOntology.getOWLOntologyManager();
		this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
		this.iriResolver = iriResolver;

		this.prefixManager = PrefixUtilities.getPrefixOWLOntologyFormat(this.activeOntology);

		if (!addPrefix()) {
			// prefixManager.setPrefix("", Constants.ANONYMOUS_DEFUALT_ID);
			// defaultPrefix = Constants.ANONYMOUS_DEFUALT_ID;
			// defaultPrefix = "";
			if (this.defaultPrefix == null) {
				this.defaultPrefix = "";
			}
		}

		fixedAnnotationProperty = activeOntology.getOWLOntologyManager().getOWLDataFactory()
				.getOWLAnnotationProperty(Constants.FIXED_ANNOTATION_NAME, prefixManager);

		initializeDataStructure();

		reloadRulesAndAxiomsFromOntology();

		// Save the reference
		Constants.activeOntologyAsStaticReference = this.activeOntology;
		Constants.owlOntologyManagerAsStaticReference = this.owlOntologyManager;
		Constants.owlDataFactoryAsStaticReference = this.owlDataFactory;
		Constants.engineAsStaticReference = this;
	}

	private int getFreshObjPropCounter() {
		int counter = 0;
		for (OWLObjectProperty owlObjectProperty : getActiveOntology().getObjectPropertiesInSignature(true)) {
			if (owlObjectProperty.getIRI().toString().contains(Constants.FRESH_PROP_NAME)) {
				counter++;
			}
		}
		counter++;
		return counter;
	}

	public String getNextFreshObjProp() {
		int counter = 0;

		// Should Work
		OWLEntity owlEntity;
		do {
			++counter;
			owlEntity = this.iriResolver.getOWLEntityToFindNextName(Constants.FRESH_PROP_NAME + counter);
		} while (owlEntity != null);

		String freshPropName = Constants.FRESH_PROP_NAME + counter;

		// no null pointer checking is necessary here
		// for getValueAsOWLCompatibleName
		// Because freshPropName never include colon(:)
		freshPropName = getValueAsOWLCompatibleName(freshPropName);

		return freshPropName;
	}

	public OWLObjectProperty createOWLObjectProperty(String Name) {

		OWLObjectProperty newOWLObjectProperty = this.owlDataFactory.getOWLObjectProperty(Name, prefixManager);

		OWLAxiom declareaxiom = this.owlDataFactory.getOWLDeclarationAxiom(newOWLObjectProperty);
		AddAxiom addAxiom = new AddAxiom(this.activeOntology, declareaxiom);
		this.owlOntologyManager.applyChange(addAxiom);

		// this.ruleEditorPanel.update();
		return newOWLObjectProperty;
	}

	public String getValueAsOWLCompatibleName(String name) {

		// try to add prefix
		// this is checked because Ontology ID may change at any time and after
		// changing ontology ID ontologyChange Event not fired
		addPrefix();

		if (name.contains(":")) {
			String[] subParts = name.split(":");
			if (subParts.length == 2) {
				if (prefixManager.containsPrefixMapping(subParts[0] + ":")) {
					return name;
				} else {
					// it should not occur because SWRL is checking the syntax
					return null;
				}
			} else {
				// it should not occur because SWRL is checking the syntax
				return null;
			}
		} else {
			// defaultPrefix
			// return this.defaultPrefix + name;
			String prefixString = this.prefixManager.getPrefix(this.defaultPrefix);
			String modifiedName = "";

			if (null != prefixString) {

				if (prefixString.endsWith("#") || prefixString.endsWith("/") || prefixString.endsWith(":")) {
					modifiedName = this.defaultPrefix + name;
				} else {
					modifiedName = this.defaultPrefix + "#" + name;
				}

				return modifiedName;
			} else {
				modifiedName = this.defaultPrefix + name;
				return modifiedName;
			}
		}
	}

	public boolean addPrefix() {
		try {
			OWLOntologyID ontoID = activeOntology.getOntologyID();

			if (ontoID == null) {

				// JOptionPane.showMessageDialog(editor, "Please Specify
				// Ontology ID(Ontology IRI) first.");
				return false;
			}

			// ontoID can contain anonymous.
			// need more checking
			com.google.common.base.Optional<IRI> iri = ontoID.getDefaultDocumentIRI();
			if (!iri.isPresent()) {

				// JOptionPane.showMessageDialog(editor, "Please Specify
				// Ontology ID(Ontology IRI) first.");
				return false;
			}

			String uriString = iri.get().toString();
			if (uriString == null) {

				// JOptionPane.showMessageDialog(editor, "Please Specify
				// Ontology ID(Ontology IRI) first.");
				return false;
			}
			String prefix;
			if (uriString.endsWith("/")) {
				String sub = uriString.substring(0, uriString.length() - 1);
				prefix = sub.substring(sub.lastIndexOf("/") + 1, sub.length());
			} else {
				prefix = uriString.substring(uriString.lastIndexOf('/') + 1, uriString.length());
			}
			if (prefix.endsWith(".owl")) {
				prefix = prefix.substring(0, prefix.length() - 4);
			}
			prefix = prefix.toLowerCase();
			if (!uriString.endsWith("#") && !uriString.endsWith("/")) {
				uriString = uriString + "#";
			}
			if (prefix.length() < 1) {
				return false;
			}

			// set current ontology id as a prefix name
			prefixManager.setPrefix(prefix, uriString);

			// get the defaultprefix if exist otherwise take the current
			// ontology id as defaultprefix
			String _defaultPrefix = prefixManager.getDefaultPrefix();
			if (_defaultPrefix != null) {
				defaultPrefix = ":";
			} else {
				defaultPrefix = prefix + ":";
			}
			return true;

		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void initializeDataStructure() {
		rulesWithID = new TreeMap<String, RuleModel>();
		axiomsWithID = new TreeMap<String, Set<OWLAxiom>>();
		newlyCreatedObjectPropertiesWithID = new TreeMap<String, Set<OWLObjectProperty>>();
	}

	public TreeMap<String, RuleModel> getRules() {

		return this.rulesWithID;
	}

	public RuleModel getRulebyID(String ruleName) {
		if (rulesWithID.containsKey(ruleName)) {
			return rulesWithID.get(ruleName);
		}
		return null;
	}

	public Set<OWLAxiom> getAxiomsbyID(String ruleName) {

		if (axiomsWithID.containsKey(ruleName)) {
			return axiomsWithID.get(ruleName);
		}
		return null;
	}

	public void addARulesWithID(String ruleName, RuleModel rule) {
		rulesWithID.put(ruleName, rule);
	}

	public void addAxiomsWithID(String ruleName, Set<OWLAxiom> axiomSet) {
		axiomsWithID.put(ruleName, axiomSet);
	}

	/**
	 * Delete Rule from the table.
	 * 
	 * 
	 * has to be sure from Adila.
	 * 
	 * Possible option 1. also remove corresponding axioms from ontology or 2.
	 * only remove corresponding annotations from those axioms
	 * 
	 * Current implementation remove corresponding axioms from ontology
	 * 
	 * @param ruleName
	 */
	public void deleteRule(String ruleName) {

		// remove the corresponding axioms
		if (axiomsWithID.containsKey(ruleName)) {
			owlOntologyManager.removeAxioms(activeOntology, axiomsWithID.get(ruleName));
			axiomsWithID.remove(ruleName);
		}

		// remove the corresponding newly created Object Properties
		if (newlyCreatedObjectPropertiesWithID.containsKey(ruleName)) {
			OWLEntityRemover remover = new OWLEntityRemover(Collections.singleton(activeOntology));

			for (OWLObjectProperty op : newlyCreatedObjectPropertiesWithID.get(ruleName)) {
				op.accept(remover);
			}

			ChangeApplied CA = owlOntologyManager.applyChanges(remover.getChanges());
			// System.out.println("changeApplied result: " + CA.name());

			// remove from map also
			newlyCreatedObjectPropertiesWithID.remove(ruleName);
		}

		// remove rules at-last
		if (rulesWithID.containsKey(ruleName)) {
			rulesWithID.remove(ruleName);
		}
	}

	/**
	 * this method is called only when new objprops is created but the rule is
	 * not integrated into the system
	 */
	public void deleteNewlyCreatedObjProperties(Set<OWLObjectProperty> owlObjectProperties) {
		OWLEntityRemover remover = new OWLEntityRemover(Collections.singleton(activeOntology));

		for (OWLObjectProperty op : owlObjectProperties) {
			op.accept(remover);
		}

		ChangeApplied CA = owlOntologyManager.applyChanges(remover.getChanges());
	}

	public void OntologyChanged() {
		// reloadRulesAndAxiomsFromOntology();
	}

	public boolean checkDuplicateRuleName(String RuleName) {
		if (rulesWithID.containsKey(RuleName))
			return true;
		else
			return false;
	}

	public boolean checkDuplicateRuleText(String RuleText) {
		if (rulesWithID.containsValue(RuleText))
			return true;
		else
			return false;
	}

	public String getAutogeneratedNextRuleName() {
		int size = rulesWithID.size() + 1;
		return "R" + size;
	}

	private void reloadRulesAndAxiomsFromOntology() {

		if (rulesWithID != null)
			rulesWithID.clear();
		if (axiomsWithID != null)
			axiomsWithID.clear();

		Set<OWLAxiom> tmpAxioms = new HashSet<OWLAxiom>();
		String tmpRuleID = "";
		int i = 0;

		/**
		 * when converting rule to owl, single rule can generate multiple
		 * axioms. That means multiple axioms need to be binded for a single
		 * rule-id
		 */
		for (OWLAxiom ax : activeOntology.getAxioms()) {
			for (OWLAnnotation ann : ax.getAnnotations()) {
				for (OWLAnnotationProperty anp : ann.getAnnotationPropertiesInSignature()) {
					if (anp.equals(fixedAnnotationProperty)) {

						String val = ann.getValue().asLiteral().get().getLiteral();
						String[] values = val.split("___");

						if (values.length >= 2) {
							String ruleID = values[0];
							String ruleText = values[1];
							String ruleComment;
							if (values.length >= 3) {
								ruleComment = values[2];
							} else {
								ruleComment = "";
							}
							if (ruleID.length() > 0 && ruleText.length() > 0) {

								// add to rulewith ID
								rulesWithID.put(ruleID, new RuleModel(ruleID, ruleText, ruleComment));

								// add into axiomsWithID
								if (axiomsWithID.containsKey(ruleID)) {
									axiomsWithID.get(ruleID).add(ax);
								} else {
									tmpAxioms = new HashSet<OWLAxiom>();
									tmpAxioms.add(ax);
									axiomsWithID.put(ruleID, tmpAxioms);
								}
								if (values.length > 3) {
									// add into
									// newlyCreatedObjectPropertiesWithID
									OWLObjectProperty owlObjectProperty;
									for (int counter = values.length; counter > 3; counter--) {
										String iriString = values[counter - 1];
										IRI iri = IRI.create(iriString);
										owlObjectProperty = owlDataFactory.getOWLObjectProperty(iri);

										if (newlyCreatedObjectPropertiesWithID.containsKey(ruleID)) {

											// create objectProperty from String
											// IRI
											newlyCreatedObjectPropertiesWithID.get(ruleID).add(owlObjectProperty);
										} else {
											Set<OWLObjectProperty> tmpOWLObjecProperties = new HashSet<OWLObjectProperty>();
											tmpOWLObjecProperties.add(owlObjectProperty);
											newlyCreatedObjectPropertiesWithID.put(ruleID, tmpOWLObjecProperties);
										}

									}
								}
							}
						} else {
							System.out.println("Cannot retrieve annotation. Annotation doesn't have proper Syntax");
						}
					}
				}
			}
		}
	}

}
