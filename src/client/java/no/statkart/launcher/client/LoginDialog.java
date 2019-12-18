package no.statkart.launcher.client;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class LoginDialog {

    private Feil feil;
    private String versjon;
    private String tittel;
    private Map<String, LoginParametre> tidligereParametre;

    private JComboBox<String> inputTjener;
    private JTextField inputBrukernavn;
    private JPasswordField inputPassord;
    private JTextField inputHeap;

    // For testing
    public static void main(String... args) {
        new LoginDialog()
                .medTidligereLoginParametre(Arrays.asList(
                        new LoginParametre().medTjener("http://foo.com/").medBrukernavn("foonavn"),
                        new LoginParametre().medTjener("http://bar.com/"),
                        new LoginParametre().medTjener("http://zot.asdf.asdf.com/").medBrukernavn("zzz").medHeap("4000")
                ))
                //.medTidligereFeil(new Feil(new IOException("asdf"), new LoginParametre().medTjener("blah").medBrukernavn("ugh").medHeap("seriøst")))
                .innhentLoginParametre();
    }

    LoginDialog medTidligereFeil(Feil feil) {
        this.feil = feil;
        return this;
    }

    LoginDialog medTidligereLoginParametre(List<LoginParametre> listeAvParametre) {
        this.tidligereParametre = new LinkedHashMap<>();
        for (LoginParametre parametre : listeAvParametre) {
            tidligereParametre.put(parametre.getTjener(), parametre);
        }
        String defaultTjener = Konfigurasjon.get(Konfigurasjonsverdi.DEFAULT_SERVER);
        if (defaultTjener != null && !tidligereParametre.containsKey(defaultTjener)) {
            tidligereParametre.put(defaultTjener, new LoginParametre().medTjener(defaultTjener));
        }
        return this;
    }

    LoginDialog medVersjon(String versjon) {
        this.versjon = versjon;
        return this;
    }

    LoginDialog medTittel(String tittel) {
        this.tittel = tittel;
        return this;
    }

    GridBagConstraints medPosisjon(int gridx, int gridy, int weightx, int gridwidth) {
        GridBagConstraints gbc = new GridBagConstraints(
                gridx, gridy, gridwidth, 1, weightx, 0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2),
                0, 0
        );
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    Optional<LoginParametre> innhentLoginParametre() {
        JPanel panel = new JPanel(new GridBagLayout());

        JLabel labelTjener = new JLabel("Tjener", SwingConstants.RIGHT);
        panel.add(labelTjener, medPosisjon(0, 0, 0, 1));
        panel.add(opprettInputTjener(), medPosisjon(1, 0, 1, 2));

        JLabel labelBrukernavn = new JLabel("Brukernavn", SwingConstants.RIGHT);
        panel.add(labelBrukernavn, medPosisjon(0, 1, 0, 1));
        panel.add(opprettInputBrukernavn(), medPosisjon(1, 1, 1, 2));

        JLabel labelPassord = new JLabel("Passord", SwingConstants.RIGHT);
        panel.add(labelPassord, medPosisjon(0, 2, 0, 1));
        panel.add(opprettInputPassord(), medPosisjon(1, 2, 1, 2));

        JLabel labelHeap = new JLabel("Tildelt minne", SwingConstants.RIGHT);
        panel.add(labelHeap, medPosisjon(0, 3, 0, 1));
        labelHeap.setVisible(false);
        JTextField inputHeap = opprettInputHeap();
        panel.add(inputHeap, medPosisjon(1, 3, 1, GridBagConstraints.RELATIVE));
        inputHeap.setVisible(false);
        JLabel labelHeapUnit = new JLabel("MB");
        panel.add(labelHeapUnit, medPosisjon(2, 3, 0, 1));
        labelHeapUnit.setVisible(false);

        if (feil != null) {
            JLabel labelFeilmelding = new JLabel(feil.tilFeilmelding());
            labelFeilmelding.setForeground(Color.RED);
            panel.add(labelFeilmelding, medPosisjon(0, 4, 1, GridBagConstraints.REMAINDER));
        }

        JButton advanced = new JButton("Avansert ...");
        ActionListener advancedListener = e -> {
            labelHeap.setVisible(!labelHeap.isVisible());
            inputHeap.setVisible(!inputHeap.isVisible());
            labelHeapUnit.setVisible(!labelHeapUnit.isVisible());
        };

        String tittelOgVersjon = versjon == null
                ? tittel + " (uversjonert)"
                : tittel + " " + versjon;

        Path iconPath = Paths.get(Work.SOURCE).resolve("login.png");
        if (!Files.exists(iconPath)) {
            throw new IllegalStateException("Finner ikke 'login.png' i work-katalogen");
        }
        ImageIcon icon = new ImageIcon(iconPath.toString());
        if (dialogOK(panel, tittelOgVersjon, icon, advanced, advancedListener)) {
            return Optional.of(new LoginParametre()
                    .medTjener((String) inputTjener.getSelectedItem())
                    .medBrukernavn(inputBrukernavn.getText())
                    .medPassord(inputPassord.getPassword())
                    .medHeap(inputHeap.getText())
                    .medOppdatert(System.currentTimeMillis())
            );
        }
        return Optional.empty();
    }

    private JComboBox<String> opprettInputTjener() {
        inputTjener = new JComboBox<>(finnTjenere());
        if (feil != null) {
            inputTjener.setSelectedItem(feil.getLoginParametre().getTjener());
            if (feil.erTjenerfeil()) {
                inputTjener.addAncestorListener(new RequestFocusListener());
                inputTjener.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        SwingUtilities.invokeLater(() -> ((JTextField) e.getSource()).selectAll());
                    }
                });
            }
        }
        inputTjener.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                endretTjener();
            }
        });
        inputTjener.setEditable(true);
        inputTjener.setPrototypeDisplayValue("https://asdf.asdf.asdf.asdf:1234/asdfasdf/");
        return inputTjener;
    }

    private JTextField opprettInputBrukernavn() {
        inputBrukernavn = new JTextField();
        aktuelleLoginParametre().ifPresent(loginParametre ->
                inputBrukernavn.setText(loginParametre.getBrukernavn())
        );
        if (feil != null && feil.erBrukerPassordFeil()) {
            inputBrukernavn.addAncestorListener(new RequestFocusListener());
            inputBrukernavn.selectAll();
        }
        inputBrukernavn.getDocument().addDocumentListener((SimpleDocumentListener) e -> endretBrukernavn());
        return inputBrukernavn;
    }

    private JPasswordField opprettInputPassord() {
        inputPassord = new JPasswordField();
        if (feil == null) {
            aktuelleLoginParametre().ifPresent(loginParametre -> {
                String brukernavn = loginParametre.getBrukernavn();
                if (brukernavn != null && !brukernavn.isEmpty()) {
                    inputPassord.addAncestorListener(new RequestFocusListener());
                }
            });
        }
        inputPassord.getDocument().addDocumentListener((SimpleDocumentListener) e -> endretPassord());
        return inputPassord;
    }

    private JTextField opprettInputHeap() {
        inputHeap = new JTextField();
        aktuelleLoginParametre().ifPresent(loginParametre ->
                inputHeap.setText(loginParametre.getHeap())
        );
        inputHeap.getDocument().addDocumentListener((SimpleDocumentListener) e -> endretHeap());
        return inputHeap;
    }

    private String[] finnTjenere() {
        List<String> resultat = new ArrayList<>();
        if (tidligereParametre != null) {
            for (LoginParametre param : tidligereParametre.values()) {
                resultat.add(param.getTjener());
            }
        }
        if (feil != null) {
            if (!resultat.contains(feil.getLoginParametre().getTjener())) {
                resultat.add(0, feil.getLoginParametre().getTjener());
            }
        }
        return resultat.toArray(new String[0]);
    }

    private Optional<LoginParametre> aktuelleLoginParametre() {
        String verdi = (String) inputTjener.getSelectedItem();
        if (verdi == null) {
            return Optional.empty();
        }
        if (feil != null) {
            LoginParametre param = feil.getLoginParametre();
            if (verdi.equals(param.getTjener())) {
                return Optional.of(param);
            }
        }
        if (tidligereParametre != null) {
            LoginParametre param = tidligereParametre.get(verdi);
            if (param != null) {
                return Optional.of(param);
            }
        }
        return Optional.empty();
    }

    private void endretTjener() {
        aktuelleLoginParametre().ifPresent(loginParametre -> {
            inputBrukernavn.setText(loginParametre.getBrukernavn());
            inputHeap.setText(loginParametre.getHeap());
        });
    }

    private void endretBrukernavn() {
        aktuelleLoginParametre().ifPresent(param -> param.medBrukernavn(inputBrukernavn.getText()));
    }

    private void endretPassord() {
        aktuelleLoginParametre().ifPresent(param -> param.medPassord(inputPassord.getPassword()));
    }

    private void endretHeap() {
        aktuelleLoginParametre().ifPresent(param -> param.medHeap(inputHeap.getText()));
    }

    private static boolean dialogOK(Object message, String title, Icon icon, JButton advanced,
                                    ActionListener advancedListener) throws HeadlessException {
        JOptionPane pane = new JOptionPane(
                message,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_CANCEL_OPTION,
                icon,
                new Object[]{"OK", advanced, "Cancel"},
                "OK"
        );
        pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
        JDialog dialog = pane.createDialog(null, title);
        pane.selectInitialValue();
        advanced.addActionListener(e -> dialog.pack());
        advanced.addActionListener(advancedListener);
        dialog.setVisible(true);
        dialog.dispose();
        Object selectedValue = pane.getValue();
        return "OK".equals(selectedValue);
    }

    private static class RequestFocusListener implements AncestorListener {
        @Override
        public void ancestorAdded(AncestorEvent event) {
            SwingUtilities.invokeLater(() -> event.getComponent().requestFocusInWindow());
        }

        @Override
        public void ancestorRemoved(AncestorEvent event) {
        }

        @Override
        public void ancestorMoved(AncestorEvent event) {
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
