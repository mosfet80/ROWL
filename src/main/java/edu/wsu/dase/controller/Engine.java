package edu.wsu.dase.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import edu.wsu.dase.model.Constants;
import edu.wsu.dase.model.RuleModel;

public class Engine {

	private OWLOntology activeOntology;
	private boolean ontologyChanged;
	private SortedMap<String, Set<OWLAxiom>> axiomsWithID;
	private SortedMap<String, RuleModel> rulesWithID;

	public Engine(OWLOntology activeOntology) {
		this.activeOntology = activeOntology;
		reloadRulesAndAxiomsFromOntology();
	}

	public SortedMap<String, RuleModel> getRules() {
		return this.rulesWithID;
	}

	public void OntologyChanged() {
		reloadRulesAndAxiomsFromOntology();
	}

	private void reloadRulesAndAxiomsFromOntology() {

		rulesWithID.clear();
		OWLAnnotationProperty annotP = activeOntology.getOWLOntologyManager().getOWLDataFactory()
				.getOWLAnnotationProperty(Constants.FIXED_ANNOTATION_NAME, new DefaultPrefixManager());

		Set<OWLAxiom> tmpAxioms = new HashSet<OWLAxiom>();
		String ruleID = "";
		int i = 0;

		for (OWLAxiom ax : activeOntology.getAxioms()) {
			for (OWLAnnotation ann : ax.getAnnotations()) {
				for (OWLAnnotationProperty anp : ann.getAnnotationPropertiesInSignature()) {
					if (anp.equals(annotP)) {
						System.out.println(ann.getValue().asLiteral().get().getLiteral());
						String val = ann.getValue().asLiteral().get().getLiteral();
						String[] values = val.split("___", 3);
						if (values.length == 3) {
							String ruleid = values[0];
							String ruleComment = values[1];
							String ruleText = values[2];
							if (ruleid.length() > 0 && ruleText.length() > 0) {
								// add to rulewith ID
								rulesWithID.put(ruleid, new RuleModel(ruleid, ruleText, ruleComment));

								// add to axioms with ID
								if (i == 0) {
									tmpAxioms.add(ax);
									ruleID = values[0];
									i++;
								} else {
									if (ruleID == values[0]) {
										tmpAxioms.add(ax);
									} else {
										axiomsWithID.put(values[0], tmpAxioms);

										tmpAxioms.clear();
										tmpAxioms.add(ax);
										ruleID = values[0];
									}
								}

							}
						}
					}
				}
			}
		}
	}

}
