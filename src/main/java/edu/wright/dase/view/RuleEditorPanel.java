package edu.wright.dase.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.protege.editor.owl.ui.prefix.PrefixUtilities;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.parser.SWRLIncompleteRuleException;
import org.swrlapi.parser.SWRLParseException;
import org.swrlapi.parser.SWRLParser;
import org.swrlapi.ui.dialog.SWRLRuleEditorDialog;
import org.swrlapi.ui.dialog.SWRLRuleEngineDialogManager;
import org.swrlapi.ui.model.SWRLAutoCompleter;
import org.swrlapi.ui.model.SWRLRuleEngineModel;
import org.swrlapi.ui.view.SWRLAPIView;
import org.swrltab.util.SWRLRuleEditorAutoCompleteState;
import org.swrltab.util.SWRLRuleEditorInitialDialogState;

import edu.wright.dase.controller.Engine;
import edu.wright.dase.controller.SuggestionPopup;
import edu.wright.dase.model.Constants;
import edu.wright.dase.model.RuleModel;
import edu.wright.dase.model.RuleTableModel;
import edu.wright.dase.model.ruletoaxiom.Translator;
import edu.wright.dase.view.axiomManchesterDialog.AxiomsDialog;

/**
 * developed by sarker.3 JPanel providing a SWRL rule
 *
 */
public class RuleEditorPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory.getLogger(RuleEditorPanel.class);

	// private static final String TITLE = "Edit";
	private static final String RULE_NAME_TITLE = "Rule Name (Mandatory). Rule Will be saved using this name. ";
	private static final String COMMENT_LABEL_TITLE = "Comment (Optional) ";
	private static final String RULE_TEXT_LABEL = "Rule Text";
	private static final String STATUS_LABEL_TITLE = "Status";
	private static final String CONVERT_TO_OWL_BUTTON_TITLE = "Convert to OWL Axiom";

	private static final String CANCEL_BUTTON_TITLE = "Clear";
	private static final String STATUS_OK = "Ok";
	private static final String STATUS_NO_RULE_TEXT = "Use Tab key to cycle through auto-completions;"
			+ " use Escape key to remove auto-complete expansion";
	private static final String INVALID_RULE_TITLE = "Invalid";
	private static final String MISSING_RULE = "Nothing to save!";
	private static final String MISSING_RULE_NAME_TITLE = "Empty Name";
	private static final String MISSING_RULE_NAME = "A name must be supplied!";
	private static final String QUIT_CONFIRM_TITLE = "Unsaved Changes";
	private static final String QUIT_CONFIRM_MESSAGE = "Are you sure you want discard your changes?";
	private static final String DUPLICATE_RULE_TEXT = "Name already in use - please pick another name.";
	private static final String DUPLICATE_RULE_TITLE = "Duplicate Name";
	private static final String INTERNAL_ERROR_TITLE = "Internal Error";
	private static final String CANT_CONVERT_TO_OWL_AXIOM_TEXT = "Not transferable to OWL Axiom.";

	private static final int BUTTON_PREFERRED_WIDTH = 200;
	private static final int BUTTON_PREFERRED_HEIGHT = 30;
	private static final int RULE_EDIT_AREA_COLUMNS = 20;
	private static final int RULE_EDIT_AREA_ROWS = 60;

	private StyleContext styleContext = StyleContext.getDefaultStyleContext();
	private AttributeSet redColor = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground,
			Color.RED);
	private AttributeSet blackColor = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground,
			Color.BLACK);

	private AttributeSet setUnderLine = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Underline,
			true);
	private AttributeSet clearUnderLine = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Underline,
			false);
	private StyledDocument styledDoc;

	private Rectangle invalidTextRectangle;

	private SuggestionPopup suggestionPopup;

	@NonNull
	private SWRLRuleEngineDialogManager dialogManager;

	private Set<OWLObjectProperty> newlyCreatedObjectProperitesList;
	private Set<OWLObjectProperty> newlyCreatedButNotUsedObjectProperitesList;

	@NonNull
	private final SWRLRuleEditorInitialDialogState initialDialogState = new SWRLRuleEditorInitialDialogState();
	@NonNull
	private final JTextField ruleNameTextField, commentTextField, statusTextField;
	@NonNull
	private final JTextPane ruleTextTextPane;
	@NonNull
	private final JButton convertToOWLButton, cancelButton;
	@NonNull
	private final Border loweredBevelBorder;
	@NonNull
	private final Border yellowBorder;

	private JScrollPane scrollPane;
	private JTabbedPane tabbedPane;
	private JLabel lbl1;
	private JLabel lbl2;

	@NonNull
	private Optional<@NonNull SWRLRuleEditorAutoCompleteState> autoCompleteState = Optional
			.<@NonNull SWRLRuleEditorAutoCompleteState>empty(); // Present if
																// auto-complete
	private boolean editMode = false;
	JPanel pnlForCreateNewEntity;

	// // for test purpose only
	// private RuleEditorPanel() {
	//
	// // this.dialogManager = dialogManager;
	// this.loweredBevelBorder = BorderFactory.createLoweredBevelBorder();
	// this.yellowBorder = BorderFactory.createLineBorder(Color.YELLOW);
	//
	// this.ruleTextTextPane = new JTextPane();
	// this.convertToOWLButton = new JButton(CONVERT_TO_OWL_BUTTON_TITLE);
	// this.cancelButton = new JButton(CANCEL_BUTTON_TITLE);
	// this.ruleNameTextField = new JTextField("");
	// this.commentTextField = new JTextField("");
	// this.statusTextField = new JTextField(STATUS_NO_RULE_TEXT);
	// initialize();
	// }

	public RuleEditorPanel(@NonNull SWRLRuleEngineDialogManager dialogManager, @NonNull JTabbedPane tabbedPane) {

		this.dialogManager = dialogManager;

		this.loweredBevelBorder = BorderFactory.createLoweredBevelBorder();
		this.yellowBorder = BorderFactory.createLineBorder(Color.YELLOW);
		this.ruleTextTextPane = new JTextPane();
		this.styledDoc = this.ruleTextTextPane.getStyledDocument();
		this.invalidTextRectangle = new Rectangle(0, 0, 0, 0);
		this.tabbedPane = tabbedPane;
		this.convertToOWLButton = new JButton(CONVERT_TO_OWL_BUTTON_TITLE);
		this.cancelButton = new JButton(CANCEL_BUTTON_TITLE);
		lbl1 = new JLabel();
		lbl2 = new JLabel();
		this.ruleNameTextField = new JTextField("");
		this.commentTextField = new JTextField("");
		this.statusTextField = new JTextField(STATUS_NO_RULE_TEXT);
		initialize();

		setInitialRuleName();
	}

	public void initialize() {

		initializeComponents();

		this.ruleTextTextPane.addKeyListener(new SWRLRuleEditorKeyAdapter());
		this.ruleTextTextPane.addMouseListener(new RuleTextPaneMouseAdapter());
		this.cancelButton.addActionListener(new CancelSWRLRuleEditActionListener());
		this.convertToOWLButton.addActionListener(new ConvertSWRLRuleActionListener(this));
	}

	public void loadEdittingRule(String ruleName, String ruleComment, String ruleText) {

		cancelEditMode();
		this.ruleNameTextField.setText(ruleName);
		this.ruleNameTextField.setCaretPosition(this.ruleNameTextField.getText().length());
		this.ruleTextTextPane.setText(ruleText);
		this.commentTextField.setText(ruleComment);
		this.statusTextField.setText("");
		updateStatus();
		this.editMode = true;
	}

	public void update() {
		updateStatus();
	}

	private void updateStatus() {
		String ruleText = getRuleText();

		if (ruleText.isEmpty()) {
			setInformationalStatusText(STATUS_NO_RULE_TEXT);
			disableSave();

		} else {
			try {
				createSWRLParser().parseSWRLRule(ruleText, true, getRuleName(), getComment());
				this.ruleTextTextPane.requestFocus();
				setInformationalStatusText(STATUS_OK);
				enableSave();
				if ((!ruleText.contains("->"))) {
					disableSave();
				}

			} catch (SWRLIncompleteRuleException e) {
				setIncompleteStatusText(e.getMessage() == null ? "" : e.getMessage());
				disableSave();

				if (e.getMessage() != null) {
					// System.out.println("incomplete State");
					// showColor(e.getMessage());
					// createSuggestion();
				}
			} catch (SWRLParseException e) {
				setErrorStatusText(e.getMessage() == null ? "" : e.getMessage());
				disableSave();
				if (e.getMessage() != null) {
					// System.out.println("parseException State");
					// showColor(e.getMessage());
					// createSuggestion();
				}
			} catch (RuntimeException e) {
				setInformationalStatusText(e.getMessage() == null ? "" : e.getMessage());
				disableSave();
			}
		}
	}

	private void cancelEditMode() {
		this.ruleNameTextField.setText("");
		this.ruleNameTextField.setEnabled(true);
		this.ruleTextTextPane.setText("");
		this.ruleTextTextPane.setEnabled(true);
		this.ruleTextTextPane.setText("");
		this.commentTextField.setText("");
		this.statusTextField.setText("");

		this.editMode = false;
	}

	private void initializeComponents() {
		JLabel ruleNameLabel = new JLabel(RULE_NAME_TITLE);
		JLabel commentLabel = new JLabel(COMMENT_LABEL_TITLE);
		JLabel statusLabel = new JLabel(STATUS_LABEL_TITLE);
		JLabel templateSentence = new JLabel(
				"<html> &nbsp; &nbsp; EXAMPLE <br>&nbsp; &nbsp; Given sentence to model: <font size='4'; font-family= 'Courier New'> If a person has a baby then that person is a parent. </font></html>");
		JLabel templateRule = new JLabel(
				"<html> <br> &nbsp; &nbsp; The above sentence modeled as rule: <font size='4'; font-family= 'Courier New'> Person(?x) ^ hasBaby(?x,?y) -> Parent(?x)</font> </html>");
		JLabel templateSuggestion = new JLabel(
				"<html> <br> &nbsp; &nbsp; <font > Now, model your own sentence as a rule. Put the rule name in the Rule Name text field and the rule itself in the Rule Text box. <br> &nbsp; &nbsp; When you are done writing your rule in ROWL Tab, click </font> <font  color='green'>Convert to OWL Axiom </font> button.  Then confirm by clicking <font  color='green'> Integrate </font> on the pop-up window.  </br> </html>");

		JLabel ruleTextLabel = new JLabel(RULE_TEXT_LABEL);
		JLabel blankLabel = new JLabel("");
		JPanel upperPanel = new JPanel(new BorderLayout());
		JPanel upperPanel1stHalf = new JPanel(new BorderLayout());
		JPanel upperPanel2ndHalf = new JPanel(new GridLayout(8, 1));
		JPanel rulePanel = new JPanel(new GridLayout(1, 1));
		JPanel lowerPanel = new JPanel(new GridLayout(1, 1));
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JPanel surroundPanel = new JPanel(new BorderLayout());
		// Container contentPane = getRootPane();

		this.ruleNameTextField.setBorder(this.loweredBevelBorder);

		// this.ruleTextTextArea.setLineWrap(true);
		// this.ruleTextTextArea.setWrapStyleWord(true);
		this.ruleTextTextPane.setBorder(this.loweredBevelBorder);
		this.ruleTextTextPane.setPreferredSize(new Dimension(300, 150));

		this.commentTextField.setDisabledTextColor(Color.BLACK);
		this.commentTextField.setBorder(this.loweredBevelBorder);

		this.statusTextField.setDisabledTextColor(Color.BLACK);
		this.statusTextField.setEnabled(false);
		this.statusTextField.setBorder(this.loweredBevelBorder);

		this.cancelButton.setPreferredSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));
		this.convertToOWLButton.setPreferredSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));

		this.setLayout(new BorderLayout());

		// consist of template example
		upperPanel1stHalf.add(templateSentence, BorderLayout.NORTH);
		upperPanel1stHalf.add(templateRule, BorderLayout.CENTER);
		upperPanel1stHalf.add(templateSuggestion, BorderLayout.SOUTH);

		// consist of ruleName, comment,template example and status.
		upperPanel2ndHalf.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		upperPanel2ndHalf.add(blankLabel);
		upperPanel2ndHalf.add(ruleNameLabel);
		upperPanel2ndHalf.add(this.ruleNameTextField);
		upperPanel2ndHalf.add(commentLabel);
		upperPanel2ndHalf.add(this.commentTextField);
		upperPanel2ndHalf.add(statusLabel);
		upperPanel2ndHalf.add(this.statusTextField);
		upperPanel2ndHalf.add(ruleTextLabel);

		// consist of upperfisrst and upper2ndHalf
		upperPanel.add(upperPanel1stHalf, BorderLayout.NORTH);
		upperPanel.add(upperPanel2ndHalf, BorderLayout.CENTER);

		// consist of rule text area
		rulePanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		this.scrollPane = new JScrollPane(this.ruleTextTextPane);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(300, 150));
		rulePanel.add(this.scrollPane);

		buttonPanel.add(cancelButton);
		buttonPanel.add(this.convertToOWLButton);

		// consist of buttons
		lowerPanel.add(buttonPanel);

		surroundPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

		surroundPanel.add(upperPanel, BorderLayout.NORTH);
		surroundPanel.add(rulePanel, BorderLayout.CENTER);
		surroundPanel.add(lowerPanel, BorderLayout.SOUTH);
		this.add(surroundPanel, BorderLayout.CENTER);
		// pack();
	}

	private void autoComplete() {
		if (!isInAutoCompleteMode()) {
			String ruleText = getRuleText();
			int textPosition = this.ruleTextTextPane.getCaretPosition();
			int i = SWRLParser.findSplittingPoint(ruleText.substring(0, textPosition));
			String prefix = ruleText.substring(i, textPosition);
			if (!prefix.equals("")) {
				List<@NonNull String> expansions = getExpansions(prefix); // All
																			// expansions
																			// will
																			// start
																			// with
																			// the
																			// empty
																			// string.

				if (expansions.size() > 1) { // More than the empty string
												// expansion; if not, do not
												// enter autoComplete mode
					SWRLRuleEditorAutoCompleteState state = new SWRLRuleEditorAutoCompleteState(textPosition, prefix,
							expansions);
					insertExpansion(textPosition, prefix, state.getNextExpansion()); // Skip
																						// the
																						// empty
																						// string
					enableAutoCompleteMode(state);
				}
			}
		} else { // Already in auto-complete mode
			int textPosition = this.autoCompleteState.get().getTextPosition();
			String prefix = this.autoCompleteState.get().getPrefix();
			String currentExpansion = this.autoCompleteState.get().getCurrentExpansion();
			String nextExpansion = this.autoCompleteState.get().getNextExpansion();

			replaceExpansion(textPosition, prefix, currentExpansion, nextExpansion);
		}
	}

	private boolean isInAutoCompleteMode() {
		return this.autoCompleteState.isPresent();
	}

	private void enableAutoCompleteMode(@NonNull SWRLRuleEditorAutoCompleteState autoCompleteState) {
		this.autoCompleteState = Optional.of(autoCompleteState);
	}

	private void disableAutoCompleteModeIfNecessary() {
		if (this.autoCompleteState.isPresent())
			disableAutoCompleteMode();
	}

	private void disableAutoCompleteMode() {
		this.autoCompleteState = Optional.<@NonNull SWRLRuleEditorAutoCompleteState>empty();
	}

	private void cancelAutoCompleteIfNecessary() {
		if (isInAutoCompleteMode()) {
			int textPosition = this.autoCompleteState.get().getTextPosition();
			String prefix = this.autoCompleteState.get().getPrefix();
			String currentExpansion = this.autoCompleteState.get().getCurrentExpansion();

			replaceExpansion(textPosition, prefix, currentExpansion, "");
			disableAutoCompleteMode();
		}
	}

	private void insertExpansion(int textPosition, @NonNull String prefix, @NonNull String expansion) {
		String expansionTail = expansion.substring(prefix.length());

		try {
			Document document = this.ruleTextTextPane.getDocument();
			if (document != null)
				document.insertString(textPosition, expansionTail, SimpleAttributeSet.EMPTY);
			else
				disableAutoCompleteMode();
		} catch (BadLocationException e) {
			disableAutoCompleteMode();
		}
	}

	private void replaceExpansion(int textPosition, @NonNull String prefix, @NonNull String currentExpansion,
			@NonNull String nextExpansion) {
		String currentExpansionTail = currentExpansion.isEmpty() ? "" : currentExpansion.substring(prefix.length());
		String nextExpansionTail = nextExpansion.isEmpty() ? "" : nextExpansion.substring(prefix.length());

		try {
			if (!currentExpansionTail.isEmpty())
				this.ruleTextTextPane.getDocument().remove(textPosition, currentExpansionTail.length());

			if (!nextExpansionTail.isEmpty())
				this.ruleTextTextPane.getDocument().insertString(textPosition, nextExpansionTail,
						SimpleAttributeSet.EMPTY);
		} catch (BadLocationException e) {
			disableAutoCompleteMode();
		}
	}

	@NonNull
	private List<@NonNull String> getExpansions(@NonNull String prefix) {
		List<@NonNull String> expansions = new ArrayList<>();

		expansions.add(""); // Add empty expansion that we can cycle back to
		expansions.addAll(createSWRLAutoCompleter().getCompletions(prefix));

		return expansions;
	}

	@NonNull
	private SWRLAutoCompleter createSWRLAutoCompleter() {
		return Constants.swrlRuleEngineModelAsStaticReference.createSWRLAutoCompleter();
	}

	private void disableSave() {
		this.convertToOWLButton.setEnabled(false);
	}

	private void enableSave() {
		this.convertToOWLButton.setEnabled(true);
	}

	private void setInformationalStatusText(@NonNull String status) {

		this.statusTextField.setBorder(loweredBevelBorder);
		this.statusTextField.setDisabledTextColor(Color.BLACK);
		this.statusTextField.setText(status);
		augmentAndSetStatusText();
	}

	private void setIncompleteStatusText(@NonNull String status) {

		this.statusTextField.setBorder(yellowBorder);
		this.statusTextField.setDisabledTextColor(Color.BLACK);
		this.statusTextField.setText(status);
		augmentAndSetStatusText();
	}

	private void setErrorStatusText(@NonNull String status) {

		this.statusTextField.setDisabledTextColor(Color.RED);
		this.statusTextField.setText(status);
		augmentAndSetStatusText();
	}

	private void augmentAndSetStatusText() {

		String errorText = this.statusTextField.getText();

		if (!errorText.contains("cannot use name of existing OWL class")) {

			if (errorText.contains("Invalid SWRL atom predicate")) {
				// class
				// add(bind("add as Class", new AddClassAction("Class"), ""));
				// object property
				// data property
				this.statusTextField.setText(errorText + " (Right click to Declare)");

			} else if (errorText.contains("Invalid OWL individual name")) {
				// namedindividual
				this.statusTextField.setText(errorText + " (Right click to Declare)");

			} else if (errorText.contains("invalid datatype name")) {
				// datatype
				this.statusTextField.setText(errorText + " (Right click to Declare)");

			} else {
				// there is no error
				// popup will not be shown
			}
		}
	}

	@NonNull
	private String getRuleName() {
		return this.ruleNameTextField.getText().trim();

	}

	@NonNull
	private String getComment() {
		return this.commentTextField.getText().trim();

	}

	@NonNull
	public String getRuleText() { // We replace the Unicode characters when
									// parsing
		return this.ruleTextTextPane.getText().replaceAll(Character.toString(SWRLParser.RING_CHAR), ".");
	}

	@NonNull
	private SWRLRuleEngine getSWRLRuleEngine() {
		return Constants.swrlRuleEngineModelAsStaticReference.getSWRLRuleEngine();
	}

	@NonNull
	private SWRLRuleEngineModel getSWRLRuleEngineModel() {
		return Constants.swrlRuleEngineModelAsStaticReference;
	}

	@NonNull
	private SWRLRuleEditorInitialDialogState getInitialDialogState() {
		return this.initialDialogState;
	}

	private SWRLParser createSWRLParser() {
		return Constants.swrlRuleEngineModelAsStaticReference.createSWRLParser();
	}

	private @NonNull RuleTableModel getRuleTableModel() {

		return Constants.engineAsStaticReference.getRuleTableModel();
	}

	@NonNull
	private SWRLRuleEngineDialogManager getDialogManager() {
		return this.dialogManager;
	}

	private void setInitialRuleName() {
		this.ruleNameTextField.setText(getEngine().getAutogeneratedNextRuleName());
	}

	private void clearIfOk() {

		this.ruleNameTextField.setText("");
		this.ruleNameTextField.setCaretPosition(this.ruleNameTextField.getText().length());
		this.ruleTextTextPane.setText("");
		this.commentTextField.setText("");
		this.statusTextField.setText("");
		updateStatus();
	}

	private void showSuggestionPopup(MouseEvent event) {

		String errorText = this.statusTextField.getText();

		if (!errorText.contains("cannot use name of existing OWL class")) {

			if (errorText.contains("Invalid SWRL atom predicate")) {
				// class
				// add(bind("add as Class", new AddClassAction("Class"), ""));
				// object property
				// data property

				this.suggestionPopup = new SuggestionPopup(this, Constants.engineAsStaticReference, errorText);
				this.suggestionPopup.show(this.statusTextField, (int) event.getX(), (int) event.getY());

			} else if (errorText.contains("Invalid OWL individual name")) {
				// namedindividual
				this.suggestionPopup = new SuggestionPopup(this, Constants.engineAsStaticReference, errorText);
				this.suggestionPopup.show(this.statusTextField, (int) event.getX(), (int) event.getY());

			} else if (errorText.contains("invalid datatype name")) {
				// datatype
				this.suggestionPopup = new SuggestionPopup(this, Constants.engineAsStaticReference, errorText);
				this.suggestionPopup.show(this.statusTextField, (int) event.getX(), (int) event.getY());

			} else {
				// there is no error
				// popup will not be shown
			}
		}
	}

	/**
	 * Not working properly To.Do Future implementation
	 */
	private void showColor(String errorText) {
		// String atom = getAtom(errorText);
		// if (atom.length() > 0) {
		// String ruleText = getRuleText();
		//
		// int firstIndex = ruleText.indexOf(atom);
		// int lastIndex = firstIndex + atom.length();
		//
		// styledDoc.setCharacterAttributes(firstIndex, lastIndex, setUnderLine,
		// true);
		// styledDoc.setCharacterAttributes(firstIndex, lastIndex, redColor,
		// false);
		//
		// //System.out.println("color set at : " + firstIndex + "\t " +
		// lastIndex + "\t of " + atom);
		//
		// } else {
		// //System.out.println("not invalid. means incomplete state");
		// removeColor();
		// }

	}

	/**
	 * Not working properly To DO. Future implementation
	 */
	private void removeColor() {
		// styledDoc.setCharacterAttributes(0, styledDoc.getLength(),
		// clearUnderLine, true);
		// styledDoc.setCharacterAttributes(0, styledDoc.getLength(),
		// blackColor, true);
		// //System.out.println("cleared color");
	}

	private class RuleTextPaneMouseAdapter extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			if (SwingUtilities.isRightMouseButton(e)) {
				showSuggestionPopup(e);
			}
			e.consume();
		}
	}

	private class StatusTextFieldMouseMotionAdapter extends MouseMotionAdapter {
		@Override
		public void mouseMoved(MouseEvent event) {
			event.consume();
		}
	}

	private class SWRLRuleEditorKeyAdapter extends KeyAdapter {
		@Override
		public void keyPressed(@NonNull KeyEvent event) {
			int code = event.getKeyCode();
			if ((code == KeyEvent.VK_TAB) || (code == KeyEvent.VK_SPACE && event.isControlDown())) {
				autoComplete();
				event.consume();
			} else if (code == KeyEvent.VK_ESCAPE) {
				cancelAutoCompleteIfNecessary();
				event.consume();
			} else if (code == KeyEvent.VK_DELETE) {
				cancelAutoCompleteIfNecessary();
			} else { // Any other key will disable auto-complete mode if it is
						// active
				disableAutoCompleteModeIfNecessary();
				super.keyPressed(event);
			}
		}

		@Override
		public void keyReleased(@NonNull KeyEvent event) {
			updateStatus();

		}
	}

	private class CancelSWRLRuleEditActionListener implements ActionListener {

		public CancelSWRLRuleEditActionListener() {

		}

		@Override
		public void actionPerformed(@NonNull ActionEvent e) {

			clearIfOk();
		}
	}

	private JPanel getPnlForSwitchToSWRLTab(String message) {
		JPanel pnl = new JPanel();
		if (pnlForCreateNewEntity == null) {
			pnlForCreateNewEntity = new JPanel();
		}

		pnlForCreateNewEntity.setLayout(new BorderLayout());
		lbl1.setText("<html><p><b><font color=\"red\">" + message + "</font></b>"
				+ " can not be transformed to OWL Axiom." + "</p></html>");
		lbl2.setText("<html><br>Do you want to switch to SWRLTab ?</html>");
		pnlForCreateNewEntity.add(lbl1, BorderLayout.PAGE_START);
		pnlForCreateNewEntity.add(lbl2, BorderLayout.PAGE_END);
		return pnlForCreateNewEntity;
	}

	private SWRLRule getSWRLRule(String ruleText) throws SWRLParseException {
		SWRLParser parser = createSWRLParser();

		Optional<@NonNull SWRLRule> rule = null;

		rule = parser.parseSWRLRule(ruleText, false, getRuleName(), getComment());

		if (rule.isPresent()) {

			return rule.get();
		}

		return null;
	}

	Set<OWLAxiom> generatedAxioms = new HashSet<OWLAxiom>();

	/**
	 * @return the generatedAxioms
	 */
	public Set<OWLAxiom> getGeneratedAxioms() {
		return generatedAxioms;
	}

	/**
	 * @param generatedAxioms
	 *            the generatedAxioms to set
	 */
	public void setGeneratedAxioms(Set<OWLAxiom> generatedAxioms) {
		this.generatedAxioms = generatedAxioms;
	}

	private void applyChangetoOntology(Set<OWLAxiom> owlAxioms) {

		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();

		for (OWLAxiom axiom : owlAxioms) {
			AddAxiom addaxiom = new AddAxiom(Constants.activeOntologyAsStaticReference, axiom);
			changes.add(addaxiom);
		}

		ChangeApplied changeResult = Constants.owlOntologyManagerAsStaticReference.applyChanges(changes);
		if (changeResult == ChangeApplied.SUCCESSFULLY) {
			this.statusTextField.setText("Selected axioms integrated with protege successfully.");
		} else if (changeResult == ChangeApplied.UNSUCCESSFULLY) {
			this.statusTextField.setText("Axiom integration with Protege unsuccessfull.");
		} else if (changeResult == ChangeApplied.NO_OPERATION) {
			this.statusTextField
					.setText("Selected axioms are duplicate. No operation carried out (change had no effect)");
		}
	}

	private OWLAnnotation getOWLAnnotation() {

		OWLAnnotationProperty fixedAnnotationProperty;
		fixedAnnotationProperty = Constants.owlDataFactoryAsStaticReference.getOWLAnnotationProperty(
				Constants.FIXED_ANNOTATION_NAME,
				PrefixUtilities.getPrefixOWLOntologyFormat(Constants.activeOntologyAsStaticReference));

		String value;

		if (newlyCreatedObjectProperitesList != null && newlyCreatedObjectProperitesList.size() > 0) {
			value = getRuleName() + "___" + getRuleText() + "___" + getComment();
			for (OWLObjectProperty owlObjectProperty : newlyCreatedObjectProperitesList) {
				value = value + "___" + owlObjectProperty.getIRI();
				// System.out.println("value: "+ value);
			}
		} else {
			value = getRuleName() + "___" + getRuleText() + "___" + getComment();
		}

		OWLAnnotationValue owlLiteral = Constants.owlDataFactoryAsStaticReference.getOWLLiteral(value);
		OWLAnnotation annotation = Constants.owlDataFactoryAsStaticReference.getOWLAnnotation(fixedAnnotationProperty,
				owlLiteral);

		return annotation;
	}

	public OWLAxiom addAxiomAnnotation(OWLAxiom axiom, OWLAnnotation annotation) {

		Set<OWLAnnotation> newAnnotations = new HashSet<OWLAnnotation>();

		newAnnotations.add(annotation);

		OWLAxiom annotatedAxiom = axiom.getAnnotatedAxiom(newAnnotations);

		return annotatedAxiom;
	}

	private void createListOfNewlyCreatedObjectProperties(Set<OWLObjectProperty> objectProperitesBeforeConverting,
			Set<OWLObjectProperty> objectProperitesAfterConverting) {

		this.newlyCreatedObjectProperitesList = new HashSet<OWLObjectProperty>();

		for (OWLObjectProperty owlObjectProperty : objectProperitesAfterConverting) {
			if (!objectProperitesBeforeConverting.contains(owlObjectProperty)) {
				this.newlyCreatedObjectProperitesList.add(owlObjectProperty);
			}
		}
	}

	private void createListOfNewlyCreatedButNotUsedObjectProperites(
			Set<OWLObjectProperty> objectProperitesAfterConverting,
			Set<OWLObjectProperty> newlyCreatedObjectProperitesList,
			Set<OWLObjectProperty> objectProperitesAfterAxiomSelection) {

		this.newlyCreatedButNotUsedObjectProperitesList = new HashSet<OWLObjectProperty>();

		for (OWLObjectProperty owlObjectProperty : objectProperitesAfterConverting) {
			if (newlyCreatedObjectProperitesList.contains(owlObjectProperty)) {
				if (!objectProperitesAfterAxiomSelection.contains(owlObjectProperty)) {
					// System.out.println("owlObjectProperty: " +
					// owlObjectProperty);
					this.newlyCreatedButNotUsedObjectProperitesList.add(owlObjectProperty);
				}
			}
		}
	}

	public void showAxiomsDialog(Set<OWLObjectProperty> objectProperitesBeforeConverting,
			Set<OWLObjectProperty> objectProperitesAfterConverting) {

		/**
		 * creates a list of newly created OWLObjectProperties
		 */
		createListOfNewlyCreatedObjectProperties(objectProperitesBeforeConverting, objectProperitesAfterConverting);

		Set<OWLObjectProperty> objectProperitesAfterAxiomSelection = new HashSet<OWLObjectProperty>();

		JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);

		// show the dialog
		AxiomsDialog axiomDialog = new AxiomsDialog(this, topFrame, Constants.activeOntologyAsStaticReference);

		/**
		 * create axioms with annotation from the generated axiom annotation:-
		 * ruleName___ruleText___ruleComment
		 */
		Set<OWLAxiom> axiomWithAnnotations = new HashSet<OWLAxiom>();

		if (axiomDialog.isClickedOK()) {

			Set<OWLAxiom> selectedAxioms = getGeneratedAxioms();

			if (!selectedAxioms.isEmpty()) {

				/// create list of newly created obj-properties from selected
				/// axiom
				for (OWLAxiom owlAxiom : selectedAxioms) {
					objectProperitesAfterAxiomSelection.addAll(owlAxiom.getObjectPropertiesInSignature());
				}

				createListOfNewlyCreatedButNotUsedObjectProperites(objectProperitesAfterConverting,
						this.newlyCreatedObjectProperitesList, objectProperitesAfterAxiomSelection);

				for (OWLAxiom axiom : selectedAxioms) {
					axiomWithAnnotations.add(addAxiomAnnotation(axiom, getOWLAnnotation()));
				}

				// if the ruleID exist then remove the corresponding axioms
				// first
				// this is implemented instead of replace Rule
				Constants.engineAsStaticReference.deleteRule(getRuleName());

				// save changes in the ontology.
				applyChangetoOntology(axiomWithAnnotations);

				// save axioms into axiomsID
				// necessary when ontology is not saved or reloaded but new rule
				// is inserted
				Constants.engineAsStaticReference.addAxiomsWithID(getRuleName(), axiomWithAnnotations);

				// show the rule in the table
				RuleModel rule = new RuleModel(getRuleName(), getRuleText(), getComment());
				Constants.engineAsStaticReference.addARulesWithID(getRuleName(), rule);
				getRuleTableModel().updateView();

				// clear the rule textarea, and set autogenerated ruleName
				setApplyChangeStatus();
				setInitialRuleName();

				// delete those objectproperties which is not used
				if (newlyCreatedButNotUsedObjectProperitesList != null
						&& newlyCreatedButNotUsedObjectProperitesList.size() > 0) {
					deleteNewlyCreatedButNotUsedObjProps();
				}

			} else {
				// try to delete the newly created opjectproperties
				if (newlyCreatedObjectProperitesList != null && newlyCreatedObjectProperitesList.size() > 0) {
					deleteNewlyCreatedObjProps();
				}
			}
		} else {
			// try to delete the newly created opjectproperties
			if (newlyCreatedObjectProperitesList != null && newlyCreatedObjectProperitesList.size() > 0) {
				deleteNewlyCreatedObjProps();
			}
		}
	}

	private void deleteNewlyCreatedObjProps() {

		if (newlyCreatedObjectProperitesList != null && newlyCreatedObjectProperitesList.size() > 0) {
			Constants.engineAsStaticReference.deleteNewlyCreatedObjProperties(newlyCreatedObjectProperitesList);
		}
	}

	private void deleteNewlyCreatedButNotUsedObjProps() {
		// selection axiom can be lower than generated axioms
		if (newlyCreatedButNotUsedObjectProperitesList != null
				&& newlyCreatedButNotUsedObjectProperitesList.size() > 0) {
			Constants.engineAsStaticReference
					.deleteNewlyCreatedObjProperties(newlyCreatedButNotUsedObjectProperitesList);
		}
	}

	private void setApplyChangeStatus() {
		this.ruleNameTextField.setText("");
		this.ruleNameTextField.setCaretPosition(this.ruleNameTextField.getText().length());
		this.ruleTextTextPane.setText("");
		this.commentTextField.setText("");

	}

	private void switchToSWRLTab(String ruleName, String ruleText, String ruleComment, boolean ExceptionOccurred) {

		if (ExceptionOccurred) {

		} else {
			if (JOptionPane.OK_OPTION == JOptionPane.showOptionDialog(this, getPnlForSwitchToSWRLTab(ruleText),
					CANT_CONVERT_TO_OWL_AXIOM_TEXT, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
					null, null)) {
				// switch to swrltab;
				tabbedPane.setSelectedIndex(1);
				SWRLRuleEditorDialog dialog = (SWRLRuleEditorDialog) this.dialogManager.getSWRLRuleEditorDialog(this,
						ruleName, ruleText, ruleComment);
				dialog.setVisible(true);
			}
		}

	}

	private Engine getEngine() {

		return Constants.engineAsStaticReference;
	}

	private class ConvertSWRLRuleActionListener implements ActionListener {

		@NonNull
		Component parent;

		public ConvertSWRLRuleActionListener(Component parent) {
			this.parent = parent;
		}

		@Override
		public void actionPerformed(@NonNull ActionEvent e) {

			String ruleName = getRuleName();
			String ruleText = getRuleText();
			String comment = getComment();
			boolean errorOccurred;

			if (ruleName.trim().length() == 0) {
				getDialogManager().showErrorMessageDialog(this.parent, MISSING_RULE_NAME, MISSING_RULE_NAME_TITLE);
				errorOccurred = true;
			} else if (ruleText.trim().length() == 0) {
				getDialogManager().showErrorMessageDialog(this.parent, MISSING_RULE, MISSING_RULE);
				errorOccurred = true;
			} else if (getRuleTableModel().hasRule(ruleName) && !RuleEditorPanel.this.editMode) {
				getDialogManager().showErrorMessageDialog(this.parent, DUPLICATE_RULE_TEXT, DUPLICATE_RULE_TITLE);
				errorOccurred = true;
			} else {
				try {

					SWRLRule swrlRule = getSWRLRule(ruleText);
					if (swrlRule != null) {

						Set<OWLObjectProperty> objectProperitesBeforeConverting = swrlRule
								.getObjectPropertiesInSignature();
						Set<OWLObjectProperty> objectProperitesAfterConverting = new HashSet<OWLObjectProperty>();

						// try to convert rule to OWL
						Translator translator = null;
						try {
							translator = new Translator(swrlRule, getEngine());
							translator.ruleToAxioms();
						} catch (Exception exception) {
							System.out.println("exception occurred when transferring to axioms");
							switchToSWRLTab(ruleName, ruleText, comment, true);
							// exception.printStackTrace();
						}
						if (!translator.resultingAxioms.isEmpty()) {

							generatedAxioms.clear();
							generatedAxioms.addAll(translator.resultingAxioms);
							for (OWLAxiom owlAxiom : translator.resultingAxioms) {
								objectProperitesAfterConverting.addAll(owlAxiom.getObjectPropertiesInSignature());
							}
							// show axioms dialog and take decisons
							showAxiomsDialog(objectProperitesBeforeConverting, objectProperitesAfterConverting);

						} else {
							// can not transfer to axioms need to switch
							// to swrltab

							switchToSWRLTab(ruleName, ruleText, comment, false);
							System.out.println();
							log.info("can not transfer to axioms");
						}
						return;
					}

					errorOccurred = false;

				} catch (SWRLParseException pe) {
					getDialogManager().showErrorMessageDialog(this.parent,
							(pe.getMessage() != null ? pe.getMessage() : ""), INVALID_RULE_TITLE);
					errorOccurred = true;
				} catch (RuntimeException pe) {
					getDialogManager().showErrorMessageDialog(this.parent,
							(pe.getMessage() != null ? pe.getMessage() : ""), INTERNAL_ERROR_TITLE);
					errorOccurred = true;
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					pe.printStackTrace(pw);
					log.warn(sw.toString());
				}

				if (!errorOccurred) {
					cancelEditMode();
				} else
					updateStatus();
			}
		}
	}

	// Only for test purpose
	// public static void main(String[] arg) {
	// JFrame frame = new JFrame();
	// frame.add(new RuleEditorPanel());
	// frame.setSize(500, 500);
	// frame.setVisible(true);
	// }

}
