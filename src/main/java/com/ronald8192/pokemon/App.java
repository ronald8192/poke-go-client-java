package com.ronald8192.pokemon;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO: use javafx
public class App extends JFrame {

	private static final long serialVersionUID = 8742922210164223685L;

	private static Logger log = LoggerFactory.getLogger(App.class);

	private static PokeFetcher poke = null;
	private static String googleAuthUrl = "";

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				poke = new PokeFetcher();
			} catch (LoginFailedException e) {
				e.printStackTrace();
			} catch (RemoteServerException e) {
				e.printStackTrace();
			}
			new App().createView().initListeners().setVisible(true);
		});
		Runtime.getRuntime().addShutdownHook(new Thread("shutdown") {
			@Override
			public void run() {
				log.trace("shutting down...");
				poke = null;
				System.gc();
			}
		});

	}

	private static JPanel mainPanel;
	private static GridBagConstraints c;
	private static String uiTitle = "PokeDetails";
	private static JLabel lblGoogleAuth;
	private static JButton btnAuth, btnDownload;
	private static JTextField txtAuthToken;

	private void prepareView() {
		mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		getContentPane().add(mainPanel);
		c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
	}

	private App createView() {
		prepareView();


		btnAuth = new JButton();
		if(poke.isLoggedIn()) {
			String playerName = null;
			try {
				playerName = poke.getPlayerName();
			} catch (LoginFailedException | RemoteServerException e) {
				e.printStackTrace();
			}
			btnAuth.setText("Sign off " + ( playerName == null ? "" : ("(Signed in as " + playerName + ")")));
		}else{
			btnAuth.setText("Auth With Google");
		}
		mainPanel.add(btnAuth);


		c.gridy++;
		c.gridy++;
		lblGoogleAuth = new JLabel("Paste your token below and click download.");
		mainPanel.add(lblGoogleAuth, c);

		c.gridx = 0;
		c.gridy++;
		txtAuthToken = new JTextField(28);
		mainPanel.add(txtAuthToken, c);

		c.gridx = 0;
		c.gridy++;
		btnDownload = new JButton("Download");
		btnDownload.setEnabled(poke.isLoggedIn());
		mainPanel.add(btnDownload, c);

		afterCreateView();
		return this;
	}

	private void afterCreateView() {
		setTitle(uiTitle);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		pack();
		setLocationRelativeTo(null);
	}

	private App initListeners() {
		btnAuth.addActionListener((e) -> {
			btnAuth.setEnabled(false);
			if (poke.isLoggedIn()) {
				log.trace("sign off");
				poke.logout();
				btnAuth.setText("Auth with Google");
				btnAuth.setEnabled(true);
				btnDownload.setEnabled(false);
			} else {
				btnAuth.setText("Getting Google OAuth link...");
				new Thread(() -> {
					try {
						googleAuthUrl = poke.getLoginToken();
					} catch (LoginFailedException e1) {
						e1.printStackTrace();
					} catch (RemoteServerException e1) {
						e1.printStackTrace();
					}
					openWebPage(googleAuthUrl);
					btnAuth.setEnabled(true);
					btnAuth.setText("Auth with Google");
					btnDownload.setEnabled(true);
				}).start();

			}
		});

		btnDownload.addActionListener((e) -> {
			btnDownload.setEnabled(false);

			new Thread(() -> {
				String token = txtAuthToken.getText();
				EDownloadStatus err = null;
				log.trace("logged in:" + poke.isLoggedIn());
				if (poke.isLoggedIn()) {
					//logged in
					btnDownload.setText("Downloading...");
					err = poke.download();
					btnDownload.setText("Update");
				} else if (token.trim().equals("")) {
					//not logged in, token box empty
					btnDownload.setText("Download");
					JOptionPane.showMessageDialog(mainPanel, "Please authenticate with Google first.");
				} else {
					//not logged in, token box not empty
					try {
						btnDownload.setText("Logging in...");
						btnAuth.setEnabled(false);
						poke.login(token);
						btnAuth.setText("Signed in");
						btnDownload.setText("Downloading...");
						err = poke.download();
						btnDownload.setText("Update");
					} catch (Exception er) {
						log.error(er.getMessage());
						err = EDownloadStatus.AUTH_ERROR;
						btnDownload.setText("Download");
					}
				}
				if (err != null) {
					JOptionPane.showMessageDialog(mainPanel, err.say());
				}

				new Thread(() -> {
					try {
						Thread.sleep(3000L);
					} catch (InterruptedException ie) {
					} finally {
						btnDownload.setEnabled(poke.isLoggedIn());
					}
				}).start();

				if(poke.isLoggedIn()){
					txtAuthToken.setText("");
					String playerName = null;
					try {
						playerName = poke.getPlayerName();
					} catch (LoginFailedException | RemoteServerException e1) {
						e1.printStackTrace();
					}
					btnAuth.setText("Sign off " + ( playerName == null ? "" : ("(Signed in as " + playerName + ")")));
				}
				btnAuth.setEnabled(true);
			}).start();

		});

		return this;
	}

	private static void openWebPage(String url) {
		try {
			java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
		} catch (java.io.IOException e) {
			log.error(e.getMessage());
		}
	}

}
