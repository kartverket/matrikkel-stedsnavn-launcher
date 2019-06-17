package no.statkart.launcher.client;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class LoginDialog {

   private Feil feil;
   private List<String> tjenere;
   private String versjon;
   private String tittel;

   LoginDialog medTidligereFeil(Feil feil) {
      this.feil = feil;
      return this;
   }

   LoginDialog medForslagTilTjenere(List<URL> tjenere) {
      this.tjenere = tjenere.stream().map(URL::toExternalForm).collect(Collectors.toList());
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

   Optional<Credentials> innhentTjenerOgBrukernavnOgPassord() {
      JPanel panel = new JPanel(new GridBagLayout());
      GridBagConstraints cs = new GridBagConstraints();
      cs.fill = GridBagConstraints.HORIZONTAL;
      cs.insets = new Insets(2, 2, 2, 2);

      JLabel serverLabel = new JLabel("Tjener", SwingConstants.RIGHT);
      cs.gridx = 0;
      cs.gridy = 0;
      cs.weightx = 0;
      cs.gridwidth = 1;
      panel.add(serverLabel, cs);

      JComboBox<String> serverInput = new JComboBox<>(finnTjenere());
      if (feil != null) {
         serverInput.setSelectedItem(feil.getCredentials().getServer());
         if (feil.erTjenerfeil()) {
            serverInput.addAncestorListener(new RequestFocusListener());
            serverInput.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
               @Override
               public void focusGained(FocusEvent e) {
                  SwingUtilities.invokeLater(() -> ((JTextField) e.getSource()).selectAll());
               }
            });
         }
      }
      serverInput.setEditable(true);
      serverInput.setPrototypeDisplayValue("https://asdf.asdf.asdf.asdf:1234/asdfasdf/");
      cs.gridx = 1;
      cs.gridy = 0;
      cs.weightx = 1;
      cs.gridwidth = 2;
      panel.add(serverInput, cs);

      JLabel userLabel = new JLabel("Brukernavn", SwingConstants.RIGHT);
      cs.gridx = 0;
      cs.gridy = 1;
      cs.weightx = 0;
      cs.gridwidth = 1;
      panel.add(userLabel, cs);

      JTextField userInput = new JTextField();
      if (feil == null) {
         userInput.addAncestorListener(new RequestFocusListener());
      } else {
         String s = feil.getCredentials().getUser();
         if (s != null && !s.isEmpty()) {
            userInput.setText(s);
         }
         if (feil.erBrukerPassordFeil()) {
            userInput.addAncestorListener(new RequestFocusListener());
            userInput.selectAll();
         }
      }
      cs.gridx = 1;
      cs.gridy = 1;
      cs.weightx = 1;
      cs.gridwidth = 2;
      panel.add(userInput, cs);

      JLabel passLabel = new JLabel("Passord", SwingConstants.RIGHT);
      cs.gridx = 0;
      cs.gridy = 2;
      cs.weightx = 0;
      cs.gridwidth = 1;
      panel.add(passLabel, cs);

      JPasswordField passInput = new JPasswordField();
      cs.gridx = 1;
      cs.gridy = 2;
      cs.weightx = 1;
      cs.gridwidth = 2;
      panel.add(passInput, cs);

      if (feil != null) {
         JLabel feilmeldingLabel = new JLabel(feil.tilFeilmelding(), SwingConstants.CENTER);
         feilmeldingLabel.setForeground(Color.RED);
         cs.gridx = 0;
         cs.gridy = 3;
         cs.weightx = 1;
         cs.gridwidth = 2;
         panel.add(feilmeldingLabel, cs);
      }

      String tittelOgVersjon = versjon == null
              ? tittel + " (uversjonert)"
              : tittel + " " + versjon;

      Path iconPath = Paths.get(Wrapper.WORK_SOURCE).resolve("login.png");
      if (!Files.exists(iconPath)) {
         throw new IllegalStateException("Finner ikke 'login.png' i work-katalogen");
      }
      ImageIcon icon = new ImageIcon(iconPath.toString());
      int resultat = JOptionPane.showConfirmDialog(
            null,
            panel,
            tittelOgVersjon,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            icon
      );
      if (resultat == JOptionPane.YES_OPTION) {
         return Optional.of(new Credentials(
               (String) serverInput.getSelectedItem(),
               userInput.getText(),
               new String(passInput.getPassword())
         ));
      }
      return Optional.empty();
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

   private String[] finnTjenere() {
      if (feil != null) {
         String forrigeTjener = feil.getCredentials().getServer();
         if (forrigeTjener != null && !forrigeTjener.isEmpty()) {
            List<String> tmp = new ArrayList<>(tjenere);
            if (!tmp.contains(forrigeTjener)) {
               tmp.add(0, forrigeTjener);
            }
            return tmp.toArray(new String[0]);
         }
      }
      return tjenere.toArray(new String[0]);
   }

}
