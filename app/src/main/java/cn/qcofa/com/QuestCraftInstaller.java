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

    public static Result install(Context context, java.io.File accountFile, java.io.File launcherConf) {
        StringBuilder log = new StringBuilder();

        if (accountFile == null || !accountFile.isFile()) {
            return new Result(false, "账号文件不存在:\n"
                    + (accountFile == null ? "(null)" : accountFile.getAbsolutePath()));
        }
        if (launcherConf == null || !launcherConf.isFile()) {
            return new Result(false, "launcher.conf 不存在:\n"
                    + (launcherConf == null ? "(null)" : launcherConf.getAbsolutePath()));
        }

        // 1. Root check
        if (!hasRoot()) {
            return new Result(false,
                    "未获取 root 权限。\n\n请确认：\n" +
                    "1. 设备已 root（Magisk）\n" +
                    "2. Magisk 中已允许本应用 su\n" +
                    "3. 若未弹出提示，尝试在 Magisk 的“超级用户”中手动授权");
        }
        log.append("✓ root 已授予\n");

        // 2. Resolve QuestCraft's numeric UID
        int uid = getPackageUid(PKG);
        if (uid < 0) {
            return new Result(false, "未检测到 QuestCraft（" + PKG + "）。\n请先在设备上安装 QuestCraft。");
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
        log.append("--- 安装输出 ---\n").append(out).append("\n");

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
        log.append("--- 验证 ---\n").append(verify);

        boolean ok = verify.contains(accountName)
                  && verify.contains("launcher.conf")
                  && !verify.contains("No such file");

        return new Result(ok,
                (ok ? "安装完成。\n\n请在断开 Wi-Fi 后启动 QuestCraft。\n\n"
                    : "安装可能未完成，请查看以下信息。\n\n")
                + log.toString());
    }

    public static String diagnose(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== QCOFA 诊断 ===\n\n");
        sb.append("root: ").append(hasRoot() ? "是" : "否").append("\n");

        int uid = getPackageUid(PKG);
        sb.append("QuestCraft UID: ").append(uid).append("\n\n");

        // Show expected vs actual so mismatches are obvious.
        String dump = runSu(
                "echo '--- 期望: 私有账号目录 " + ACCOUNTS_DIR + " (owner " + uid + ", 700) ---'\n" +
                "ls -lad '" + ACCOUNTS_DIR + "' 2>&1\n" +
                "echo\n" +
                "echo '--- 私有账号文件 ---'\n" +
                "ls -la '" + ACCOUNTS_DIR + "' 2>&1\n" +
                "echo\n" +
                "echo '--- 公共 launcher.conf (期望: 不要 chown) ---'\n" +
                "ls -la '" + PUBLIC_FILES + "/launcher.conf' 2>&1\n" +
                "echo\n" +
                "echo '--- 公共目录 ---'\n" +
                "ls -lad '" + PUBLIC_FILES + "' 2>&1\n" +
                "echo\n" +
                "echo '--- SELinux 上下文 ---'\n" +
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
            return "su 调用失败: " + e.getMessage() + "\n";
        } finally {
            if (p != null) p.destroy();
        }
    }
}
