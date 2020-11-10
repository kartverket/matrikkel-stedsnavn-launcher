package no.statkart.launcher.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class InputDialog {

    private Feil feil;
    private String versjon;
    private String tittel;
    private String melding;
    private boolean visTjener = true;
    private boolean visBrukerPassord = true;
    private boolean visHeap = true;
    private List<Parametre> tidligereParametre = new ArrayList<>();
    private final Parametre fallback = new Parametre();
    private Parametre forrige;

    private JComboBox<String> inputTjener;
    private JTextField inputBrukernavn;
    private JPasswordField inputPassord;
    private JTextField inputHeap;

    InputDialog medTittel(String tittel) {
        this.tittel = tittel;
        return this;
    }

    InputDialog medVersjon(String versjon) {
        this.versjon = versjon;
        return this;
    }

    InputDialog medMelding(String melding) {
        this.melding = melding;
        return this;
    }

    InputDialog visTjener(boolean visTjener) {
        this.visTjener = visTjener;
        return this;
    }

    InputDialog visBrukerPassord(boolean visBrukerPassord) {
        this.visBrukerPassord = visBrukerPassord;
        return this;
    }

    InputDialog visHeap(boolean visHeap) {
        this.visHeap = visHeap;
        return this;
    }

    InputDialog medTidligereFeil(Feil feil) {
        this.feil = feil;
        return this;
    }

    InputDialog medTidligereInputParametre(List<Parametre> listeAvParametre) {
        this.tidligereParametre = new ArrayList<>(listeAvParametre);
        return this;
    }

    Parametre innhentInputParametre() {
        JPanel panel = new JPanel(new GridBagLayout());
        int yPanel = 0;
        if (melding != null) {
            yPanel = leggTilMelding(panel, yPanel);
        }
        if (visTjener) {
            yPanel = leggTilTjener(panel, yPanel);
        }
        if (visBrukerPassord) {
            yPanel = leggTilBrukernavnOgPassord(panel, yPanel);
        }
        if (trengerAvansert()) {
            yPanel = leggTilAvansert(panel, yPanel);
        }
        leggTilFeil(panel, yPanel);
        if (dialogOK(panel)) {
            return aktuelleInputParametre();
        }
        return null;
    }

    private int leggTilMelding(JPanel panel, int y) {
        JLabel labelMelding = new JLabel(melding, SwingConstants.LEFT);
        panel.add(labelMelding, medPosisjon(0, y, 1, GridBagConstraints.REMAINDER,
                new Insets(0, 2, 10, 2)));
        return y + 1;
    }

    private int leggTilTjener(JPanel panel, int y) {
        JLabel labelTjener = new JLabel("Tjener", SwingConstants.RIGHT);
        panel.add(labelTjener, medPosisjon(0, y, 0, 1));
        panel.add(opprettInputTjener(), medPosisjon(1, y, 1, 2));
        return y + 1;
    }

    private int leggTilBrukernavnOgPassord(JPanel panel, int y) {
        JLabel labelBrukernavn = new JLabel("Brukernavn", SwingConstants.RIGHT);
        panel.add(labelBrukernavn, medPosisjon(0, y, 0, 1));
        panel.add(opprettInputBrukernavn(), medPosisjon(1, y, 1, 2));
        JLabel labelPassord = new JLabel("Passord", SwingConstants.RIGHT);
        panel.add(labelPassord, medPosisjon(0, y + 1, 0, 1));
        panel.add(opprettInputPassord(), medPosisjon(1, y + 1, 1, 2));
        return y + 2;
    }

    private void leggTilHeap(JPanel panel, int y) {
        JLabel labelHeap = new JLabel("Tildelt minne", SwingConstants.RIGHT);
        panel.add(labelHeap, medPosisjon(0, y, 0, 1));
        JTextField inputHeap = opprettInputHeap();
        panel.add(inputHeap, medPosisjon(1, y, 1, GridBagConstraints.RELATIVE));
        JLabel labelHeapUnit = new JLabel("MB");
        panel.add(labelHeapUnit, medPosisjon(2, y, 0, 1));
        // return y + 1;
    }

    private void leggTilFeil(JPanel panel, int y) {
        if (feil != null) {
            JLabel labelFeilmelding = new JLabel(feil.tilFeilmelding());
            labelFeilmelding.setForeground(responsfarge());
            panel.add(labelFeilmelding, medPosisjon(0, y, 1, GridBagConstraints.REMAINDER,
                    new Insets(10, 2, 2, 2)));
        }
        // return y + 1;
    }

    private boolean trengerAvansert() {
        return visHeap;
    }

    private Color responsfarge() {
        return feil.erRedFlag() ? Color.RED : Color.BLACK;
    }

    private int leggTilAvansert(JPanel panel, int y) {
        JPanel avansert = new JPanel(new GridBagLayout());
        avansert.setBorder(new EmptyBorder(0, 10, 0, 0));
        avansert.setVisible(feil != null && feil.erHeapFeil());
        JButton switcher = opprettAvansertPanelSwitcher(avansert);
        panel.add(switcher, medPosisjon(0, y, 1, 3,
                new Insets(10, 2, 2, 2)));
        int yAvansert = 0;
        if (visHeap) {
            leggTilHeap(avansert, yAvansert);
        }
        panel.add(avansert, medPosisjon(0, y + 1, 1, GridBagConstraints.REMAINDER));
        return y + 2;
    }

    private JButton opprettAvansertPanelSwitcher(JPanel panelToSwitch) {
        Color linkColor = new Color(0x27, 0x71, 0xbb);
        JButton switcher = new JButton(switcherLabel(panelToSwitch));
        switcher.setBorderPainted(false);
        switcher.setContentAreaFilled(false);
        switcher.setForeground(linkColor);
        switcher.setHorizontalAlignment(SwingConstants.LEFT);
        switcher.getModel().addChangeListener(e -> {
            ButtonModel model = (ButtonModel) e.getSource();
            if (model.isRollover()) {
                switcher.setForeground(linkColor.brighter());
            } else {
                switcher.setForeground(linkColor);
            }
        });
        switcher.addActionListener(e -> {
            panelToSwitch.setVisible(!panelToSwitch.isVisible());
            switcher.setText(switcherLabel(panelToSwitch));
            ((Window) panelToSwitch.getRootPane().getParent()).pack();
        });
        return switcher;
    }

    private String switcherLabel(JPanel panelToSwitch) {
        return panelToSwitch.isVisible() ? "\u25bc Avansert" : "\u25b6 Avansert";
    }

    private JComboBox<String> opprettInputTjener() {
        inputTjener = new JComboBox<>(finnTjenere());
        if (feil != null) {
            inputTjener.setSelectedItem(feil.getInputParametre().getTjener());
        }
        forrige = aktuelleInputParametre();
        inputTjener.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                endretTjener();
                forrige = aktuelleInputParametre();
            }
        });
        inputTjener.setEditable(true);
        inputTjener.setPrototypeDisplayValue("https://asdf.asdf.asdf.asdf:1234/asdfasdf/");
        return inputTjener;
    }

    private JTextField opprettInputBrukernavn() {
        inputBrukernavn = new JTextField();
        inputBrukernavn.setText(aktuelleInputParametre().getBrukernavn());
        inputBrukernavn.getDocument().addDocumentListener((SimpleDocumentListener) e -> endretBrukernavn());
        return inputBrukernavn;
    }

    private JPasswordField opprettInputPassord() {
        inputPassord = new JPasswordField();
        inputPassord.getDocument().addDocumentListener((SimpleDocumentListener) e -> endretPassord());
        return inputPassord;
    }

    private JTextField opprettInputHeap() {
        inputHeap = new JTextField();
        inputHeap.setText(aktuelleInputParametre().getHeap());
        inputHeap.getDocument().addDocumentListener((SimpleDocumentListener) e -> endretHeap());
        return inputHeap;
    }

    private String[] finnTjenere() {
        List<String> resultat = new ArrayList<>();
        for (Parametre param : tidligereParametre) {
            resultat.add(param.getTjener());
        }
        if (feil != null) {
            if (!resultat.contains(feil.getInputParametre().getTjener())) {
                resultat.add(0, feil.getInputParametre().getTjener());
            }
        }
        return resultat.toArray(new String[0]);
    }

    private Parametre aktuelleInputParametre() {
        if (inputTjener == null) {
            if (tidligereParametre.isEmpty()) {
                return fallback;
            }
            return tidligereParametre.get(0);
        }
        int idx = inputTjener.getSelectedIndex();
        if (idx < 0) {
            return fallback;
        }
        if (tidligereParametre.size() < inputTjener.getItemCount()) {
            if (idx == 0) {
                return feil == null ? fallback : feil.getInputParametre();
            }
            return tidligereParametre.get(idx - 1);
        }
        return tidligereParametre.get(idx);
    }

    private void endretTjener() {
        if (inputTjener.getSelectedIndex() < 0) {
            fallback.medTjener((String) inputTjener.getSelectedItem())
                    .medBrukernavn(forrige.getBrukernavn())
                    .medPassord(forrige.getPassord())
                    .medHeap(forrige.getHeap());
        }
        Parametre param = aktuelleInputParametre();
        if (inputBrukernavn != null) {
            inputBrukernavn.setText(param.getBrukernavn());
        }
        if (inputHeap != null) {
            inputHeap.setText(param.getHeap());
        }
    }

    private void endretBrukernavn() {
        aktuelleInputParametre().medBrukernavn(inputBrukernavn.getText());
    }

    private void endretPassord() {
        aktuelleInputParametre().medPassord(inputPassord.getPassword());
    }

    private void endretHeap() {
        aktuelleInputParametre().medHeap(inputHeap.getText());
    }

    private boolean dialogOK(Object message) throws HeadlessException {
        String tittelOgVersjon = versjon == null
                ? tittel + " (uversjonert)"
                : tittel + " " + versjon;
        Path iconPath = Paths.get(Work.SOURCE).resolve("login.png");
        if (!Files.exists(iconPath)) {
            throw new IllegalStateException("Finner ikke 'login.png' i work-katalogen");
        }
        ImageIcon icon = new ImageIcon(iconPath.toString());
        JOptionPane pane = new JOptionPane(
                message,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_CANCEL_OPTION,
                icon,
                new Object[]{"OK", "Avbryt"},
                "OK"
        );
        pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
        pane.selectInitialValue();
        JDialog dialog = pane.createDialog(null, tittelOgVersjon);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                setFokus();
            }
        });
        dialog.setVisible(true);
        dialog.dispose();
        Object selectedValue = pane.getValue();
        return "OK".equals(selectedValue);
    }

    private GridBagConstraints medPosisjon(int gridx, int gridy, int weightx, int gridwidth) {
        return medPosisjon(gridx, gridy, weightx, gridwidth, new Insets(0, 2, 2, 2));
    }

    private GridBagConstraints medPosisjon(int gridx, int gridy, int weightx, int gridwidth, Insets insets) {
        GridBagConstraints gbc = new GridBagConstraints(
                gridx, gridy, gridwidth, 1, weightx, 0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                insets, 0, 0
        );
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private void setFokus() {
        Feil f = feil == null ? aktuelleInputParametre().tilFeil() : feil;
        if (f == null) {
            fokusBrukernavn();
        } else if (f.erTjenerfeil()) {
            fokusTjener();
        } else if (f.erBrukerPassordFeil()) {
            fokusBrukernavn();
        } else if (f.erHeapFeil()) {
            fokusHeap();
        }
    }

    private void fokusTjener() {
        if (inputTjener != null) {
            SwingUtilities.invokeLater(() -> {
                inputTjener.requestFocusInWindow();
                ((JTextField) inputTjener.getEditor().getEditorComponent()).selectAll();
            });
        }
    }

    private void fokusBrukernavn() {
        if (inputBrukernavn != null) {
            SwingUtilities.invokeLater(() -> {
                inputBrukernavn.requestFocusInWindow();
                inputBrukernavn.selectAll();
            });
        }
    }

    private void fokusHeap() {
        if (inputHeap != null) {
            SwingUtilities.invokeLater(() -> {
                inputHeap.requestFocusInWindow();
                inputHeap.selectAll();
            });
        }
    }

    @FunctionalInterface
    interface SimpleDocumentListener extends DocumentListener {
        void update(DocumentEvent e);

        @Override
        default void insertUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        default void removeUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        default void changedUpdate(DocumentEvent e) {
            update(e);
        }
    }

}