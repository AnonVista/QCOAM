package cn.qcofa.com;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuestCraftInstaller {

    private static final String TAG = "QcofA";
    private static final String PKG = "com.qcxr.qcxr";
    private static final String APP_DATA = "/data/user/0/" + PKG + "/files";
    private static final String ACCOUNTS_DIR = APP_DATA + "/accounts";
    private static final String PUBLIC_FILES =
            "/storage/emulated/0/Android/data/" + PKG + "/files";

    public static final class Result {
        public final boolean success;
        public final String message;
        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private QuestCraftInstaller() {}

    // ---------------------------------------------------------------- public

    public static Result requestRoot() {
        String out = runSu("id\n");
        boolean ok = out.contains("uid=0");
        return new Result(ok, ok
                ? "Magisk/root granted access to QcofA.\n\n" + out
                : "The su request did not obtain UID 0.\n\nIf Magisk did not show a prompt, check its Superuser list for QcofA and remove/reset any remembered denial, then try again.\n\nRaw su output:\n" + out);
    }

    public static Result install(Context context, java.io.File accountFile, java.io.File launcherConf) {
        StringBuilder log = new StringBuilder();

        if (accountFile == null || !accountFile.isFile()) {
            return new Result(false, "Account file does not exist:\n"
                    + (accountFile == null ? "(null)" : accountFile.getAbsolutePath()));
        }
        if (launcherConf == null || !launcherConf.isFile()) {
            return new Result(false, "launcher.conf 不存在:\n"
                    + (launcherConf == null ? "(null)" : launcherConf.getAbsolutePath()));
        }

        // 1. Root check
        if (!hasRoot()) {
            return new Result(false,
                    "Root access was not granted.\n\nCheck:\n" +
                    "1. The device is rooted (Magisk)\n" +
                    "2. QcofA is allowed root access in Magisk\n" +
                    "3. If no prompt appeared, check/reset QcofA in Magisk's Superuser list");
        }
        log.append("✓ root granted\n");

        // 2. Resolve QuestCraft's numeric UID
        int uid = getPackageUid(PKG);
        if (uid < 0) {
            return new Result(false, "QuestCraft was not detected（" + PKG + "）。\nInstall QuestCraft on the device first.");
        }
        log.append("✓ QuestCraft UID: ").append(uid).append("\n");

        // 3. Build the root script
        String accountName = accountFile.getName();
        String accountDest = ACCOUNTS_DIR + "/" + accountName;
        String confDest    = PUBLIC_FILES + "/launcher.conf";

        // Private account file: owned by QuestCraft's UID, 700/600, relabelled.
        // Emulated launcher.conf: just copy; do NOT chown.
        String script =
                "mkdir -p '" + ACCOUNTS_DIR + "'\n" +
                "mkdir -p '" + PUBLIC_FILES + "'\n" +
                "[ -f '" + accountDest + "' ] && cp '" + accountDest + "' '" + accountDest + ".bak' || true\n" +
                "[ -f '" + confDest    + "' ] && cp '" + confDest    + "' '" + confDest    + ".bak' || true\n" +
                "cp '" + accountFile.getAbsolutePath() + "' '" + accountDest + "'\n" +
                "cp '" + launcherConf.getAbsolutePath() + "' '" + confDest + "'\n" +
                "chown " + uid + ":" + uid + " '" + ACCOUNTS_DIR + "'\n" +
                "chown " + uid + ":" + uid + " '" + accountDest + "'\n" +
                "chmod 700 '" + ACCOUNTS_DIR + "'\n" +
                "chmod 600 '" + accountDest + "'\n" +
                "restorecon -R '" + ACCOUNTS_DIR + "' 2>/dev/null || true\n";

        String out = runSu(script);
        log.append("--- Install output ---\n").append(out).append("\n");

        // 4. Verify
        String verify = runSu(
                "echo '--- account ---'\n" +
                "ls -la '" + accountDest + "' 2>&1\n" +
                "echo\n" +
                "echo '--- launcher.conf ---'\n" +
                "ls -la '" + confDest + "' 2>&1\n" +
                "echo\n" +
                "echo '--- accounts/ dir ---'\n" +
                "ls -lad '" + ACCOUNTS_DIR + "' 2>&1\n");
        log.append("--- Verification ---\n").append(verify);

        boolean ok = verify.contains(accountName)
                  && verify.contains("launcher.conf")
                  && !verify.contains("No such file");

        return new Result(ok,
                (ok ? "Installation complete.\n\nStart QuestCraft after disconnecting Wi-Fi.\n\n"
                    : "Installation may not have completed; see details below.\n\n")
                + log.toString());
    }

    public static String diagnose(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== QCOFA diagnostics ===\n\n");
        sb.append("root: ").append(hasRoot() ? "yes" : "no").append("\n");

        int uid = getPackageUid(PKG);
        sb.append("QuestCraft UID: ").append(uid).append("\n\n");

        // Show expected vs actual so mismatches are obvious.
        String dump = runSu(
                "echo '--- Expected private account directory " + ACCOUNTS_DIR + " (owner " + uid + ", 700) ---'\n" +
                "ls -lad '" + ACCOUNTS_DIR + "' 2>&1\n" +
                "echo\n" +
                "echo '--- Private account files ---'\n" +
                "ls -la '" + ACCOUNTS_DIR + "' 2>&1\n" +
                "echo\n" +
                "echo '--- Public launcher.conf (do not chown) ---'\n" +
                "ls -la '" + PUBLIC_FILES + "/launcher.conf' 2>&1\n" +
                "echo\n" +
                "echo '--- Public directory ---'\n" +
                "ls -lad '" + PUBLIC_FILES + "' 2>&1\n" +
                "echo\n" +
                "echo '--- SELinux context ---'\n" +
                "ls -Z '" + ACCOUNTS_DIR + "' 2>&1\n");

        sb.append(dump);
        return sb.toString();
    }

    // --------------------------------------------------------------- helpers

    private static boolean hasRoot() {
        return runSu("id\n").contains("uid=0");
    }

    private static int getPackageUid(String pkg) {
        String out = runSu("pm list packages -U " + pkg + "\n");
        Matcher m = Pattern.compile("uid:(\\d+)").matcher(out);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    /** Executes a shell script via su. stderr is merged into stdout. */
    private static String runSu(String script) {
        Process p = null;
        try {
            p = new ProcessBuilder("su")
                    .redirectErrorStream(true)
                    .start();

            OutputStream stdin = p.getOutputStream();
            stdin.write(script.getBytes("UTF-8"));
            if (!script.endsWith("\n")) stdin.write('\n');
            stdin.write("exit\n".getBytes("UTF-8"));
            stdin.flush();
            stdin.close();

            StringBuilder sb = new StringBuilder();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }

            int exitCode = p.waitFor();
            sb.append("[exit=").append(exitCode).append("]\n");
            return sb.toString();

        } catch (Exception e) {
            Log.e(TAG, "runSu failed", e);
            return "su invocation failed: " + e.getMessage() + "\n";
        } finally {
            if (p != null) p.destroy();
        }
    }
}
