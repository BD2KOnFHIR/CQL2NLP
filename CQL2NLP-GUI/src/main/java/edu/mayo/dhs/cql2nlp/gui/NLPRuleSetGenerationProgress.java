package edu.mayo.dhs.cql2nlp.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import edu.mayo.hsr.dhs.cql2nlp.UMLSBrowser;
import edu.mayo.hsr.dhs.cql2nlp.ValueSetResolver;
import edu.mayo.hsr.dhs.cql2nlp.structs.CodifiedValueSetElement;
import edu.mayo.hsr.dhs.cql2nlp.structs.VSACCodeSystem;
import org.hl7.fhir.r4.model.ValueSet;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class NLPRuleSetGenerationProgress extends JDialog {

    public static Map<String, Set<String>> rulesets;

    private JPanel currentProgressText;
    private JButton abortButton;

    public NLPRuleSetGenerationProgress() {
        setContentPane(currentProgressText);
        setModal(true);

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // call onCancel() on ESCAPE
        currentProgressText.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        ValueSetResolver rls = new ValueSetResolver(GUI.utsUser, new String(GUI.utsPass));
        UMLSBrowser oet = new UMLSBrowser(GUI.utsUser, new String(GUI.utsPass));
        rulesets = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Set<String>> cuiSets = new ConcurrentHashMap<>();
        SwingWorker<Void, Object> worker = new SwingWorker<Void, Object>() {
            @Override
            protected Void doInBackground() {
                try {
                    CQLEditing.valueSetsToResolve.forEach((id, oid) -> {
                        try {
                            ValueSet vs = rls.getValueSetForOID(oid);
                            Set<CodifiedValueSetElement> codes = rls.resolveValueSetCodes(vs);
                            Set<String> displayNames = ConcurrentHashMap.newKeySet();
                            for (CodifiedValueSetElement element : codes) {
                                Set<CodifiedValueSetElement> vals = null;
                                try {
                                    vals = oet.getCUIsForValueSetElement(element, true);
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                    continue;
                                }
                                new ArrayList<>(vals).parallelStream().forEach(
                                        val -> {
                                            try {
                                                oet.getDisplayNamesForCUI(val.getCode()).stream().map(s -> val.getCode() + "|" + s).forEach(displayNames::add);
                                            } catch (JsonProcessingException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                );
                                cuiSets.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet())
                                        .addAll(vals.stream()
                                                .filter(c -> c.getCodeSystem().equals(VSACCodeSystem.UMLS))
                                                .map(CodifiedValueSetElement::getCode).collect(Collectors.toSet())
                                        );
                            }

                            rulesets.put(id, displayNames);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    });
                    ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
                    try {
                        ow.writeValue(new File("cui_mappings.json"), cuiSets);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    synchronized (GUI.nextPhaseFlag) {
                        GUI.nextPhaseFlag.set(true);
                        GUI.nextPhaseFlag.notifyAll();
                    }
                    dispose();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "An error occurred, please check your console");
                }
                return null;
            }

            // Can safely update the GUI from this method.
            protected void process(List<Object> chunks) {

            }
        };
        worker.execute();
        abortButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        currentProgressText = new JPanel();
        currentProgressText.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Generating NLP Rulesets for Specified Valuesets...");
        currentProgressText.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        currentProgressText.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        abortButton = new JButton();
        abortButton.setText("Abort");
        panel1.add(abortButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return currentProgressText;
    }

}
