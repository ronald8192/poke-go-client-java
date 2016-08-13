package com.ronald8192.pokemon;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends JFrame {

	private static final long serialVersionUID = 8742922210164223685L;

	private static Logger log = LoggerFactory.getLogger(App.class);

	private static PokeFetcher poke = null;
	private static String googleAuthUrl = "";

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				poke = new PokeFetcher();
				new App().createView().initListeners().setVisible(true);
			}
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
	private static JButton btnOpenAuth, btnDownload;
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

		btnOpenAuth = new JButton("Auth with Google");
		mainPanel.add(btnOpenAuth);

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
		btnOpenAuth.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
//				log.debug(googleAuthUrl);
				
				btnOpenAuth.setEnabled(false);
				btnOpenAuth.setText("Getting Google OAuth link...");
				new Thread(new Runnable() {
					@Override
					public void run() {
						googleAuthUrl = poke.getLoginToken();
						openWebPage(googleAuthUrl);
						btnOpenAuth.setEnabled(true);
						btnOpenAuth.setText("Auth with Google");
					}
				}).start();
				
				
			}
		});

		btnDownload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				btnDownload.setEnabled(false);
				btnDownload.setText("Downloading...");

				new Thread(new Runnable() {

					@Override
					public void run() {
						String token = txtAuthToken.getText();
						EDownloadStatus err = null;
						if (poke.isLoggedIn()) {
							err = poke.download();
						} else {
							if (token.trim().equals("")) {
								JOptionPane.showMessageDialog(mainPanel, "Please authenticate with Google first.");
							} else {
								try {
									poke.login(token);
									err = poke.download();
								} catch (Exception er) {
									log.error(er.getMessage());
									err = EDownloadStatus.AUTH_ERROR;
								}
								
							}
						}
						if(err != null){
							JOptionPane.showMessageDialog(mainPanel, err.say());
						}
						btnDownload.setText("Download");
						new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									Thread.sleep(3000L);
								} catch (InterruptedException e) {
								}finally{
									btnDownload.setEnabled(true);
								}
							}
						}).start();
						
					}
				}).start();

			}
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
