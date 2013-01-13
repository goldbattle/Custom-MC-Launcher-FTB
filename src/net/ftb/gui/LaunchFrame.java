/*
 * This file is part of FTB Launcher.
 *
 * Copyright © 2012-2013, FTB Launcher Contributors <https://github.com/Slowpoke101/FTBLaunch/>
 * FTB Launcher is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ftb.gui;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.ftb.data.LauncherStyle;
import net.ftb.data.LoginResponse;
import net.ftb.data.ModPack;
import net.ftb.data.Settings;
import net.ftb.data.TexturePack;
import net.ftb.data.UserManager;
import net.ftb.gui.dialogs.InstallDirectoryDialog;
import net.ftb.gui.dialogs.LauncherUpdateDialog;
import net.ftb.gui.dialogs.PasswordDialog;
import net.ftb.gui.dialogs.PlayOfflineDialog;
import net.ftb.gui.dialogs.ProfileAdderDialog;
import net.ftb.gui.dialogs.ProfileEditorDialog;
import net.ftb.gui.panes.ILauncherPane;
import net.ftb.gui.panes.ModpacksPane;
import net.ftb.gui.panes.OptionsPane;
import net.ftb.gui.panes.TexturepackPane;
import net.ftb.locale.I18N;
import net.ftb.locale.I18N.Locale;
import net.ftb.log.LogEntry;
import net.ftb.log.LogLevel;
import net.ftb.log.Logger;
import net.ftb.log.StreamLogger;
import net.ftb.mclauncher.MinecraftLauncher;
import net.ftb.tools.MinecraftVersionDetector;
import net.ftb.tools.ModManager;
import net.ftb.tools.ProcessMonitor;
import net.ftb.tools.TextureManager;
import net.ftb.updater.UpdateChecker;
import net.ftb.util.DownloadUtils;
import net.ftb.util.ErrorUtils;
import net.ftb.util.FileUtils;
import net.ftb.util.OSUtils;
import net.ftb.util.OSUtils.OS;
import net.ftb.util.StyleUtil;
import net.ftb.workers.GameUpdateWorker;
import net.ftb.workers.LoginWorker;

public class LaunchFrame extends JFrame {
	private LoginResponse RESPONSE;
	public static JPanel panel;
	private JPanel footer = new JPanel();
	private JLabel footerLogo = new JLabel(new ImageIcon(this.getClass().getResource("/image/logo_ftb.png")));
	private JLabel tpInstallLocLbl = new JLabel();
	private JButton launch = new JButton(), edit = new JButton(), tpInstall = new JButton();

	private static String[] dropdown_ = {"Select Profile", "Create Profile"};
	private static JComboBox users, tpInstallLocation;
	private static LaunchFrame instance = null;
	private static String version = "1.2.1";

	public final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);	

	protected static UserManager userManager;

	public static ModpacksPane modPacksPane;
	public TexturepackPane tpPane;
	public OptionsPane optionsPane;

	public static int buildNumber = 121;
	public static boolean noConfig = false;
	public static LauncherConsole con;
	public static String tempPass = "";
	public static Panes currentPane = Panes.MODPACK;

	public static final String FORGENAME = "MinecraftForge.zip";

	protected enum Panes {
		MODPACK,
		TEXTURE,
		OPTIONS

	}

	/**
	 * Launch the application.
	 * @param args - CLI arguments
	 */
	public static void main(String[] args) {
		if(new File(Settings.getSettings().getInstallPath(), "FTBLauncherLog.txt").exists()) {
			new File(Settings.getSettings().getInstallPath(), "FTBLauncherLog.txt").delete();
		}

		if(new File(Settings.getSettings().getInstallPath(), "MinecraftLog.txt").exists()) {
			new File(Settings.getSettings().getInstallPath(), "MinecraftLog.txt").delete();
		}

		DownloadUtils thread = new DownloadUtils();
		thread.start();

		Logger.logInfo("FTBLaunch starting up (version "+ version + ")");
		Logger.logInfo("Java version: "+System.getProperty("java.version"));
		Logger.logInfo("Java vendor: "+System.getProperty("java.vendor"));
		Logger.logInfo("Java home: "+System.getProperty("java.home"));
		Logger.logInfo("Java specification: " + System.getProperty("java.vm.specification.name") + " version: " +
				System.getProperty("java.vm.specification.version") + " by " + System.getProperty("java.vm.specification.vendor"));
		Logger.logInfo("Java vm: "+System.getProperty("java.vm.name") + " version: " + System.getProperty("java.vm.version") + " by " + System.getProperty("java.vm.vendor"));
		Logger.logInfo("OS: "+System.getProperty("os.arch") + " " + System.getProperty("os.name") + " " + System.getProperty("os.version"));

		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				StyleUtil.loadUiStyles();
				try {
					for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
						if ("Nimbus".equals(info.getName())) {
							UIManager.setLookAndFeel(info.getClassName());
							break;
						}
					}
				} catch (Exception e) {
					try {
						UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
					} catch (Exception e1) { }
				}
				I18N.setupLocale();
				I18N.setLocale(Settings.getSettings().getLocale());

				if (noConfig) {
					InstallDirectoryDialog installDialog = new InstallDirectoryDialog();
					installDialog.setVisible(true);
				}

				File installDir = new File(Settings.getSettings().getInstallPath());
				if (!installDir.exists()) {
					installDir.mkdirs();
				}
				File dynamicDir = new File(OSUtils.getDynamicStorageLocation());
				if (!dynamicDir.exists()) {
					dynamicDir.mkdirs();
				}

				userManager = new UserManager(new File(OSUtils.getDynamicStorageLocation(), "logindata"));
				con = new LauncherConsole();
				if (Settings.getSettings().getConsoleActive()) {
					con.setVisible(true);
				}

				LaunchFrame frame = new LaunchFrame(0);
				instance = frame;
				frame.setVisible(true);

				Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						Logger.logError("Unhandled exception in " + t.toString(), e);
					}
				});

				ModPack.addListener(frame.modPacksPane);
				ModPack.loadXml(getXmls());

				TexturePack.addListener(frame.tpPane);
//				TexturePack.loadAll();

			}
		});
	}

	/**
	 * Create the frame.
	 */
	public LaunchFrame(final int tab) {
		setFont(new Font("a_FuturaOrto", Font.PLAIN, 12));
		setResizable(false);
		setTitle("Feed the Beast Launcher (Soartex Edition)");
		setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/image/logo_ftb.png")));

		panel = new JPanel();

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		if(OSUtils.getCurrentOS() == OS.WINDOWS) {
			setBounds(100, 100, 842, 480);
		} else {
			setBounds(100, 100, 850, 480);
		}
		panel.setBounds(0, 0, 850, 480);
		panel.setLayout(null);
		footer.setBounds(0, 380, 850, 100);
		footer.setLayout(null);
		footer.setBackground(LauncherStyle.getCurrentStyle().footerColor);
		tabbedPane.setBounds(0, 0, 850, 380);
		panel.add(tabbedPane);
		panel.add(footer);
		setContentPane(panel);

		//Footer
		
		//ftb logo
		footerLogo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		footerLogo.setBounds(20, 20, 42, 42);

		//profile stuff
		dropdown_[0] = I18N.getLocaleString("PROFILE_SELECT");
		dropdown_[1] = I18N.getLocaleString("PROFILE_CREATE");

		String[] dropdown = concatenateArrays(dropdown_, UserManager.getNames().toArray(new String[]{}));
		users = new JComboBox(dropdown);
		if(Settings.getSettings().getLastUser() != null) {
			for(int i = 0; i < dropdown.length; i++) {
				if(dropdown[i].equalsIgnoreCase(Settings.getSettings().getLastUser())) {
					users.setSelectedIndex(i);
				}
			}
		}

		users.setBounds(550, 20, 150, 30);
		users.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(users.getSelectedIndex() == 1) {
					ProfileAdderDialog p = new ProfileAdderDialog(getInstance(), true);
					users.setSelectedIndex(0);
					p.setVisible(true);
				}
				edit.setEnabled(users.getSelectedIndex() > 1);
			}
		});

		edit = new JButton(I18N.getLocaleString("EDIT_BUTTON"));
		edit.setBounds(480, 20, 60, 30);
		edit.setVisible(true);
		edit.setEnabled(users.getSelectedIndex() > 1);
		edit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(users.getSelectedIndex() > 1) {
					ProfileEditorDialog p = new ProfileEditorDialog(getInstance(), (String)users.getSelectedItem(), true);
					users.setSelectedIndex(0);
					p.setVisible(true);
				}
				edit.setEnabled(users.getSelectedIndex() > 1);
			}
		});

		//launch button
		launch.setText(I18N.getLocaleString("LAUNCH_BUTTON"));
		launch.setBounds(711, 20, 100, 30);
		launch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(users.getSelectedIndex() > 1 && modPacksPane.packPanels.size() > 0) {
					Settings.getSettings().setLastPack(ModPack.getSelectedPack().getDir());
					saveSettings();
					doLogin(UserManager.getUsername(users.getSelectedItem().toString()), UserManager.getPassword(users.getSelectedItem().toString()));
				} else if(users.getSelectedIndex() <= 1) {
					ErrorUtils.tossError("Please select a profile!");
				}
			}
		});

		
		//texture pack installing
		tpInstall.setBounds(650, 20, 160, 30);
		tpInstall.setText(I18N.getLocaleString("INSTALL_TEXTUREPACK"));
		tpInstall.setVisible(false);
		tpInstall.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(tpPane.texturePackPanels.size() > 0 && getSelectedTexturePackIndex() >= 0) {
					TextureManager man = new TextureManager(new JFrame(), true);
					man.setVisible(true);
				}
			}
		});

		tpInstallLocation = new JComboBox();
		tpInstallLocation.setBounds(480, 20, 160, 30);
		tpInstallLocation.setToolTipText("Install to...");
		tpInstallLocation.setVisible(false);

		tpInstallLocLbl.setText("Install to...");
		tpInstallLocLbl.setBounds(480, 20, 80, 30);
		tpInstallLocLbl.setVisible(false);

		footer.add(edit);
		footer.add(users);
		footer.add(footerLogo);
		footer.add(launch);
		footer.add(tpInstall);
		footer.add(tpInstallLocation);

		modPacksPane = new ModpacksPane();
		tpPane = new TexturepackPane();
		optionsPane = new OptionsPane(Settings.getSettings());

		getRootPane().setDefaultButton(launch);
		updateLocale();

		
		tabbedPane.add(modPacksPane, 0);
		tabbedPane.add(tpPane, 1);
		tabbedPane.add(optionsPane, 2);
		tabbedPane.setIconAt(0, new ImageIcon(this.getClass().getResource("/image/tabs/modpacks.png")));
		tabbedPane.setIconAt(1, new ImageIcon(this.getClass().getResource("/image/tabs/texturepacks.png")));
		tabbedPane.setIconAt(2, new ImageIcon(this.getClass().getResource("/image/tabs/options.png")));
		tabbedPane.setSelectedIndex(tab);

		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				if(tabbedPane.getSelectedComponent() instanceof ILauncherPane) {
					((ILauncherPane)tabbedPane.getSelectedComponent()).onVisible();
					currentPane = Panes.values()[tabbedPane.getSelectedIndex()];
					updateFooter();
				}
			}
		});
	}

	/**
	 * call this to login
	 */
	private void doLogin(final String username, String password) {
		if(password.isEmpty()) {
			PasswordDialog p = new PasswordDialog(this, true);
			p.setVisible(true);
			if(tempPass.isEmpty()){
				enableObjects();
				return;
			}
			password = tempPass;
		}

		Logger.logInfo("Logging in...");

		tabbedPane.setEnabledAt(0, false);
		tabbedPane.setEnabledAt(1, false);
		tabbedPane.setEnabledAt(2, false);
		tabbedPane.getSelectedComponent().setEnabled(false);

		launch.setEnabled(false);
		users.setEnabled(false);
		edit.setEnabled(false);
		tpInstall.setEnabled(false);
		tpInstallLocation.setEnabled(false);

		LoginWorker loginWorker = new LoginWorker(username, password) {
			@Override
			public void done() {
				String responseStr;
				try {
					responseStr = get();
				} catch (InterruptedException err) {
					Logger.logError(err.getMessage(), err);
					enableObjects();
					return;
				} catch (ExecutionException err) {
					if(err.getCause() instanceof IOException || err.getCause() instanceof MalformedURLException) {
						Logger.logError(err.getMessage(), err);
						PlayOfflineDialog d = new PlayOfflineDialog("mcDown", username);
						d.setVisible(true);
					}
					enableObjects();
					return;
				}

				try {
					RESPONSE = new LoginResponse(responseStr);
				} catch (IllegalArgumentException e) {
					if(responseStr.contains(":")) {
						Logger.logError("Received invalid response from server.");
					} else {
						if(responseStr.equalsIgnoreCase("bad login")) {
							ErrorUtils.tossError("Invalid username or password.");
						} else if(responseStr.equalsIgnoreCase("old version")) {
							ErrorUtils.tossError("Outdated launcher.");
						} else {
							ErrorUtils.tossError("Login failed: " + responseStr);
							PlayOfflineDialog d = new PlayOfflineDialog("mcDown", username);
							d.setVisible(true);
						}
					}
					enableObjects();
					return;
				}
				Logger.logInfo("Login complete.");
				runGameUpdater(RESPONSE);
			}
		};
		loginWorker.execute();
	}

	/**
	 * checks whether an update is needed, and then starts the update process off
	 * @param response - the response from the minecraft servers
	 */
	private void runGameUpdater(final LoginResponse response) {
		final String installPath = Settings.getSettings().getInstallPath();
		final ModPack pack = ModPack.getSelectedPack();
		if(Settings.getSettings().getForceUpdate() && new File(installPath, pack.getDir() + File.separator + "version").exists()) {
			new File(installPath, pack.getDir() + File.separator + "version").delete();
		}
		if(!initializeMods()) {
			enableObjects();
			return;
		}
		try {
			TextureManager.updateTextures();
		} catch (Exception e1) { }
		MinecraftVersionDetector mvd = new MinecraftVersionDetector();
		if(!new File(installPath, pack.getDir() + "/minecraft/bin/minecraft.jar").exists() || mvd.shouldUpdate(installPath + "/" + pack.getDir() + "/minecraft")) {
			final ProgressMonitor progMonitor = new ProgressMonitor(this, "Downloading minecraft...", "", 0, 100);
			final GameUpdateWorker updater = new GameUpdateWorker(pack.getMcVersion(), new File(installPath, pack.getDir() + "/minecraft/bin").getPath()) {
				@Override
				public void done() {
					progMonitor.close();
					try {
						if(get()) {
							Logger.logInfo("Game update complete");
							FileUtils.killMetaInf();
							launchMinecraft(installPath + "/" + pack.getDir() + "/minecraft", RESPONSE.getUsername(), RESPONSE.getSessionID());
						} else {
							ErrorUtils.tossError("Error occurred during downloading the game");
						}
					} catch (CancellationException e) { 
						ErrorUtils.tossError("Game update canceled.");
					} catch (InterruptedException e) { 
						ErrorUtils.tossError("Game update interrupted.");
					} catch (ExecutionException e) { 
						ErrorUtils.tossError("Failed to download game.");
					} finally {
						enableObjects();
					}
				}
			};

			updater.addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (progMonitor.isCanceled()) {
						updater.cancel(false);
					}
					if (!updater.isDone()) {
						int prog = updater.getProgress();
						if (prog < 0) {
							prog = 0;
						} else if (prog > 100) {
							prog = 100;
						}
						progMonitor.setProgress(prog);
						progMonitor.setNote(updater.getStatus());
					}
				}
			});
			updater.execute();
		} else {
			launchMinecraft(installPath + "/" + pack.getDir() + "/minecraft", RESPONSE.getUsername(), RESPONSE.getSessionID());
		}
	}

	/**
	 * launch the game with the mods in the classpath
	 * @param workingDir - install path
	 * @param username - the MC username
	 * @param password - the MC password
	 */
	public void launchMinecraft(String workingDir, String username, String password) {
		try {
			Process minecraftProcess = MinecraftLauncher.launchMinecraft(workingDir, username, password, FORGENAME, Settings.getSettings().getRamMax());
			StreamLogger.start(minecraftProcess.getInputStream(), new LogEntry().level(LogLevel.UNKNOWN));
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) { }
			try {
				minecraftProcess.exitValue();
			} catch (IllegalThreadStateException e) {
				this.setVisible(false);
				ProcessMonitor.create(minecraftProcess, new Runnable() {
					@Override
					public void run() {
						if(!Settings.getSettings().getKeepLauncherOpen()) {
							System.exit(0);
						} else {
							LaunchFrame launchFrame = LaunchFrame.this;
							launchFrame.setVisible(true);
							launchFrame.enableObjects();
							try {
								Settings.getSettings().load(new FileInputStream(Settings.getSettings().getConfigFile()));
								tabbedPane.remove(1);
								optionsPane = new OptionsPane(Settings.getSettings());
								tabbedPane.add(optionsPane, 1);
								tabbedPane.setIconAt(1, new ImageIcon(this.getClass().getResource("/image/tabs/options.png")));
							} catch (Exception e1) {
								Logger.logError("Failed to reload settings after launcher closed", e1);
							}
						}
					}
				});
			}
		} catch(Exception e) { }
	}

	/**
	 * @param modPackName - The pack to install (should already be downloaded)
	 * @throws IOException
	 */
	protected void installMods(String modPackName) throws IOException {
		String installpath = Settings.getSettings().getInstallPath();
		String temppath = OSUtils.getDynamicStorageLocation();
		ModPack pack = ModPack.getPack(modPacksPane.getSelectedModIndex());
		Logger.logInfo("dirs mk'd");
		File source = new File(temppath, "ModPacks/" + pack.getDir() + "/.minecraft");
		if(!source.exists()) {
			source = new File(temppath, "ModPacks/" + pack.getDir() + "/minecraft");
		}
		FileUtils.copyFolder(source, new File(installpath, pack.getDir() + "/minecraft/"));
		FileUtils.copyFolder(new File(temppath, "ModPacks/" + pack.getDir() + "/instMods/"), new File(installpath, pack.getDir() + "/instMods/"));
	}

	/**
	 * "Saves" the settings from the GUI controls into the settings class.
	 */
	public void saveSettings() {
		Settings.getSettings().setLastUser(String.valueOf(users.getSelectedItem()));
		instance.optionsPane.saveSettingsInto(Settings.getSettings());
	}

	/**
	 * @param user - user added/edited
	 */
	public static void writeUsers(String user) {
		try {
			userManager.write();
		} catch (IOException e) { }
		String[] usernames = concatenateArrays(dropdown_, UserManager.getNames().toArray(new String[]{}));
		users.removeAllItems();
		for(int i = 0; i < usernames.length; i++) {
			users.addItem(usernames[i]);
			if(usernames[i].equals(user)) {
				users.setSelectedIndex(i);
			}
		}
	}

	/**
	 * updates the tpInstall to the available ones
	 * @param locations - the available locations to install the tp to
	 */
	public static void updateTpInstallLocs(String[] locations) {
		tpInstallLocation.removeAllItems();
		for(String location : locations) {
			if(!location.isEmpty()) {
				tpInstallLocation.addItem(ModPack.getPack(location.trim()).getName());
			}
		}
		tpInstallLocation.setSelectedItem(ModPack.getSelectedPack().getName());
	}

	/**
	 * @param first - First array
	 * @param rest - Rest of the arrays
	 * @return - Outputs concatenated arrays
	 */
	public static <T> T[] concatenateArrays(T[] first, T[]... rest) {
		int totalLength = first.length;
		for (T[] array : rest) {
			totalLength += array.length;
		}
		T[] result = Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (T[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		return result;
	}

	/**
	 * @return - Outputs selected modpack index
	 */
	public static int getSelectedModIndex() {
		return instance.modPacksPane.getSelectedModIndex();
	}

	/**
	 * @return - Outputs selected texturepack index
	 */
	public static int getSelectedTexturePackIndex() {
		return instance.tpPane.getSelectedTexturePackIndex();
	}

	/**
	 * @return - Outputs selected texturepack install index
	 */
	public static int getSelectedTPInstallIndex() {
		return instance.tpInstallLocation.getSelectedIndex();
	}

	/**
	 * @return - Outputs LaunchFrame instance
	 */
	public static LaunchFrame getInstance() {
		return instance;
	}

	/**
	 * Enables all items that are disabled upon launching
	 */
	private void enableObjects() {
		tabbedPane.setEnabledAt(0, true);
		tabbedPane.setEnabledAt(1, true);
		tabbedPane.setEnabledAt(2, true);
		tabbedPane.getSelectedComponent().setEnabled(true);
		updateFooter();
		tpInstall.setEnabled(true);
		launch.setEnabled(true);
		users.setEnabled(true);
		tpInstallLocation.setEnabled(true);
		TextureManager.updating = false;
	}

	/**
	 * Download and install mods
	 * @return boolean - represents whether it was successful in initializing mods
	 */
	private boolean initializeMods() {
		Logger.logInfo(ModPack.getSelectedPack().getDir());
		ModManager man = new ModManager(new JFrame(), true);
		man.setVisible(true);
		if(man.erroneous) {
			return false;
		}
		try {
			installMods(ModPack.getSelectedPack().getDir());
			man.cleanUp();
		} catch (IOException e) { }
		return true;
	}

	/**
	 * disables the buttons that are usually active on the footer
	 */
	public void disableMainButtons() {
		launch.setVisible(false);
		edit.setVisible(false);
		users.setVisible(false);
	}

	/**
	 * disables the footer buttons active when the texture pack tab is selected
	 */
	public void disableTextureButtons() {
		tpInstall.setVisible(false);
		tpInstallLocation.setVisible(false);
	}

	/**
	 * update the footer to the correct buttons for active tab
	 */
	public void updateFooter() {
		switch(currentPane) {
		case TEXTURE:
			tpInstall.setVisible(true);
			tpInstallLocation.setVisible(true);
			disableMainButtons();
			break;
		default:
			launch.setVisible(true);
			edit.setEnabled(users.getSelectedIndex() > 1);
			edit.setVisible(true);
			users.setVisible(true);
			disableTextureButtons();
			break;
		}
	}

	// TODO: Make buttons dynamically sized.
	/**
	 * updates the buttons/text to language specific
	 */
	public void updateLocale() {
		if(I18N.currentLocale == Locale.deDE) {
			edit.setBounds(420, 20, 120, 30);
			tpInstallLocation.setBounds(420, 20, 190, 30);
			tpInstall.setBounds(620, 20, 190, 30);
		} else {
			edit.setBounds(480, 20, 60, 30);
			tpInstallLocation.setBounds(480, 20, 160, 30);
			tpInstall.setBounds(650, 20, 160, 30);
		}
		launch.setText(I18N.getLocaleString("LAUNCH_BUTTON"));
		edit.setText(I18N.getLocaleString("EDIT_BUTTON"));
		tpInstall.setText(I18N.getLocaleString("INSTALL_TEXTUREPACK"));
		dropdown_[0] = I18N.getLocaleString("PROFILE_SELECT");
		dropdown_[1] = I18N.getLocaleString("PROFILE_CREATE");
		writeUsers((String)users.getSelectedItem());
		optionsPane.updateLocale();
		modPacksPane.updateLocale();
		tpPane.updateLocale();
	}

	private static ArrayList<String> getXmls() {
		ArrayList<String> s = Settings.getSettings().getPrivatePacks();
		if(s == null) {
			s = new ArrayList<String>();
		}
		for(int i = 0; i < s.size(); i++) {
			if(s.get(i).isEmpty()) {
				s.remove(i);
				i--;
			} else {
				String temp = s.get(i);
				if(!temp.endsWith(".xml")) {
					s.remove(i);
					s.add(i, temp + ".xml");
				}
			}
		}
		s.add(0, "modpacks.xml");
		return s;
	}

}
