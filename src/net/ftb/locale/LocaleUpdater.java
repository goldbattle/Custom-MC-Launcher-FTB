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
package net.ftb.locale;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import net.ftb.log.Logger;
import net.ftb.util.DownloadUtils;
import net.ftb.util.FileUtils;
import net.ftb.util.OSUtils;

public class LocaleUpdater extends Thread {
	private final String root = OSUtils.getDynamicStorageLocation();
	private File local = new File(root, "locale" + File.separator + "version");
	private File archive = new File(root, "locales.zip");
	private int remoteVer;

	public LocaleUpdater() {
		setName("Locale Updater");
		setPriority(MIN_PRIORITY);
	}

	private void updateFiles() {
	}

	public void run() {
	}

	private void cleanUpFiles() {
		if (archive.exists()) {
			archive.delete();
		}
	}
}
