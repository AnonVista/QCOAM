package cn.qcofa.com;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// 【修改 v1.4】SwitchMaterial 替代 CheckBox 实现左右滑槽开关样式
import com.google.android.material.switchmaterial.SwitchMaterial;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "QcofA";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private EditText usernameInput;
    private EditText customUuidInput;
    private Spinner userTypeSpinner;
    private TextView uuidDisplay;
    private EditText ramValueInput;
    // 【修改 v1.4】SwitchMaterial 替代之前的 CheckBox
    private SwitchMaterial legalCheck, devModsCheck, customRamCheck, demoModeCheck;
    private Button manualInstallJreBtn;
    private Button viewAccountsBtn;
    private Button requestRootBtn;
    private Button installQuestCraftBtn;
    // 【新增 v1.4】Change skin、Save version list按钮
    private Button skinChangeBtn;
    private Button saveVersionListBtn;
    // 【新增 v1.4】UI style选择器（Material / Miuix）
    private Spinner themeStyleSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 【新增 v1.4】在 setContentView 之前应用保存的主题风格（Material / Miuix）
        applyThemeStyle();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        initViews();

        // 加载并显示Current account信息
        loadAndShowCurrentAccount();

        // 请求必要的权限
        requestPermissions();

        // 设置按钮点击事件
        setupClickListeners();
    }

    // 【新增 v1.4】从 SharedPreferences 读取保存的主题风格并应用
    // 在 setTheme() 必须在 super.onCreate() 之前调用才能生效
    private void applyThemeStyle() {
        SharedPreferences prefs = getSharedPreferences("theme_style", MODE_PRIVATE);
        String style = prefs.getString("style", "Material");
        if ("Miuix".equals(style)) {
            setTheme(R.style.Theme_QcofA_Miuix);
        } else {
            setTheme(R.style.Theme_QcofA_Material);
        }
    }

    // 【新增 v1.4】获取存储目录：优先使用根目录 QCOFA.COM（兼容 Pico 等 VR 设备）
    // 若创建失败则回退到应用私有目录
    private File getStorageDir() {
        // 使用公共存储根目录下的 QCOFA.COM 文件夹，兼容 Pico 等 VR 设备
        File storageDir = new File(Environment.getExternalStorageDirectory(), "QCOFA.COM");
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                // 如果创建失败（可能是权限不足），回退到应用私有目录
                Log.w(TAG, "无法在根目录创建 QCOFA.COM 文件夹，回退到应用私有目录");
                File fallbackDir = new File(getExternalFilesDir(null), "QCOFA.COM");
                fallbackDir.mkdirs();
                return fallbackDir;
            }
        }
        return storageDir;
    }

    private void initViews() {
        usernameInput = findViewById(R.id.usernameInput);
        customUuidInput = findViewById(R.id.customUuidInput);
        userTypeSpinner = findViewById(R.id.userTypeSpinner);
        uuidDisplay = findViewById(R.id.uuidDisplay);
        ramValueInput = findViewById(R.id.ramValueInput);
        
        legalCheck = findViewById(R.id.legalCheck);
        devModsCheck = findViewById(R.id.devModsCheck);
        customRamCheck = findViewById(R.id.customRamCheck);
        demoModeCheck = findViewById(R.id.demoModeCheck);
        manualInstallJreBtn = findViewById(R.id.manualInstallJreBtn);
        viewAccountsBtn = findViewById(R.id.viewAccountsBtn);
        requestRootBtn = findViewById(R.id.requestRootBtn);
        installQuestCraftBtn = findViewById(R.id.installQuestCraftBtn);
        skinChangeBtn = findViewById(R.id.skinChangeBtn);
        saveVersionListBtn = findViewById(R.id.saveVersionListBtn);
        // 【新增 v1.4】UI style Spinner 绑定
        themeStyleSpinner = findViewById(R.id.themeStyleSpinner);

        // 设置Account type选择器
        setupUserTypeSpinner();

        // 【新增 v1.4】设置UI style选择器
        setupThemeStyleSpinner();

        // 设置默认值
        ramValueInput.setText("2048");
        // 不再自动设置Username，让用户自行输入
    }

    private void setupUserTypeSpinner() {
        android.widget.ArrayAdapter<CharSequence> adapter = android.widget.ArrayAdapter.createFromResource(
                this, 
                R.array.user_types_array, 
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userTypeSpinner.setAdapter(adapter);
    }

    // 【新增 v1.4】初始化UI style选择器（Material / Miuix）
    // 选择后保存到 SharedPreferences，触发 recreate() 刷新界面
    private void setupThemeStyleSpinner() {
        android.widget.ArrayAdapter<CharSequence> adapter = android.widget.ArrayAdapter.createFromResource(
                this,
                R.array.theme_styles_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeStyleSpinner.setAdapter(adapter);

        // 读取保存的主题风格并设置选中项
        String savedStyle = getSharedPreferences("theme_style", MODE_PRIVATE)
                .getString("style", "Material");
        int selection = savedStyle.equals("Miuix") ? 1 : 0;
        themeStyleSpinner.setSelection(selection);

        // 监听选择变化，切换主题
        themeStyleSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedStyle = parent.getItemAtPosition(position).toString();
                String currentStyle = getSharedPreferences("theme_style", MODE_PRIVATE)
                        .getString("style", "Material");
                if (!selectedStyle.equals(currentStyle)) {
                    getSharedPreferences("theme_style", MODE_PRIVATE)
                            .edit()
                            .putString("style", selectedStyle)
                            .apply();
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        Button createAccountBtn = findViewById(R.id.createAccountBtn);
        createAccountBtn.setOnClickListener(v -> createAccountFile());

        Button saveConfigBtn = findViewById(R.id.saveConfigBtn);
        saveConfigBtn.setOnClickListener(v -> saveConfigFiles());

        manualInstallJreBtn.setOnClickListener(v -> showJreInstallationDialog());
        
        viewAccountsBtn.setOnClickListener(v -> showAccountsList());

        // 【新增 v1.4】Change skin按钮：弹出卡片式版本列表，右侧带下载图标
        skinChangeBtn.setOnClickListener(v -> showSkinChangeDialog());

        // 【新增 v1.4】Save version list按钮：将 supportedVersions.json 导出到存储目录
        saveVersionListBtn.setOnClickListener(v -> saveVersionListToStorage());

        // 设置折叠/展开功能的点击事件
        LinearLayout expandableSectionHeader = findViewById(R.id.expandableSectionHeader);
        TextView expandIndicator = findViewById(R.id.expandIndicator);
        LinearLayout expandableSection = findViewById(R.id.expandableSection);
        
        expandableSectionHeader.setOnClickListener(v -> {
            boolean isExpanded = expandableSection.getVisibility() == View.VISIBLE;
            if (isExpanded) {
                expandableSection.setVisibility(View.GONE);
                expandIndicator.setText("▶");
            } else {
                expandableSection.setVisibility(View.VISIBLE);
                expandIndicator.setText("▼");
            }
        });

        // 为Username输入框添加文本变化监听器，自动触发UUID生成
        usernameInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                autoGenerateUUIDIfNeeded();
            }
        });
        
        // 为自定义UUID输入框添加文本变化监听器
        customUuidInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                // 当用户输入自定义UUID时，更新显示
                String customUuid = customUuidInput.getText().toString().trim();
                if (!customUuid.isEmpty()) {
                    uuidDisplay.setText("UUID: " + customUuid);
                } else {
                    // 如果清空了自定义UUID，则重新自动生成
                    autoGenerateUUIDIfNeeded();
                }
            }
        });
    }

    private void autoGenerateUUIDIfNeeded() {
        String username = usernameInput.getText().toString().trim();
        if (!username.isEmpty()) {
            String generatedUUID = generateUUID(username);
            uuidDisplay.setText("UUID: " + generatedUUID);
        } else {
            uuidDisplay.setText("UUID: ");
        }
    }

    private String generateUUID(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            
            // 格式化为正确的UUID格式：8-4-4-4-12
            String formattedUUID = String.format("%s-%s-%s-%s-%s",
                hashtext.substring(0, 8),
                hashtext.substring(8, 12),
                hashtext.substring(12, 16),
                hashtext.substring(16, 20),
                hashtext.substring(20, 32));
            
            return formattedUUID;
        } catch (Exception e) {
            Log.e("QcofA", "MD5哈希计算失败", e);
            return UUID.randomUUID().toString();
        }
    }

    private String extractUUIDFromDisplay() {
        String uuidText = uuidDisplay.getText().toString();
        if (uuidText.startsWith("UUID: ")) {
            return uuidText.substring(6); // 移除 "UUID: " 前缀
        }
        return uuidText;
    }

    private void createAccountFile() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "请输入Username", Toast.LENGTH_SHORT).show();
            return;
        }

        String uuid = extractUUIDFromDisplay();
        if (uuid == null || uuid.isEmpty()) {
            Toast.makeText(this, "Generate a UUID first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 创建存储目录
            File storageDir = getStorageDir();
            if (!storageDir.exists()) {
                storageDir.mkdirs();
                Log.d(TAG, "创建目录: " + storageDir.getAbsolutePath());
            }

            // 创建账号JSON文件
            File jsonFile = new File(storageDir, uuid + ".json");
            JSONObject accountJson = new JSONObject();
            accountJson.put("accessToken", "0");
            accountJson.put("expiresOn", 0);
            accountJson.put("isDemoMode", demoModeCheck.isChecked());
            accountJson.put("userType", getUserType());
            accountJson.put("username", username);
            accountJson.put("uuid", uuid);

            // 写入文件
            FileWriter writer = new FileWriter(jsonFile);
            writer.write(accountJson.toString(2)); // 格式化缩进
            writer.close();

            Toast.makeText(this, "Account file created: " + jsonFile.getName(), Toast.LENGTH_LONG).show();
            Log.d(TAG, "Account file created: " + jsonFile.getAbsolutePath());
            
            // 同时更新launcher.conf文件，将新创建的账号添加到配置文件中
            updateLauncherConf(username, uuid);

        } catch (Exception e) {
            Log.e(TAG, "Create account file失败", e);
            Toast.makeText(this, "Create account file失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateLauncherConf(String username, String uuid) {
        try {
            // 创建launcher.conf文件
            File storageDir = getStorageDir();
            File confFile = new File(storageDir, "launcher.conf");

            JSONObject confJson = new JSONObject();
            confJson.put("acceptedLegal", legalCheck.isChecked());
            confJson.put("setDevMods", devModsCheck.isChecked());
            confJson.put("setCustomRAM", customRamCheck.isChecked());
            confJson.put("customRAMValue", ramValueInput.getText().toString().isEmpty() ? "2048" : ramValueInput.getText().toString());
            confJson.put("lastSelectedInstance", 0);
            confJson.put("lastSelectedAccount", 0);

            // 创建或更新账号数组
            JSONArray accountsArray = new JSONArray();
            
            // 如果配置文件已存在，加载已有账号并添加新账号
            if (confFile.exists()) {
                String existingContent = readFileToString(confFile);
                JSONObject existingConf = new JSONObject(existingContent);
                
                if (existingConf.has("accounts")) {
                    JSONArray existingAccounts = existingConf.getJSONArray("accounts");
                    // 添加已有的账号
                    for (int i = 0; i < existingAccounts.length(); i++) {
                        JSONObject existingAccount = existingAccounts.getJSONObject(i);
                        // 避免重复添加相同UUID的账号
                        if (!existingAccount.getString("uuid").equals(uuid)) {
                            accountsArray.put(existingAccount);
                        }
                    }
                }
            }
            
            // 添加当前新创建的账号
            JSONObject accountObj = new JSONObject();
            accountObj.put("username", username);
            accountObj.put("uuid", uuid);
            accountsArray.put(accountObj);
            
            confJson.put("accounts", accountsArray);

            // 写入文件
            FileWriter writer = new FileWriter(confFile);
            writer.write(confJson.toString(2)); // 格式化缩进
            writer.close();

            Toast.makeText(this, "Configuration updated: " + confFile.getName(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Configuration updated: " + confFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Failed to update configuration", e);
            Toast.makeText(this, "Failed to update configuration: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getUserType() {
        return userTypeSpinner.getSelectedItem().toString();
    }

    private String readFileToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString().trim();
    }

    private void saveConfigFiles() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "请输入Username", Toast.LENGTH_SHORT).show();
            return;
        }

        String uuid = extractUUIDFromDisplay();
        if (uuid == null || uuid.isEmpty()) {
            Toast.makeText(this, "Generate a UUID first", Toast.LENGTH_SHORT).show();
            return;
        }

        updateLauncherConf(username, uuid);
        
        // 保存Current account信息到SharedPreferences
        saveCurrentAccount(username, uuid);
        
        // 显示Current account信息
        showCurrentAccountInfo();
    }

    private void saveCurrentAccount(String username, String uuid) {
        SharedPreferences.Editor editor = getSharedPreferences("current_account", MODE_PRIVATE).edit();
        editor.putString("username", username);
        editor.putString("uuid", uuid);
        editor.apply();
    }

    private void loadAndShowCurrentAccount() {
        SharedPreferences prefs = getSharedPreferences("current_account", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String uuid = prefs.getString("uuid", "");
        
        if (!username.isEmpty() && !uuid.isEmpty()) {
            usernameInput.setText(username);
            uuidDisplay.setText("UUID: " + uuid);
        }
    }

    private void showJreInstallationDialog() {
        // 创建带有三个按钮的对话框 - 使用Material Design
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Manually install JRE Runtime");
        builder.setMessage("当您启动游戏过程中无法正常下载和安装JRE提供了两个选项可以给您手动下载和安装JRE");

        // 添加"手动下载安装"按钮
        builder.setPositiveButton("手动下载安装", (dialog, which) -> openJreDownloadPage());
        
        // 添加"本地导出JRE"按钮
        builder.setNeutralButton("本地导出JRE", (dialog, which) -> exportJreToPrivateDirectory());
        
        // 添加"Cancel"按钮
        builder.setNegativeButton("Cancel", null);
        
        builder.show();
    }

    private void openJreDownloadPage() {
        try {
            // 打开浏览器跳转到JRE下载页面
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/QuestCraftPlusPlus/android-openjdk-build-multiarch/releases/tag/jre22-6.0.0"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open browser: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportJreToPrivateDirectory() {
        try {
            // 获取应用外部存储目录 (/storage/emulated/0/QCOFA.COM/jre_runtime)
            File privateDir = new File(getStorageDir(), "jre_runtime");
            
            if (!privateDir.exists()) {
                privateDir.mkdirs();
            }

            // 定义目标文件路径
            File jreZipFile = new File(privateDir, "JRE.zip");

            // 从assets中读取JRE.zip并写入私有目录
            InputStream inputStream = getAssets().open("JRE.zip");
            FileOutputStream outputStream = new FileOutputStream(jreZipFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Toast.makeText(this, "JRE exported to: " + jreZipFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to export JRE", e);
            Toast.makeText(this, "Failed to export JRE: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showAccountsList() {
        try {
            // 读取launcher.conf文件
            File storageDir = getStorageDir();
            File confFile = new File(storageDir, "launcher.conf");
            
            if (!confFile.exists()) {
                Toast.makeText(this, "No accounts have been created", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String content = readFileToString(confFile);
            JSONObject confJson = new JSONObject(content);
            
            JSONArray accountsArray = confJson.getJSONArray("accounts");
            
            if (accountsArray.length() == 0) {
                Toast.makeText(this, "No accounts have been created", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 创建自定义视图的对话框
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Created accounts");
            
            // 创建列表视图
            LinearLayout listLayout = new LinearLayout(this);
            listLayout.setOrientation(LinearLayout.VERTICAL);
            listLayout.setPadding(20, 10, 20, 10);
            
            // 为每个账号创建列表项
            for (int i = 0; i < accountsArray.length(); i++) {
                JSONObject account = accountsArray.getJSONObject(i);
                String username = account.getString("username");
                String uuid = account.getString("uuid");
                
                // 创建账号项视图
                View accountItemView = getLayoutInflater().inflate(R.layout.account_list_item, null);
                
                // 设置账号信息
                TextView usernameView = accountItemView.findViewById(R.id.accountUsername);
                TextView uuidView = accountItemView.findViewById(R.id.accountUuid);
                TextView accountTypeLabel = accountItemView.findViewById(R.id.accountTypeLabel);
                
                usernameView.setText("Username: " + username);
                uuidView.setText("UUID: " + uuid);
                
                // 获取账户类型，如果没有则默认为Offline account
                String accountType = "offline";
                if (account.has("accountType")) {
                    accountType = account.getString("accountType");
                }
                
                // 根据账户类型设置标签
                if ("premium".equals(accountType)) {
                    accountTypeLabel.setText("Premium account");
                    accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_success));
                } else {
                    accountTypeLabel.setText("Offline account");
                    accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_info));
                }
                
                // 设置标签点击事件
                final int accountIndex = i;
                final String currentAccountType = accountType; // 创建final副本
                accountTypeLabel.setOnClickListener(v -> {
                    try {
                        // 切换账户类型
                        String newAccountType = "offline".equals(currentAccountType) ? "premium" : "offline";
                        
                        // 更新JSON中的账户类型
                        accountsArray.getJSONObject(accountIndex).put("accountType", newAccountType);
                        
                        // 保存更新后的JSON到文件
                        confJson.put("accounts", accountsArray);
                        writeStringToFile(confFile, confJson.toString(2));
                        
                        // 更新标签显示
                        if ("premium".equals(newAccountType)) {
                            accountTypeLabel.setText("Premium account");
                            accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_success));
                        } else {
                            accountTypeLabel.setText("Offline account");
                            accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_info));
                        }
                        
                        Toast.makeText(MainActivity.this, "Account type updated", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "更新账户类型失败", e);
                        Toast.makeText(MainActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                
                listLayout.addView(accountItemView);
                
                // 添加分隔线（除了最后一个）
                if (i < accountsArray.length() - 1) {
                    View divider = new View(this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    divider.setBackgroundColor(getResources().getColor(R.color.outline_variant));
                    listLayout.addView(divider);
                }
            }
            
            // 创建ScrollView包装列表
            ScrollView scrollView = new ScrollView(this);
            scrollView.addView(listLayout);
            
            builder.setView(scrollView);
            builder.setPositiveButton("OK", null);
            builder.show();
                    
        } catch (Exception e) {
            Log.e(TAG, "Failed to read account list", e);
            Toast.makeText(this, "Failed to read account list: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCurrentAccountInfo() {
        String username = usernameInput.getText().toString().trim();
        String uuid = extractUUIDFromDisplay();
        
        if (!username.isEmpty() && !uuid.isEmpty()) {
            Toast.makeText(this, "Current account：\nUsername: " + username + "\nUUID: " + uuid, Toast.LENGTH_LONG).show();
        }
    }

    // 【新增 v1.4】权限请求：Android 11+ 使用 MANAGE_EXTERNAL_STORAGE 跳转系统设置
    // Android 6-10 使用常规 WRITE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上版本：需要 MANAGE_EXTERNAL_STORAGE 特殊权限
            if (!Environment.isExternalStorageManager()) {
                // 打开系统设置页面让用户授予权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 版本：使用常规权限请求
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
            }
        }
        // Android 6 以下：无需权限
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Storage permission missing; shared storage cannot be used", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Required permission missing; some features may not work", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void writeStringToFile(File file, String content) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "写入文件失败", e);
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 【新增 v1.4】Change skin对话框：从 assets/supportedVersions.json 读取版本列表
    // 以卡片式布局展示，每行左侧版本号 + 右侧下载图标（仅UI展示，不做功能）
    private void showSkinChangeDialog() {
        try {
            InputStream inputStream = getAssets().open("supportedVersions.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            org.json.JSONObject json = new org.json.JSONObject(sb.toString());
            org.json.JSONArray versions = json.getJSONArray("supportedVersions");

            // 创建卡片式列表
            LinearLayout listLayout = new LinearLayout(this);
            listLayout.setOrientation(LinearLayout.VERTICAL);
            listLayout.setPadding(0, 8, 0, 8);

            for (int i = 0; i < versions.length(); i++) {
                final String version = versions.getString(i);

                // 每个版本项作为一个卡片
                android.widget.LinearLayout itemCard = new android.widget.LinearLayout(this);
                itemCard.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                itemCard.setPadding(16, 14, 16, 14);

                // 设置卡片背景
                android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
                cardBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                cardBg.setCornerRadius(12);
                cardBg.setColor(getResources().getColor(R.color.surface_container));
                cardBg.setStroke(1, getResources().getColor(R.color.outline_variant));
                itemCard.setBackground(cardBg);

                android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(12, 4, 12, 4);
                itemCard.setLayoutParams(cardParams);

                // 版本名称（左侧）
                TextView versionText = new TextView(this);
                versionText.setText(version);
                versionText.setTextSize(16);
                versionText.setTextColor(getResources().getColor(R.color.on_surface));
                versionText.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                versionText.setGravity(android.view.Gravity.CENTER_VERTICAL);

                // 下载图标（右侧）- 使用Unicode字符 ⬇
                TextView downloadIcon = new TextView(this);
                downloadIcon.setText("⬇");
                downloadIcon.setTextSize(20);
                downloadIcon.setTextColor(getResources().getColor(R.color.primary));
                downloadIcon.setPadding(8, 0, 0, 0);
                downloadIcon.setGravity(android.view.Gravity.CENTER_VERTICAL);

                itemCard.addView(versionText);
                itemCard.addView(downloadIcon);

                listLayout.addView(itemCard);
            }

            // 包装到ScrollView
            ScrollView scrollView = new ScrollView(this);
            scrollView.addView(listLayout);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Select skin version");
            builder.setView(scrollView);
            builder.setPositiveButton("Close", null);
            builder.show();
        } catch (Exception e) {
            Log.e("QcofA", "Failed to read version list", e);
            Toast.makeText(this, "Failed to read version list", Toast.LENGTH_SHORT).show();
        }
    }

    // 【新增 v1.4】Save version list：将 assets/supportedVersions.json 复制到 QCOFA.COM 目录
    // 用于在没有网络时手动提供版本列表文件
    private void saveVersionListToStorage() {
        try {
            File storageDir = getStorageDir();
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            File destFile = new File(storageDir, "supportedVersions.json");
            InputStream inputStream = getAssets().open("supportedVersions.json");
            FileOutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Toast.makeText(this, "Version list saved to: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("QcofA", "Save version list失败", e);
            Toast.makeText(this, "Save version list失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}